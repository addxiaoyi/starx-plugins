package io.github.addxiaoyi.starx.velocity.http.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.addxiaoyi.starx.common.auth.AuthResult;
import io.github.addxiaoyi.starx.common.auth.AuthService;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DeleteUserHandlerTest {

  private static final int PORT = 18794;

  @Mock private AuthService authService;

  private AutoCloseable mocks;
  private Javalin app;
  private HttpClient client;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (app != null) {
      app.stop();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  void shouldDeleteUser() throws Exception {
    when(authService.deleteUser("dave")).thenReturn(AuthResult.success("用户已删除"));

    app = Javalin.create(config -> config.showJavalinBanner = false);
    new DeleteUserHandler(authService).register(app);
    app.start("127.0.0.1", PORT);

    HttpResponse<String> response = post("/v1/admin/delete-user", "{\"username\":\"dave\"}");

    assertThat(response.statusCode()).isEqualTo(200);
    verify(authService).deleteUser("dave");
  }

  private HttpResponse<String> post(String path, String body)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
