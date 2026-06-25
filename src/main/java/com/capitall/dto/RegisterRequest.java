package com.capitall.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        String username,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "Numer telefonu jest wymagany")
        @Pattern(
                regexp = "^\\+?[0-9 ()\\-]{7,20}$",
                message = "Numer telefonu musi mieć 7-20 cyfr (dozwolone +, spacje, myślniki, nawiasy)"
        )
        String phoneNumber,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Password must contain lowercase, uppercase, digit and special character"
        )
        String password
) {}
