package finalproj.mavenproject1;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public final class BrandPanel {

    private static final String UE_RED = "#E41135";

    private BrandPanel() {}

    public static VBox buildSidebarBrand() {
        return buildSidebarBrand("Student Portal");
    }

    public static VBox buildSidebarBrand(String portalLabel) {
        VBox brand = new VBox(8);
        brand.setAlignment(Pos.TOP_CENTER);
        brand.setPadding(new Insets(2, 4, 4, 4));

        brand.getChildren().add(UeImages.ueLogo(72));

        Label uniTop = new Label("UNIVERSITY OF THE EAST");
        uniTop.setWrapText(true);
        uniTop.setAlignment(Pos.CENTER);
        uniTop.setMaxWidth(Double.MAX_VALUE);
        uniTop.setStyle(
                "-fx-text-fill: #141416;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-alignment: center;"
        );

        Label uniBottom = new Label("CALOOCAN");
        uniBottom.setAlignment(Pos.CENTER);
        uniBottom.setMaxWidth(Double.MAX_VALUE);
        uniBottom.setStyle(
                "-fx-text-fill: #141416;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-alignment: center;"
        );

        VBox uniText = new VBox(1, uniTop, uniBottom);
        uniText.setAlignment(Pos.CENTER);
        brand.getChildren().add(uniText);
        Line topSeparator = new Line(0, 0, 180, 0);
        topSeparator.setStroke(Color.web(UE_RED));
        topSeparator.setStrokeWidth(2);

        HBox separatorBox = new HBox(topSeparator);
        separatorBox.setAlignment(Pos.CENTER);
        separatorBox.setMaxWidth(Double.MAX_VALUE);
        brand.getChildren().add(separatorBox);
        VBox.setMargin(separatorBox, new Insets(6, 0, 2, 0));

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER);
        titleRow.setMaxWidth(Double.MAX_VALUE);
        titleRow.setPadding(new Insets(2, 0, 0, 0));

        ImageView bagLogo = UeImages.imageView(UeImages.loadBag(), 48, 48);
        if (bagLogo != null) {
            titleRow.getChildren().add(bagLogo);
        }

        VBox titleText = new VBox(-2);
        titleText.setAlignment(Pos.CENTER_LEFT);
        Label line1 = new Label("Lost and");
        line1.setStyle(
                "-fx-text-fill: #141416;"
                + "-fx-font-size: 15px;"
                + "-fx-font-weight: bold;"
        );
        Label line2 = new Label("Java-Found");
        line2.setStyle(
                "-fx-text-fill: " + UE_RED + ";"
                + "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
        );
        titleText.getChildren().addAll(line1, line2);
        titleRow.getChildren().add(titleText);

        brand.getChildren().add(titleRow);

        Label portal = new Label(portalLabel == null || portalLabel.isBlank()
                ? "Portal" : portalLabel);
        portal.setStyle(
                "-fx-text-fill: #141416;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-alignment: center;"
        );

        Line leftLine = new Line(0, 0, 36, 0);
        leftLine.setStroke(Color.web(UE_RED));
        leftLine.setStrokeWidth(2);

        Line rightLine = new Line(0, 0, 36, 0);
        rightLine.setStroke(Color.web(UE_RED));
        rightLine.setStrokeWidth(2);

        HBox portalRow = new HBox(8, leftLine, portal, rightLine);
        portalRow.setAlignment(Pos.CENTER);
        portalRow.setMaxWidth(Double.MAX_VALUE);
        brand.getChildren().add(portalRow);
        VBox.setMargin(portalRow, new Insets(2, 0, 4, 0));

        return brand;
    }
}
