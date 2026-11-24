package finalproj.mavenproject1;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.text.TextAlignment;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/** Login footer dialogs: Help FAQ and About the Developers. */
final class AppInfoDialogs {

    private static final String GOAL_TEXT =
            "Lost and Java-Found aims to provide the Student Affairs Office (SAO) with a more organized, "
                    + "efficient, and accountable system for managing lost and found items within the university. "
                    + "Through a centralized platform, the system enables the secure registration and monitoring "
                    + "of surrendered belongings, matches them with submitted lost-item reports, and facilitates "
                    + "ownership verification before the release of any item. Students also receive timely "
                    + "notifications regarding the progress of their claims and the availability of their "
                    + "belongings for collection. By streamlining the reporting, matching, claiming, and "
                    + "verification processes, Lost and Java-Found promotes transparency, efficiency, and "
                    + "responsible management of lost and found items across the campus.";

    /** Alphabetical by surname: Basconcillo → Dela Cruz → Lopez. */
    private static final String[] DEV_EMAILS = {
            "basconcillo.karlangelo@ue.edu.ph",
            "delacruz.dwyaneallen@ue.edu.ph",
            "lopez.gwenangelynn@ue.edu.ph"
    };

    private AppInfoDialogs() {}

    static void showHelp(Window owner) {
        Dialog<ButtonType> dialog = baseDialog(owner, "Need Help?", "Guide");
        dialog.getDialogPane().setPrefWidth(460);
        dialog.getDialogPane().setPrefHeight(560);

        Label subtitle = new Label("How can we assist you today?");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + UiTheme.TEXT_MUTED + ";");

        VBox menu = new VBox(10);
        menu.getChildren().addAll(
                helpMenuCard(
                        helpIconBook(),
                        "Getting Started",
                        "Learn how to navigate and use the system.",
                        () -> showHelpDetail(owner, "Getting Started",
                                sectionLabel("Quick start"),
                                para("1. Choose Student or Administrator at the top of the login card."),
                                para("2. Students sign in with username, UE email ("
                                        + AccountRules.UE_EMAIL_DOMAIN + "), or 11-digit student number."),
                                para("3. New students use Register Here — email must end in "
                                        + AccountRules.UE_EMAIL_DOMAIN
                                        + ", and password needs 8+ characters with a letter and a number."),
                                para("4. Admin accounts are created by staff (not via Register Here)."),
                                sectionLabel("After you sign in"),
                                para("• File Lost (missing item) or Found (turned in to SAO) reports under My Reports."),
                                para("• Cancel a report only while Pending / Under Review — it becomes Cancelled and stays on file (never deleted)."),
                                para("• SAO may Archive old cases; both Cancelled and Archived remain in the database for audit."),
                                para("• When staff match your Lost report, submit a claim with ownership answers."),
                                para("• If approved, you get a unique pickup code in Notifications — bring that code + UE ID to the desk (no printed slip needed).")
                        )
                ),
                helpMenuCard(
                        helpIconFaq(),
                        "Frequently Asked Questions",
                        "Find answers to common questions.",
                        () -> showHelpDetail(owner, "Frequently Asked Questions",
                                sectionLabel("Accounts"),
                                para("• Signup and profile emails must use " + AccountRules.UE_EMAIL_DOMAIN + " only."),
                                para("• Passwords are stored securely (hashed). Staff can reset a password but cannot view your old one."),
                                sectionLabel("Lost & Found workflow"),
                                para("• Lost report = you are missing an item. Found report = you turned an item in to staff."),
                                para("• Same account may file Lost and Found for different items; the same item cannot be both Lost and Found by you."),
                                para("• Admins register Found reports / walk-ins into inventory — not Lost reports."),
                                para("• You cannot claim your own lost report until staff matches it (70%–100% suggested matches). Then submit an ownership claim with proof."),
                                para("• Admins verify claims privately (inventory is not browsable by students)."),
                                para("• Approved claims send a digital pickup code in-app — bring the code + UE ID to the desk."),
                                para("• No paper pickup slip is required; the unique code is enough for release.")
                        )
                ),
                helpMenuCard(
                        helpIconWrench(),
                        "Troubleshooting",
                        "Resolve common system problems.",
                        () -> showHelpDetail(owner, "Troubleshooting",
                                sectionLabel("Common issues"),
                                para("• Wrong role selected → switch Student / Administrator, then try again."),
                                para("• Forgot password → use Forgot Password? with student # / username and your "
                                        + AccountRules.UE_EMAIL_DOMAIN + " email."),
                                para("• SAO reset your password → log in with the temporary password, then you must "
                                        + "set a new password before the dashboard opens."),
                                para("• Password rejected → use at least 8 characters with one letter and one number."),
                                para("• Too many failed logins → wait about 60 seconds, then try again."),
                                para("• Signed out unexpectedly → idle timeout after "
                                        + IdleSessionGuard.IDLE_MINUTES
                                        + " minutes protects shared campus PCs."),
                                para("• Account inactive → contact Lost & Found / your administrator.")
                        )
                ),
                helpMenuCard(
                        helpIconBug(),
                        "Report a Problem",
                        "Submit an issue or technical concern.",
                        () -> showHelpDetail(owner, "Report a Problem",
                                sectionLabel("Technical issues"),
                                para("Found a bug or something that isn’t working? Email the development team and include what you were doing, any error message, and your role (Student / Administrator)."),
                                sectionLabel("Developer emails"),
                                emailLine(DEV_EMAILS[0]),
                                emailLine(DEV_EMAILS[1]),
                                emailLine(DEV_EMAILS[2])
                        )
                ),
                helpMenuCard(
                        helpIconHeadphones(),
                        "Contact Support",
                        "Get assistance from the administrator.",
                        () -> showHelpDetail(owner, "Contact Support",
                                sectionLabel("Campus support"),
                                para("UE Caloocan Lost & Found desk — bring your UE ID when claiming an item."),
                                para("For account or claim questions, contact Lost & Found / your administrator."),
                                sectionLabel("Development team"),
                                emailLine(DEV_EMAILS[0]),
                                emailLine(DEV_EMAILS[1]),
                                emailLine(DEV_EMAILS[2])
                        )
                )
        );

