package io.github.addxiaoyi.starx.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecoveryCodeGeneratorTest {

  @Test
  void generatesEightCodes() {
    List<String> codes = RecoveryCodeGenerator.generate();
    assertThat(codes).hasSize(8);
  }

  @Test
  void eachCodeHasTenCharacters() {
    List<String> codes = RecoveryCodeGenerator.generate();
    assertThat(codes).allSatisfy(code -> assertThat(code).hasSize(10));
  }

  @Test
  void codesOnlyContainValidChars() {
    String valid = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    List<String> codes = RecoveryCodeGenerator.generate();
    assertThat(codes)
        .allSatisfy(code -> assertThat(code.chars().allMatch(c -> valid.indexOf(c) >= 0)).isTrue());
  }

  @Test
  void codesAreUnique() {
    List<String> codes = RecoveryCodeGenerator.generate();
    assertThat(codes).doesNotHaveDuplicates();
  }

  @Test
  void multipleGenerationsProduceDifferentCodes() {
    List<String> batch1 = RecoveryCodeGenerator.generate();
    List<String> batch2 = RecoveryCodeGenerator.generate();
    assertThat(batch1).isNotEqualTo(batch2);
  }
}
