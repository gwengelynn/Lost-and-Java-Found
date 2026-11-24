package finalproj.mavenproject1;

import java.io.InputStream;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Shared image helpers so Login, Signup, Dashboard, and dialogs all use the official UE logo.
 */
public final class UeImages {

    private UeImages() {}

    /**
     * Official UE logo for UI marks. Prefer {@code UE LOGO.png}, then the red seal asset.
     * Text "UE" is only a last-resort fallback if no image loads.
     */
    public static Node ueLogo(double size) {
        Image image = loadUeLogo();
        if (image == null) {
            Label fallback = new Label("UE");
            fallback.setStyle(
                    "-fx-text-fill: #E41135;"
                    + "-fx-font-weight: bold;"
                    + "-fx-font-size: " + (size / 2.6) + "px;"
            );
            StackPane badge = new StackPane(fallback);
            badge.setPrefSize(size, size);
            badge.setMinSize(size, size);
            badge.setMaxSize(size, size);
            badge.setStyle(
                    "-fx-background-color: white;"
                    + "-fx-background-radius: " + (size / 2) + ";"
                    + "-fx-border-color: #E41135;"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: " + (size / 2) + ";"
            );
            return badge;
        }

        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(size);
        view.setFitHeight(size);
        // Softer shadow on smaller marks so the crest stays fully readable.
        double blur = size >= 80 ? 14 : size >= 50 ? 8 : 4;
        view.setEffect(new DropShadow(
                BlurType.GAUSSIAN,
                Color.rgb(0, 0, 0, size >= 80 ? 0.45 : 0.28),
                blur, 0.28, 0, 1
        ));

        // Extra pad so the full logo + shadow is not clipped.
        double pad = Math.max(4, blur * 0.35);
        StackPane wrap = new StackPane(view);
        wrap.setPadding(new Insets(pad));
        wrap.setMinSize(size + pad * 2, size + pad * 2);
        wrap.setPrefSize(size + pad * 2, size + pad * 2);
        wrap.setMaxSize(size + pad * 2, size + pad * 2);
        return wrap;
    }

    /**
     * Compact dialog header mark — white disc with red "UE" text (readable on red heroes).
     * Login / sidebar / window icon still use {@link #ueLogo(double)} (official crest).
     */
    public static Node dialogBrandMark() {
        return dialogBrandMark(18);
    }

    /** Compact dialog header mark sized for red branded headers. */
    public static Node dialogBrandMark(double textSizePx) {
        double discR = 18;
        double glowR = 22;
        javafx.scene.shape.Circle glow = new javafx.scene.shape.Circle(glowR, Color.web("rgba(255,255,255,0.22)"));
        javafx.scene.shape.Circle disc = new javafx.scene.shape.Circle(discR, Color.web("#FFFFFF"));
        Label ue = new Label("UE");
        ue.setStyle(
                "-fx-text-fill: " + UiTheme.UE_RED + ";"
                + "-fx-font-size: " + textSizePx + "px;"
                + "-fx-font-weight: bold;"
        );
        StackPane mark = new StackPane(glow, disc, ue);
        mark.setMinSize(44, 44);
        mark.setPrefSize(44, 44);
        mark.setMaxSize(44, 44);
        return mark;
    }

    /**
     * Sets the window / taskbar icon to the official UE logo at proper OS sizes.
     * Uses small scaled copies so the title-bar icon is not oversized.
     */
    public static void applyStageIcon(Stage stage) {
        if (stage == null) return;
        Image icon16 = loadUeLogoSized(16);
        Image icon32 = loadUeLogoSized(32);
        Image icon48 = loadUeLogoSized(48);
        if (icon16 == null && icon32 == null && icon48 == null) return;
        java.util.List<Image> icons = new java.util.ArrayList<>();
        if (icon16 != null) icons.add(icon16);
        if (icon32 != null) icons.add(icon32);
        if (icon48 != null) icons.add(icon48);
        stage.getIcons().setAll(icons);
    }

    public static Image loadUeLogo() {
        return loadUeLogoSized(0);
    }

    /** Loads the official crest; pass size &gt; 0 to decode at that pixel size. */
    public static Image loadUeLogoSized(double size) {
        Image img = loadImageNamed("UE LOGO", size);
        if (img == null) img = loadImageNamed("UE_LOGO", size);
        if (img == null) img = loadImageNamed("UE_RED_SEAL_09", size);
        return img;
    }

    public static Image loadCampus() {
        return loadImageNamed("ue-campus");
    }

    public static Image loadBag() {
        return loadImageNamed("BAG");
    }

    public static ImageView imageView(Image image, double fitW, double fitH) {
        if (image == null) return null;
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(fitW);
        view.setFitHeight(fitH);
        return view;
    }

    public static Image loadImageNamed(String baseName) {
        return loadImageNamed(baseName, 0);
    }

    public static Image loadImageNamed(String baseName, double size) {
        String[] exts = {".png", ".jpg", ".jpeg", ".PNG", ".JPG", ".JPEG"};
        for (String ext : exts) {
            String path = "/images/" + baseName + ext;
            InputStream in = UeImages.class.getResourceAsStream(path);
            if (in != null) {
                if (size > 0) {
                    return new Image(in, size, size, true, true);
                }
                return new Image(in);
            }
        }
        return null;
    }
}
