
package finalproj.mavenproject1;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Sign up — full-screen campus photo + LIGHT white card (dark readable input text).
 */
public class SignupView {

    private static final String UE_RED = "#E41135";
    private static final String CARD_BG = "rgba(255, 252, 252, 0.92)";
    private static final String FIELD_BG = "rgba(255, 255, 255, 0.98)";
    private static final String TEXT_DARK = "#141416";
    private static final String TEXT_MUTED = "#3F3F46";
    private static final String BORDER = "rgba(228, 17, 53, 0.28)";

    private final Stage stage;
    private final StackPane root;

    private TextField fullNameField;
    private TextField usernameField;
    private TextField studentNumberField;
    private TextField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private TextField courseField;
    private ComboBox<String> yearLevelCombo;
    private Label statusLabel;

    public SignupView(Stage stage) {
        this.stage = stage;
        this.root = buildUi();
    }

    public StackPane getRoot() {
        return root;
    }

    private StackPane buildUi() {
        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: #F5F5F7;");

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

        Region wash = new Region();
        wash.setStyle("-fx-background-color: rgba(80, 8, 18, 0.18);");
        wash.prefWidthProperty().bind(rootPane.widthProperty());
        wash.prefHeightProperty().bind(rootPane.heightProperty());
        rootPane.getChildren().add(wash);

        BorderPane content = new BorderPane();
        content.setPadding(new Insets(36, 48, 36, 48));
        content.setLeft(buildLeftBrandPanel());
        BorderPane.setAlignment(content.getLeft(), Pos.CENTER_LEFT);

        StackPane right = new StackPane(buildSignupCard());
        right.setAlignment(Pos.CENTER_RIGHT);
        content.setRight(right);

        rootPane.getChildren().add(content);
        return rootPane;
    }

    /** Same logos as Login: UE LOGO (top) + BAG (beside title). */
    private VBox buildLeftBrandPanel() {
        VBox brand = new VBox(10);
        brand.setAlignment(Pos.TOP_LEFT);
        brand.setMaxWidth(480);
        brand.setPadding(new Insets(0, 24, 16, 8));
        VBox.setVgrow(brand, Priority.ALWAYS);

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

        HBox titleRow = new HBox(14);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        ImageView bagLogo = UeImages.imageView(UeImages.loadBag(), 120, 120);
        if (bagLogo != null) {
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

        Label system = new Label("Create your student account");
        system.setWrapText(true);
        system.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 8 0 0 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 10, 0.4, 0, 1);"
        );

        Region rule = new Region();
        rule.setPrefHeight(2);
        rule.setMaxWidth(220);
        rule.setStyle("-fx-background-color: " + UE_RED + ";");

        Label tagline = new Label("Reuniting what matters.");
        tagline.setStyle(
                "-fx-font-size: 15px; -fx-font-style: italic; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 6 0 0 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 10, 0.4, 0, 1);"
        );

        brand.getChildren().addAll(ueHeader, grow, titleRow, system, rule, tagline);
        return brand;
    }

    private VBox buildSignupCard() {
        VBox card = new VBox(9);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(22, 26, 18, 26));
        card.setMaxWidth(420);
        card.setPrefWidth(420);
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 16;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 8);"
        );

        Label heading = new Label("Create Account");
        heading.setStyle(
                "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";"
        );

        Label hint = new Label("Sign up creates a STUDENT account. Admins are seeded only.");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_MUTED + ";");

        fullNameField = input("Full name");
        usernameField = input("Username");
        studentNumberField = input("Student number (e.g. 2025-12345)");
        emailField = input("Email");
        passwordField = password("Password");
        confirmPasswordField = password("Confirm password");
        courseField = input("Course (e.g. BSCPE)");

        yearLevelCombo = new ComboBox<>();
        yearLevelCombo.getItems().setAll("1", "2", "3", "4");
        yearLevelCombo.getSelectionModel().selectFirst();
        yearLevelCombo.setMaxWidth(Double.MAX_VALUE);
        yearLevelCombo.setPrefHeight(40);
        yearLevelCombo.setStyle(
                "-fx-background-color: " + FIELD_BG + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
        );

        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + UE_RED + "; -fx-font-size: 12px;");

        Button signupButton = new Button("Create account");
        signupButton.setMaxWidth(Double.MAX_VALUE);
        signupButton.setStyle(
                "-fx-background-color: " + UE_RED + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 11 16 11 16;"
                + "-fx-cursor: hand;"
        );
        signupButton.setOnAction(e -> handleSignup());
        signupButton.setDefaultButton(true);

        Label haveAccount = new Label("Already have an account?");
        haveAccount.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 13px;");

        Hyperlink backLink = new Hyperlink("Back to Login");
        backLink.setStyle(
                "-fx-text-fill: " + UE_RED + "; -fx-font-size: 13px; -fx-font-weight: bold;"
        );
        backLink.setOnAction(e -> goToLogin());

        HBox backRow = new HBox(6, haveAccount, backLink);
        backRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(
                heading,
                hint,
                fieldLabel("Full name"),
                fullNameField,
                fieldLabel("Username"),
                usernameField,
                fieldLabel("Student number"),
                studentNumberField,
                fieldLabel("Email"),
                emailField,
                fieldLabel("Password"),
                passwordField,
                fieldLabel("Confirm password"),
                confirmPasswordField,
                fieldLabel("Course"),
                courseField,
                fieldLabel("Year level"),
                yearLevelCombo,
                statusLabel,
                signupButton,
                backRow
        );
        return card;
    }

    private void handleSignup() {
        String fullName = text(fullNameField);
        String username = text(usernameField);
        String studentNumber = text(studentNumberField);
        String email = text(emailField);
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        String course = text(courseField);
        String yearLevel = yearLevelCombo.getValue() == null ? "" : yearLevelCombo.getValue();

        if (fullName.isEmpty() || username.isEmpty() || studentNumber.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirm.isEmpty() || course.isEmpty() || yearLevel.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (User.usernameExists(username)) {
            showError("Username \"" + username + "\" is already taken. Choose another.");
            return;
        }

        User account = new User();
        account.setFullName(fullName);
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash(password);
        account.setRole("STUDENT");
        account.setCourse(course);
        account.setYearLevel(yearLevel);
        account.setStudentNumber(studentNumber);
        account.setContactNumber("");
        account.setAccountStatus("Active");

        boolean ok = account.register();
        if (!ok) {
            showError("Could not create account. Username or student number may already exist.");
            return;
        }

        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Account created");
        success.setHeaderText("Welcome, " + fullName + "!");
        success.setContentText(
                "Your STUDENT account \"" + username + "\" is ready.\n"
                + "Click OK to return to Login, then sign in.\n"
                + "Select the Student role toggle on the Login screen."
        );
        success.showAndWait();
        goToLogin();
    }

    private void goToLogin() {
        LoginView loginView = new LoginView(stage);
        stage.setTitle("Lost and Java-Found — Login");
        stage.setScene(new javafx.scene.Scene(loginView.getRoot(), 1100, 680));
    }

    private static String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Sign up failed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MUTED + ";"
        );
        return label;
    }

    private static TextField input(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        styleLightInput(field);
        return field;
    }

    private static PasswordField password(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        styleLightInput(field);
        return field;
    }

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
        field.setPrefHeight(40);
        field.setMaxWidth(Double.MAX_VALUE);
    }

    private static Image loadCampusImage() {
        return UeImages.loadCampus();
    }
}