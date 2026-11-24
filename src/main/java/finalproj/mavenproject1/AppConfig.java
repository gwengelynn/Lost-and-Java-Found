package finalproj.mavenproject1;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Loads db.properties (classpath) with optional environment overrides. */
final class AppConfig {

    private static final Properties PROPS = load();

    private AppConfig() {}

    static String dbUrl() {
        return firstNonBlank(System.getenv("DB_URL"), PROPS.getProperty("db.url"),
                "jdbc:mysql://localhost:3306/lost_and_found_db");
    }

    static String dbUser() {
        return firstNonBlank(System.getenv("DB_USER"), PROPS.getProperty("db.user"), "root");
    }

    static String dbPassword() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null) return env;
        return PROPS.getProperty("db.password", "");
    }

    static boolean mailEnabled() {
        return Boolean.parseBoolean(PROPS.getProperty("mail.enabled", "false"));
    }

    static String mailSmtpHost() { return PROPS.getProperty("mail.smtp.host", ""); }
    static String mailSmtpPort() { return PROPS.getProperty("mail.smtp.port", "587"); }
    static String mailSmtpUser() { return PROPS.getProperty("mail.smtp.user", ""); }
    static String mailSmtpPassword() { return PROPS.getProperty("mail.smtp.password", ""); }
    static String mailFrom() {
        return PROPS.getProperty("mail.from", "lostandfound@ue.edu.ph");
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.out.println("db.properties not found on classpath — using defaults / env.");
            }
        } catch (IOException e) {
            System.out.println("Could not read db.properties: " + e.getMessage());
        }
        return props;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }
}
