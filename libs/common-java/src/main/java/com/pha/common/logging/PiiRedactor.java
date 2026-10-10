package com.pha.common.logging;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks personal data (names, DOB, MRN, national ID, contact details) in free text.
 */
public final class PiiRedactor {

    public static final String MASK = "[REDACTED]";

    private static final Set<String> SENSITIVE_KEY_SUFFIXES = Set.of(
            "firstname", "lastname", "givenname", "familyname", "fullname", "surname",
            "patientname", "staffname", "dob", "dateofbirth", "birthdate",
            "mrn", "medicalrecordnumber", "nationalid", "email", "phone",
            "phonenumber", "mobile", "address", "contactnumber");

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(\"?)([A-Za-z_][A-Za-z0-9_.-]*)\\1(\\s*[:=]\\s*)(\"[^\"]*\"|[^\\s,;&}\\]]+)");

    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    // PNG numbers: any +675 number, or an 8-digit mobile starting with 7.
    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\d+])(?:\\+675[\\s-]?\\d{3,4}[\\s-]?\\d{4}|7\\d{3}[\\s-]?\\d{4})(?!\\d)");

    private PiiRedactor() {
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.equals("name")) {
            return true;
        }
        return SENSITIVE_KEY_SUFFIXES.stream().anyMatch(normalized::endsWith);
    }

    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = redactSensitiveKeyValues(text);
        result = EMAIL.matcher(result).replaceAll(MASK);
        return PHONE.matcher(result).replaceAll(MASK);
    }

    private static String redactSensitiveKeyValues(String text) {
        Matcher matcher = KEY_VALUE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String replacement = isSensitiveKey(matcher.group(2))
                    ? matcher.group(1) + matcher.group(2) + matcher.group(1) + matcher.group(3) + quoteLike(matcher.group(4))
                    : matcher.group();
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String quoteLike(String originalValue) {
        return originalValue.startsWith("\"") ? "\"" + MASK + "\"" : MASK;
    }
}
