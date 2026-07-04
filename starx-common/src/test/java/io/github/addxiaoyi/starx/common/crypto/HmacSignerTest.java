package io.github.addxiaoyi.starx.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class HmacSignerTest {

  @Test
  void signProducesExpectedHexSignature() throws Exception {
    String secret = "shhh";
    String timestamp = "1700000000";
    String rawBody = "{\"hello\":\"world\"}";
    String expected = hmacSha256Hex(secret, timestamp + "\n" + rawBody);

    String signature = HmacSigner.sign(secret, timestamp, rawBody);

    assertThat(signature).isEqualToIgnoringCase(expected);
  }

  @Test
  void verifyAcceptsValidSignature() {
    String secret = "key";
    String timestamp = "1700000001";
    String rawBody = "{\"a\":1}";
    String signature = HmacSigner.sign(secret, timestamp, rawBody);

    assertThat(HmacSigner.verify(secret, timestamp, rawBody, signature)).isTrue();
  }

  @Test
  void verifyRejectsTamperedBody() {
    String secret = "key";
    String timestamp = "1700000001";
    String signature = HmacSigner.sign(secret, timestamp, "{\"a\":1}");

    assertThat(HmacSigner.verify(secret, timestamp, "{\"a\":2}", signature)).isFalse();
  }

  private String hmacSha256Hex(String secret, String data) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
