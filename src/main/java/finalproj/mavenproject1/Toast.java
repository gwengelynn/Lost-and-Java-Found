package finalproj.mavenproject1;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Compact bottom snackbar — width/height hug the message text (no empty black slab).
 * Unmanaged positioning so shell StackPanes cannot stretch it.
 */
final class Toast {

    enum Kind { SUCCESS, ERROR, INFO }

    private static final String STYLE_CLASS = "ue-toast";
    private static final double HOLD_SUCCESS = 3.4;
    private static final double HOLD_ERROR = 4.8;
    private static final double HOLD_INFO = 3.6;
    /** Cap for long messages only — short ones stay tight. */
    private static final double WRAP_AT = 340;
    private static final double SIDE_PAD = 24;
    private static final double BOTTOM_PAD = 28;

    private Toast() {}

    static void show(Window owner, String message) {
        success(owner, message);
    }

    static void success(Window owner, String message) {
        show(owner, Kind.SUCCESS, message);
    }

    static void error(Window owner, String message) {
        show(owner, Kind.ERROR, message);
    }

    static void info(Window owner, String message) {
        show(owner, Kind.INFO, message);
    }

    static void notify(Window owner, String title, String message) {
        String t = title == null ? "" : title.trim();
        String m = message == null ? "" : message.trim();
        String display = dedupeTitleMessage(t, m);
        // Classify from title + body so "Passwords do not match" is Error, not Success.
        if (looksLikeError(t) || looksLikeError(m) || looksLikeError(display)) {
            error(owner, display);
        } else if (looksLikeInfo(t) || looksLikeInfo(m)) {
            info(owner, display);
        } else {
            success(owner, display);
        }
    }

    /** Avoid "Passwords do not match — Passwords do not match." style duplicates. */
    private static String dedupeTitleMessage(String title, String message) {
        if (title.isEmpty()) return message;
        if (message.isEmpty()) return title;
        String tn = normalizePhrase(title);
        String mn = normalizePhrase(message);
        if (tn.equals(mn) || mn.startsWith(tn) || tn.startsWith(mn)) {
            return message.length() >= title.length() ? message : title;
        }
        return title + " — " + message;
    }

