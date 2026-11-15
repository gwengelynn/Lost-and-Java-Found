package finalproj.mavenproject1;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Dashboard sidebar brand — sized to consume space like the login brand mockup.
 */
public final class BrandPanel {

    private static final String UE_RED = "#E41135";
    private static final String TEXT_HALO =
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.95), 12, 0.6, 0, 0);";

    private BrandPanel() {}

    public static VBox buildSidebarBrand() {
        return buildSidebarBrand("Student Portal");
    }

    public static VBox buildSidebarBrand(String portalLabel) {
        VBox brand = new VBox(12);
        brand.setAlignment(Pos.TOP_CENTER);
        brand.setPadding(new Insets(6, 4, 8, 4));

        // Larger seal — matches the first reference presence
        brand.getChildren().add(UeImages.ueLogo(96));

        Label uniTop = new Label("UNIVERSITY OF THE EAST");
        uniTop.setWrapText(true);
        uniTop.setAlignment(Pos.CENTER);
        uniTop.setMaxWidth(Double.MAX_VALUE);
        uniTop.setStyle(
                "-fx-text-fill: #141416;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-alignment: center;"
                + TEXT_HALO
        );

        Label uniBottom = new Label("PHILIPPINES");
        uniBottom.setAlignment(Pos.CENTER);
        uniBottom.setMaxWidth(Double.MAX_VALUE);
        uniBottom.setStyle(
                "-fx-text-fill: #141416;"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-alignment: center;"
                + TEXT_HALO
        );

        VBox uniText = new VBox(2, uniTop, uniBottom);
        uniText.setAlignment(Pos.CENTER);
        brand.getChildren().add(uniText);

        // Bigger bag + title block, centered like the first pic
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER);
        titleRow.setMaxWidth(Double.MAX_VALUE);
        titleRow.setPadding(new Insets(4, 0, 0, 0));

        ImageView bagLogo = UeImages.imageView(UeImages.loadBag(), 64, 64);
        if (bagLogo != null) {
            bagLogo.setEffect(new DropShadow(
                    BlurType.GAUSSIAN,
                    Color.rgb(255, 255, 255, 0.95),
                    16, 0.55, 0, 0
            ));
            titleRow.getChildren().add(bagLogo);
        }

        VBox titleText = new VBox(-2);
        titleText.setAlignment(Pos.CENTER_LEFT);
        Label line1 = new Label("Lost and");
        line1.setStyle(
                "-fx-text-fill: #141416;"
                + "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
                + TEXT_HALO
        );
        Label line2 = new Label("Java-Found");
        line2.setStyle(
                "-fx-text-fill: " + UE_RED + ";"
                + "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
                + TEXT_HALO
        );
        titleText.getChildren().addAll(line1, line2);
        titleRow.getChildren().add(titleText);

        brand.getChildren().add(titleRow);

        Label portal = new Label(portalLabel == null || portalLabel.isBlank()
                ? "Portal" : portalLabel);
        portal.setAlignment(Pos.CENTER);
        portal.setMaxWidth(Double.MAX_VALUE);
        portal.setStyle(
                "-fx-text-fill: #141416;"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-alignment: center;"
                + TEXT_HALO
        );
        brand.getChildren().add(portal);

        return brand;
    }
}