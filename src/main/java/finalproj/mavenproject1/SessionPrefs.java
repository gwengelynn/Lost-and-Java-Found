package finalproj.mavenproject1;

import java.util.prefs.Preferences;

/** Remember-me: stores login key + role only (never the password). */
final class SessionPrefs {

    private static final Preferences PREFS = Preferences.userRoot().node("lost-and-java-found");

    private SessionPrefs() {}

    static void save(String loginKey, String role) {
        PREFS.put("remember", "true");
        PREFS.put("loginKey", loginKey == null ? "" : loginKey.trim());
        PREFS.put("role", role == null ? "STUDENT" : role.trim().toUpperCase());
    }

    static void clear() {
        PREFS.remove("remember");
        PREFS.remove("loginKey");
        PREFS.remove("role");
    }

    static boolean isRemembered() {
        return PREFS.getBoolean("remember", false);
    }

    static String loginKey() {
        return PREFS.get("loginKey", "");
    }

    static String role() {
        return PREFS.get("role", "STUDENT");
    }
}