    private static String normalizePhrase(String s) {
        return s.toLowerCase()
                .replaceAll("[\\s.]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean looksLikeError(String text) {
        if (text == null || text.isBlank()) return false;
        String k = text.toLowerCase();
        return k.contains("fail") || k.contains("invalid") || k.contains("incorrect")
                || k.contains("error") || k.contains("required") || k.contains("cannot")
                || k.contains("weak") || k.contains("denied") || k.contains("locked")
                || k.contains("too short") || k.contains("mismatch") || k.contains("taken")
                || k.contains("no matched") || k.contains("not allowed") || k.contains("missing")
                || k.contains("do not match") || k.contains("don't match") || k.contains("does not match")
                || k.contains("not match") || k.contains("please fill");
    }

    private static boolean looksLikeInfo(String title) {
        if (title == null) return false;
        String k = title.toLowerCase();
        return k.contains("signed out") || k.contains("notice") || k.contains("tip");
    }

    private static void show(Window owner, Kind kind, String message) {
        if (message == null || message.isBlank()) return;
        Platform.runLater(() -> showNow(owner, kind, message.trim()));
    }

    private static void showNow(Window owner, Kind kind, String message) {
        Window resolved = owner;
        if (resolved == null || !resolved.isShowing()) {
            resolved = Window.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);
        }
        if (resolved == null) return;
        final Window window = resolved;

        StackPane host = UeChrome.toastHost(window);
        if (host == null) return;

        clearExisting(host);

        String accent;
        String titleText;
        String iconPath;
        double hold;
        switch (kind) {
            case ERROR -> {
                accent = "#DC2626";
                titleText = "Error";
                iconPath = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z";
                hold = HOLD_ERROR;
            }
            case INFO -> {
                accent = "#3B82F6";
                titleText = "Notice";
                iconPath = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
                hold = HOLD_INFO;
            }
            default -> {
                accent = UiTheme.UE_RED;
                titleText = "Success";
                iconPath = "M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z";
                hold = HOLD_SUCCESS;
            }
        }

        Circle disc = new Circle(11, Color.web(accent));
        SVGPath glyph = new SVGPath();
        glyph.setContent(iconPath);
        glyph.setFill(Color.WHITE);
        glyph.setScaleX(kind == Kind.SUCCESS ? 0.50 : 0.44);
        glyph.setScaleY(kind == Kind.SUCCESS ? 0.50 : 0.44);
        StackPane icon = new StackPane(disc, glyph);
        icon.setMinSize(26, 26);
        icon.setPrefSize(26, 26);
        icon.setMaxSize(26, 26);

        Label title = new Label(titleText);
        title.setMinWidth(Region.USE_PREF_SIZE);
        title.setMaxWidth(Region.USE_PREF_SIZE);
        title.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.72);"
                + "-fx-font-size: 10.5px;"
                + "-fx-font-weight: bold;"
        );

        Label body = new Label(message);
        body.setMinWidth(Region.USE_PREF_SIZE);
        // wrapText(true) + large maxWidth makes Label prefer the max width — only wrap long copy.
        boolean wrap = message.length() > 54 || message.contains("\n");
        body.setWrapText(wrap);
        if (wrap) {
            double hostW = host.getWidth() > 1 ? host.getWidth()
                    : (window.getWidth() > 1 ? window.getWidth() : 900);
            body.setMaxWidth(Math.min(WRAP_AT, Math.max(220, hostW - SIDE_PAD * 2 - 90)));
        } else {
            body.setMaxWidth(Region.USE_PREF_SIZE);
        }
        body.setStyle(
                "-fx-text-fill: white;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-line-spacing: 1px;"
        );

        VBox textCol = new VBox(2, title, body);
        textCol.setAlignment(Pos.CENTER_LEFT);
        textCol.setFillWidth(false);
        textCol.setMinWidth(Region.USE_PREF_SIZE);
        textCol.setMaxWidth(Region.USE_PREF_SIZE);

        Region accentBar = new Region();
        accentBar.setMinSize(3, 24);
        accentBar.setPrefSize(3, 24);
        accentBar.setMaxSize(3, Region.USE_PREF_SIZE);
        accentBar.setStyle(
                "-fx-background-color: " + accent + ";"
                + "-fx-background-radius: 2;"
        );

        HBox box = new HBox(10, accentBar, icon, textCol);
        box.getStyleClass().add(STYLE_CLASS);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 16, 12, 12));
        box.setSpacing(10);
        box.setFillHeight(false);
        box.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setMouseTransparent(true);
        box.setPickOnBounds(false);

        String border = kind == Kind.ERROR
                ? "rgba(220, 38, 38, 0.75)"
                : kind == Kind.INFO
                ? "rgba(59, 130, 246, 0.65)"
                : "rgba(228, 17, 53, 0.65)";
        box.setStyle(
                "-fx-background-color: rgba(24, 16, 18, 0.97);"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-radius: 12;"
                + "-fx-border-width: 1;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.32), 14, 0.18, 0, 4);"
        );
        box.setOpacity(0);
        box.setTranslateY(8);

        box.setManaged(false);
        host.getChildren().add(box);

        Runnable place = () -> {
            box.applyCss();
            box.autosize();
            double measuredW = box.getWidth();
            double measuredH = box.getHeight();
            final double w = (measuredW < 8) ? box.prefWidth(-1) : measuredW;
            final double h = (measuredH < 8) ? box.prefHeight(-1) : measuredH;
            double hw = host.getWidth() > 1 ? host.getWidth()
                    : (window.getWidth() > 1 ? window.getWidth() : 900);
            double hh = host.getHeight() > 1 ? host.getHeight()
                    : (window.getHeight() > 1 ? window.getHeight() : 640);
            // Keep fully inside the window, bottom-centered to the text block.
            double rawX = Math.max(SIDE_PAD, (hw - w) / 2.0);
            double rawY = Math.max(SIDE_PAD, hh - h - BOTTOM_PAD);
            final double x = (rawX + w > hw - SIDE_PAD)
                    ? Math.max(SIDE_PAD, hw - w - SIDE_PAD) : rawX;
            final double y = (rawY + h > hh - 8)
                    ? Math.max(8, hh - h - 8) : rawY;
            box.relocate(x, y);
        };

        InvalidationListener relayout = obs -> place.run();
        host.widthProperty().addListener(relayout);
        host.heightProperty().addListener(relayout);
        Platform.runLater(place);
        Platform.runLater(place);

        FadeTransition in = new FadeTransition(Duration.millis(170), box);
        in.setFromValue(0);
        in.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(170), box);
        slide.setFromY(8);
        slide.setToY(0);
        in.play();
        slide.play();

        PauseTransition holdPause = new PauseTransition(Duration.seconds(hold));
        holdPause.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(200), box);
            out.setFromValue(1);
            out.setToValue(0);
            TranslateTransition down = new TranslateTransition(Duration.millis(200), box);
            down.setFromY(0);
            down.setToY(6);
            out.setOnFinished(ev -> {
                host.widthProperty().removeListener(relayout);
                host.heightProperty().removeListener(relayout);
                host.getChildren().remove(box);
            });
            out.play();
            down.play();
        });
        holdPause.play();
    }

    private static void clearExisting(StackPane host) {
        host.getChildren().removeIf(n ->
                n.getStyleClass() != null && n.getStyleClass().contains(STYLE_CLASS));
    }
}
