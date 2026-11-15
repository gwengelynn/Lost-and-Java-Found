
package finalproj.mavenproject1;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

/**
 * Login — mockup layout, LIGHTER look (readable inputs):
 *   FULL-SCREEN campus photo behind everything
 *   LEFT  = brand text
 *   RIGHT = white/light card (dark text in fields)
 *
 * Images in src/main/resources/images/:
 *   ue-campus.png  — full-screen background
 *   UE LOGO.png    — top-left university seal
 *   BAG.png        — bag icon beside title
 * Demo: student/student123 (Student), admin/admin123 (Admin)
 */
public class LoginView {

    private static final String UE_RED = "#E41135";
    private static final String RED_SOFT = "#FCE8EC";
    // Softer card — less chalky white so campus photo still shows
    private static final String CARD_BG = "rgba(255, 252, 252, 0.92)";
    private static final String FIELD_BG = "rgba(255, 255, 255, 0.98)";
    private static final String TEXT_DARK = "#141416";
    private static final String TEXT_MUTED = "#3F3F46";
    private static final String BORDER = "rgba(228, 17, 53, 0.28)";

    private final Stage stage;
    private final StackPane root;

    private ToggleButton studentToggle;
    private ToggleButton adminToggle;
    private ToggleGroup roleGroup;
    private TextField userField;
    private PasswordField passwordField;
    private TextField passwordVisibleField;
    private CheckBox showPasswordCheck;
    private CheckBox rememberCheck;
    private Label statusLabel;

    public LoginView(Stage stage) {
        this.stage = stage;
        this.root = buildUi();
    }

    public StackPane getRoot() {
        return root;
    }

    private StackPane buildUi() {
        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: #F5F5F7;");

        // Full-screen campus photo
        Image campus = loadCampusImage();
        if (campus != null) {
            ImageView photo = new ImageView(campus);
            photo.setPreserveRatio(false);
            photo.setSmooth(true);
            photo.fitWidthProperty().bind(rootPane.widthProperty());
            photo.fitHeightProperty().bind(rootPane.heightProperty());
            rootPane.getChildren().add(photo);
        } else {
            Region fallback = new Region();
            fallback.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #FCE8EC, #F5F5F7);"
            );
            fallback.prefWidthProperty().bind(rootPane.widthProperty());
            fallback.prefHeightProperty().bind(rootPane.heightProperty());
            rootPane.getChildren().add(fallback);
        }

        // Soft red wash only (no heavy white veil)
        Region wash = new Region();
        wash.setStyle("-fx-background-color: rgba(80, 8, 18, 0.18);");
        wash.prefWidthProperty().bind(rootPane.widthProperty());
        wash.prefHeightProperty().bind(rootPane.heightProperty());
        rootPane.getChildren().add(wash);

        BorderPane content = new BorderPane();
        content.setPadding(new Insets(36, 48, 36, 48));

        content.setLeft(buildLeftBrandPanel());
        BorderPane.setAlignment(content.getLeft(), Pos.CENTER_LEFT);

        StackPane right = new StackPane(buildLoginCard());
        right.setAlignment(Pos.CENTER_RIGHT);
        content.setRight(right);

