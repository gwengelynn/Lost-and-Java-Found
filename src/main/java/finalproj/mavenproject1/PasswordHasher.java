package finalproj.mavenproject1;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt password hashing — one-way storage.
 * Users still type their normal password; the database only stores a hash.
 * Legacy plaintext rows are accepted once, then upgraded on successful login.
 */
final class PasswordHasher {

    /** Minimum length for signup / change / forgot-password / admin reset. */
    static final int MIN_LENGTH = 8;

    /** BCrypt cost factor (10 is standard; higher = slower / safer). */
    private static final int BCRYPT_ROUNDS = 10;

    private PasswordHasher() {}

    /**
     * Strong password rule: at least {@link #MIN_LENGTH} characters,
     * with at least one letter and one number.
     */
    static boolean meetsPolicy(String password) {
        if (password == null || password.length() < MIN_LENGTH) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) return true;
        }
        return false;
    }

    /** @deprecated use {@link #meetsPolicy(String)} */
    static boolean meetsMinimumLength(String password) {
        return meetsPolicy(password);
    }

    static String policyMessage() {
        return "Password must be at least " + MIN_LENGTH
                + " characters and include at least one letter and one number."
                + " Example: uecaloocan1";
    }

    static String minimumLengthMessage() {
        return policyMessage();
    }

    /**
     * Same password + confirm rules as Register:
     * both filled → must match → must meet policy.
     * @return {@code null} if OK, otherwise a user-facing error message
     */
    static String validateNewPasswordPair(String password, String confirm) {
        String plain = password == null ? "" : password;
        String conf = confirm == null ? "" : confirm;
        if (plain.isEmpty() || conf.isEmpty()) {
            return "Please fill in password and confirm password.";
        }
        if (!plain.equals(conf)) {
            return "Passwords do not match.";
        }
        if (!meetsPolicy(plain)) {
            return policyMessage();
        }
        return null;
    }

    /** Hash a plaintext password for storage. Never store the return value of this as display text. */
    static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password required");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    static boolean looksHashed(String value) {
        return value != null && (value.startsWith("$2a$")
                || value.startsWith("$2b$")
                || value.startsWith("$2y$"));
    }

    /**
     * Verify typed password against stored value.
     * Accepts BCrypt hashes, or legacy plaintext (migration only).
     */
    static boolean matches(String plainPassword, String stored) {
        if (plainPassword == null || stored == null) return false;
        if (looksHashed(stored)) {
            if (stored.length() < 60) return false; // truncated hash cannot verify
            try {
                return BCrypt.checkpw(plainPassword, stored);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        // Legacy plaintext row (pre-hashing).
        return stored.equals(plainPassword);
    }
}
