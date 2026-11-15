
package finalproj.mavenproject1;

/**
 * Shared visual tokens — lighter glass so UE campus stays visible,
 * with dark text for readability.
 */
public final class UiTheme {

    public static final String UE_RED = "#E41135";
    public static final String UE_RED_HOVER = "#C70E2E";
    public static final String UE_RED_SOFT = "rgba(252, 232, 236, 0.72)";
    public static final String UE_RED_SOFT_ALPHA = "rgba(228, 17, 53, 0.12)";

    public static final String BG_FALLBACK = "#F5F5F7";
    /** Sidebar glass — campus shows through. */
    public static final String SIDEBAR_BG = "rgba(255, 250, 250, 0.48)";
    /** Main content shell — airy so campus remains visible. */
    public static final String PANEL_BG = "rgba(255, 250, 250, 0.42)";
    /** Inner cards — only slightly stronger than the shell. */
    public static final String CARD_BG = "rgba(255, 255, 255, 0.58)";
    public static final String FIELD_BG = "rgba(255, 255, 255, 0.78)";
    public static final String ROW_HOVER = "rgba(228, 17, 53, 0.10)";
    public static final String BORDER = "rgba(228, 17, 53, 0.30)";
    public static final String BORDER_STRONG = "rgba(228, 17, 53, 0.50)";

    public static final String TEXT = "#141416";
    public static final String TEXT_MUTED = "#2F2F35";
    public static final String TEXT_DIM = "#3F3F46";
    public static final String TEXT_ON_RED = "#FFFFFF";

    public static final String STATUS_PENDING_BG = "rgba(255, 248, 220, 0.85)";
    public static final String STATUS_PENDING_FG = "#7A5A00";
    public static final String STATUS_APPROVED_BG = "rgba(220, 252, 231, 0.85)";
    public static final String STATUS_APPROVED_FG = "#166534";
    public static final String STATUS_REJECTED_BG = "rgba(254, 226, 226, 0.88)";
    public static final String STATUS_REJECTED_FG = "#B40E2C";
    public static final String STATUS_HOLD_BG = "rgba(240, 240, 245, 0.88)";
    public static final String STATUS_HOLD_FG = "#3F3F46";
    public static final String STATUS_RESOLVED_BG = "rgba(219, 234, 254, 0.88)";
    public static final String STATUS_RESOLVED_FG = "#1D4ED8";

    private UiTheme() {}

    public static String cardStyle() {
        return "-fx-background-color: " + CARD_BG + ";"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 14;"
                + "-fx-border-width: 1;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 3);";
    }

    public static String primaryButton() {
        return "-fx-background-color: " + UE_RED + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 18 10 18;"
                + "-fx-cursor: hand;";
    }

    public static String ghostButton() {
        return "-fx-background-color: " + FIELD_BG + ";"
                + "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 8;"
                + "-fx-padding: 6 12 6 12;"
                + "-fx-cursor: hand;";
    }

    public static String linkButton() {
        return "-fx-background-color: transparent;"
                + "-fx-text-fill: " + UE_RED + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 4 8 4 8;"
                + "-fx-cursor: hand;";
    }
}