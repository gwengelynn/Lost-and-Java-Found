package finalproj.mavenproject1;

import java.util.Optional;

/**
 * Mirrors important in-app notifications to the user's email when configured.
 * Default mode logs a queue entry (safe for demos without SMTP).
 * Set mail.enabled=true and SMTP fields in db.properties to enable real sending later.
 */
final class EmailNotifier {

    private EmailNotifier() {}

    static void notifyUser(int userId, String subject, String body) {
        Optional<User> account = User.findById(userId);
        if (account.isEmpty()) return;
        String email = account.get().getEmail();
        if (email == null || email.isBlank() || !email.contains("@")) {
            System.out.println("[EmailNotifier] No email on file for user #" + userId + " — skipped.");
            return;
        }
        send(email, subject == null ? "Lost and Java-Found" : subject, body);
    }

    static void send(String toEmail, String subject, String body) {
        // Intentionally no hard SMTP dependency — keeps the project building offline.
        // When mail.enabled=true, this still records a clear queue line for defense demos.
        String mode = AppConfig.mailEnabled() ? "ENABLED (configure SMTP client to deliver)" : "LOG-ONLY";
        System.out.println("[EmailNotifier/" + mode + "] → " + toEmail
                + " | " + subject + " | " + shortBody(body));
        if (AppConfig.mailEnabled()
                && AppConfig.mailSmtpHost() != null
                && !AppConfig.mailSmtpHost().isBlank()) {
            System.out.println("  SMTP host configured: " + AppConfig.mailSmtpHost()
                    + ":" + AppConfig.mailSmtpPort()
                    + " (hook a mail library here if campus SMTP is available)");
        }
    }

    private static String shortBody(String body) {
        if (body == null) return "";
        String oneLine = body.replace('\n', ' ').trim();
        return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 117) + "...";
    }
}
