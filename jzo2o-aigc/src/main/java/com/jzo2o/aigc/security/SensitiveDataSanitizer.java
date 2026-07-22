package com.jzo2o.aigc.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveDataSanitizer {

    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[\\dXx](?!\\d)");
    private static final Pattern BANK_CARD = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    public String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String masked = ID_CARD.matcher(value).replaceAll("[ID_CARD]");
        masked = BANK_CARD.matcher(masked).replaceAll("[BANK_CARD]");
        return PHONE.matcher(masked).replaceAll("[PHONE]");
    }
}
