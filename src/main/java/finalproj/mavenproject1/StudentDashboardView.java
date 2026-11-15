
package finalproj.mavenproject1;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * Student dashboard — aligned with Login/Signup UE campus + light glass look.
 * Uses the five main blueprint classes: User, Report, Claim, Inventory, Notification.
 */
public class StudentDashboardView {

    private enum Nav {
        DASHBOARD("Dashboard"),
        REPORTS("My Reports"),
        CLAIMS("My Claims"),
        NOTIFICATIONS("Notifications"),
        PROFILE("Profile"),
        SETTINGS("Settings");

        private final String label;

        Nav(String label) {
            this.label = label;
        }
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy  •  h:mm a", Locale.ENGLISH);

    private final Stage stage;
    private final User user;
    private final BorderPane root;
    private final StackPane contentHost = new StackPane();
    private final Label headerTitle = new Label();
    private final Label headerSubtitle = new Label();
    private final Label unreadBadge = new Label();
    private final Label sidebarUnreadBadge = new Label();

    private Nav currentNav = Nav.DASHBOARD;
    private VBox navButtonsBox;
    private String claimsFilter = null;
    private boolean reportsShowLost = true;

    public StudentDashboardView(Stage stage, User user) {
        this.stage = stage;
        this.user = user;
        this.root = buildShell();
        navigate(Nav.DASHBOARD);
    }

    public BorderPane getRoot() {
        return root;
    }

    private BorderPane buildShell() {
        BorderPane shell = new BorderPane();
        shell.setStyle("-fx-background-color: " + UiTheme.BG_FALLBACK + ";");

        StackPane background = new StackPane();
        Image campus = UeImages.loadCampus();
        if (campus != null) {
            ImageView photo = new ImageView(campus);
            photo.setPreserveRatio(false);
            photo.setSmooth(true);
            photo.fitWidthProperty().bind(shell.widthProperty());
            photo.fitHeightProperty().bind(shell.heightProperty());
            background.getChildren().add(photo);
        } else {
            Region fallback = new Region();
            fallback.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #FCE8EC, #F5F5F7);"
            );
            fallback.prefWidthProperty().bind(shell.widthProperty());
            fallback.prefHeightProperty().bind(shell.heightProperty());
            background.getChildren().add(fallback);
        }

        // Very light wash — keep UE campus clearly visible
        Region wash = new Region();
        wash.setStyle("-fx-background-color: rgba(80, 8, 18, 0.06);");
        wash.prefWidthProperty().bind(shell.widthProperty());
        wash.prefHeightProperty().bind(shell.heightProperty());
        background.getChildren().add(wash);

        BorderPane layout = new BorderPane();
        layout.setLeft(buildSidebar());
        layout.setCenter(buildMainColumn());

        StackPane layered = new StackPane(background, layout);
        shell.setCenter(layered);
        return shell;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(260);
        sidebar.setMaxWidth(260);
        sidebar.setPadding(new Insets(14, 12, 12, 12));
        sidebar.setStyle(
                "-fx-background-color: " + UiTheme.SIDEBAR_BG + ";"
                + "-fx-background-radius: 0 16 16 0;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-width: 0 1 0 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 14, 0, 2, 0);"
        );

        VBox brand = BrandPanel.buildSidebarBrand();

        Region brandRule = new Region();
        brandRule.setPrefHeight(2);
        brandRule.setMaxWidth(Double.MAX_VALUE);
        brandRule.setStyle("-fx-background-color: " + UiTheme.UE_RED + "; -fx-background-radius: 1;");
        VBox.setMargin(brandRule, new Insets(10, 4, 4, 4));

        Label section = new Label("MENU");
        section.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT_DIM + ";"
                + "-fx-font-size: 10px; -fx-font-weight: bold;"
                + "-fx-padding: 12 8 8 8;"
        );

        navButtonsBox = new VBox(3);
        for (Nav nav : Nav.values()) {
            navButtonsBox.getChildren().add(makeNavButton(nav));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox userRow = new HBox(10);
        userRow.setAlignment(Pos.CENTER_LEFT);
        userRow.setPadding(new Insets(12));
        userRow.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED_SOFT + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 12;"
        );
        StackPane avatar = circleAvatar(initials(user.getFullName()), 36);
        VBox userMeta = new VBox(2);
        Label name = new Label(user.getFullName());
        name.setStyle("-fx-text-fill: " + UiTheme.TEXT + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label role = new Label(prettyStatus(user.getUserRole()));
        role.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 11px;");
        userMeta.getChildren().addAll(name, role);
        userRow.getChildren().addAll(avatar, userMeta);

        Button logout = new Button("Logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 12 10 12;"
                + "-fx-cursor: hand;"
        );
        logout.setOnAction(e -> logout());

        sidebar.getChildren().addAll(brand, brandRule, section, navButtonsBox, spacer, userRow, logout);
        VBox.setMargin(logout, new Insets(12, 0, 0, 0));
        refreshUnreadBadges();
        return sidebar;
    }

    private Button makeNavButton(Nav nav) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Node icon = NavIcons.forNav(nav.name());
        icon.setId("nav-icon");

