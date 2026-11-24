package finalproj.mavenproject1;

import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/** Applies hover / press feedback and optional icons without changing layout structure. */
final class UiButtons {

    private UiButtons() {}

    enum Kind { PRIMARY, GHOST, LINK }

    static Button style(Button button, Kind kind) {
        if (button == null) return null;
        applyIdle(button, kind);
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) applyHover(button, kind);
        });
        button.setOnMouseExited(e -> {
            if (!button.isDisabled()) applyIdle(button, kind);
        });
        button.setOnMousePressed(e -> {
            if (!button.isDisabled()) applyPressed(button, kind);
        });
        button.setOnMouseReleased(e -> {
            if (!button.isDisabled()) {
                if (button.isHover()) applyHover(button, kind);
                else applyIdle(button, kind);
            }
        });
        button.disableProperty().addListener((o, a, disabled) -> {
            if (disabled) button.setStyle(UiTheme.disabledButton());
            else applyIdle(button, kind);
        });
        if (button.isDisabled()) {
            button.setStyle(UiTheme.disabledButton());
        }
        return button;
    }

    static Button primary(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return style(b, Kind.PRIMARY);
    }

    static Button ghost(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return style(b, Kind.GHOST);
    }

    static Button link(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return style(b, Kind.LINK);
    }

    /** Primary button with a small white glyph icon. */
    static Button primaryIcon(String text, String iconKey, Runnable action) {
        Button b = primary(text, action);
        Node icon = actionIcon(iconKey, true);
        if (icon != null) {
            b.setGraphic(icon);
            b.setContentDisplay(ContentDisplay.LEFT);
            b.setGraphicTextGap(8);
        }
        return b;
    }

    static Button ghostIcon(String text, String iconKey, Runnable action) {
        Button b = ghost(text, action);
        Node icon = actionIcon(iconKey, false);
        if (icon != null) {
            b.setGraphic(icon);
            b.setContentDisplay(ContentDisplay.LEFT);
            b.setGraphicTextGap(8);
        }
        return b;
    }

    static void pulseSelect(Node node) {
        if (node == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
        st.setFromX(0.97);
        st.setFromY(0.97);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }

    static Node actionIcon(String key, boolean onRed) {
        String content = switch (key == null ? "" : key.toUpperCase()) {
            case "PLUS", "ADD" -> "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
            case "SEARCH" -> "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";
            case "REFRESH" -> "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
            case "PRINT" -> "M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z";
            case "EXPORT" -> "M19 12v7H5v-7H3v7c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-7h-2zm-6 .67l2.59-2.58L17 11.5l-5 5-5-5 1.41-1.41L11 12.67V3h2z";
            case "CHECK" -> "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
            case "LOGOUT" -> "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z";
            default -> null;
        };
        if (content == null) return null;
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setFill(onRed ? Color.WHITE : Color.web(UiTheme.UE_RED));
        path.setScaleX(0.75);
        path.setScaleY(0.75);
        return path;
    }

    private static void applyIdle(Button button, Kind kind) {
        button.setStyle(switch (kind) {
            case PRIMARY -> UiTheme.primaryButton();
            case GHOST -> UiTheme.ghostButton();
            case LINK -> UiTheme.linkButton();
        });
    }

    private static void applyHover(Button button, Kind kind) {
        button.setStyle(switch (kind) {
            case PRIMARY -> UiTheme.primaryButtonHover();
            case GHOST -> UiTheme.ghostButtonHover();
            case LINK -> UiTheme.linkButtonHover();
        });
    }

    private static void applyPressed(Button button, Kind kind) {
        button.setStyle(switch (kind) {
            case PRIMARY -> UiTheme.primaryButtonPressed();
            case GHOST -> UiTheme.ghostButtonPressed();
            case LINK -> UiTheme.linkButtonHover();
        });
    }

    static Label emptyTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");
        return label;
    }

    static HBox iconText(Node icon, String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");
        HBox row = new HBox(8, icon, label);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }
}
