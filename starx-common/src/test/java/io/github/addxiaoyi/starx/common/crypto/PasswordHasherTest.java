package io.github.addxiaoyi.starx.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

  @Test
  void hashProducesDifferentString() {
    String password = "plain-password";
    String hashed = PasswordHasher.hash(password);

    assertThat(hashed).isNotBlank().isNotEqualTo(password);
    assertThat(hashed).startsWith("$2a$");
  }

  @Test
  void verifyCorrectPasswordReturnsTrue() {
    String password = "my-secret";
    String hashed = PasswordHasher.hash(password);

    assertThat(PasswordHasher.verify(password, hashed)).isTrue();
  }

  @Test
  void verifyWrongPasswordReturnsFalse() {
    String hashed = PasswordHasher.hash("correct");

    assertThat(PasswordHasher.verify("wrong", hashed)).isFalse();
  }
}
