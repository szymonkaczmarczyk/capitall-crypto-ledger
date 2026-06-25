package com.capitall.security;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private static final Pattern COMPLEXITY = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$");

    private PasswordPolicy() {}

    public static String validate(String password) {
        if (password == null || password.isBlank()) {
            return "Hasło nie może być puste.";
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return "Hasło musi mieć od " + MIN_LENGTH + " do " + MAX_LENGTH + " znaków.";
        }
        if (!COMPLEXITY.matcher(password).matches()) {
            return "Hasło musi zawierać małą i wielką literę, cyfrę oraz znak specjalny.";
        }
        return null;
    }
}