        rootPane.getChildren().add(content);
        return rootPane;
    }

    /**
     * Left side like the mockup:
     *   TOP    = UE LOGO + UNIVERSITY OF THE EAST / PHILIPPINES
     *   BOTTOM = BAG + Lost and Java-Found + tagline
     */
    private VBox buildLeftBrandPanel() {
        VBox brand = new VBox(10);
        brand.setAlignment(Pos.TOP_LEFT);
        brand.setMaxWidth(480);
        brand.setPadding(new Insets(0, 24, 16, 8));
        VBox.setVgrow(brand, Priority.ALWAYS);

        // ---- Top: UE LOGO + university name (same visual height) ----
        HBox ueHeader = new HBox(14);
        ueHeader.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.Node ueLogoNode = UeImages.ueLogo(78);
        if (ueLogoNode != null) {
            ueHeader.getChildren().add(ueLogoNode);
        }

        VBox uniText = new VBox(2);
        uniText.setAlignment(Pos.CENTER_LEFT);
        Label uniTop = new Label("UNIVERSITY OF THE EAST");
        uniTop.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 10, 0.4, 0, 1);"
        );
        Label uniBottom = new Label("PHILIPPINES");
        uniBottom.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 10, 0.4, 0, 1);"
        );
        uniText.getChildren().addAll(uniTop, uniBottom);
        ueHeader.getChildren().add(uniText);

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);

        // ---- Bottom: BAG + title ----
        HBox titleRow = new HBox(14);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        ImageView bagLogo = UeImages.imageView(UeImages.loadBag(), 120, 120);
        if (bagLogo != null) {
            // Soft white glow so the red bag outline pops on the campus photo
            bagLogo.setEffect(new javafx.scene.effect.DropShadow(
                    javafx.scene.effect.BlurType.GAUSSIAN,
                    javafx.scene.paint.Color.rgb(255, 255, 255, 0.75),
                    12, 0.4, 0, 0
            ));
            titleRow.getChildren().add(bagLogo);
        }

        VBox titleText = new VBox(0);
        Label line1 = new Label("Lost and");
        line1.setStyle(
                "-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: white;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 12, 0.45, 0, 1);"
        );
        Label line2 = new Label("Java-Found");
        line2.setStyle(
                "-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: " + UE_RED + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.95), 8, 0.55, 0, 0);"
        );
        titleText.getChildren().addAll(line1, line2);
        titleRow.getChildren().add(titleText);

        Label system = new Label("SCHOOL LOST AND FOUND MANAGEMENT SYSTEM");
        system.setWrapText(true);
        system.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 8 0 0 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 10, 0.4, 0, 1);"
        );

        Region rule = new Region();
        rule.setPrefHeight(2);
        rule.setMaxWidth(220);
        rule.setStyle("-fx-background-color: " + UE_RED + "; -fx-background-radius: 1;");

        Label tagline = new Label("Reuniting what matters.");
        tagline.setStyle(
                "-fx-font-size: 15px; -fx-font-style: italic; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 6 0 0 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 10, 0.4, 0, 1);"
        );

        brand.getChildren().addAll(ueHeader, grow, titleRow, system, rule, tagline);
        return brand;
    }

    private VBox buildLoginCard() {
        VBox card = new VBox(14);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(26, 28, 22, 28));
        card.setMaxWidth(400);
        card.setPrefWidth(400);
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 16;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 8);"
        );

        roleGroup = new ToggleGroup();
        studentToggle = new ToggleButton("Student");
        studentToggle.setToggleGroup(roleGroup);
        studentToggle.setSelected(true);
        studentToggle.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(studentToggle, Priority.ALWAYS);
        styleRoleToggle(studentToggle, true);

        adminToggle = new ToggleButton("Administrator");
        adminToggle.setToggleGroup(roleGroup);
        adminToggle.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(adminToggle, Priority.ALWAYS);
        styleRoleToggle(adminToggle, false);

        roleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            styleRoleToggle(studentToggle, studentToggle.isSelected());
            styleRoleToggle(adminToggle, adminToggle.isSelected());
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
            }
        });

        HBox roleRow = new HBox(8, studentToggle, adminToggle);
        roleRow.setMaxWidth(Double.MAX_VALUE);

        Label welcome = new Label("Welcome Back!");
        welcome.setStyle(
                "-fx-font-size: 26px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + TEXT_DARK + ";"
                + "-fx-padding: 6 0 0 0;"
        );

        Label subtitle = new Label("Log in to your account");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_MUTED + ";");

        userField = new TextField();
        userField.setPromptText("Email or Student Number");
        styleLightInput(userField);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        styleLightInput(passwordField);

        passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Password");
        styleLightInput(passwordVisibleField);
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);

        passwordField.textProperty().addListener((o, a, b) -> {
            if (!passwordVisibleField.isFocused()) {
                passwordVisibleField.setText(b);
            }
        });
        passwordVisibleField.textProperty().addListener((o, a, b) -> {
            if (!passwordField.isFocused()) {
                passwordField.setText(b);
            }
        });

        showPasswordCheck = new CheckBox("Show password");
        showPasswordCheck.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");
        showPasswordCheck.selectedProperty().addListener((o, was, show) -> {
            if (show) {
                passwordVisibleField.setText(passwordField.getText());
                passwordField.setManaged(false);
                passwordField.setVisible(false);
                passwordVisibleField.setManaged(true);
                passwordVisibleField.setVisible(true);
            } else {
                passwordField.setText(passwordVisibleField.getText());
                passwordVisibleField.setManaged(false);
                passwordVisibleField.setVisible(false);
                passwordField.setManaged(true);
                passwordField.setVisible(true);
            }
        });

        rememberCheck = new CheckBox("Remember me");
        rememberCheck.setStyle("-fx-text-fill: " + TEXT_MUTED + ";");

        Hyperlink forgot = new Hyperlink("Forgot Password?");
        forgot.setStyle("-fx-text-fill: " + UE_RED + "; -fx-font-size: 12px;");
        forgot.setOnAction(e -> infoStub(
                "Forgot Password",
                "Password reset is not built yet. Use a demo account or Register."
        ));

        Region mid = new Region();
        HBox.setHgrow(mid, Priority.ALWAYS);
        HBox rememberRow = new HBox(8, rememberCheck, mid, forgot);
        rememberRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + UE_RED + "; -fx-font-size: 12px;");

        Button loginButton = new Button("Log In  →");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(
                "-fx-background-color: " + UE_RED + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 15px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 12 16 12 16;"
                + "-fx-cursor: hand;"
        );
        loginButton.setOnAction(e -> handleLogin());
        loginButton.setDefaultButton(true);

        HBox orRow = buildOrDivider();

        Label noAccount = new Label("Don't have an account?");
        noAccount.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 13px;");

        Hyperlink registerLink = new Hyperlink("Register Here");
        registerLink.setStyle(
                "-fx-text-fill: " + UE_RED + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
        );
        registerLink.setOnAction(e -> goToSignup());

        HBox registerRow = new HBox(6, noAccount, registerLink);
        registerRow.setAlignment(Pos.CENTER);

        Hyperlink reports = footerLink("View System Reports");
        reports.setOnAction(e -> infoStub("System Reports", "Reports screen is not built yet."));
        Hyperlink help = footerLink("Need Help?");
        help.setOnAction(e -> infoStub("Help", "Help / support is not built yet."));
        Label pipe = new Label("|");
        pipe.setStyle("-fx-text-fill: " + BORDER + ";");

        HBox footer = new HBox(10, reports, pipe, help);
        footer.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                roleRow,
                welcome,
                subtitle,
                userField,
                passwordField,
                passwordVisibleField,
                showPasswordCheck,
                rememberRow,
                statusLabel,
                loginButton,
                orRow,
                registerRow,
                footer
        );

        userField.textProperty().addListener((o, a, b) -> statusLabel.setText(""));
        passwordField.textProperty().addListener((o, a, b) -> statusLabel.setText(""));

        return card;
    }

    private HBox buildOrDivider() {
        Line left = new Line(0, 0, 70, 0);
        left.setStroke(javafx.scene.paint.Color.web(BORDER));
        Line right = new Line(0, 0, 70, 0);
        right.setStroke(javafx.scene.paint.Color.web(BORDER));
        Label or = new Label("OR");
        or.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px; -fx-padding: 0 10 0 10;");
        HBox row = new HBox(left, or, right);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private void handleLogin() {
        String username = userField.getText() == null ? "" : userField.getText().trim();
        String password = getPasswordText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username/email and password.");
            return;
        }

        String expectedRole = adminToggle.isSelected() ? "ADMIN" : "STUDENT";
        User attempt = new User();
        attempt.setUsername(username);
        attempt.setPasswordHash(password);
        attempt.setRole(expectedRole);

        if (!attempt.login()) {
            showError("Invalid credentials for " + expectedRole + ". Check username, password, and role.");
            return;
        }

        if (rememberCheck.isSelected()) {
            System.out.println("Remember me checked (not saved yet — demo only).");
        }

        statusLabel.setText("");
        User u = User.getCurrentSession();
        if ("STUDENT".equalsIgnoreCase(u.getUserRole())) {
            StudentDashboardView dashboard = new StudentDashboardView(stage, u);
            stage.setTitle("Lost and Java-Found — Student");
            stage.setScene(new javafx.scene.Scene(dashboard.getRoot(), 1280, 760));
            stage.setMinWidth(1024);
            stage.setMinHeight(640);
            return;
        }

        if ("ADMIN".equalsIgnoreCase(u.getUserRole())) {
            AdminDashboardView dashboard = new AdminDashboardView(stage, u);
            stage.setTitle("Lost and Java-Found — Admin");
            stage.setScene(new javafx.scene.Scene(dashboard.getRoot(), 1280, 760));
            stage.setMinWidth(1024);
            stage.setMinHeight(640);
            return;
        }

        showError("Unknown account role. Please contact support.");
    }

    private String getPasswordText() {
        if (passwordVisibleField.isVisible()) {
            return passwordVisibleField.getText() == null ? "" : passwordVisibleField.getText();
        }
        return passwordField.getText() == null ? "" : passwordField.getText();
    }

    private void goToSignup() {
        SignupView signupView = new SignupView(stage);
        stage.setTitle("Lost and Java-Found — Sign up");
        stage.setScene(new javafx.scene.Scene(signupView.getRoot(), 1100, 700));
    }

    private void showError(String message) {
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login failed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void infoStub(String title, String message) {
        Alert tip = new Alert(Alert.AlertType.INFORMATION);
        tip.setTitle(title);
        tip.setHeaderText(null);
        tip.setContentText(message);
        tip.showAndWait();
    }

    private static Hyperlink footerLink(String text) {
        Hyperlink link = new Hyperlink(text);
        link.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");
        return link;
    }

    /** White field + dark typed text (readable). */
    private static void styleLightInput(TextField field) {
        field.setStyle(
                "-fx-background-color: " + FIELD_BG + ";"
                + "-fx-text-fill: " + TEXT_DARK + ";"
                + "-fx-prompt-text-fill: " + TEXT_MUTED + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 12 10 12;"
                + "-fx-font-size: 14px;"
        );
        field.setPrefHeight(42);
        field.setMaxWidth(Double.MAX_VALUE);
    }

    private static void styleRoleToggle(ToggleButton button, boolean selected) {
        if (selected) {
            button.setStyle(
                    "-fx-background-color: " + UE_RED + ";"
                    + "-fx-text-fill: white;"
                    + "-fx-font-weight: bold;"
                    + "-fx-font-size: 13px;"
                    + "-fx-background-radius: 10;"
                    + "-fx-padding: 10 12 10 12;"
                    + "-fx-cursor: hand;"
            );
        } else {
            button.setStyle(
                    "-fx-background-color: " + RED_SOFT + ";"
                    + "-fx-text-fill: " + TEXT_DARK + ";"
                    + "-fx-font-size: 13px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-background-radius: 10;"
                    + "-fx-border-color: " + BORDER + ";"
                    + "-fx-border-radius: 10;"
                    + "-fx-padding: 10 12 10 12;"
                    + "-fx-cursor: hand;"
            );
        }
    }

    private static Image loadCampusImage() {
        Image img = UeImages.loadCampus();
        if (img == null) {
            System.out.println("Campus photo not found. Add src/main/resources/images/ue-campus.png");
        }
        return img;
    }
}