        VBox body = new VBox(14, subtitle, menu, helpDownHint());
        body.setPadding(new Insets(4, 2, 4, 2));
        body.setAlignment(Pos.TOP_CENTER);

        dialog.getDialogPane().setContent(contentShell(scroll(body, 380)));
        dialog.showAndWait();
    }

    private static void showHelpDetail(Window owner, String title, Node... nodes) {
        Dialog<ButtonType> dialog = baseDialog(owner, title, "Help");
        dialog.getDialogPane().setPrefWidth(460);
        dialog.getDialogPane().setPrefHeight(420);

        VBox body = new VBox(10);
        body.setPadding(new Insets(4, 2, 4, 2));
        body.getChildren().addAll(nodes);

        dialog.getDialogPane().setContent(contentShell(scroll(body, 300)));
        dialog.showAndWait();
    }

    private static HBox helpMenuCard(Node icon, String title, String description, Runnable onOpen) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");

        Label descLbl = new Label(description);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + UiTheme.TEXT_MUTED + ";");

        VBox text = new VBox(3, titleLbl, descLbl);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label chevron = new Label("›");
        chevron.setStyle(
                "-fx-font-size: 20px; -fx-text-fill: " + UiTheme.UE_RED + "; -fx-opacity: 0.55; -fx-padding: 0 2 2 0;"
        );

        StackPane iconBubble = new StackPane(icon);
        iconBubble.setMinSize(36, 36);
        iconBubble.setPrefSize(36, 36);
        iconBubble.setMaxSize(36, 36);
        iconBubble.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED_SOFT + ";"
                + "-fx-background-radius: 10;"
        );

        HBox card = new HBox(12, iconBubble, text, chevron);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 14, 14, 14));
        card.setCursor(Cursor.HAND);
        applyHelpCardStyle(card, chevron, false);

        card.setOnMouseEntered(e -> applyHelpCardStyle(card, chevron, true));
        card.setOnMouseExited(e -> applyHelpCardStyle(card, chevron, false));
        card.setOnMouseClicked(e -> {
            if (onOpen != null) {
                onOpen.run();
            }
        });
        return card;
    }

    private static void applyHelpCardStyle(HBox card, Label chevron, boolean hover) {
        String bg = hover ? UiTheme.ROW_HOVER : UiTheme.FIELD_BG;
        String border = hover ? UiTheme.BORDER_STRONG : UiTheme.BORDER;
        card.setStyle(
                "-fx-background-color: " + bg + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-radius: 12;"
                + "-fx-border-width: 1;"
        );
        chevron.setStyle(
                "-fx-font-size: 20px; -fx-text-fill: " + UiTheme.UE_RED + ";"
                + "-fx-opacity: " + (hover ? "1.0" : "0.55") + "; -fx-padding: 0 2 2 0;"
        );
    }

    private static Label emailLine(String email) {
        Label label = new Label("• " + email);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + UiTheme.UE_RED + "; -fx-font-weight: bold;");
        return label;
    }

    private static StackPane helpDownHint() {
        Circle ring = new Circle(14, Color.web("rgba(228, 17, 53, 0.08)"));
        ring.setStroke(Color.web(UiTheme.UE_RED));
        ring.setStrokeWidth(1.2);
        ring.setOpacity(0.45);

        SVGPath arrow = new SVGPath();
        arrow.setContent("M 8 10 L 14 16 L 20 10");
        arrow.setFill(Color.TRANSPARENT);
        arrow.setStroke(Color.web(UiTheme.UE_RED));
        arrow.setStrokeWidth(1.8);
        arrow.setOpacity(0.55);
        arrow.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        arrow.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        StackPane hint = new StackPane(ring, arrow);
        hint.setPadding(new Insets(6, 0, 2, 0));
        hint.setMouseTransparent(true);
        return hint;
    }

    private static Node helpIconBook() {
        SVGPath book = new SVGPath();
        book.setContent(
                "M 4 6 C 4 6 8 4 14 6 L 14 22 C 8 20 4 22 4 22 Z "
                + "M 24 6 C 24 6 20 4 14 6 L 14 22 C 20 20 24 22 24 22 Z"
        );
        return helpIconWrap(book);
    }

    private static Node helpIconFaq() {
        SVGPath bubble = new SVGPath();
        bubble.setContent(
                "M 5 6 Q 5 4 7 4 L 21 4 Q 23 4 23 6 L 23 16 Q 23 18 21 18 L 11 18 L 7 22 L 8 18 "
                + "Q 5 18 5 16 Z"
        );
        Label q = new Label("?");
        q.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.UE_RED + ";");
        q.setTranslateY(-1);
        strokeHelpIcon(bubble);
        StackPane stack = new StackPane(bubble, q);
        return helpIconBox(stack);
    }

    private static Node helpIconWrench() {
        SVGPath wrench = new SVGPath();
        wrench.setContent(
                "M 18 5 C 16 5 14.5 6 14 7.5 L 8 13.5 L 6.5 12 L 5 13.5 L 8.5 17 "
                + "L 10 15.5 L 8.5 14 L 14.5 8 C 16 7.5 17 7.8 18.5 9 "
                + "C 20.5 11 20 14 17.5 15 L 19 16.5 C 22.5 14.5 23.5 10 21 7.5 C 20.2 6.2 19.2 5 18 5 Z"
        );
        return helpIconWrap(wrench);
    }

    private static Node helpIconBug() {
        SVGPath body = new SVGPath();
        body.setContent(
                "M 14 8 C 11 8 9 10 9 13 L 9 17 C 9 20 11 22 14 22 C 17 22 19 20 19 17 L 19 13 "
                + "C 19 10 17 8 14 8 Z "
                + "M 11 8 C 11 6.5 12.2 5.5 14 5.5 C 15.8 5.5 17 6.5 17 8 "
                + "M 9 12 L 5 10 M 9 15 L 5 15 M 9 18 L 5 20 "
                + "M 19 12 L 23 10 M 19 15 L 23 15 M 19 18 L 23 20"
        );
        return helpIconWrap(body);
    }

    private static Node helpIconHeadphones() {
        SVGPath phones = new SVGPath();
        phones.setContent(
                "M 6 15 L 6 13 C 6 8.5 9.5 5 14 5 C 18.5 5 22 8.5 22 13 L 22 15 "
                + "M 4 15 L 8 15 L 8 21 L 4 21 Z "
                + "M 20 15 L 24 15 L 24 21 L 20 21 Z"
        );
        return helpIconWrap(phones);
    }

    private static Node helpIconWrap(SVGPath path) {
        strokeHelpIcon(path);
        return helpIconBox(path);
    }

    private static void strokeHelpIcon(Shape shape) {
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(Color.web(UiTheme.UE_RED));
        shape.setStrokeWidth(1.7);
        shape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
    }

    private static Node helpIconBox(Node drawing) {
        StackPane box = new StackPane(drawing);
        box.setMinSize(28, 28);
        box.setPrefSize(28, 28);
        box.setMaxSize(28, 28);
        box.setMouseTransparent(true);
        drawing.setScaleX(0.92);
        drawing.setScaleY(0.92);
        return box;
    }

    static void showAboutDevelopers(Window owner) {
        Dialog<ButtonType> dialog = baseDialog(owner, "About the Developers", "Credits");
        dialog.getDialogPane().setPrefWidth(720);
        dialog.getDialogPane().setPrefHeight(560);

        HBox teamRow = new HBox(14);
        teamRow.setAlignment(Pos.TOP_CENTER);
        teamRow.setFillHeight(true);
        teamRow.setMaxWidth(Double.MAX_VALUE);

        // Display order: Dela Cruz → Lopez → Basconcillo
        VBox cardDela = memberPhotoCard(
                "team-dela-cruz",
                "Dela Cruz, Dwyane Allen F.",
                "Project Manager and Lead Developer"
        );
        VBox cardLopez = memberPhotoCard(
                "team-lopez",
                "Lopez, Gwen Angelynn M.",
                "Database and Backend Developer"
        );
        VBox cardBas = memberPhotoCard(
                "team-basconcillo",
                "Basconcillo, Karl Angelo P.",
                "UI/UX Designer and Documentation Lead"
        );

        HBox.setHgrow(cardDela, Priority.ALWAYS);
        HBox.setHgrow(cardLopez, Priority.ALWAYS);
        HBox.setHgrow(cardBas, Priority.ALWAYS);
        teamRow.getChildren().addAll(cardDela, cardLopez, cardBas);

        VBox body = new VBox(16);
        body.setPadding(new Insets(6, 4, 8, 4));
        body.setFillWidth(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().addAll(
                sectionLabel("Development team"),
                teamRow,
                softDivider(),
                sectionLabel("Our Goal"),
                justifiedPara(GOAL_TEXT),
                softDivider(),
                muted("University of the East - Caloocan — Computer Engineering / Software Design Course Project")
        );

        dialog.getDialogPane().setContent(contentShell(scroll(body, 380)));
        dialog.showAndWait();
    }

    private static VBox memberPhotoCard(String imageBase, String name, String role) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(14, 12, 14, 12));
        card.setMinWidth(0);
        card.setPrefWidth(200);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(teamCardStyle());

        card.getChildren().add(portrait(imageBase, 118));

        Label nameLbl = new Label(name);
        nameLbl.setWrapText(true);
        nameLbl.setAlignment(Pos.CENTER);
        nameLbl.setMaxWidth(200);
        nameLbl.setStyle(
                "-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT
                        + "; -fx-text-alignment: center;"
        );

        Label roleLbl = new Label(role);
        roleLbl.setWrapText(true);
        roleLbl.setAlignment(Pos.CENTER);
        roleLbl.setMaxWidth(200);
        roleLbl.setStyle(
                "-fx-font-size: 11.5px; -fx-text-fill: " + UiTheme.TEXT_MUTED
                        + "; -fx-text-alignment: center;"
        );

        card.getChildren().addAll(nameLbl, roleLbl);
        return card;
    }

    private static StackPane portrait(String imageBase, double size) {
        StackPane frame = new StackPane();
        frame.setMinSize(size, size);
        frame.setPrefSize(size, size);
        frame.setMaxSize(size, size);
        frame.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: " + (size / 2) + ";"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-radius: " + (size / 2) + ";"
                + "-fx-border-width: 2;"
        );

        // Load full image (no forced square decode) so aspect ratio stays natural.
        Image image = UeImages.loadImageNamed(imageBase);
        if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
            double diameter = size - 6;
            double iw = image.getWidth();
            double ih = image.getHeight();
            // Cover-fit: scale up enough to fill the circle, then clip — never stretch.
            double scale = Math.max(diameter / iw, diameter / ih);
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setFitWidth(iw * scale);
            view.setFitHeight(ih * scale);

            StackPane clipHost = new StackPane(view);
            clipHost.setAlignment(Pos.CENTER);
            clipHost.setMinSize(diameter, diameter);
            clipHost.setPrefSize(diameter, diameter);
            clipHost.setMaxSize(diameter, diameter);
            clipHost.setClip(new Circle(diameter / 2.0, diameter / 2.0, diameter / 2.0));
            frame.getChildren().add(clipHost);
        } else {
            Label initials = new Label("UE");
            initials.setStyle(
                    "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.UE_RED + ";"
            );
            frame.getChildren().add(initials);
        }
        return frame;
    }

    private static String teamCardStyle() {
        return "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-background-radius: 14;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 14;"
                + "-fx-border-width: 1;";
    }

    private static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.UE_RED + ";"
        );
        return label;
    }

    private static Label muted(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + UiTheme.TEXT_MUTED + ";");
        return label;
    }

    private static Dialog<ButtonType> baseDialog(Window owner, String title, String eyebrow) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        DialogPane pane = dialog.getDialogPane();
        pane.setGraphic(null);
        UeChrome.apply(pane);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setPrefWidth(500);
        pane.setPrefHeight(460);
        pane.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-radius: 14;"
                + "-fx-background-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(165,12,38,0.22), 28, 0.18, 0, 10);"
        );
        pane.setHeader(hero(title, eyebrow));
        dialog.setOnShown(e -> {
            pane.setGraphic(null);
            Button close = (Button) pane.lookupButton(ButtonType.CLOSE);
            if (close != null) {
                close.setText("Close");
                // Same lively UE primary style as Create account / other main actions.
                UiButtons.style(close, UiButtons.Kind.PRIMARY);
            }
            polishButtonBar(pane);
        });
        return dialog;
    }

    private static void polishButtonBar(DialogPane pane) {
        var bar = pane.lookup(".button-bar");
        if (bar != null) {
            bar.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #FFF5F6, #FCE8EC);"
                    + "-fx-padding: 12 16 14 16;"
                    + "-fx-background-radius: 0 0 14 14;"
            );
        }
    }

    private static VBox hero(String title, String eyebrow) {
        Label eye = new Label(eyebrow.toUpperCase());
        eye.setStyle(
                "-fx-font-size: 10.5px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: rgba(255,255,255,0.82);"
        );
        Label titleLbl = new Label(title);
        titleLbl.setWrapText(true);
        titleLbl.setStyle(
                "-fx-font-size: 17px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: white;"
        );
        VBox text = new VBox(4, eye, titleLbl);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(14, UeImages.dialogBrandMark(), text);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(row);
        header.setPadding(new Insets(18, 20, 18, 20));
        header.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F0183C, #C70E2E 55%, #A50C26);"
                + "-fx-background-radius: 14 14 0 0;"
        );
        return header;
    }

    private static VBox contentShell(ScrollPane scroll) {
        VBox shell = new VBox(scroll);
        shell.setPadding(new Insets(14, 16, 8, 16));
        shell.setStyle("-fx-background-color: linear-gradient(to bottom, #FFF8F9, #FFFFFF);");
        return shell;
    }

    private static Separator softDivider() {
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 4 0 4 0;");
        return sep;
    }

    private static ScrollPane scroll(VBox body) {
        return scroll(body, 300);
    }

    private static ScrollPane scroll(VBox body, double prefHeight) {
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefHeight(prefHeight);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private static Label para(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + UiTheme.TEXT_DIM + ";");
        return label;
    }

    private static Label justifiedPara(String content) {
        Label label = new Label(content);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.JUSTIFY);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + UiTheme.TEXT_DIM + ";");
        return label;
    }

    /** Outcome banner — routes to Toast (confirmations still use BrandedDialog). */
    static void info(Window owner, String title, String message) {
        Toast.notify(owner, title, message);
    }
}
