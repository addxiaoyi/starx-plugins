package io.github.addxiaoyi.starx.velocity.module.integrations.napcat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NapCatWebSocketClient {

  private static final Logger log = LoggerFactory.getLogger(NapCatWebSocketClient.class);
  private static final int MAX_RECONNECT_DELAY_MS = 30_000;
  private static final int INITIAL_RECONNECT_DELAY_MS = 1_000;

  private final String wsUrl;
  private final String httpUrl;
  private final MessageHandler messageHandler;
  private final HttpClient httpClient;
  private final ScheduledExecutorService scheduler;
  private final Gson gson;
  private final AtomicBoolean running;
  private WebSocket webSocket;
  private int reconnectAttempts;

  public NapCatWebSocketClient(String wsUrl, String httpUrl, MessageHandler messageHandler) {
    this.wsUrl = Objects.requireNonNull(wsUrl, "wsUrl");
    this.httpUrl = httpUrl != null && !httpUrl.isBlank() ? httpUrl : httpUrlFromWs(wsUrl);
    this.messageHandler = Objects.requireNonNull(messageHandler, "messageHandler");
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "napcat-ws");
              t.setDaemon(true);
              return t;
            });
    this.gson = new Gson();
    this.running = new AtomicBoolean(false);
  }

  private static String httpUrlFromWs(String wsUrl) {
    return wsUrl
        .replace("ws://", "http://")
        .replace("wss://", "https://")
        .replaceAll(":\\d+$", ":3000");
  }

  public void start() {
    if (running.compareAndSet(false, true)) {
      reconnectAttempts = 0;
      doConnect();
    }
  }

  public void stop() {
    running.set(false);
    if (webSocket != null) {
      webSocket.sendClose(1000, "Shutdown");
      webSocket = null;
    }
    scheduler.shutdownNow();
  }

  public void sendPrivateMessage(long userId, String message) {
    sendApiCall("send_private_msg", Map.of("user_id", userId, "message", message));
  }

  public void sendGroupMessage(long groupId, String message) {
    sendApiCall("send_group_msg", Map.of("group_id", groupId, "message", message));
  }

  private void sendApiCall(String action, Map<String, Object> params) {
    String json = gson.toJson(params);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(httpUrl + "/" + action))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    httpClient
        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .exceptionally(
            t -> {
              log.warn("NapCat HTTP API call failed: {} - {}", action, t.getMessage());
              return null;
            });
  }

  private void doConnect() {
    if (!running.get()) return;
    log.info("Connecting to NapCat at {} ...", wsUrl);
    httpClient
        .newWebSocketBuilder()
        .buildAsync(URI.create(wsUrl), new OneBotListener())
        .thenAccept(
            ws -> {
              this.webSocket = ws;
              reconnectAttempts = 0;
              log.info("Connected to NapCat WebSocket: {}", wsUrl);
            })
        .exceptionally(
            t -> {
              log.warn(
                  "Failed to connect to NapCat (attempt {}): {}",
                  reconnectAttempts + 1,
                  t.getMessage());
              scheduleReconnect();
              return null;
            });
  }

  private void scheduleReconnect() {
    if (!running.get()) return;
    long delay =
        Math.min(
            (long) (INITIAL_RECONNECT_DELAY_MS * Math.pow(2, Math.min(reconnectAttempts, 5))),
            MAX_RECONNECT_DELAY_MS);
    reconnectAttempts++;
    log.info("Reconnecting to NapCat in {}ms (attempt {})", delay, reconnectAttempts);
    scheduler.schedule(this::doConnect, delay, TimeUnit.MILLISECONDS);
  }

  private final class OneBotListener implements WebSocket.Listener {

    private final StringBuilder buffer = new StringBuilder();

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
      buffer.append(data);
      if (last) {
        String json = buffer.toString();
        buffer.setLength(0);
        try {
          handleEvent(json);
        } catch (Exception e) {
          log.warn("Failed to handle NapCat event: {}", e.getMessage());
        }
      }
      ws.request(1);
      return null;
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
      log.warn("NapCat WebSocket error: {}", error.getMessage());
      scheduleReconnect();
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
      log.info("NapCat WebSocket closed: {} {}", status, reason);
      scheduleReconnect();
      return null;
    }
  }

  private void handleEvent(String json) {
    JsonObject obj = gson.fromJson(json, JsonObject.class);
    if (obj == null) return;

    String postType = getString(obj, "post_type");
    if (!"message".equals(postType)) return;

    String messageType = getString(obj, "message_type");
    String rawMessage = getString(obj, "raw_message");
    if (rawMessage.isEmpty()) rawMessage = getString(obj, "message");
    if (rawMessage.isEmpty()) return;

    long userId = getLong(obj, "user_id");
    String nickname = "Unknown";
    if (obj.has("sender") && obj.get("sender").isJsonObject()) {
      JsonObject sender = obj.getAsJsonObject("sender");
      nickname = getString(sender, "nickname");
      if (nickname.isEmpty()) nickname = getString(sender, "card");
    }

    if ("private".equals(messageType)) {
      messageHandler.onPrivateMessage(userId, rawMessage, nickname);
    } else if ("group".equals(messageType)) {
      long groupId = getLong(obj, "group_id");
      messageHandler.onGroupMessage(groupId, userId, rawMessage, nickname);
    }
  }

  private static String getString(JsonObject obj, String key) {
    return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
  }

  private static long getLong(JsonObject obj, String key) {
    return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsLong() : 0L;
  }

  public interface MessageHandler {
    void onPrivateMessage(long userId, String message, String nickname);

    void onGroupMessage(long groupId, long userId, String message, String nickname);
  }
}
