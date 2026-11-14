package com.recircle.validation;

import com.recircle.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class InputValidator {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final List<String> allowedDomains;
    private final boolean allowPublicEmails;

    public InputValidator(
        @Value("${recircle.campus.allowed-domains:srmist.edu.in,campus.edu,edu.in}") String domainsCsv,
        @Value("${recircle.auth.allow-public-emails:true}") boolean allowPublicEmails
    ) {
        this.allowedDomains = Arrays.stream(domainsCsv.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(s -> !s.isEmpty())
            .toList();
        this.allowPublicEmails = allowPublicEmails;
    }

    public boolean isCampusDomain(String email) {
        if (email == null || email.isBlank()) return false;
        String lowerEmail = email.trim().toLowerCase();
        return allowedDomains.stream().anyMatch(d -> lowerEmail.endsWith("@" + d) || lowerEmail.endsWith("." + d));
    }

    public void validateRegistrationInput(
        String fullName,
        String email,
        String password,
        String confirmPassword,
        String registrationNumber,
        String department,
        Integer yearOfStudy,
        String contactNumber
    ) {
        requireNonBlank(fullName, "Full name is required.");
        requireNonBlank(email, "Email is required.");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "Please provide a valid email address.");
        }
        if (!allowPublicEmails) {
            if (!isCampusDomain(email)) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CAMPUS_DOMAIN",
                    "Email must belong to an authorized campus domain (" + String.join(", ", allowedDomains) + ")."
                );
            }
        }
        validatePassword(password, confirmPassword);
        if (yearOfStudy != null && (yearOfStudy < 1 || yearOfStudy > 6)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_YEAR", "Year of study must be between 1 and 6.");
        }
        requireNonBlank(contactNumber, "Contact number is required.");
    }

    public void validatePassword(String password, String confirmPassword) {
        if (password == null || password.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Password must be at least 8 characters long.");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "WEAK_PASSWORD",
                "Password must contain at least one uppercase letter, one lowercase letter, and one digit."
            );
        }
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Password and confirm password do not match.");
        }
    }

    public void requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
        }
    }

    public String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        return input.trim()
            .replace("<script", "&lt;script")
            .replace("</script>", "&lt;/script&gt;");
    }
}
