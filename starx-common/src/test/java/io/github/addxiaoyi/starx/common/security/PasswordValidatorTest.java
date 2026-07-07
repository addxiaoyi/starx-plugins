package io.github.addxiaoyi.starx.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

  @Test
  void rejectsNull() {
    assertThat(PasswordValidator.validate(null)).isNotNull();
  }

  @Test
  void rejectsEmpty() {
    assertThat(PasswordValidator.validate("")).isNotNull();
  }

  @Test
  void rejectsTooShort() {
    assertThat(PasswordValidator.validate("ab1")).isNotNull();
  }

  @Test
  void rejectsTooLong() {
    String longPw = "a1" + "x".repeat(128);
    assertThat(PasswordValidator.validate(longPw)).isNotNull();
  }

  @Test
  void rejectsWithoutLetter() {
    assertThat(PasswordValidator.validate("123456")).isNotNull();
  }

  @Test
  void rejectsWithoutDigit() {
    assertThat(PasswordValidator.validate("abcdef")).isNotNull();
  }

  @Test
  void acceptsValidPassword() {
    assertThat(PasswordValidator.validate("Str0ng!Pass")).isNull();
  }

  @Test
  void acceptsMinLengthWithLetterAndDigit() {
    assertThat(PasswordValidator.validate("a1bbbb")).isNull();
  }

  @Test
  void acceptsMaxLength() {
    String pw = "a1" + "x".repeat(126);
    assertThat(PasswordValidator.validate(pw)).isNull();
  }
}
