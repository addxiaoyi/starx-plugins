package io.github.addxiaoyi.starx.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpGeneratorTest {

  private static final String SECRET = "JBSWY3DPEHPK3PXP";

  @Test
  void generateReturnsSixDigitCode() {
    Instant now = Instant.ofEpochSecond(1_000_000L);

    String code = TotpGenerator.generate(SECRET, now);

    assertThat(code).hasSize(6).containsOnlyDigits();
  }

  @Test
  void verifyAcceptsGeneratedCode() {
    Instant now = Instant.ofEpochSecond(1_234_567L);
    String code = TotpGenerator.generate(SECRET, now);

    assertThat(TotpGenerator.verify(SECRET, code, now)).isTrue();
  }

  @Test
  void verifyRejectsWrongCode() {
    Instant now = Instant.ofEpochSecond(1_234_567L);

    assertThat(TotpGenerator.verify(SECRET, "000000", now)).isFalse();
  }

  @Test
  void provisioningUriContainsExpectedParts() {
    String uri = TotpGenerator.provisioningUri("issuer", "user@example.com", SECRET);

    assertThat(uri).startsWith("otpauth://totp/");
    assertThat(uri).contains("issuer:user%40example.com");
    assertThat(uri).contains("secret=" + SECRET);
    assertThat(uri).contains("issuer=issuer");
  }
}
