package com.jobpulse.common.util;

import java.util.regex.Pattern;

/**
 * Email validation utility using RFC 5322 standard.
 */
public class EmailValidator {
    private static final String EMAIL_PATTERN =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    public static boolean isValid(String email) {
        return email != null && pattern.matcher(email).matches();
    }
}