        Label text = new Label(nav.label);
        text.setId("nav-label");
        text.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
        );
        row.getChildren().addAll(icon, text);

        if (nav == Nav.NOTIFICATIONS) {
            sidebarUnreadBadge.setStyle(
                    "-fx-background-color: " + UiTheme.UE_RED + ";"
                    + "-fx-text-fill: white;"
                    + "-fx-background-radius: 10;"
                    + "-fx-font-size: 11px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-padding: 2 7 2 7;"
                    + "-fx-min-width: 20;"
                    + "-fx-alignment: center;"
            );
            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            row.getChildren().addAll(grow, sidebarUnreadBadge);
        }

        Button btn = new Button();
        btn.setGraphic(row);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setUserData(nav);
        btn.setOnAction(e -> navigate(nav));
        styleNavButton(btn, false);
        return btn;
    }

    private void styleNavButton(Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                    "-fx-background-color: linear-gradient(to right, " + UiTheme.UE_RED
                    + ", " + UiTheme.UE_RED_HOVER + ");"
                    + "-fx-background-radius: 10;"
                    + "-fx-padding: 8 10 8 10;"
                    + "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: " + UiTheme.UE_RED_SOFT + ";"
                    + "-fx-background-radius: 10;"
                    + "-fx-border-color: " + UiTheme.BORDER + ";"
                    + "-fx-border-radius: 10;"
                    + "-fx-padding: 8 10 8 10;"
                    + "-fx-cursor: hand;"
            );
        }

        if (btn.getGraphic() instanceof HBox row) {
            for (Node child : row.getChildren()) {
                if ("nav-icon".equals(child.getId())) {
                    NavIcons.applyColor(child, active);
                } else if (child instanceof Label label && "nav-label".equals(label.getId())) {
                    label.setStyle(
                            "-fx-text-fill: " + (active ? "white" : UiTheme.TEXT) + ";"
                            + "-fx-font-size: 13px;"
                            + "-fx-font-weight: bold;"
                    );
                }
            }
        }
    }

    private void refreshNavStyles() {
        for (Node node : navButtonsBox.getChildren()) {
            if (node instanceof Button btn) {
                styleNavButton(btn, btn.getUserData() == currentNav);
            }
        }
    }

    private BorderPane buildMainColumn() {
        BorderPane main = new BorderPane();
        // Extra outer gap so campus peeks around the content shell
        main.setPadding(new Insets(20, 22, 22, 16));

        BorderPane panel = new BorderPane();
        panel.setPadding(new Insets(14, 16, 14, 16));
        panel.setStyle(
                "-fx-background-color: " + UiTheme.PANEL_BG + ";"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 16;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 14, 0, 0, 3);"
        );
        panel.setTop(buildTopBar());
        panel.setCenter(wrapScroll(contentHost));

        main.setCenter(panel);
        return main;
    }

    private HBox buildTopBar() {
        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(2, 2, 12, 2));

        VBox titles = new VBox(2);
        headerTitle.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 24px;"
                + "-fx-font-weight: bold;"
                + "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.75), 6, 0.4, 0, 0);"
        );
        headerSubtitle.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
        );
        headerSubtitle.setWrapText(true);
        titles.getChildren().addAll(headerTitle, headerSubtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button bell = new Button("🔔");
        bell.setStyle(
                "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-background-radius: 20;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 20;"
                + "-fx-min-width: 40; -fx-min-height: 40;"
                + "-fx-cursor: hand;"
        );
        bell.setOnAction(e -> navigate(Nav.NOTIFICATIONS));

        unreadBadge.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED + ";"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 9;"
                + "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 1 6 1 6;"
        );
        StackPane bellWrap = new StackPane(bell, unreadBadge);
        StackPane.setAlignment(unreadBadge, Pos.TOP_RIGHT);

        HBox userChip = new HBox(10);
        userChip.setAlignment(Pos.CENTER_LEFT);
        userChip.setPadding(new Insets(6, 12, 6, 6));
        userChip.setStyle(
                "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-background-radius: 22;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 22;"
        );
        userChip.getChildren().addAll(
                circleAvatar(initials(user.getFullName()), 28),
                labeled(user.getFullName(), UiTheme.TEXT, 12, true)
        );

        top.getChildren().addAll(titles, bellWrap, userChip);
        return top;
    }

    private void navigate(Nav nav) {
        currentNav = nav;
        refreshNavStyles();
        refreshUnreadBadges();

        switch (nav) {
            case DASHBOARD -> {
                setHeader("Dashboard", "Overview of your lost reports and claims.");
                setContent(buildDashboardPage());
            }
            case REPORTS -> {
                setHeader("My Reports", "View and manage your lost-item reports.");
                setContent(buildReportsPage());
            }
            case CLAIMS -> {
                setHeader("My Claims", "Track the status of your ownership claims.");
                setContent(buildClaimsPage());
            }
            case NOTIFICATIONS -> {
                setHeader("Notifications", "Stay updated on reports, matches, and claims.");
                setContent(buildNotificationsPage());
            }
            case PROFILE -> {
                setHeader("Profile", "View and update your account information.");
                setContent(buildProfilePage());
            }
            case SETTINGS -> {
                setHeader("Settings", "Manage password and email preferences.");
                setContent(buildSettingsPage());
            }
        }
    }

    private void setHeader(String title, String subtitle) {
        headerTitle.setText(title);
        headerSubtitle.setText(subtitle);
    }

    private void setContent(Node node) {
        contentHost.getChildren().setAll(node);
    }

    private Notification notificationForUser() {
        Notification n = new Notification();
        n.setUserId(user.getUserId());
        return n;
    }

    private Report reportForUser() {
        Report r = new Report();
        r.setUserId(user.getUserId());
        return r;
    }

    private Claim claimForUser() {
        Claim c = new Claim();
        c.setUserId(user.getUserId());
        return c;
    }

    private void refreshUnreadBadges() {
        int unread = notificationForUser().countUnreadNotifications();
        String text = unread > 9 ? "9+" : String.valueOf(unread);
        unreadBadge.setText(text);
        sidebarUnreadBadge.setText(text);
        unreadBadge.setVisible(unread > 0);
        sidebarUnreadBadge.setVisible(unread > 0);
        unreadBadge.setManaged(unread > 0);
        sidebarUnreadBadge.setManaged(unread > 0);
    }

    // ------------------------------------------------------------------ pages

    private Node buildDashboardPage() {
        List<Report> reports = reportForUser().viewMyReports();
        Claim claimsProbe = claimForUser();
        long pendingReports = reports.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getReportStatus())
                        || "UNDER_REVIEW".equalsIgnoreCase(r.getReportStatus()))
                .count();
        long pendingClaims = claimsProbe.countByStatus("PENDING");
        long approvedClaims = claimsProbe.countByStatus("APPROVED");
        int unread = notificationForUser().countUnreadNotifications();

        HBox stats = new HBox(12);
        stats.getChildren().addAll(
                statCard("Active Reports", String.valueOf(pendingReports), "Open lost-item cases"),
                statCard("Pending Claims", String.valueOf(pendingClaims), "Waiting for review"),
                statCard("Approved", String.valueOf(approvedClaims), "Ready for return"),
                statCard("Unread", String.valueOf(unread), "Notifications")
        );

        VBox recent = sectionCard("Recent activity",
                "Latest updates from your reports and claims.");
        VBox list = new VBox(10);
        notificationForUser().viewNotifications().stream().limit(4).forEach(n ->
                list.getChildren().add(notificationRow(n, false)));
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(muted("No notifications yet."));
        }
        recent.getChildren().add(list);

        HBox actions = new HBox(12);
        Button reportBtn = new Button("+ Report lost item");
        reportBtn.setStyle(UiTheme.primaryButton());
        reportBtn.setOnAction(e -> showReportDialog());
        Button claimsBtn = new Button("View my claims");
        claimsBtn.setStyle(UiTheme.ghostButton());
        claimsBtn.setOnAction(e -> navigate(Nav.CLAIMS));
        actions.getChildren().addAll(reportBtn, claimsBtn);

        return new VBox(18, stats, actions, recent);
    }

    private Node buildReportsPage() {
        VBox page = new VBox(14);

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup group = new ToggleGroup();
        ToggleButton lostTab = pillToggle("Lost Items", true, group);
        ToggleButton foundTab = pillToggle("Recovered", false, group);
        lostTab.setSelected(reportsShowLost);
        foundTab.setSelected(!reportsShowLost);
        lostTab.setOnAction(e -> {
            reportsShowLost = true;
            navigate(Nav.REPORTS);
        });
        foundTab.setOnAction(e -> {
            reportsShowLost = false;
            navigate(Nav.REPORTS);
        });

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Button add = new Button("+ Report Item");
        add.setStyle(UiTheme.primaryButton());
        add.setOnAction(e -> showReportDialog());
        toolbar.getChildren().addAll(lostTab, foundTab, grow, add);

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));

        if (reportsShowLost) {
            tableCard.getChildren().add(tableHeader(
                    "ID", "Item Name", "Category", "Date Reported", "Status", "Actions"
            ));
            List<Report> reports = reportForUser().viewMyReports();
            if (reports.isEmpty()) {
                tableCard.getChildren().add(emptyState("No lost reports yet.",
                        "Submit a report when something goes missing."));
            } else {
                for (Report report : reports) {
                    tableCard.getChildren().add(reportRow(report));
                }
            }
            page.getChildren().addAll(toolbar, tableCard,
                    muted("Showing " + reports.size() + " report" + (reports.size() == 1 ? "" : "s") + "."));
        } else {
            List<Claim> recovered = claimForUser().filterByStatus("APPROVED");
            tableCard.getChildren().add(tableHeader(
                    "Claim ID", "Item Name", "Date", "Status", "Note", "Actions"
            ));
            if (recovered.isEmpty()) {
                tableCard.getChildren().add(emptyState(
                        "No recovered items yet.",
                        "Approved claims appear here. Full found inventory stays private to admins."
                ));
            } else {
                for (Claim claim : recovered) {
                    tableCard.getChildren().add(claimRow(claim));
                }
            }
            page.getChildren().addAll(toolbar, tableCard);
        }
        return page;
    }

    private Node buildClaimsPage() {
        VBox page = new VBox(14);

        HBox filters = new HBox(8);
        filters.getChildren().addAll(
                filterChip("All Claims", claimsFilter == null, () -> {
                    claimsFilter = null;
                    navigate(Nav.CLAIMS);
                }),
                filterChip("Pending", "PENDING".equals(claimsFilter), () -> {
                    claimsFilter = "PENDING";
                    navigate(Nav.CLAIMS);
                }),
                filterChip("Approved", "APPROVED".equals(claimsFilter), () -> {
                    claimsFilter = "APPROVED";
                    navigate(Nav.CLAIMS);
                }),
                filterChip("Rejected", "REJECTED".equals(claimsFilter), () -> {
                    claimsFilter = "REJECTED";
                    navigate(Nav.CLAIMS);
                }),
                filterChip("On Hold", "ON_HOLD".equals(claimsFilter), () -> {
                    claimsFilter = "ON_HOLD";
                    navigate(Nav.CLAIMS);
                })
        );

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.getChildren().add(tableHeader(
                "Claim ID", "Item Name", "Date Submitted", "Status", "Details", "Actions"
        ));

        List<Claim> claims = claimForUser().filterByStatus(claimsFilter);
        if (claims.isEmpty()) {
            tableCard.getChildren().add(emptyState("No claims in this filter.",
                    "Submit a claim from a matched report once available."));
        } else {
            for (Claim claim : claims) {
                tableCard.getChildren().add(claimRow(claim));
            }
        }

        page.getChildren().addAll(filters, tableCard,
                muted("Showing " + claims.size() + " claim" + (claims.size() == 1 ? "" : "s") + "."));
        return page;
    }

    private Node buildNotificationsPage() {
        VBox page = new VBox(14);

        HBox toolbar = new HBox();
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        Button markAll = new Button("✓  Mark all as read");
        markAll.setStyle(UiTheme.linkButton());
        markAll.setOnAction(e -> {
            notificationForUser().markAllAsRead();
            navigate(Nav.NOTIFICATIONS);
        });
        toolbar.getChildren().add(markAll);

        VBox list = new VBox(10);
        List<Notification> notes = notificationForUser().viewNotifications();
        if (notes.isEmpty()) {
            list.getChildren().add(emptyState("You're all caught up.", "New updates will appear here."));
        } else {
            for (Notification note : notes) {
                list.getChildren().add(notificationRow(note, true));
            }
        }

        page.getChildren().addAll(toolbar, list);
        return page;
    }

    private Node buildProfilePage() {
        VBox page = new VBox(16);

        HBox toolbar = new HBox();
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        Button edit = new Button("✎  Edit Profile");
        edit.setStyle(UiTheme.primaryButton());
        edit.setOnAction(e -> showEditProfileDialog());
        toolbar.getChildren().add(edit);

        HBox body = new HBox(22);
        body.setAlignment(Pos.TOP_LEFT);

        VBox avatarCard = new VBox(14);
        avatarCard.setAlignment(Pos.TOP_CENTER);
        avatarCard.setPadding(new Insets(28, 24, 28, 24));
        avatarCard.setPrefWidth(220);
        avatarCard.setStyle(UiTheme.cardStyle());
        avatarCard.getChildren().addAll(
                circleAvatar(initials(user.getFullName()), 88),
                labeled(user.getFullName(), UiTheme.TEXT, 15, true),
                muted(user.getCourse() == null || user.getCourse().isBlank()
                        ? "Student" : user.getCourse() + " • Year " + nullSafe(user.getYearLevel())),
                softButton("Change Avatar", () -> info(
                        "Avatar",
                        "Avatar upload will be available once media storage is connected."
                ))
        );

        VBox infoCard = new VBox(0);
        infoCard.setPadding(new Insets(8, 8, 8, 8));
        infoCard.setStyle(UiTheme.cardStyle());
        HBox.setHgrow(infoCard, Priority.ALWAYS);
        infoCard.getChildren().addAll(
                profileField("Full Name", user.getFullName()),
                profileField("Student Number", nullSafe(user.getStudentNumber())),
                profileField("Course", nullSafe(user.getCourse())),
                profileField("Year Level", nullSafe(user.getYearLevel())),
                profileField("Contact Number", nullSafe(user.getContactNumber())),
                profileField("Email Address", user.getEmail()),
                profileField("Account Status", nullSafe(user.getAccountStatus()))
        );

        body.getChildren().addAll(avatarCard, infoCard);
        page.getChildren().addAll(toolbar, body);
        return page;
    }

    private Node buildSettingsPage() {
        VBox page = new VBox(14);
        page.getChildren().add(settingsTile(
                "🔒",
                "Change Password",
                "Update the password used to sign in to your account.",
                this::showChangePasswordDialog
        ));
        page.getChildren().add(settingsTile(
                "✉",
                "Update Email Address",
                "Change the email linked to notifications and account recovery.",
                this::showChangeEmailDialog
        ));

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 0, 0, 0));
        Button save = new Button("Save Changes");
        save.setStyle(UiTheme.primaryButton());
        save.setOnAction(e -> info("Settings", "Open a setting tile above to make changes."));
        footer.getChildren().add(save);
        page.getChildren().add(footer);
        return page;
    }

    // ------------------------------------------------------------------ rows / widgets

    private HBox tableHeader(String... cols) {
        HBox row = new HBox();
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle(
                "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-width: 0 0 1 0;"
        );
        for (int i = 0; i < cols.length; i++) {
            Label label = new Label(cols[i].toUpperCase(Locale.ENGLISH));
            label.setStyle(
                    "-fx-text-fill: " + UiTheme.TEXT_DIM + ";"
                    + "-fx-font-size: 11px;"
                    + "-fx-font-weight: bold;"
            );
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
            if (i == cols.length - 1) {
                label.setAlignment(Pos.CENTER_RIGHT);
            }
            row.getChildren().add(label);
        }
        return row;
    }

    private HBox reportRow(Report report) {
        HBox row = dataRow();
        row.getChildren().addAll(
                cell(report.getDisplayId(), true),
                cell(report.getItemName(), false),
                cell(shortCategory(report.getCategory()), false),
                cell(DATE_FMT.format(report.getDateSubmitted()), false),
                statusBadge(prettyStatus(report.getReportStatus())),
                actionCell(
                        () -> showReportDetails(report),
                        !"RESOLVED".equalsIgnoreCase(report.getReportStatus())
                                && !"CANCELLED".equalsIgnoreCase(report.getReportStatus())
                                ? () -> cancelReport(report) : null
                )
        );
        return row;
    }

    private HBox claimRow(Claim claim) {
        HBox row = dataRow();
        row.getChildren().addAll(
                cell(claim.getDisplayId(), true),
                cell(claim.getItemName(), false),
                cell(DATE_FMT.format(claim.getClaimDate()), false),
                statusBadge(prettyStatus(claim.getClaimStatus())),
                cell(shortText(nullSafe(claim.getOwnershipAnswer()), 28), false),
                actionCell(() -> showClaimDetails(claim), null)
        );
        return row;
    }

    private HBox dataRow() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle("-fx-border-color: " + UiTheme.BORDER + "; -fx-border-width: 0 0 1 0;");
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: " + UiTheme.ROW_HOVER + ";"
                + "-fx-border-color: " + UiTheme.BORDER + "; -fx-border-width: 0 0 1 0;"
        ));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-border-color: " + UiTheme.BORDER + "; -fx-border-width: 0 0 1 0;"
        ));
        return row;
    }

    private Label cell(String text, boolean emphasize) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 13px;"
                + (emphasize ? "-fx-font-weight: bold;" : "")
        );
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        return label;
    }

    private HBox actionCell(Runnable viewAction, Runnable secondaryAction) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(box, Priority.ALWAYS);
        Button view = new Button("View");
        view.setStyle(UiTheme.linkButton());
        view.setOnAction(e -> viewAction.run());
        box.getChildren().add(view);
        if (secondaryAction != null) {
            Button cancel = new Button("Cancel");
            cancel.setStyle(
                    "-fx-background-color: transparent;"
                    + "-fx-text-fill: " + UiTheme.TEXT_MUTED + ";"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-cursor: hand;"
            );
            cancel.setOnAction(e -> secondaryAction.run());
            box.getChildren().add(cancel);
        }
        return box;
    }

    private StackPane statusBadge(String status) {
        String bg;
        String fg;
        String key = status.toLowerCase(Locale.ENGLISH);
        if (key.contains("pending") || key.contains("review")) {
            bg = UiTheme.STATUS_PENDING_BG;
            fg = UiTheme.STATUS_PENDING_FG;
        } else if (key.contains("approved") || key.contains("matched")) {
            bg = UiTheme.STATUS_APPROVED_BG;
            fg = UiTheme.STATUS_APPROVED_FG;
        } else if (key.contains("reject") || key.contains("cancel")) {
            bg = UiTheme.STATUS_REJECTED_BG;
            fg = UiTheme.STATUS_REJECTED_FG;
        } else if (key.contains("hold")) {
            bg = UiTheme.STATUS_HOLD_BG;
            fg = UiTheme.STATUS_HOLD_FG;
        } else {
            bg = UiTheme.STATUS_RESOLVED_BG;
            fg = UiTheme.STATUS_RESOLVED_FG;
        }
        Label label = new Label(status);
        label.setStyle(
                "-fx-text-fill: " + fg + ";"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 4 10 4 10;"
        );
        StackPane badge = new StackPane(label);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 20;");
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(badge, Priority.ALWAYS);
        return badge;
    }

    private HBox notificationRow(Notification note, boolean clickable) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 18, 16, 18));
        row.setStyle(UiTheme.cardStyle()
                + (note.isRead() ? "" : "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"));

        StackPane icon = new StackPane();
        icon.setPrefSize(40, 40);
        icon.setStyle(
                "-fx-background-color: " + notifColor(note.getNotificationType()) + ";"
                + "-fx-background-radius: 20;"
        );
        Label glyph = new Label(notifGlyph(note.getNotificationType()));
        glyph.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        icon.getChildren().add(glyph);

        VBox text = new VBox(4);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label message = new Label(note.getMessage());
        message.setWrapText(true);
        message.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 13px;"
                + (note.isRead() ? "" : "-fx-font-weight: bold;")
        );
        Label time = new Label(DATE_TIME_FMT.format(note.getSentDate()));
        time.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
        );
        text.getChildren().addAll(message, time);

        row.getChildren().addAll(icon, text);
        if (!note.isRead()) {
            Region dot = new Region();
            dot.setPrefSize(8, 8);
            dot.setMinSize(8, 8);
            dot.setStyle("-fx-background-color: " + UiTheme.UE_RED + "; -fx-background-radius: 4;");
            row.getChildren().add(dot);
        }

        if (clickable) {
            row.setOnMouseClicked(e -> {
                Notification mark = new Notification();
                mark.setNotificationId(note.getNotificationId());
                mark.setUserId(user.getUserId());
                mark.markAsRead();
                navigate(Nav.NOTIFICATIONS);
            });
            row.setStyle(row.getStyle() + "-fx-cursor: hand;");
        }
        return row;
    }

    private VBox sectionCard(String title, String subtitle) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(UiTheme.cardStyle());
        card.getChildren().addAll(
                labeled(title, UiTheme.TEXT, 15, true),
                muted(subtitle)
        );
        return card;
    }

    private VBox statCard(String label, String value, String hint) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setPrefWidth(160);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle(UiTheme.cardStyle());
        card.getChildren().addAll(
                muted(label),
                labeled(value, UiTheme.TEXT, 24, true),
                muted(hint)
        );
        return card;
    }

    private HBox settingsTile(String glyph, String title, String subtitle, Runnable action) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20, 22, 20, 22));
        row.setStyle(UiTheme.cardStyle() + "-fx-cursor: hand;");

        StackPane icon = new StackPane(new Label(glyph));
        icon.setPrefSize(44, 44);
        icon.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED_SOFT + ";"
                + "-fx-background-radius: 12;"
        );
        ((Label) icon.getChildren().get(0)).setStyle("-fx-font-size: 16px;");

        VBox text = new VBox(4);
        HBox.setHgrow(text, Priority.ALWAYS);
        text.getChildren().addAll(
                labeled(title, UiTheme.TEXT, 15, true),
                muted(subtitle)
        );

        Label chevron = new Label("›");
        chevron.setStyle("-fx-text-fill: " + UiTheme.TEXT_DIM + "; -fx-font-size: 22px;");

        row.getChildren().addAll(icon, text, chevron);
        row.setOnMouseClicked(e -> action.run());
        return row;
    }

    private VBox profileField(String label, String value) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setStyle("-fx-border-color: " + UiTheme.BORDER + "; -fx-border-width: 0 0 1 0;");
        box.getChildren().addAll(
                muted(label),
                labeled(value == null || value.isBlank() ? "—" : value, UiTheme.TEXT, 14, false)
        );
        return box;
    }

    private VBox emptyState(String title, String subtitle) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(42, 24, 42, 24));
        box.getChildren().addAll(
                labeled(title, UiTheme.TEXT, 15, true),
                muted(subtitle)
        );
        return box;
    }

    private ToggleButton pillToggle(String text, boolean activeLook, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(activeLook);
        stylePill(btn, activeLook);
        btn.selectedProperty().addListener((o, a, selected) -> stylePill(btn, selected));
        return btn;
    }

    private void stylePill(ToggleButton btn, boolean selected) {
        if (selected) {
            btn.setStyle(
                    "-fx-background-color: " + UiTheme.UE_RED + ";"
                    + "-fx-text-fill: white;"
                    + "-fx-font-weight: bold;"
                    + "-fx-background-radius: 20;"
                    + "-fx-padding: 8 16 8 16;"
                    + "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                    + "-fx-text-fill: " + UiTheme.TEXT + ";"
                    + "-fx-font-weight: bold;"
                    + "-fx-background-radius: 20;"
                    + "-fx-border-color: " + UiTheme.BORDER + ";"
                    + "-fx-border-radius: 20;"
                    + "-fx-padding: 8 16 8 16;"
                    + "-fx-cursor: hand;"
            );
        }
    }

    private Button filterChip(String text, boolean active, Runnable action) {
        Button btn = new Button(text);
        if (active) {
            btn.setStyle(
                    "-fx-background-color: " + UiTheme.UE_RED + ";"
                    + "-fx-text-fill: white;"
                    + "-fx-font-weight: bold;"
                    + "-fx-background-radius: 18;"
                    + "-fx-padding: 8 14 8 14;"
                    + "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                    + "-fx-text-fill: " + UiTheme.TEXT + ";"
                    + "-fx-font-weight: bold;"
                    + "-fx-background-radius: 18;"
                    + "-fx-border-color: " + UiTheme.BORDER + ";"
                    + "-fx-border-radius: 18;"
                    + "-fx-padding: 8 14 8 14;"
                    + "-fx-cursor: hand;"
            );
        }
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Button softButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(UiTheme.ghostButton());
        btn.setOnAction(e -> action.run());
        return btn;
    }

    // ------------------------------------------------------------------ dialogs

    private void showReportDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Report Lost Item");
        TextField itemName = dialog.field("Item name");
        ComboBox<String> category = dialog.combo("Category",
                "Devices", "Personal Belongings");
        TextField subCategory = dialog.field("Subcategory");
        TextField color = dialog.field("Color");
        TextField brand = dialog.field("Brand (optional)");
        TextField location = dialog.field("Last known location");
        TextArea description = dialog.area("Description");

        dialog.show(() -> {
            if (itemName.getText().isBlank() || location.getText().isBlank()
                    || description.getText().isBlank()) {
                info("Incomplete", "Please fill in item name, location, and description.");
                return false;
            }
            Report report = new Report();
            report.setUserId(user.getUserId());
            report.setItemName(itemName.getText().trim());
            report.setCategory(category.getValue());
            report.setSubCategory(subCategory.getText().trim());
            report.setColor(color.getText().trim());
            report.setBrand(brand.getText().trim());
            report.setDescription(description.getText().trim());
            report.setLostLocation(location.getText().trim());
            report.setLostDate(java.time.LocalDate.now());
            report.setReportStatus("PENDING");
            if (!report.submitReport()) {
                info("Failed", "Could not submit report.");
                return false;
            }
            Notification note = new Notification();
            note.setUserId(user.getUserId());
            note.setReportId(report.getReportId());
            note.setMessage("Your lost-item report (" + report.getItemName() + ") was submitted.");
            note.notifyReportUpdate();
            info("Report submitted", "Your lost-item report was filed successfully.");
            navigate(Nav.REPORTS);
            return true;
        });
    }

    private void showEditProfileDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Edit Profile");
        TextField fullName = dialog.field("Full name", user.getFullName());
        TextField studentNumber = dialog.field("Student number", nullSafe(user.getStudentNumber()));
        TextField course = dialog.field("Course", nullSafe(user.getCourse()));
        TextField year = dialog.field("Year level", nullSafe(user.getYearLevel()));
        TextField contact = dialog.field("Contact number", nullSafe(user.getContactNumber()));
        TextField email = dialog.field("Email", user.getEmail());

        dialog.show(() -> {
            if (fullName.getText().isBlank()) {
                info("Invalid", "Full name is required.");
                return false;
            }
            user.setFullName(fullName.getText().trim());
            user.setStudentNumber(studentNumber.getText().trim());
            user.setCourse(course.getText().trim());
            user.setYearLevel(year.getText().trim());
            user.setContactNumber(contact.getText().trim());
            user.setEmail(email.getText().trim());
            if (!user.updateProfile()) {
                info("Failed", "Could not update profile.");
                return false;
            }
            navigate(Nav.PROFILE);
            return true;
        });
    }

    private void showChangePasswordDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Change Password");
        PasswordField current = dialog.password("Current password");
        PasswordField next = dialog.password("New password");
        PasswordField confirm = dialog.password("Confirm new password");
        dialog.show(() -> {
            if (!user.getPasswordHash().equals(current.getText())) {
                info("Incorrect password", "Current password does not match.");
                return false;
            }
            if (next.getText().isBlank() || !next.getText().equals(confirm.getText())) {
                info("Invalid", "New passwords must match and not be empty.");
                return false;
            }
            user.setPasswordHash(next.getText());
            if (!user.changePassword()) {
                info("Failed", "Could not change password.");
                return false;
            }
            info("Password updated", "Your password was changed successfully.");
            return true;
        });
    }

    private void showChangeEmailDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Update Email");
        TextField email = dialog.field("New email", user.getEmail());
        dialog.show(() -> {
            if (email.getText().isBlank() || !email.getText().contains("@")) {
                info("Invalid email", "Please enter a valid email address.");
                return false;
            }
            user.setEmail(email.getText().trim());
            if (!user.updateProfile()) {
                info("Failed", "Could not update email.");
                return false;
            }
            info("Email updated", "Your email was changed to " + email.getText().trim());
            navigate(Nav.SETTINGS);
            return true;
        });
    }

    private void showReportDetails(Report report) {
        report.viewReport();
        info("Report " + report.getDisplayId(),
                "Item: " + report.getItemName()
                + "\nCategory: " + report.getCategory()
                + "\nSubcategory: " + nullSafe(report.getSubCategory())
                + "\nColor: " + nullSafe(report.getColor())
                + "\nBrand: " + nullSafe(report.getBrand())
                + "\nLocation: " + nullSafe(report.getLostLocation())
                + "\nStatus: " + prettyStatus(report.getReportStatus())
                + "\nSubmitted: " + DATE_FMT.format(report.getDateSubmitted())
                + "\n\n" + nullSafe(report.getDescription()));
    }

    private void showClaimDetails(Claim claim) {
        claim.viewClaimDetails();
        info("Claim " + claim.getDisplayId(),
                "Item: " + claim.getItemName()
                + "\nStatus: " + prettyStatus(claim.getClaimStatus())
                + "\nSubmitted: " + DATE_FMT.format(claim.getClaimDate())
                + "\nAdmin result: " + nullSafe(claim.getAdminResult())
                + "\nAdmin note: " + nullSafe(claim.getAdminNote())
                + "\nOwnership answer:\n" + nullSafe(claim.getOwnershipAnswer()));
    }

    private void cancelReport(Report report) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel report");
        confirm.setHeaderText("Cancel " + report.getDisplayId() + "?");
        confirm.setContentText("This will mark the report as cancelled.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                Report target = new Report();
                target.setReportId(report.getReportId());
                target.setUserId(user.getUserId());
                target.deleteReport();
                navigate(Nav.REPORTS);
            }
        });
    }

    private void logout() {
        user.logout();
        LoginView loginView = new LoginView(stage);
        stage.setTitle("Lost and Java-Found — Login");
        stage.setScene(new javafx.scene.Scene(loginView.getRoot(), 1100, 680));
    }

    // ------------------------------------------------------------------ helpers

    private static ScrollPane wrapScroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: transparent;"
                + "-fx-background-color: transparent;"
                + "-fx-border-color: transparent;"
        );
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        if (scroll.getContent() != null) {
            scroll.getContent().setStyle("-fx-background-color: transparent;");
        }
        return scroll;
    }

    private static StackPane circleAvatar(String text, double size) {
        Circle circle = new Circle(size / 2.0);
        circle.setFill(javafx.scene.paint.Color.web(UiTheme.UE_RED));
        Label initials = new Label(text);
        initials.setStyle(
                "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: " + Math.max(11, size / 3.2) + "px;"
        );
        StackPane stack = new StackPane(circle, initials);
        stack.setPrefSize(size, size);
        stack.setMaxSize(size, size);
        return stack;
    }

    private static Label labeled(String text, String color, double size, boolean bold) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(
                "-fx-text-fill: " + color + ";"
                + "-fx-font-size: " + size + "px;"
                + "-fx-font-weight: bold;"
        );
        return label;
    }

    private static Label muted(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
        );
        return label;
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ENGLISH);
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ENGLISH);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String prettyStatus(String status) {
        if (status == null || status.isBlank()) return "—";
        String[] parts = status.toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static String shortCategory(String category) {
        if (category == null) return "—";
        if (category.toLowerCase(Locale.ENGLISH).contains("device")) return "Devices";
        if (category.toLowerCase(Locale.ENGLISH).contains("personal")) return "Personal";
        return category;
    }

    private static String shortText(String text, int max) {
        if (text == null || text.isBlank()) return "—";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static String notifGlyph(String type) {
        if (type == null) return "•";
        return switch (type.toUpperCase(Locale.ENGLISH)) {
            case "CLAIM_DECISION" -> "i";
            case "POSSIBLE_MATCH" -> "✓";
            case "ALERT", "ITEM_NOT_REGISTERED" -> "!";
            case "RETURN" -> "↩";
            default -> "•";
        };
    }

    private static String notifColor(String type) {
        if (type == null) return "#F59E0B";
        return switch (type.toUpperCase(Locale.ENGLISH)) {
            case "CLAIM_DECISION" -> "#3B82F6";
            case "POSSIBLE_MATCH" -> "#22C55E";
            case "ALERT", "ITEM_NOT_REGISTERED" -> UiTheme.UE_RED;
            case "RETURN" -> "#8B5CF6";
            default -> "#F59E0B";
        };
    }

    private static void info(String title, String message) {
        Alert tip = new Alert(Alert.AlertType.INFORMATION);
        tip.setTitle(title);
        tip.setHeaderText(null);
        tip.setContentText(message);
        tip.showAndWait();
    }


    private static final class DialogPaneBuilder {
        private final Alert alert;
        private final GridPane grid = new GridPane();
        private int row = 0;

        DialogPaneBuilder(String title) {
            alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(title);
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(8, 4, 4, 4));
            ColumnConstraints c0 = new ColumnConstraints();
            c0.setPercentWidth(32);
            ColumnConstraints c1 = new ColumnConstraints();
            c1.setPercentWidth(68);
            grid.getColumnConstraints().addAll(c0, c1);
            alert.getDialogPane().setContent(grid);
            alert.getDialogPane().setPrefWidth(420);
        }

        TextField field(String label) {
            return field(label, "");
        }

        TextField field(String label, String value) {
            TextField tf = new TextField(value);
            add(label, tf);
            return tf;
        }

        PasswordField password(String label) {
            PasswordField pf = new PasswordField();
            add(label, pf);
            return pf;
        }

        TextArea area(String label) {
            TextArea area = new TextArea();
            area.setPrefRowCount(3);
            area.setWrapText(true);
            add(label, area);
            return area;
        }

        ComboBox<String> combo(String label, String... values) {
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().addAll(values);
            combo.getSelectionModel().selectFirst();
            combo.setMaxWidth(Double.MAX_VALUE);
            add(label, combo);
            return combo;
        }

        private void add(String label, Node node) {
            grid.add(new Label(label), 0, row);
            grid.add(node, 1, row);
            row++;
        }

        void show(java.util.function.Supplier<Boolean> onConfirm) {
            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    onConfirm.get();
                }
            });
        }
    }
}