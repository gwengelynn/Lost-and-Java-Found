package finalproj.mavenproject1;

import java.util.Optional;
import java.util.function.Supplier;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Branded form dialog — UE look (not plain default JavaFX chrome).
 * Same usage pattern as the old DialogPaneBuilder; does not change save/confirm logic.
 */
final class BrandedDialog {

    static final class PhotoPick {
        String storedPath;
        final Label label = new Label("No photo selected");
        private Runnable syncClear = () -> {};

        void setPath(String path) {
            storedPath = (path == null || path.isBlank()) ? null : path;
            label.setText(storedPath == null ? "No photo selected" : storedPath);
            syncClear.run();
        }
    }

    private final Alert alert;
    private final GridPane grid = new GridPane();
    private final Stage ownerStage;
    private final String title;
    private int row = 0;
    private String helpText;
    private boolean submitting;

    BrandedDialog(Stage owner, String title) {
        this.ownerStage = owner;
        this.title = title == null ? "" : title;
        alert = new Alert(Alert.AlertType.NONE);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(this.title);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        if (owner != null) alert.initOwner(owner);

        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(4, 2, 2, 2));
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(30);
        c0.setHgrow(Priority.NEVER);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(70);
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        ScrollPane formScroll = new ScrollPane(buildFormShell(grid));
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setStyle(
                "-fx-background: transparent;"
                + "-fx-background-color: transparent;"
                + "-fx-border-color: transparent;"
                + "-fx-padding: 0;"
        );
        if (formScroll.getContent() != null) {
            formScroll.getContent().setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #FFF8F9, #FFFFFF);"
            );
        }
        // Keep header + OK/Cancel visible; only the form fields scroll.
        double scrollH = 420;
        if (owner != null && owner.getHeight() > 0) {
            scrollH = Math.max(260, Math.min(480, owner.getHeight() * 0.52));
        }
        formScroll.setPrefHeight(scrollH);
        formScroll.setMaxHeight(scrollH);
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        VBox body = new VBox(0, buildHeroHeader(this.title, true), formScroll);
        body.setPadding(new Insets(0));

        DialogPane pane = alert.getDialogPane();
        pane.setGraphic(null);
        pane.setHeader(null);
        pane.setContent(body);
        pane.setPrefWidth(520);
        pane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialogPane(pane);

        alert.setOnShown(e -> {
            pane.setGraphic(null);
            styleActionButtons(pane);
            polishButtonBar(pane);
            // Re-fit scroll height to the live window so tall forms stay usable.
            Window w = pane.getScene() != null ? pane.getScene().getWindow() : null;
            Window basis = owner != null ? owner : w;
            if (basis != null && basis.getHeight() > 0) {
                double h = Math.max(240, Math.min(500, basis.getHeight() * 0.52));
                formScroll.setPrefHeight(h);
                formScroll.setMaxHeight(h);
            }
            FadeTransition fade = new FadeTransition(Duration.millis(160), pane);
            fade.setFromValue(0.92);
            fade.setToValue(1.0);
            fade.play();
        });
    }

    BrandedDialog help(String text) {
        this.helpText = text;
        return this;
    }

    void setPrefWidth(double width) {
        alert.getDialogPane().setPrefWidth(width);
    }

    TextField field(String label) {
        return field(label, "");
    }

    TextField field(String label, String value) {
        TextField tf = new TextField(value == null ? "" : value);
        styleField(tf);
        add(label, tf);
        return tf;
    }

    PasswordField password(String label) {
        PasswordField pf = new PasswordField();
        styleField(pf);
        add(label, pf);
        return pf;
    }

    TextArea area(String label) {
        TextArea area = new TextArea();
        area.setPrefRowCount(3);
        area.setWrapText(true);
        styleArea(area);
        add(label, area);
        return area;
    }

    TextArea stackedArea(String question) {
        Label qLabel = new Label(question);
        qLabel.setWrapText(true);
        qLabel.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT
                        + "; -fx-font-size: 12.5px;"
        );
        TextArea area = new TextArea();
        area.setPrefRowCount(2);
        area.setWrapText(true);
        area.setPromptText("Type your answer here");
        area.setMaxWidth(Double.MAX_VALUE);
        styleArea(area);
        VBox block = new VBox(8, qLabel, area);
        block.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED_SOFT + ";"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 12;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 12;"
        );
        grid.add(block, 0, row, 2, 1);
        row++;
        return area;
    }

    ComboBox<String> combo(String label, String... values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(values);
        combo.getSelectionModel().selectFirst();
        combo.setMaxWidth(Double.MAX_VALUE);
        styleCombo(combo);
        add(label, combo);
        return combo;
    }

    PhotoPick photoPicker(String label, String storePrefix) {
        PhotoPick pick = new PhotoPick();
        pick.label.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
        Button choose = new Button("Browse…");
        UiButtons.style(choose, UiButtons.Kind.GHOST);
        Button clear = new Button("Remove");
        UiButtons.style(clear, UiButtons.Kind.LINK);
        clear.setVisible(false);
        clear.setManaged(false);
        pick.syncClear = () -> {
            boolean has = pick.storedPath != null && !pick.storedPath.isBlank();
            clear.setVisible(has);
            clear.setManaged(has);
            if (!has) pick.label.setText("No photo selected");
        };
        choose.setOnAction(e -> {
            Window w = ownerStage != null ? ownerStage : alert.getDialogPane().getScene().getWindow();
            String stored = PhotoStorage.chooseAndStore(w, storePrefix);
            if (stored != null) pick.setPath(stored);
        });
        clear.setOnAction(e -> pick.setPath(null));
        HBox rowBox = new HBox(10, choose, clear, pick.label);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        add(label, rowBox);
        pick.syncClear.run();
        return pick;
    }

    void addCustom(String label, Node node) {
        add(label, node);
    }

    /** Returns true if OK was pressed and onConfirm returned true (or null). */
    boolean show(Supplier<Boolean> onConfirm) {
        DialogPane pane = alert.getDialogPane();
        pane.setGraphic(null);
        styleActionButtons(pane);
        // Prevent double-clicks from running the save logic twice.
        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (submitting) {
                    event.consume();
                    return;
                }
                submitting = true;
                okBtn.setDisable(true);
            });
        }
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            submitting = false;
            if (okBtn != null) okBtn.setDisable(false);
            return false;
        }
        try {
            Boolean ok = onConfirm.get();
            boolean success = ok == null || ok;
            if (!success) {
                submitting = false;
                if (okBtn != null) okBtn.setDisable(false);
            }
            return success;
        } catch (RuntimeException ex) {
            submitting = false;
            if (okBtn != null) okBtn.setDisable(false);
            throw ex;
        }
    }

    static void info(Window owner, String title, String message) {
        showMessage(owner, title, message, false);
    }

    static boolean confirm(Window owner, String title, String message) {
        return showMessage(owner, title, message, true);
    }

    /**
     * Branded “View” dialog (UE header) with optional secondary action (e.g. Timeline)
     * and a Close button. Replaces plain JavaFX Alert detail popups.
     */
    static Optional<ButtonType> details(Window owner, String title, String eyebrow,
                                        String message, ButtonType actionOrNull) {
        return details(owner, title, eyebrow, message, null, actionOrNull);
    }

    static Optional<ButtonType> details(Window owner, String title, String eyebrow,
                                        String message, Node mediaOrNull, ButtonType actionOrNull) {
        Alert tip = new Alert(Alert.AlertType.NONE);
        tip.initStyle(StageStyle.UTILITY);
        tip.setTitle(title == null ? "Details" : title);
        tip.setHeaderText(null);
        tip.setGraphic(null);
        if (owner != null) tip.initOwner(owner);

        Label body = new Label(message == null ? "" : message);
        body.setWrapText(true);
        body.setMaxWidth(460);
        body.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-font-size: 13.5px;"
                + "-fx-line-spacing: 3px;"
        );
        VBox messageBox = new VBox(12);
        if (mediaOrNull != null) {
            Label photoLbl = new Label("Photo");
            photoLbl.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.UE_RED + ";"
            );
            messageBox.getChildren().addAll(photoLbl, mediaOrNull);
        }
        messageBox.getChildren().add(body);
        ScrollPane scroll = new ScrollPane(messageBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(340);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        String eye = (eyebrow == null || eyebrow.isBlank()) ? "Details" : eyebrow;
        VBox shell = new VBox(0,
                buildHeroHeaderStatic(title == null ? "Details" : title, eye),
                paddedContent(scroll)
        );

        DialogPane pane = tip.getDialogPane();
        pane.setGraphic(null);
        pane.setHeader(null);
        pane.setContent(shell);
        pane.setPrefWidth(520);
        if (actionOrNull != null) {
            pane.getButtonTypes().setAll(actionOrNull, ButtonType.CLOSE);
        } else {
            pane.getButtonTypes().setAll(ButtonType.CLOSE);
        }
        styleDialogPane(pane);
        tip.setOnShown(e -> {
            pane.setGraphic(null);
            styleActionButtons(pane);
            polishButtonBar(pane);
            Button action = actionOrNull == null ? null : (Button) pane.lookupButton(actionOrNull);
            if (action != null) UiButtons.style(action, UiButtons.Kind.PRIMARY);
            Button close = (Button) pane.lookupButton(ButtonType.CLOSE);
            if (close != null) {
                close.setText("Close");
                UiButtons.style(close, UiButtons.Kind.GHOST);
            }
        });
        return tip.showAndWait();
    }

    private static boolean showMessage(Window owner, String title, String message, boolean confirm) {
        Alert tip = new Alert(Alert.AlertType.NONE);
        tip.initStyle(StageStyle.UTILITY);
        tip.setTitle(title);
        tip.setHeaderText(null);
        tip.setGraphic(null);
        if (owner != null) tip.initOwner(owner);

        Label body = new Label(message == null ? "" : message);
        body.setWrapText(true);
        body.setMaxWidth(440);
        body.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 13.5px;"
                + "-fx-line-spacing: 4px;"
                + "-fx-padding: 2 2 4 2;"
        );

        VBox shell = new VBox(0,
                buildHeroHeaderStatic(title == null ? "" : title, confirm ? "Confirm" : "Notice"),
                paddedContent(body)
        );

        DialogPane pane = tip.getDialogPane();
        pane.setGraphic(null);
        pane.setHeader(null);
        pane.setContent(shell);
        pane.setPrefWidth(480);
        pane.setMinWidth(420);
        if (confirm) {
            pane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        } else {
            pane.getButtonTypes().setAll(ButtonType.OK);
        }
        styleDialogPane(pane);
        tip.setOnShown(e -> {
            pane.setGraphic(null);
            styleActionButtons(pane);
            polishButtonBar(pane);
            Button ok = (Button) pane.lookupButton(ButtonType.OK);
            if (ok != null && !confirm) ok.setText("Got it");
        });
        Optional<ButtonType> result = tip.showAndWait();
        return confirm && result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showHelp() {
        String text = (helpText == null || helpText.isBlank())
                ? "Fill in the required fields, then press OK to save.\nCancel closes without saving."
                : helpText;
        info(ownerStage, "Help", text);
    }

    private void add(String label, Node node) {
        Label lbl = new Label(label);
        lbl.setWrapText(true);
        lbl.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 12.5px;"
                + "-fx-font-weight: bold;"
        );
        grid.add(lbl, 0, row);
        grid.add(node, 1, row);
        row++;
    }

    private Node buildHeroHeader(String heading, boolean withHelp) {
        Label titleLbl = new Label(heading);
        titleLbl.setWrapText(true);
        titleLbl.setStyle(
                "-fx-font-size: 17px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: white;"
        );

        Label sub = new Label("UE Caloocan Lost & Found");
        sub.setStyle(
                "-fx-font-size: 11.5px;"
                + "-fx-text-fill: rgba(255,255,255,0.88);"
                + "-fx-font-weight: bold;"
        );

        VBox textCol = new VBox(3, titleLbl, sub);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox row = new HBox(14, brandMark(), textCol);
        row.setAlignment(Pos.CENTER_LEFT);

        if (withHelp) {
            Button helpBtn = new Button();
            helpBtn.setGraphic(helpGlyph());
            helpBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.18);"
                    + "-fx-background-radius: 16;"
                    + "-fx-min-width: 32; -fx-min-height: 32;"
                    + "-fx-max-width: 32; -fx-max-height: 32;"
                    + "-fx-padding: 0; -fx-cursor: hand;"
                    + "-fx-border-color: rgba(255,255,255,0.45);"
                    + "-fx-border-radius: 16;"
                    + "-fx-border-width: 1;"
            );
            helpBtn.setOnMouseEntered(e -> helpBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.30);"
                    + "-fx-background-radius: 16;"
                    + "-fx-min-width: 32; -fx-min-height: 32;"
                    + "-fx-max-width: 32; -fx-max-height: 32;"
                    + "-fx-padding: 0; -fx-cursor: hand;"
                    + "-fx-border-color: rgba(255,255,255,0.70);"
                    + "-fx-border-radius: 16;"
                    + "-fx-border-width: 1;"
            ));
            helpBtn.setOnMouseExited(e -> helpBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.18);"
                    + "-fx-background-radius: 16;"
                    + "-fx-min-width: 32; -fx-min-height: 32;"
                    + "-fx-max-width: 32; -fx-max-height: 32;"
                    + "-fx-padding: 0; -fx-cursor: hand;"
                    + "-fx-border-color: rgba(255,255,255,0.45);"
                    + "-fx-border-radius: 16;"
                    + "-fx-border-width: 1;"
            ));
            helpBtn.setOnAction(e -> showHelp());
            row.getChildren().add(helpBtn);
        }

        VBox header = new VBox(row);
        header.setPadding(new Insets(18, 20, 18, 20));
        header.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F0183C, #C70E2E 55%, #A50C26);"
                + "-fx-background-radius: 14 14 0 0;"
        );
        return header;
    }

    private static Node buildHeroHeaderStatic(String heading, String eyebrow) {
        Label eye = new Label(eyebrow.toUpperCase());
        eye.setStyle(
                "-fx-font-size: 10.5px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: rgba(255,255,255,0.82);"
        );
        Label titleLbl = new Label(heading);
        titleLbl.setWrapText(true);
        titleLbl.setStyle(
                "-fx-font-size: 17px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: white;"
        );
        VBox textCol = new VBox(4, eye, titleLbl);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox row = new HBox(14, brandMark(), textCol);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(row);
        header.setPadding(new Insets(18, 20, 18, 20));
        header.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F0183C, #C70E2E 55%, #A50C26);"
                + "-fx-background-radius: 14 14 0 0;"
        );
        return header;
    }

    private static Node paddedContent(Node content) {
        VBox box = new VBox(content);
        box.setPadding(new Insets(18, 20, 8, 20));
        box.setStyle("-fx-background-color: rgba(255,252,252,0.98);");
        return box;
    }

    private static Node buildFormShell(Node form) {
        VBox shell = new VBox(form);
        shell.setPadding(new Insets(18, 18, 10, 18));
        shell.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #FFF8F9, #FFFFFF);"
        );
        return shell;
    }

    private static Node brandMark() {
        return UeImages.dialogBrandMark();
    }

    private static void styleDialogPane(DialogPane pane) {
        pane.setGraphic(null);
        UeChrome.apply(pane);
        pane.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-radius: 14;"
                + "-fx-background-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(165,12,38,0.22), 28, 0.18, 0, 10);"
        );
        pane.getStyleClass().removeAll("alert", "confirmation", "information", "warning", "error");
        pane.getStyleClass().add("branded-dialog");
        // Kill leftover default alert padding/graphic slots.
        pane.lookupAll(".graphic-container").forEach(n -> {
            n.setVisible(false);
            n.setManaged(false);
            if (n instanceof Region r) {
                r.setMinSize(0, 0);
                r.setPrefSize(0, 0);
                r.setMaxSize(0, 0);
            }
        });
    }

    private static void polishButtonBar(DialogPane pane) {
        Node bar = pane.lookup(".button-bar");
        if (bar != null) {
            bar.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #FFF5F6, #FCE8EC);"
                    + "-fx-padding: 12 16 14 16;"
                    + "-fx-background-radius: 0 0 14 14;"
                    + "-fx-border-color: transparent transparent transparent transparent;"
            );
        }
        // Soft accent strip above buttons
        if (pane.getExpandableContent() == null) {
            Rectangle strip = new Rectangle(1, 3);
            strip.setManaged(false);
        }
    }

    private static void styleActionButtons(DialogPane pane) {
        Button ok = (Button) pane.lookupButton(ButtonType.OK);
        Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
        Button close = (Button) pane.lookupButton(ButtonType.CLOSE);
        Button yes = (Button) pane.lookupButton(ButtonType.YES);
        if (ok != null) {
            if (ok.getText() == null || ok.getText().isBlank() || "OK".equalsIgnoreCase(ok.getText())) {
                ok.setText("OK");
            }
            UiButtons.style(ok, UiButtons.Kind.PRIMARY);
            ButtonBar.setButtonData(ok, ButtonBar.ButtonData.OK_DONE);
        }
        if (yes != null) UiButtons.style(yes, UiButtons.Kind.PRIMARY);
        if (cancel != null) {
            cancel.setText("Cancel");
            UiButtons.style(cancel, UiButtons.Kind.GHOST);
        }
        if (close != null) UiButtons.style(close, UiButtons.Kind.GHOST);
    }

    private static void styleField(TextField field) {
        field.setPrefHeight(38);
        field.setStyle(fieldStyle());
        field.focusedProperty().addListener((o, a, focused) ->
                field.setStyle(focused ? fieldStyleFocused() : fieldStyle()));
        field.setMaxWidth(Double.MAX_VALUE);
    }

    private static void styleArea(TextArea area) {
        area.setStyle(areaStyle());
        area.focusedProperty().addListener((o, a, focused) ->
                area.setStyle(focused ? areaStyleFocused() : areaStyle()));
        area.setMaxWidth(Double.MAX_VALUE);
    }

    private static void styleCombo(ComboBox<String> combo) {
        combo.getStyleClass().add("ue-combo");
        combo.setVisibleRowCount(8);
        combo.setStyle(comboStyle());
        combo.setPrefHeight(38);
        combo.focusedProperty().addListener((o, a, focused) ->
                combo.setStyle(focused ? comboStyleFocused() : comboStyle()));
    }

    private static String fieldStyle() {
        return "-fx-background-color: rgba(255,255,255,0.92);"
                + "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-prompt-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 8 12 8 12;"
                + "-fx-font-size: 13px;"
                + "-fx-effect: dropshadow(gaussian, rgba(228,17,53,0.06), 6, 0, 0, 1);";
    }

    private static String fieldStyleFocused() {
        return "-fx-background-color: #FFFFFF;"
                + "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-prompt-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-border-color: " + UiTheme.UE_RED + ";"
                + "-fx-border-width: 1.6;"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 8 12 8 12;"
                + "-fx-font-size: 13px;"
                + "-fx-effect: dropshadow(gaussian, rgba(228,17,53,0.22), 10, 0, 0, 2);";
    }

    private static String areaStyle() {
        return "-fx-background-color: rgba(255,255,255,0.92);"
                + "-fx-control-inner-background: rgba(255,255,255,0.92);"
                + "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-prompt-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-font-size: 13px;"
                + "-fx-effect: dropshadow(gaussian, rgba(228,17,53,0.06), 6, 0, 0, 1);";
    }

    private static String areaStyleFocused() {
        return "-fx-background-color: #FFFFFF;"
                + "-fx-control-inner-background: #FFFFFF;"
                + "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-prompt-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-border-color: " + UiTheme.UE_RED + ";"
                + "-fx-border-width: 1.6;"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-font-size: 13px;"
                + "-fx-effect: dropshadow(gaussian, rgba(228,17,53,0.22), 10, 0, 0, 2);";
    }

    private static String comboStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #FFFFFF, #FCEEEF);"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-font-size: 13px;"
                + "-fx-effect: dropshadow(gaussian, rgba(228,17,53,0.06), 6, 0, 0, 1);";
    }

    private static String comboStyleFocused() {
        return "-fx-background-color: #FFFFFF;"
                + "-fx-border-color: " + UiTheme.UE_RED + ";"
                + "-fx-border-width: 1.6;"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-font-size: 13px;"
                + "-fx-effect: dropshadow(gaussian, rgba(228,17,53,0.22), 10, 0, 0, 2);";
    }

    private static Node helpGlyph() {
        SVGPath q = new SVGPath();
        q.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z");
        q.setFill(Color.WHITE);
        q.setScaleX(0.52);
        q.setScaleY(0.52);
        StackPane box = new StackPane(q);
        box.setMouseTransparent(true);
        return box;
    }
}
