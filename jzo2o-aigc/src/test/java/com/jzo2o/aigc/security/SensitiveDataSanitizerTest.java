package com.jzo2o.aigc.security;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer();

    @Test
    void shouldMaskPhoneIdAndBankCardInLongestRuleOrder() {
        String input = "电话13800138000，身份证110101199001011234，银行卡6222020202020202";

        assertThat(sanitizer.sanitize(input))
                .isEqualTo("电话[PHONE]，身份证[ID_CARD]，银行卡[BANK_CARD]");
    }

    @Test
    void shouldReturnNullAndRemainASpringInjectableComponent() {
        assertThat(sanitizer.sanitize(null)).isNull();
        assertThat(SensitiveDataSanitizer.class).hasAnnotation(Component.class);
    }

    @Test
    void shouldSupportUpperAndLowerCaseIdSuffixes() {
        assertThat(sanitizer.sanitize("11010119900101123X 11010119900101123x"))
                .isEqualTo("[ID_CARD] [ID_CARD]");
    }

    @Test
    void shouldNotPartiallyMaskNumbersThatCrossRuleBoundaries() {
        assertThat(sanitizer.sanitize(
                "前缀913800138000后缀 62220202020202021234 11010119900101123450"))
                .isEqualTo("前缀913800138000后缀 62220202020202021234 11010119900101123450");
    }
}
