package finalproj.mavenproject1;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * University-scoped account validation — UE Caloocan emails only.
 */
final class AccountRules {

    /** Official campus email domain (signup / profile / admin updates). */
    static final String UE_EMAIL_DOMAIN = "@ue.edu.ph";

    private static final Pattern UE_EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@ue\\.edu\\.ph$",
            Pattern.CASE_INSENSITIVE
    );

    /** Digits only, 11 chars starting with 09 (common PH mobile). */
    private static final Pattern PH_MOBILE = Pattern.compile("^09\\d{9}$");

    private AccountRules() {}

    static boolean isUeEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return UE_EMAIL.matcher(email.trim()).matches();
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ENGLISH);
    }

    static String ueEmailMessage() {
        return "Use your University of the East email ending in "
                + UE_EMAIL_DOMAIN + " (example: juan.delacruz" + UE_EMAIL_DOMAIN + ").";
    }

    /**
     * Optional contact check — empty is allowed (some forms leave it blank);
     * if provided, must be 11-digit PH mobile (09xxxxxxxxx) or the same with dashes/spaces stripped.
     */
    static boolean isValidContactOptional(String contact) {
        if (contact == null || contact.isBlank()) return true;
        String digits = contact.replaceAll("\\D", "");
        return PH_MOBILE.matcher(digits).matches();
    }

    static String contactMessage() {
        return "Contact number must be an 11-digit PH mobile starting with 09 "
                + "(example: 09171234567).";
    }
}
