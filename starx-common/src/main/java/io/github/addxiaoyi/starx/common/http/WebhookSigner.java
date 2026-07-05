package io.github.addxiaoyi.starx.common.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.addxiaoyi.starx.api.dto.WebhookPayload;
import io.github.addxiaoyi.starx.common.crypto.HmacSigner;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/** 对 {@link WebhookPayload} 进行 VLA HMAC 签名，输出请求头。签名仅使用原始 JSON 正文， X-VLA-Timestamp 仅用于审计用途。 */
public final class WebhookSigner {

  private static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
          .disableHtmlEscaping()
          .create();

  private WebhookSigner() {}

  /**
   * 对 payload 进行签名并返回请求头。
   *
   * @param payload 待签名 payload
   * @param secret 共享密钥
   * @return 包含 X-VLA-Timestamp 与 X-VLA-Signature 的请求头映射
   */
  public static Map<String, String> sign(WebhookPayload payload, String secret) {
    String rawBody = toJson(payload);
    String timestamp = String.valueOf(payload.timestamp().getEpochSecond());
    String signature = HmacSigner.sign(secret, rawBody);
    return Map.of("X-VLA-Timestamp", timestamp, "X-VLA-Signature", signature);
  }

  /**
   * 将 payload 序列化为原始 JSON 字符串。
   *
   * @param payload payload
   * @return JSON 字符串
   */
  public static String toJson(WebhookPayload payload) {
    return GSON.toJson(payload);
  }

  private static final class InstantTypeAdapter extends TypeAdapter<Instant> {

    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
      out.value(value.toString());
    }

    @Override
    public Instant read(JsonReader in) {
      throw new UnsupportedOperationException();
    }
  }
}
