
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

/**
 * Shared image helpers so Login, Signup, and Dashboard all use the same UE seal.
 */
public final class UeImages {

    private UeImages() {}

    /**
     * New red seal already includes its own circular frame — show it directly
     * with a soft shadow (no extra white badge that washes it out).
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
        view.setEffect(new DropShadow(
                BlurType.GAUSSIAN,
                Color.rgb(0, 0, 0, 0.45),
                14, 0.35, 0, 2
        ));

        StackPane wrap = new StackPane(view);
        wrap.setPrefSize(size, size);
        wrap.setMinSize(size, size);
        wrap.setMaxSize(size, size);
        wrap.setPadding(new Insets(0));
        return wrap;
    }

    public static Image loadUeLogo() {
        // Prefer the new seal, then legacy names
        Image img = loadImageNamed("UE_RED_SEAL_09");
        if (img == null) img = loadImageNamed("UE LOGO");
        if (img == null) img = loadImageNamed("UE_LOGO");
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
        String[] exts = {".png", ".jpg", ".jpeg", ".PNG", ".JPG", ".JPEG"};
        for (String ext : exts) {
            String path = "/images/" + baseName + ext;
            InputStream in = UeImages.class.getResourceAsStream(path);
            if (in != null) {
                return new Image(in);
            }
        }
        return null;
    }
}