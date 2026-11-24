
package finalproj.mavenproject1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
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
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Administrator dashboard — same UE campus + light glass look as the student portal.
 */
public class AdminDashboardView {

    private enum Nav {
        DASHBOARD("Dashboard"),
        MANAGE_REPORTS("Manage Reports"),
        MANAGE_CLAIMS("Manage Claims"),
        INVENTORY("Manage Inventory"),
        USERS("Users Management"),
        ANALYTICS("Reports & Analytics"),
        SETTINGS("Settings");

        private final String label;

        Nav(String label) {
            this.label = label;
        }
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final Stage stage;
    private final User user;
    private final BorderPane root;
    private final StackPane contentHost = new StackPane();
    private final Label headerTitle = new Label();
    private final Label headerSubtitle = new Label();

    private Nav currentNav = Nav.DASHBOARD;
    private VBox navButtonsBox;
    private String claimsFilter = null;
    /** null = all, LOST, or FOUND — Manage Reports type split. */
    private String reportsTypeFilter = null;
    /** ACTIVE (default) | CANCELLED | ARCHIVED | ALL — soft-closed rows stay in MySQL. */
    private String reportsRecordFilter = "ACTIVE";
    private String reportsSearchKey = "";
    private String inventorySearchKey = "";
    private String inventoryStatusFilter = null;

    /** Date-range filters (additive; null bounds = all dates). */
    private DateRanges.Preset reportsDatePreset = DateRanges.Preset.ALL;
    private LocalDate reportsFrom = null;
    private LocalDate reportsTo = null;
    private DateRanges.Preset claimsDatePreset = DateRanges.Preset.ALL;
    private LocalDate claimsFrom = null;
    private LocalDate claimsTo = null;
    private DateRanges.Preset analyticsDatePreset = DateRanges.Preset.MONTH;
    private LocalDate analyticsFrom = LocalDate.now().withDayOfMonth(1);
    private LocalDate analyticsTo = LocalDate.now();

    public AdminDashboardView(Stage stage, User user) {
        this.stage = stage;
        this.user = user;
        this.root = buildShell();
        navigate(Nav.DASHBOARD);
    }

    public BorderPane getRoot() {
        return root;
    }

    // ------------------------------------------------------------------ shell

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

        VBox brand = BrandPanel.buildSidebarBrand("Admin Portal");

        Region brandRule = new Region();
        brandRule.setPrefHeight(2);
        brandRule.setMaxWidth(Double.MAX_VALUE);
        brandRule.setStyle("-fx-background-color: " + UiTheme.UE_RED + "; -fx-background-radius: 1;");
        VBox.setMargin(brandRule, new Insets(6, 4, 2, 4));

        Label section = new Label("MENU");
        section.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT_DIM + ";"
                + "-fx-font-size: 10px; -fx-font-weight: bold;"
                + "-fx-padding: 8 8 6 8;"
        );

        navButtonsBox = new VBox(8);
        for (Nav nav : Nav.values()) {
            navButtonsBox.getChildren().add(makeNavButton(nav));
        }

        ScrollPane navScroll = new ScrollPane(navButtonsBox);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        navScroll.setMinHeight(260);
        navScroll.setStyle(
                "-fx-background: transparent;"
                + "-fx-background-color: transparent;"
                + "-fx-border-color: transparent;"
                + "-fx-padding: 0;"
        );
        if (navScroll.getContent() != null) {
            navScroll.getContent().setStyle("-fx-background-color: transparent;");
        }
        VBox.setVgrow(navScroll, Priority.ALWAYS);

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
        Label role = new Label("Administrator");
        role.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 11px;");
        userMeta.getChildren().addAll(name, role);
        userRow.getChildren().addAll(avatar, userMeta);

        Button logout = new Button("Logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setStyle(UiTheme.primaryButton());
        logout.setOnAction(e -> logout());

        // Keep profile + logout pinned at the bottom (always visible).
        VBox footer = new VBox(12, userRow, logout);
        footer.setMinHeight(Region.USE_PREF_SIZE);

        sidebar.getChildren().addAll(brand, brandRule, section, navScroll, footer);
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
        btn.getStyleClass().removeAll("ue-nav", "ue-nav-idle", "ue-nav-active");
        btn.getStyleClass().addAll("ue-nav", active ? "ue-nav-active" : "ue-nav-idle");
        btn.setFocusTraversable(false);
        // Clear inline style so CSS :focused/:armed stay red (not Modena white).
        btn.setStyle("");

        if (btn.getGraphic() instanceof HBox graphic) {
            for (Node child : graphic.getChildren()) {
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
        );
        headerSubtitle.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
        );
        headerSubtitle.setWrapText(true);
        titles.getChildren().addAll(headerTitle, headerSubtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);

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

        top.getChildren().addAll(titles, userChip);
        return top;
    }

    private void navigate(Nav nav) {
        currentNav = nav;
        refreshNavStyles();

        switch (nav) {
            case DASHBOARD -> {
                setHeader(
                        "Welcome back, " + user.getFullName() + "!",
                        "University of the East - Caloocan — Admin"
                );
                setContent(buildDashboardPage());
            }
            case MANAGE_REPORTS -> {
                setHeader("Manage Reports", "Review and update all lost-item reports.");
                setContent(buildManageReportsPage());
            }
            case MANAGE_CLAIMS -> {
                setHeader("Manage Claims",
                        "Approve, reject, hold/resume, or confirm pickup. Claims with no registered item auto-hold; admin can still decide.");
                setContent(buildManageClaimsPage());
            }
            case INVENTORY -> {
                setHeader("Manage Inventory", "Register and track found items.");
                setContent(buildInventoryPage());
            }
            case USERS -> {
                setHeader("Users Management", "Activate or deactivate student accounts.");
                setContent(buildUsersPage());
            }
            case ANALYTICS -> {
                setHeader("Reports & Analytics", "Summary counts and export.");
                setContent(buildAnalyticsPage());
            }
            case SETTINGS -> {
                setHeader("Settings", "Manage password and email.");
                setContent(buildSettingsPage());
            }
        }
    }

    private void setHeader(String title, String subtitle) {
        headerTitle.setText(title);
        headerSubtitle.setText(subtitle);
    }

    private void setContent(Node node) {
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            StackPane.setAlignment(node, Pos.TOP_CENTER);
        }
        contentHost.getChildren().setAll(node);
    }

    // ------------------------------------------------------------------ pages

    private Node buildDashboardPage() {
        Report allReports = new Report();
        allReports.setUserId(0);
        int totalReports = allReports.filterReports().size();
        int totalClaims = Claim.viewAllClaims().size();
        int totalUsers = User.listAccounts().size();

        Inventory returnedProbe = new Inventory();
        returnedProbe.setItemStatus("RETURNED");
        long invReturned = returnedProbe.filterItems().size();
        long claimReturns = Claim.viewAllClaims().stream()
                .filter(c -> c.getReturnDate() != null)
                .count();
        long returnedItems = invReturned + claimReturns;

        HBox stats = new HBox(12);
        stats.getChildren().addAll(
                statCard("Total Reports", String.valueOf(totalReports), "All filed reports", UiTheme.UE_RED, "DOCUMENT"),
                statCard("Total Claims", String.valueOf(totalClaims), "Student ownership requests", "#EAB308", "SHIELD"),
                statCard("Returned Items", String.valueOf(returnedItems), "Confirmed handovers", "#22C55E", "BOX"),
                statCard("Total Users", String.valueOf(totalUsers), "Registered accounts", "#3B82F6", "USERS")
        );

        // Mid row: clickable pie overview + recent activity
        HBox mid = new HBox(14);
        long lostOnly = allReports.filterReports().stream().filter(Report::isLostReport).count();
        Node pieCard = buildReportsOverviewPie(lostOnly, returnedItems);
        HBox.setHgrow(pieCard, Priority.ALWAYS);

        VBox recent = sectionCard("Recent Activity", "Latest claim and report updates.");
        HBox.setHgrow(recent, Priority.ALWAYS);
        List<ActivityItem> items = new ArrayList<>();
        for (Claim claim : Claim.viewAllClaims()) {
            items.add(new ActivityItem(
                    claim.getItemName(),
                    prettyStatus(claim.getClaimStatus()),
                    claim.getClaimDate()
            ));
        }
        for (Report report : allReports.filterReports()) {
            items.add(new ActivityItem(
                    report.getItemName(),
                    reportStatusLabel(report),
                    report.getDateSubmitted()
            ));
        }
        items.sort(Comparator.comparing(ActivityItem::date).reversed());
        if (items.isEmpty()) {
            recent.getChildren().add(muted("No activity yet."));
        } else {
            GridPane table = newActivityTable();
            int r = 0;
            r = addTableHeader(table, r, "Item", "Status", "Date");
            for (ActivityItem item : items.stream().limit(8).toList()) {
                r = addActivityRow(table, r, item);
            }
            recent.getChildren().add(table);
        }

        mid.getChildren().addAll(pieCard, recent);

        // Quick Actions sit below Reports Overview (mockup layout)
        Node quickActions = buildQuickActionsPanel();
        Node todaysPickups = buildTodaysPickupsPanel();
        Node aging = buildAgingAlertsPanel();

        return new VBox(18, stats, mid, aging, todaysPickups, quickActions);
    }

    /** Additive SLA panel — open Lost reports / Pending claims aging past 3 or 7 days. */
    private Node buildAgingAlertsPanel() {
        List<CampusInsights.AgingItem> alerts = CampusInsights.agingAlerts();
        VBox panel = sectionCard(
                "Needs attention (aging)",
                alerts.isEmpty()
                        ? "No open Lost reports or Pending claims older than 3 days."
                        : "Items waiting longer than usual — prioritize desk follow-up."
        );
        if (alerts.isEmpty()) {
            panel.getChildren().add(muted("All open cases are within the 3-day window."));
            return panel;
        }
        VBox list = new VBox(8);
        int shown = 0;
        for (CampusInsights.AgingItem a : alerts) {
            if (shown >= 6) break;
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            boolean critical = "Critical".equals(a.severity());
            row.setStyle(
                    "-fx-background-color: " + (critical ? "rgba(254,226,226,0.88)" : UiTheme.FIELD_BG) + ";"
                    + "-fx-background-radius: 10;"
                    + "-fx-border-color: " + (critical ? UiTheme.STATUS_REJECTED_FG : UiTheme.BORDER) + ";"
                    + "-fx-border-radius: 10;"
            );
            VBox text = new VBox(2);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label title = new Label(a.kind() + " · " + a.id() + " — " + a.title());
            title.setWrapText(true);
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");
            Label meta = new Label(a.daysOpen() + " day" + (a.daysOpen() == 1 ? "" : "s")
                    + " open · " + a.severity()
                    + (critical ? " (7+ days)" : " (3+ days)"));
            meta.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
            text.getChildren().addAll(title, meta);
            Button go = UiButtons.link(
                    a.kind().startsWith("Claim") ? "Open claim" : "Open report",
                    () -> openAgingItem(a)
            );
            row.getChildren().addAll(text, go);
            list.getChildren().add(row);
            shown++;
        }
        panel.getChildren().add(list);
        return panel;
    }

    /** Additive dashboard card — approved claims awaiting pickup today / overall. */
    private Node buildTodaysPickupsPanel() {
        List<Claim> awaiting = Claim.viewAllClaims().stream()
                .filter(c -> c.getClaimStatus() != null
                        && "APPROVED".equalsIgnoreCase(c.getClaimStatus())
                        && c.getReturnDate() == null)
                .sorted(Comparator.comparing(Claim::getClaimDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        VBox panel = sectionCard("Today's pickups",
                awaiting.isEmpty()
                        ? "No approved claims waiting for personal pickup."
                        : "Approved claims ready for desk handover — or look up by pickup code under Manage Claims.");
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.getChildren().add(UiButtons.primary("Lookup code", () -> navigate(Nav.MANAGE_CLAIMS)));
        toolbar.getChildren().add(UiButtons.primaryIcon("Refresh", "REFRESH",
                () -> navigate(Nav.DASHBOARD)));
        panel.getChildren().add(toolbar);

        if (awaiting.isEmpty()) {
            panel.getChildren().add(muted("When you approve a claim, it appears here until pickup is confirmed."));
            return panel;
        }

        VBox list = new VBox(8);
        int shown = 0;
        for (Claim claim : awaiting) {
            if (shown >= 5) break;
            String student = User.findById(claim.getUserId())
                    .map(User::getFullName).orElse("Student #" + claim.getUserId());
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            row.setStyle(
                    "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                    + "-fx-background-radius: 10;"
                    + "-fx-border-color: " + UiTheme.BORDER + ";"
                    + "-fx-border-radius: 10;"
            );
            VBox text = new VBox(2);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label title = new Label(claim.getDisplayId() + " — " + claim.getItemName());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");
            String code = claim.getPickupCode() == null || claim.getPickupCode().isBlank()
                    ? "no code yet"
                    : claim.getPickupCode();
            Label meta = new Label(student + " · code " + code + " · approved "
                    + (claim.getClaimDate() == null ? "—" : DATE_FMT.format(claim.getClaimDate())));
            meta.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
            text.getChildren().addAll(title, meta);
            Button open = UiButtons.link("Open", () -> showClaimDetails(claim));
            Button copy = UiButtons.primary("Copy code", () -> {
                Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
                if (!live.ensurePickupCode()) {
                    info("Failed", "Could not create or load a pickup code for this claim.");
                    return;
                }
                Claim refreshed = Claim.findById(live.getClaimId()).orElse(live);
                copyPickupCode(refreshed.getPickupCode());
            });
            Button resend = UiButtons.primary("Resend code", () -> {
                Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
                if (!live.ensurePickupCode()) {
                    info("Failed", "Could not create or load a pickup code for this claim.");
                    return;
                }
                Claim refreshed = Claim.findById(live.getClaimId()).orElse(live);
                ClaimSlipPrinter.sendPickupNoticeToStudent(refreshed);
                Toast.show(stage, "Pickup code " + refreshed.getPickupCode() + " sent to student.");
            });
            row.getChildren().addAll(text, open, copy, resend);
            list.getChildren().add(row);
            shown++;
        }
        panel.getChildren().add(list);
        return panel;
    }

    /**
     * Found reports waiting to be logged into inventory (student turned an item in).
     * This is the only report type that may use Register → Manage Inventory.
     */
    private Node buildFoundTipsToRegisterPanel() {
        List<Report> tips = Report.listFoundTipsNeedingRegistration();
        VBox panel = sectionCard(
                "Found reports to register",
                tips.isEmpty()
                        ? "No open Found reports. Walk-in finds can still be registered from Manage Inventory."
                        : "Students turned these items in — register them into inventory (not Lost reports)."
        );
        if (tips.isEmpty()) {
            panel.getChildren().add(muted("Lost reports never appear here — they are matched, not registered."));
            return panel;
        }
        VBox list = new VBox(8);
        int shown = 0;
        for (Report report : tips) {
            if (shown >= 5) break;
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            row.setStyle(
                    "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                    + "-fx-background-radius: 10;"
                    + "-fx-border-color: " + UiTheme.BORDER + ";"
                    + "-fx-border-radius: 10;"
            );
            VBox text = new VBox(2);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label title = new Label(report.getDisplayId() + " — " + report.getItemName());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");
            Label meta = new Label("Found report · " + nullSafe(report.getCategory())
                    + " · " + nullSafe(report.getColor())
                    + " · " + nullSafe(report.getLostLocation()));
            meta.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
            text.getChildren().addAll(title, meta);
            Button register = new Button("Register");
            register.setStyle(UiTheme.primaryButton());
            register.setOnAction(e -> showRegisterItemDialog(report));
            row.getChildren().addAll(text, register);
            list.getChildren().add(row);
            shown++;
        }
        panel.getChildren().add(list);
        return panel;
    }

    /**
     * Lost reports that already overlap available inventory —
     * match to existing found items only (never create inventory from a Lost report).
     */
    private Node buildSuggestedMatchesPanel() {
        List<Report> candidates = Report.listLostReportsNeedingMatch();
        VBox panel = sectionCard(
                "Suggested matches (Lost → inventory)",
                candidates.isEmpty()
                        ? "No pending lost reports currently overlap with available inventory."
                        : "Lost reports with possible found items already in inventory — match, do not register."
        );

        if (candidates.isEmpty()) {
            panel.getChildren().add(muted("After a Found report or walk-in is registered, matching Lost reports appear here."));
            return panel;
        }

        VBox list = new VBox(8);
        int shown = 0;
        for (Report report : candidates) {
            if (shown >= 6) break;
            java.util.List<MatchScorer.ScoredMatch> ranked = MatchScorer.rankForReport(report);
            MatchScorer.ScoredMatch top = ranked.isEmpty() ? null : ranked.get(0);
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            String glow = top == null ? UiTheme.BORDER : matchGlowColor(top.score());
            String tint = top == null ? UiTheme.FIELD_BG : matchGlowSoftBg(top.score());
            row.setStyle(
                    "-fx-background-color: " + tint + ";"
                    + "-fx-background-radius: 10;"
                    + "-fx-border-color: " + glow + ";"
                    + "-fx-border-radius: 10;"
                    + "-fx-border-width: 1.4;"
            );
            if (top != null) {
                DropShadow rowGlow = new DropShadow();
                rowGlow.setColor(Color.web(glow).deriveColor(0, 1, 1, 0.35));
                rowGlow.setRadius(12);
                rowGlow.setSpread(0.18);
                row.setEffect(rowGlow);
            }
            VBox text = new VBox(2);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label title = new Label(report.getDisplayId() + " — " + report.getItemName());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");
            Label meta = new Label(
                    ranked.size() + " candidate" + (ranked.size() == 1 ? "" : "s")
                    + " · " + nullSafe(report.getCategory())
                    + " · " + nullSafe(report.getColor())
            );
            meta.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
            text.getChildren().addAll(title, meta);
            if (top != null) {
                Label why = new Label(top.item().getTrackingNumber() + " — " + top.reasonLine());
                why.setStyle("-fx-text-fill: " + glow + "; -fx-font-size: 11px; -fx-font-weight: bold;");
                text.getChildren().add(why);
            }

            Node scoreChip = top == null
                    ? muted("No score")
                    : matchScoreBadge(top.score());
            Button viewMatches = new Button("Matches");
            viewMatches.setStyle(UiTheme.ghostButton());
            viewMatches.setOnAction(e -> showReportMatches(report));
            Button match = new Button("Match");
            match.setStyle(UiTheme.primaryButton());
            match.setOnAction(e -> showMatchLostToInventoryDialog(report));
            row.getChildren().addAll(scoreChip, text, viewMatches, match);
            list.getChildren().add(row);
            shown++;
        }
        panel.getChildren().add(list);
        return panel;
    }

    /** Icon tiles under the pie chart — Manage Reports / Claims / Inventory / Analytics. */
    private Node buildQuickActionsPanel() {
        VBox panel = sectionCard("Quick Actions", "Jump to common admin tasks.");

        HBox tiles = new HBox(12);
        tiles.setAlignment(Pos.CENTER);
        tiles.getChildren().addAll(
                quickActionTile(NavIcons.forNav("MANAGE_REPORTS"), "#E41135",
                        "View All Reports", () -> navigate(Nav.MANAGE_REPORTS)),
                quickActionTile(NavIcons.forNav("MANAGE_CLAIMS"), "#E8A317",
                        "Manage Claims", () -> navigate(Nav.MANAGE_CLAIMS)),
                quickActionTile(NavIcons.forNav("INVENTORY"), "#22C55E",
                        "Manage Inventory", () -> navigate(Nav.INVENTORY)),
                quickActionTile(NavIcons.forNav("ANALYTICS"), "#8B5CF6",
                        "Generate Reports", () -> navigate(Nav.ANALYTICS))
        );
        for (Node tile : tiles.getChildren()) {
            HBox.setHgrow(tile, Priority.ALWAYS);
        }
        panel.getChildren().add(tiles);
        return panel;
    }

    private VBox quickActionTile(Node icon, String accentHex, String label, Runnable action) {
        VBox tile = new VBox(10);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(16, 12, 16, 12));
        tile.setPrefHeight(118);
        tile.setStyle(
                "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 12;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 2);"
        );

        // Tint the outline icon to the accent color
        NavIcons.applyColor(icon, false);
        paintIconAccent(icon, Color.web(accentHex));

        StackPane iconWrap = new StackPane(icon);
        iconWrap.setPrefSize(44, 44);
        iconWrap.setStyle(
                "-fx-background-color: rgba(228, 17, 53, 0.06);"
                + "-fx-background-radius: 12;"
        );

        Label text = new Label(label);
        text.setWrapText(true);
        text.setAlignment(Pos.CENTER);
        text.setMaxWidth(Double.MAX_VALUE);
        text.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-alignment: center;"
        );

        tile.getChildren().addAll(iconWrap, text);
        tile.setOnMouseClicked(e -> action.run());
        tile.setOnMouseEntered(e -> tile.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED_SOFT + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-radius: 12;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);"
        ));
        tile.setOnMouseExited(e -> tile.setStyle(
                "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 12;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 2);"
        ));
        return tile;
    }

    private static void paintIconAccent(Node node, Color color) {
        if (node instanceof javafx.scene.shape.Shape shape) {
            shape.setStroke(color);
            if (!(shape instanceof javafx.scene.shape.Line)) {
                shape.setFill(Color.TRANSPARENT);
            }
        } else if (node instanceof Group group) {
            for (Node child : group.getChildren()) {
                paintIconAccent(child, color);
            }
        } else if (node instanceof StackPane pane) {
            for (Node child : pane.getChildren()) {
                paintIconAccent(child, color);
            }
        }
    }

    /**
     * Reports Overview pie chart — click anywhere to open Reports & Analytics.
     * Slices: Lost reports, Found inventory, Returned items.
     */
    private Node buildReportsOverviewPie(long lostReports, long returnedItems) {
        Inventory foundProbe = new Inventory();
        long foundItems = foundProbe.filterItems().stream()
                .filter(i -> {
                    String s = i.getItemStatus();
                    return s != null && (
                            "AVAILABLE".equalsIgnoreCase(s)
                            || "CLAIM_PENDING".equalsIgnoreCase(s)
                            || "CLAIMED".equalsIgnoreCase(s)
                    );
                })
                .count();

        long lostN = Math.max(lostReports, 0);
        long foundN = Math.max(foundItems, 0);
        long returnedN = Math.max(returnedItems, 0);
        long realTotal = lostN + foundN + returnedN;

        double sliceLost = lostN;
        double sliceFound = foundN;
        double sliceReturned = returnedN;
        if (realTotal <= 0) {
            sliceLost = 1; // empty-state donut placeholder only
        }
        double sliceTotal = sliceLost + sliceFound + sliceReturned;
        double lostPct = realTotal == 0 ? 0 : lostN * 100.0 / realTotal;
        double foundPct = realTotal == 0 ? 0 : foundN * 100.0 / realTotal;
        double returnedPct = realTotal == 0 ? 0 : returnedN * 100.0 / realTotal;

        double radius = 82;
        double cx = 96;
        double cy = 96;
        Group pieGroup = new Group();

        // Soft halo behind the donut
        Circle halo = new Circle(cx, cy, radius + 10);
        halo.setFill(Color.web("rgba(228, 17, 53, 0.08)"));
        pieGroup.getChildren().add(halo);
        Circle haloRing = new Circle(cx, cy, radius + 4);
        haloRing.setFill(Color.TRANSPARENT);
        haloRing.setStroke(Color.web("rgba(228, 17, 53, 0.18)"));
        haloRing.setStrokeWidth(2);
        pieGroup.getChildren().add(haloRing);

        double angle = 90;
        angle = addPieSlice(pieGroup, cx, cy, radius, angle, sliceLost / sliceTotal * 360, Color.web("#E41135"));
        angle = addPieSlice(pieGroup, cx, cy, radius, angle, sliceFound / sliceTotal * 360, Color.web("#E8C04A"));
        addPieSlice(pieGroup, cx, cy, radius, angle, sliceReturned / sliceTotal * 360, Color.web("#3B82F6"));

        Circle hole = new Circle(cx, cy, 40);
        hole.setFill(Color.web("#FFF8F8"));
        DropShadow holeShadow = new DropShadow();
        holeShadow.setColor(Color.web("rgba(20, 20, 22, 0.10)"));
        holeShadow.setRadius(8);
        holeShadow.setOffsetY(1);
        hole.setEffect(holeShadow);
        pieGroup.getChildren().add(hole);

        StackPane pie = new StackPane(pieGroup);
        pie.setPrefSize(192, 192);
        pie.setMinSize(192, 192);
        pie.setAlignment(Pos.CENTER);

        VBox centerStats = new VBox(0);
        centerStats.setAlignment(Pos.CENTER);
        centerStats.setMouseTransparent(true);
        Label totalValue = new Label(String.valueOf(realTotal));
        totalValue.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 28px;"
                + "-fx-font-weight: bold;"
        );
        Label totalCaption = new Label(realTotal == 1 ? "CASE" : "CASES");
        totalCaption.setStyle(
                "-fx-text-fill: " + UiTheme.UE_RED + ";"
                + "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
                + "-fx-letter-spacing: 1;"
        );
        centerStats.getChildren().addAll(totalValue, totalCaption);
        pie.getChildren().add(centerStats);

        VBox legend = pieLegendList(
                new String[]{"#E41135", "#E8C04A", "#3B82F6"},
                new String[]{"rgba(228,17,53,0.10)", "rgba(232,192,74,0.16)", "rgba(59,130,246,0.12)"},
                new String[]{"Lost Items", "Found Items", "Returned Items"},
                new long[]{lostN, foundN, returnedN},
                new double[]{lostPct, foundPct, returnedPct}
        );

        VBox chartStack = new VBox(16, pie, legend);
        chartStack.setAlignment(Pos.TOP_CENTER);
        chartStack.setPadding(new Insets(4, 2, 2, 2));
        chartStack.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(legend, Priority.ALWAYS);

        // Header with accent + CTA chip
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Region accent = new Region();
        accent.setPrefSize(4, 36);
        accent.setMinSize(4, 36);
        accent.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " + UiTheme.UE_RED
                + ", #F87171);"
                + "-fx-background-radius: 4;"
        );
        VBox titles = new VBox(2);
        HBox.setHgrow(titles, Priority.ALWAYS);
        Label title = labeled("Reports Overview", UiTheme.TEXT, 16, true);
        Label subtitle = muted("Campus case mix at a glance");
        titles.getChildren().addAll(title, subtitle);
        Label cta = new Label("Analytics ›");
        cta.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 14;"
                + "-fx-padding: 6 12 6 12;"
        );
        header.getChildren().addAll(accent, titles, cta);

        VBox card = new VBox(14);
        card.setPadding(new Insets(16));
        card.setStyle(
                UiTheme.cardStyle()
                + "-fx-cursor: hand;"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-width: 1.2;"
        );
        card.getChildren().addAll(header, chartStack);
        card.setOnMouseClicked(e -> navigate(Nav.ANALYTICS));
        card.setOnMouseEntered(e -> card.setStyle(
                UiTheme.cardStyle()
                + "-fx-cursor: hand;"
                + "-fx-border-color: " + UiTheme.UE_RED + ";"
                + "-fx-border-width: 1.5;"
                + "-fx-effect: dropshadow(gaussian, rgba(228,17,53,0.22), 18, 0, 0, 4);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                UiTheme.cardStyle()
                + "-fx-cursor: hand;"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-width: 1.2;"
        ));

        card.setOpacity(0);
        card.setScaleX(0.96);
        card.setScaleY(0.96);
        FadeTransition fade = new FadeTransition(Duration.millis(420), card);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(420), card);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();

        return card;
    }

    private static double addPieSlice(Group pie, double cx, double cy, double radius,
                                      double startAngle, double length, Color color) {
        if (length <= 0) {
            return startAngle;
        }
        Arc arc = new Arc(cx, cy, radius, radius, startAngle, -length);
        arc.setType(ArcType.ROUND);
        arc.setFill(color);
        arc.setStroke(Color.web("#FFF8F8"));
        arc.setStrokeWidth(3);
        DropShadow sliceGlow = new DropShadow();
        sliceGlow.setColor(color.deriveColor(0, 1, 1, 0.35));
        sliceGlow.setRadius(6);
        sliceGlow.setSpread(0.15);
        arc.setEffect(sliceGlow);
        pie.getChildren().add(arc);
        return startAngle - length;
    }

    /** One-by-one legend rows — tinted cards, accent bar, progress share, bold stats. */
    private static VBox pieLegendList(String[] colors, String[] tints, String[] labels,
                                      long[] counts, double[] pcts) {
        VBox list = new VBox(10);
        list.setMaxWidth(Double.MAX_VALUE);
        list.setFillWidth(true);
        for (int i = 0; i < labels.length; i++) {
            final String color = colors[i];
            final String tint = tints[i];
            final double pct = pcts[i];

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setPadding(new Insets(12, 14, 12, 12));
            row.setStyle(
                    "-fx-background-color: " + tint + ";"
                    + "-fx-background-radius: 14;"
                    + "-fx-border-color: " + color + "55;"
                    + "-fx-border-radius: 14;"
                    + "-fx-border-width: 1;"
            );

            Region accentBar = new Region();
            accentBar.setPrefSize(4, 42);
            accentBar.setMinSize(4, 42);
            accentBar.setMaxHeight(42);
            accentBar.setStyle(
                    "-fx-background-color: " + color + ";"
                    + "-fx-background-radius: 4;"
            );

            StackPane iconHost = new StackPane();
            iconHost.setPrefSize(28, 28);
            iconHost.setMinSize(28, 28);
            Circle iconBg = new Circle(14, Color.web(color));
            Circle iconInner = new Circle(6, Color.WHITE);
            iconHost.getChildren().addAll(iconBg, iconInner);

            VBox textCol = new VBox(5);
            textCol.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(textCol, Priority.ALWAYS);
            Label name = new Label(labels[i]);
            name.setStyle(
                    "-fx-text-fill: " + UiTheme.TEXT + ";"
                    + "-fx-font-size: 13px;"
                    + "-fx-font-weight: bold;"
            );

            StackPane track = new StackPane();
            track.setAlignment(Pos.CENTER_LEFT);
            track.setMaxWidth(Double.MAX_VALUE);
            track.setPrefHeight(7);
            track.setMinHeight(7);
            track.setStyle(
                    "-fx-background-color: rgba(20,20,22,0.08);"
                    + "-fx-background-radius: 6;"
            );
            Region fill = new Region();
            fill.setMinHeight(7);
            fill.setMaxHeight(7);
            fill.setPrefHeight(7);
            fill.setStyle(
                    "-fx-background-color: " + color + ";"
                    + "-fx-background-radius: 6;"
            );
            track.widthProperty().addListener((obs, oldW, newW) -> {
                double w = newW == null ? 0 : newW.doubleValue();
                fill.setPrefWidth(pct <= 0 ? 0 : Math.max(6, w * pct / 100.0));
            });
            track.getChildren().add(fill);
            textCol.getChildren().addAll(name, track);

            VBox stats = new VBox(2);
            stats.setAlignment(Pos.CENTER_RIGHT);
            Label count = new Label(String.valueOf(counts[i]));
            count.setStyle(
                    "-fx-text-fill: " + UiTheme.TEXT + ";"
                    + "-fx-font-size: 18px;"
                    + "-fx-font-weight: bold;"
            );
            Label share = new Label(String.format(Locale.ENGLISH, "%.0f%%", pct));
            share.setStyle(
                    "-fx-background-color: " + color + ";"
                    + "-fx-text-fill: white;"
                    + "-fx-font-size: 10px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-background-radius: 10;"
                    + "-fx-padding: 2 8 2 8;"
            );
            stats.getChildren().addAll(count, share);

            row.getChildren().addAll(accentBar, iconHost, textCol, stats);
            row.setOnMouseEntered(e -> row.setStyle(
                    "-fx-background-color: " + tint + ";"
                    + "-fx-background-radius: 14;"
                    + "-fx-border-color: " + color + ";"
                    + "-fx-border-radius: 14;"
                    + "-fx-border-width: 1.4;"
                    + "-fx-effect: dropshadow(gaussian, " + color + "44, 10, 0, 0, 2);"
            ));
            row.setOnMouseExited(e -> row.setStyle(
                    "-fx-background-color: " + tint + ";"
                    + "-fx-background-radius: 14;"
                    + "-fx-border-color: " + color + "55;"
                    + "-fx-border-radius: 14;"
                    + "-fx-border-width: 1;"
            ));
            list.getChildren().add(row);
        }
        return list;
    }

    private Node buildManageReportsPage() {
        VBox page = new VBox(14);

        // Context panels follow the active type filter.
        if (reportsTypeFilter == null || "FOUND".equals(reportsTypeFilter)) {
            page.getChildren().add(buildFoundTipsToRegisterPanel());
        }
        if (reportsTypeFilter == null || "LOST".equals(reportsTypeFilter)) {
            page.getChildren().add(buildSuggestedMatchesPanel());
        }

        HBox typeFilters = new HBox(8);
        typeFilters.getChildren().addAll(
                filterChip("All Types", reportsTypeFilter == null, () -> {
                    reportsTypeFilter = null;
                    navigate(Nav.MANAGE_REPORTS);
                }),
                filterChip("Lost", "LOST".equals(reportsTypeFilter), () -> {
                    reportsTypeFilter = "LOST";
                    navigate(Nav.MANAGE_REPORTS);
                }),
                filterChip("Found", "FOUND".equals(reportsTypeFilter), () -> {
                    reportsTypeFilter = "FOUND";
                    navigate(Nav.MANAGE_REPORTS);
                })
        );

        HBox recordFilters = new HBox(8);
        recordFilters.getChildren().addAll(
                filterChip("Active", "ACTIVE".equals(reportsRecordFilter), () -> {
                    reportsRecordFilter = "ACTIVE";
                    navigate(Nav.MANAGE_REPORTS);
                }),
                filterChip("Cancelled", "CANCELLED".equals(reportsRecordFilter), () -> {
                    reportsRecordFilter = "CANCELLED";
                    navigate(Nav.MANAGE_REPORTS);
                }),
                filterChip("Archived", "ARCHIVED".equals(reportsRecordFilter), () -> {
                    reportsRecordFilter = "ARCHIVED";
                    navigate(Nav.MANAGE_REPORTS);
                }),
                filterChip("All records", "ALL".equals(reportsRecordFilter), () -> {
                    reportsRecordFilter = "ALL";
                    navigate(Nav.MANAGE_REPORTS);
                })
        );

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField search = new TextField(reportsSearchKey);
        search.setPromptText("Search reports...");
        search.setStyle(fieldStyle());
        HBox.setHgrow(search, Priority.ALWAYS);
        Button searchBtn = new Button("Search");
        searchBtn.setStyle(UiTheme.ghostButton());
        searchBtn.setOnAction(e -> {
            reportsSearchKey = search.getText().trim();
            navigate(Nav.MANAGE_REPORTS);
        });
        toolbar.getChildren().addAll(search, searchBtn);

        Node dateBar = buildDateRangeBar(
                reportsDatePreset, reportsFrom, reportsTo,
                (preset, from, to) -> {
                    reportsDatePreset = preset;
                    reportsFrom = from;
                    reportsTo = to;
                    navigate(Nav.MANAGE_REPORTS);
                }
        );

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.setMaxWidth(Double.MAX_VALUE);

        LocalDate[] bounds = DateRanges.boundsFor(reportsDatePreset, reportsFrom, reportsTo);
        List<Report> reports;
        if (reportsSearchKey.isBlank()) {
            Report probe = new Report();
            probe.setUserId(0);
            reports = probe.filterReports();
        } else {
            Report probe = new Report();
            probe.setItemName(reportsSearchKey);
            reports = probe.searchReports();
        }
        if ("LOST".equals(reportsTypeFilter)) {
            reports = reports.stream().filter(Report::isLostReport).toList();
        } else if ("FOUND".equals(reportsTypeFilter)) {
            reports = reports.stream().filter(Report::isFoundReport).toList();
        }
        final String recordFilter = reportsRecordFilter == null ? "ACTIVE" : reportsRecordFilter;
        reports = reports.stream()
                .filter(r -> Report.matchesRecordFilter(r, recordFilter))
                .filter(r -> DateRanges.includes(r.getDateSubmitted(), bounds[0], bounds[1]))
                .toList();

        if (reports.isEmpty()) {
            String emptyHint = "CANCELLED".equals(recordFilter)
                    ? "No cancelled reports. Student cancel keeps the row for audit."
                    : "ARCHIVED".equals(recordFilter)
                    ? "No archived reports. Archive moves cases out of the active queue without deleting them."
                    : "FOUND".equals(reportsTypeFilter)
                    ? "No Found reports in this filter. Found reports are items students turned in for inventory."
                    : "LOST".equals(reportsTypeFilter)
                    ? "No Lost reports in this filter. Lost reports are matched to found inventory — not registered."
                    : "Adjust search, date range, or wait for new submissions.";
            tableCard.getChildren().add(emptyState("No reports found.", emptyHint));
        } else {
            GridPane table = newDataTable();
            int r = 0;
            r = addTableHeader(table, r, "ID", "Item Name", "Type", "Date Reported", "Status", "Actions");
            for (Report report : reports) {
                r = addAdminReportRow(table, r, report);
            }
            tableCard.getChildren().add(table);
        }

        String guide = "Active queue hides Cancelled/Archived. Soft-close keeps MySQL history — never hard-delete reports.";
        page.getChildren().addAll(typeFilters, recordFilters, dateBar, toolbar, tableCard,
                muted("Showing " + reports.size() + " report" + (reports.size() == 1 ? "" : "s")
                        + " · " + DateRanges.describe(reportsDatePreset, bounds[0], bounds[1])
                        + ". " + guide));
        return page;
    }

    private Node buildManageClaimsPage() {
        VBox page = new VBox(14);

        // Auto-hold stale Pending claims (7+ days) so admin queue stays manageable.
        int autoHeld = Claim.autoHoldStalePending(7);
        if (autoHeld > 0) {
            Toast.show(stage, autoHeld + " pending claim"
                    + (autoHeld == 1 ? "" : "s")
                    + " auto-held after 7 days — review under On Hold.");
        }

        page.getChildren().add(buildPickupCodeLookupBar());

        HBox filters = new HBox(8);
        filters.getChildren().addAll(
                filterChip("All", claimsFilter == null, () -> {
                    claimsFilter = null;
                    navigate(Nav.MANAGE_CLAIMS);
                }),
                filterChip("Pending", "PENDING".equals(claimsFilter), () -> {
                    claimsFilter = "PENDING";
                    navigate(Nav.MANAGE_CLAIMS);
                }),
                filterChip("Approved", "APPROVED".equals(claimsFilter), () -> {
                    claimsFilter = "APPROVED";
                    navigate(Nav.MANAGE_CLAIMS);
                }),
                filterChip("Rejected", "REJECTED".equals(claimsFilter), () -> {
                    claimsFilter = "REJECTED";
                    navigate(Nav.MANAGE_CLAIMS);
                }),
                filterChip("On Hold", "ON_HOLD".equals(claimsFilter), () -> {
                    claimsFilter = "ON_HOLD";
                    navigate(Nav.MANAGE_CLAIMS);
                })
        );

        Node dateBar = buildDateRangeBar(
                claimsDatePreset, claimsFrom, claimsTo,
                (preset, from, to) -> {
                    claimsDatePreset = preset;
                    claimsFrom = from;
                    claimsTo = to;
                    navigate(Nav.MANAGE_CLAIMS);
                }
        );

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.setMaxWidth(Double.MAX_VALUE);

        LocalDate[] bounds = DateRanges.boundsFor(claimsDatePreset, claimsFrom, claimsTo);
        List<Claim> claims = Claim.filterAllByStatus(claimsFilter).stream()
                .filter(c -> DateRanges.includes(c.getClaimDate(), bounds[0], bounds[1]))
                .toList();
        if (claims.isEmpty()) {
            tableCard.getChildren().add(emptyState("No claims in this filter.",
                    "Try another status or date range. New claims appear after students submit."));
        } else {
            GridPane table = newDataTable();
            int r = 0;
            r = addTableHeader(table, r, "Claim ID", "Item Name", "Student", "Date", "Status", "Actions");
            for (Claim claim : claims) {
                r = addAdminClaimRow(table, r, claim);
            }
            tableCard.getChildren().add(table);
        }

        page.getChildren().addAll(filters, dateBar, tableCard,
                muted("Showing " + claims.size() + " claim" + (claims.size() == 1 ? "" : "s")
                        + " · " + DateRanges.describe(claimsDatePreset, bounds[0], bounds[1])
                        + ". Auto On Hold when no registered item matches the claim. "
                        + "Admin: Pending → Approve / Reject / Hold · On Hold → Resume / Approve / Reject"
                        + " · Approved → Confirm pickup. Stale Pending (7+ days) also auto-hold."));
        return page;
    }

    /** Additive desk tool — look up approved claim by pickup code and open Confirm pickup. */
    private Node buildPickupCodeLookupBar() {
        VBox panel = sectionCard(
                "Desk pickup lookup",
                "Ask the student for their pickup code, type it here, then confirm release."
        );
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        TextField codeField = new TextField();
        codeField.setPromptText("e.g. UE-7K2M");
        codeField.setPrefWidth(160);
        codeField.setStyle(fieldStyle());
        HBox.setHgrow(codeField, Priority.ALWAYS);
        Button find = UiButtons.primary("Find & confirm", () -> lookupPickupCode(codeField.getText()));
        codeField.setOnAction(e -> lookupPickupCode(codeField.getText()));
        row.getChildren().addAll(codeField, find);
        panel.getChildren().add(row);
        return panel;
    }

    private void lookupPickupCode(String raw) {
        if (raw == null || raw.isBlank()) {
            info("Pickup code required", "Type the code from the student’s notification (e.g. UE-7K2M).");
            return;
        }
        Optional<Claim> found = Claim.findByPickupCode(raw.trim());
        if (found.isEmpty()) {
            info("Not found", "No claim matches that pickup code. Check spelling with the student.");
            return;
        }
        Claim live = Claim.findById(found.get().getClaimId()).orElse(found.get());
        String status = live.getClaimStatus() == null ? "" : live.getClaimStatus().toUpperCase(Locale.ENGLISH);
        if (!"APPROVED".equals(status)) {
            info("Not ready for pickup",
                    live.getDisplayId() + " is " + prettyStatus(live.getClaimStatus())
                            + " — only Approved claims can be released.");
            return;
        }
        if (live.getReturnDate() != null) {
            info("Already released",
                    live.getDisplayId() + " was already confirmed on "
                            + DATE_FMT.format(live.getReturnDate()) + ".");
            return;
        }
        showConfirmReturnDialog(live);
    }

    private Node buildInventoryPage() {
        VBox page = new VBox(14);

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField search = new TextField(inventorySearchKey);
        search.setPromptText("Search by name, brand, or tracking #...");
        search.setStyle(fieldStyle());
        HBox.setHgrow(search, Priority.ALWAYS);
        Button searchBtn = new Button("Search");
        searchBtn.setStyle(UiTheme.ghostButton());
        searchBtn.setOnAction(e -> {
            inventorySearchKey = search.getText().trim();
            navigate(Nav.INVENTORY);
        });
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Button register = UiButtons.primaryIcon("Register Item", "PLUS",
                () -> showRegisterItemDialog(null));
        Button refreshInv = UiButtons.ghostIcon("Refresh", "REFRESH", () -> navigate(Nav.INVENTORY));
        toolbar.getChildren().addAll(search, searchBtn, refreshInv, grow, register);
        UiButtons.style(searchBtn, UiButtons.Kind.GHOST);

        boolean viewingArchive = "ARCHIVED".equals(inventoryStatusFilter);

        HBox statusFilters = new HBox(8);
        statusFilters.getChildren().addAll(
                filterChip("All", inventoryStatusFilter == null, () -> {
                    inventoryStatusFilter = null;
                    navigate(Nav.INVENTORY);
                }),
                filterChip("Available", "AVAILABLE".equals(inventoryStatusFilter), () -> {
                    inventoryStatusFilter = "AVAILABLE";
                    navigate(Nav.INVENTORY);
                }),
                filterChip("Claimed", "CLAIMED".equals(inventoryStatusFilter), () -> {
                    inventoryStatusFilter = "CLAIMED";
                    navigate(Nav.INVENTORY);
                }),
                filterChip("Returned", "RETURNED".equals(inventoryStatusFilter), () -> {
                    inventoryStatusFilter = "RETURNED";
                    navigate(Nav.INVENTORY);
                }),
                filterChip("Archived", viewingArchive, () -> {
                    inventoryStatusFilter = "ARCHIVED";
                    navigate(Nav.INVENTORY);
                })
        );

        // Additive hint only — does not change storage assignment logic.
        // Hidden while browsing archive (active-box counts only).
        Node boxCapacity = viewingArchive
                ? muted("Archive view — items here are removed from active inventory but kept for records.")
                : buildStorageBoxCapacityHint();

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.setMaxWidth(Double.MAX_VALUE);

        List<Inventory> items = loadInventoryForCurrentFilter();

        if (items.isEmpty()) {
            tableCard.getChildren().add(emptyState(
                    viewingArchive ? "No archived items." : "No inventory items found.",
                    viewingArchive
                            ? "Archived items appear here after you archive them from active inventory."
                            : "Register a found item to get started."
            ));
        } else {
            GridPane table = newDataTable();
            int r = 0;
            r = addTableHeader(table, r, "Tracking #", "Item Name", "Category", "Status", "Storage Box", "Actions");
            for (Inventory item : items) {
                r = addInventoryRow(table, r, item);
            }
            tableCard.getChildren().add(table);
        }

        page.getChildren().addAll(toolbar, boxCapacity, statusFilters, tableCard,
                muted("Showing " + items.size()
                        + (viewingArchive ? " archived" : "")
                        + " item" + (items.size() == 1 ? "" : "s") + "."));
        return page;
    }

    /** Active filters hide ARCHIVED; Archive chip shows only archived rows. */
    private List<Inventory> loadInventoryForCurrentFilter() {
        Inventory probe = new Inventory();
        String key = inventorySearchKey == null ? "" : inventorySearchKey.trim().toLowerCase(Locale.ENGLISH);
        List<Inventory> all = probe.filterItems();
        return all.stream()
                .filter(i -> {
                    String status = i.getItemStatus() == null ? "" : i.getItemStatus().toUpperCase(Locale.ENGLISH);
                    if ("ARCHIVED".equals(inventoryStatusFilter)) {
                        return "ARCHIVED".equals(status);
                    }
                    if ("ARCHIVED".equals(status)) return false;
                    if (inventoryStatusFilter == null || inventoryStatusFilter.isBlank()) return true;
                    return inventoryStatusFilter.equalsIgnoreCase(status);
                })
                .filter(i -> key.isEmpty()
                        || (i.getItemName() != null && i.getItemName().toLowerCase(Locale.ENGLISH).contains(key))
                        || (i.getTrackingNumber() != null
                        && i.getTrackingNumber().toLowerCase(Locale.ENGLISH).contains(key))
                        || (i.getBrand() != null && i.getBrand().toLowerCase(Locale.ENGLISH).contains(key)))
                .toList();
    }

    private Node buildStorageBoxCapacityHint() {
        Inventory probe = new Inventory();
        List<Inventory> all = probe.filterItems();
        java.util.Map<String, Long> counts = new LinkedHashMap<>();
        for (Inventory item : all) {
            String status = item.getItemStatus() == null ? "" : item.getItemStatus().toUpperCase(Locale.ENGLISH);
            if ("RETURNED".equals(status) || "ARCHIVED".equals(status)) continue;
            String box = item.getStorageBox() == null || item.getStorageBox().isBlank()
                    ? "Unassigned" : item.getStorageBox();
            counts.merge(box, 1L, Long::sum);
        }
        VBox card = sectionCard("Storage box capacity",
                "How many active items are currently assigned per box (hint only).");
        if (counts.isEmpty()) {
            card.getChildren().add(muted("No active inventory in boxes yet."));
            return card;
        }
        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        long softLimit = 8;
        for (var entry : counts.entrySet()) {
            boolean busy = entry.getValue() >= softLimit;
            Label chip = new Label(entry.getKey() + ": " + entry.getValue()
                    + (busy ? " (filling up)" : ""));
            chip.setStyle(
                    "-fx-background-color: " + (busy ? UiTheme.STATUS_PENDING_BG : UiTheme.FIELD_BG) + ";"
                    + "-fx-text-fill: " + (busy ? UiTheme.STATUS_PENDING_FG : UiTheme.TEXT) + ";"
                    + "-fx-font-size: 12px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 14;"
                    + "-fx-padding: 6 12 6 12;"
                    + "-fx-border-color: " + UiTheme.BORDER + ";"
                    + "-fx-border-radius: 14;"
            );
            chips.getChildren().add(chip);
        }
        card.getChildren().add(chips);
        return card;
    }

    private Node buildUsersPage() {
        VBox page = new VBox(14);
        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.setMaxWidth(Double.MAX_VALUE);

        List<User> accounts = User.listAccounts();
        if (accounts.isEmpty()) {
            tableCard.getChildren().add(emptyState("No accounts.", "Accounts appear after registration."));
        } else {
            GridPane table = newDataTable();
            int r = 0;
            r = addTableHeader(table, r, "ID", "Full Name", "Username / Student #", "Role", "Status", "Actions");
            for (User account : accounts) {
                r = addUserRow(table, r, account);
            }
            tableCard.getChildren().add(table);
        }

        page.getChildren().addAll(tableCard,
                muted("Showing " + accounts.size() + " account" + (accounts.size() == 1 ? "" : "s") + "."));
        return page;
    }

    private Node buildAnalyticsPage() {
        LocalDate[] bounds = DateRanges.boundsFor(analyticsDatePreset, analyticsFrom, analyticsTo);
        LocalDate from = bounds[0];
        LocalDate to = bounds[1];
        String rangeLabel = DateRanges.describe(analyticsDatePreset, from, to);

        Node dateBar = buildDateRangeBar(
                analyticsDatePreset, analyticsFrom, analyticsTo,
                (preset, f, t) -> {
                    analyticsDatePreset = preset;
                    analyticsFrom = f;
                    analyticsTo = t;
                    navigate(Nav.ANALYTICS);
                }
        );

        Report reportProbe = new Report();
        reportProbe.setUserId(0);
        long totalReports = reportProbe.filterReports().stream()
                .filter(r -> DateRanges.includes(r.getDateSubmitted(), from, to))
                .count();
        List<Claim> rangedClaims = Claim.viewAllClaims().stream()
                .filter(c -> DateRanges.includes(c.getClaimDate(), from, to))
                .toList();
        long pendingClaims = rangedClaims.stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getClaimStatus())).count();
        long approvedClaims = rangedClaims.stream()
                .filter(c -> "APPROVED".equalsIgnoreCase(c.getClaimStatus())).count();
        long rejectedClaims = rangedClaims.stream()
                .filter(c -> "REJECTED".equalsIgnoreCase(c.getClaimStatus())).count();
        long onHoldClaims = rangedClaims.stream()
                .filter(c -> "ON_HOLD".equalsIgnoreCase(c.getClaimStatus())).count();

        Inventory inv = new Inventory();
        List<Inventory> rangedItems = inv.filterItems().stream()
                .filter(i -> DateRanges.includes(i.getFoundDate(), from, to))
                .toList();
        long avail = rangedItems.stream()
                .filter(i -> "AVAILABLE".equalsIgnoreCase(i.getItemStatus())).count();
        long claimed = rangedItems.stream()
                .filter(i -> "CLAIMED".equalsIgnoreCase(i.getItemStatus())).count();
        long returned = Claim.viewAllClaims().stream()
                .filter(c -> c.getReturnDate() != null
                        && "APPROVED".equalsIgnoreCase(c.getClaimStatus())
                        && DateRanges.includes(c.getReturnDate(), from, to))
                .count();

        VBox summary = sectionCard("Summary counts", "Counts for " + rangeLabel + ".");
        long maxSummary = Math.max(1, Math.max(
                Math.max(totalReports, pendingClaims),
                Math.max(approvedClaims, returned)
        ));
        summary.getChildren().addAll(
                barRow("Total reports", totalReports, maxSummary),
                barRow("Pending claims", pendingClaims, maxSummary),
                barRow("Approved claims", approvedClaims, maxSummary),
                barRow("Returned items", returned, maxSummary)
        );

        CampusInsights.MonthSummary period = CampusInsights.rangeSummary(from, to);
        VBox monthCard = sectionCard(
                "Period overview — " + period.monthLabel(),
                "Lost filed, Found registered, matched cases, and returns."
        );
        long maxMonth = Math.max(1, Math.max(
                Math.max(period.lostFiled(), period.foundRegistered()),
                Math.max(period.lostMatched(), period.returned())
        ));
        monthCard.getChildren().addAll(
                barRow("Lost reports filed", period.lostFiled(), maxMonth),
                barRow("Found items registered", period.foundRegistered(), maxMonth),
                barRow("Lost reports matched", period.lostMatched(), maxMonth),
                barRow("Items returned", period.returned(), maxMonth),
                muted("Avg days approved → pickup: "
                        + formatAvgDays(period.avgDaysToReturn())),
                muted("Mean of (pickup date − approval date) for returned claims that have a recorded approval date.")
        );

        List<CampusInsights.LocationCount> hotspots = CampusInsights.topLostLocations(from, to, 6);
        VBox heatmap = sectionCard(
                "Lost location hotspots",
                hotspots.isEmpty()
                        ? "No Lost report locations logged in this date range."
                        : "Where students say items went missing."
        );
        if (hotspots.isEmpty()) {
            heatmap.getChildren().add(muted("File Lost reports with a location to build this map."));
        } else {
            long maxLoc = Math.max(1, hotspots.get(0).count());
            for (CampusInsights.LocationCount loc : hotspots) {
                heatmap.getChildren().add(barRow(loc.location(), loc.count(), maxLoc));
            }
        }

        VBox claimsBreakdown = sectionCard("Claims by status", "Relative volume per status · " + rangeLabel + ".");
        long maxClaim = Math.max(1, Math.max(Math.max(pendingClaims, approvedClaims), Math.max(rejectedClaims, onHoldClaims)));
        claimsBreakdown.getChildren().addAll(
                barRow("Pending", pendingClaims, maxClaim),
                barRow("Approved", approvedClaims, maxClaim),
                barRow("Rejected", rejectedClaims, maxClaim),
                barRow("On hold", onHoldClaims, maxClaim)
        );

        VBox invBreakdown = sectionCard("Inventory by status",
                "Items found/registered in range · Available vs claimed (returns use pickup date).");
        long maxInv = Math.max(1, Math.max(avail, Math.max(claimed, returned)));
        invBreakdown.getChildren().addAll(
                barRow("Available", avail, maxInv),
                barRow("Claimed", claimed, maxInv),
                barRow("Returned", returned, maxInv)
        );

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button exportMonth = UiButtons.primary("Export monthly CSV", this::exportMonthlyCsv);
        Button export = UiButtons.ghost("Export full CSV", this::exportCsv);
        footer.getChildren().addAll(export, exportMonth);

        return new VBox(14, dateBar, monthCard, heatmap, summary, claimsBreakdown, invBreakdown, footer);
    }

    @FunctionalInterface
    private interface DateRangeApply {
        void apply(DateRanges.Preset preset, LocalDate from, LocalDate to);
    }

    /** Shared date-range bar: All / This week / This month + optional From–To custom. */
    private Node buildDateRangeBar(DateRanges.Preset activePreset, LocalDate from, LocalDate to,
                                   DateRangeApply onApply) {
        VBox wrap = new VBox(8);
        wrap.setMaxWidth(Double.MAX_VALUE);

        HBox presets = new HBox(8);
        presets.setAlignment(Pos.CENTER_LEFT);
        presets.getChildren().addAll(
                filterChip("All dates", activePreset == DateRanges.Preset.ALL, () ->
                        onApply.apply(DateRanges.Preset.ALL, null, null)),
                filterChip("This week", activePreset == DateRanges.Preset.WEEK, () -> {
                    LocalDate[] b = DateRanges.boundsFor(DateRanges.Preset.WEEK, null, null);
                    onApply.apply(DateRanges.Preset.WEEK, b[0], b[1]);
                }),
                filterChip("This month", activePreset == DateRanges.Preset.MONTH, () -> {
                    LocalDate[] b = DateRanges.boundsFor(DateRanges.Preset.MONTH, null, null);
                    onApply.apply(DateRanges.Preset.MONTH, b[0], b[1]);
                })
        );

        HBox custom = new HBox(10);
        custom.setAlignment(Pos.CENTER_LEFT);
        Label fromLbl = new Label("From");
        fromLbl.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        DatePicker fromPick = new DatePicker(from);
        fromPick.setPrefWidth(140);
        fromPick.setStyle(fieldStyle());
        Label toLbl = new Label("To");
        toLbl.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        DatePicker toPick = new DatePicker(to);
        toPick.setPrefWidth(140);
        toPick.setStyle(fieldStyle());
        Button apply = UiButtons.ghost("Apply dates", () -> {
            LocalDate f = fromPick.getValue();
            LocalDate t = toPick.getValue();
            if (f != null && t != null && t.isBefore(f)) {
                info("Invalid date range", "The To date must be on or after the From date.");
                return;
            }
            onApply.apply(DateRanges.Preset.CUSTOM, f, t);
        });
        Button clear = UiButtons.ghost("Clear dates", () -> onApply.apply(DateRanges.Preset.ALL, null, null));
        custom.getChildren().addAll(fromLbl, fromPick, toLbl, toPick, apply, clear);

        wrap.getChildren().addAll(presets, custom);
        return wrap;
    }

    private Node buildSettingsPage() {
        VBox page = new VBox(14);
        page.getChildren().add(settingsTile(
                "LOCK",
                "Change Password",
                "Update the password used to sign in to your administrator account.",
                this::showChangePasswordDialog
        ));
        page.getChildren().add(settingsTile(
                "MAIL",
                "Update Email Address",
                "Change the email linked to your admin account.",
                this::showChangeEmailDialog
        ));
        page.getChildren().add(settingsTile(
                "DOCUMENT",
                "Audit Log",
                "Who matched, approved, rejected, or confirmed pickup — and when.",
                () -> setContent(buildAuditLogPage())
        ));
        page.getChildren().add(muted(
                "Security: BCrypt password hashing · idle sign-out after "
                        + IdleSessionGuard.IDLE_MINUTES
                        + " min · login lock after " + LoginThrottle.MAX_FAILURES
                        + " fails · UE emails only (" + AccountRules.UE_EMAIL_DOMAIN + ")."
        ));
        return page;
    }

    private Node buildAuditLogPage() {
        VBox page = new VBox(14);
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Button back = UiButtons.ghost("← Settings", () -> navigate(Nav.SETTINGS));
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Button refresh = UiButtons.ghostIcon("Refresh", "REFRESH", () -> setContent(buildAuditLogPage()));
        top.getChildren().addAll(back, grow, refresh);

        VBox card = sectionCard(
                "Desk audit log",
                "Append-only history of admin and sign-in actions (newest first)."
        );

        List<AuditLog.Entry> entries = AuditLog.recent(100);
        if (entries.isEmpty()) {
            card.getChildren().add(muted("No audit events yet. Desk actions will appear here."));
        } else {
            DateTimeFormatter whenFmt =
                    DateTimeFormatter.ofPattern("MMM d, yyyy  •  h:mm a", Locale.ENGLISH);
            VBox list = new VBox(8);
            for (AuditLog.Entry e : entries) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.TOP_LEFT);
                row.setPadding(new Insets(10, 12, 10, 12));
                row.setStyle(
                        "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: " + UiTheme.BORDER + ";"
                        + "-fx-border-radius: 10;"
                );
                VBox text = new VBox(3);
                HBox.setHgrow(text, Priority.ALWAYS);
                String who = (e.actorName() == null || e.actorName().isBlank())
                        ? "(unknown)" : e.actorName();
                String role = e.actorRole() == null || e.actorRole().isBlank()
                        ? "" : " · " + e.actorRole();
                Label title = new Label(e.action()
                        + (e.entityId() == null || e.entityId().isBlank() ? "" : " · " + e.entityId()));
                title.setWrapText(true);
                title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");
                Label meta = new Label(who + role
                        + (e.createdAt() == null ? "" : " · " + whenFmt.format(e.createdAt())));
                meta.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
                text.getChildren().addAll(title, meta);
                if (e.details() != null && !e.details().isBlank()) {
                    Label detail = new Label(e.details());
                    detail.setWrapText(true);
                    detail.setStyle("-fx-text-fill: " + UiTheme.TEXT_DIM + "; -fx-font-size: 12px;");
                    text.getChildren().add(detail);
                }
                row.getChildren().add(text);
                list.getChildren().add(row);
            }
            card.getChildren().add(list);
        }

        setHeader("Audit Log", "Accountability trail for SAO desk actions.");
        page.getChildren().addAll(top, card);
        return page;
    }

    // ------------------------------------------------------------------ table rows
    // One shared GridPane per table so header + every data row use the SAME columns.

    /**
     * Only FOUND tips may be registered into inventory (student turned an item in).
     * LOST reports must never create inventory — they are matched to existing found items.
     */
    private static boolean canRegisterFoundTip(Report report) {
        return report != null && report.isFoundReport() && report.isOpenForStaffAction();
    }

    /** LOST reports that may be matched (Pending / Under Review, or On Hold with candidates). */
    private static boolean canMatchLostReport(Report report) {
        return report != null && report.isLostReport() && report.allowsStaffMutation()
                && !MatchScorer.rankForReport(report).isEmpty();
    }

    /** Pending / On Hold only — approved+picked-up and rejected stay read-only. */
    private static boolean isClaimOpenForDecision(Claim claim) {
        if (claim == null) return false;
        if (claim.getReturnDate() != null) return false;
        String s = claim.getClaimStatus() == null ? "" : claim.getClaimStatus().toUpperCase(Locale.ENGLISH);
        return "PENDING".equals(s) || "ON_HOLD".equals(s);
    }

    private int addAdminReportRow(GridPane table, int row, Report report) {
        List<Button> actions = new ArrayList<>();
        actions.add(linkBtn("View", () -> showReportDetails(report)));
        // Mid/finished cases: no Match/Status mutation — View + Archive (when allowed).
        if (report.allowsStaffMutation()) {
            if (report.isLostReport()) {
                actions.add(linkBtn("Matches", () -> showReportMatches(report)));
                if (canMatchLostReport(report)) {
                    actions.add(linkBtn("Match", () -> showMatchLostToInventoryDialog(report)));
                }
            } else if (canRegisterFoundTip(report)) {
                actions.add(linkBtn("Register", () -> showRegisterItemDialog(report)));
            }
            actions.add(linkBtn("Status", () -> showUpdateReportStatusDialog(report)));
        }
        if (report.canBeArchived()) {
            actions.add(linkBtn("Archive", () -> confirmArchiveReport(report)));
        }
        if (report.canBeRestoredFromArchive()) {
            actions.add(linkBtn("Restore", () -> confirmRestoreReport(report)));
        }
        if (report.canBeReopenedFromCancelled()) {
            actions.add(linkBtn("Reopen", () -> confirmReopenCancelledReport(report)));
        }
        putData(table, 0, row, cell(report.getDisplayId(), true));
        putData(table, 1, row, cell(report.getItemName(), false));
        putData(table, 2, row, cell(report.isFoundReport() ? "Found" : "Lost", false));
        putData(table, 3, row, cell(DATE_FMT.format(report.getDateSubmitted()), false));
        HBox statusCell = new HBox(6);
        statusCell.setAlignment(Pos.CENTER);
        statusCell.getChildren().add(statusBadge(reportStatusLabel(report)));
        if (canMatchLostReport(report)) {
            List<MatchScorer.ScoredMatch> ranked = MatchScorer.rankForReport(report);
            if (!ranked.isEmpty()) {
                statusCell.getChildren().add(matchScoreBadge(ranked.get(0).score()));
            }
        }
        putData(table, 4, row, statusCell);
        putData(table, 5, row, multiActions(actions.toArray(Button[]::new)));
        return row + 1;
    }

    private int addAdminClaimRow(GridPane table, int row, Claim claim) {
        String student = User.findById(claim.getUserId())
                .map(User::getFullName)
                .orElse("User #" + claim.getUserId());
        putData(table, 0, row, cell(claim.getDisplayId(), true));
        putData(table, 1, row, cell(claim.getItemName(), false));
        putData(table, 2, row, cell(student, false));
        putData(table, 3, row, cell(DATE_FMT.format(claim.getClaimDate()), false));
        putData(table, 4, row, statusBadge(prettyStatus(claim.getClaimStatus())));
        putData(table, 5, row, claimActionCell(claim));
        return row + 1;
    }

    private HBox claimActionCell(Claim claim) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(linkBtn("View", () -> showClaimDetails(claim)));
        String status = claim.getClaimStatus() == null ? "" : claim.getClaimStatus().toUpperCase(Locale.ENGLISH);
        boolean pickedUp = claim.getReturnDate() != null;
        // Completed (picked up) or Rejected: View only — no further decision buttons.
        if (pickedUp || "REJECTED".equals(status)) {
            return multiActions(buttons.toArray(new Button[0]));
        }
        if ("PENDING".equals(status)) {
            buttons.add(linkBtn("Approve", () -> showApproveClaimDialog(claim)));
            buttons.add(linkBtn("Reject", () -> showRejectClaimDialog(claim)));
            buttons.add(linkBtn("Hold", () -> showHoldClaimDialog(claim)));
        } else if ("ON_HOLD".equals(status)) {
            buttons.add(linkBtn("Resume", () -> resumeHeldClaim(claim)));
            buttons.add(linkBtn("Approve", () -> showApproveClaimDialog(claim)));
            buttons.add(linkBtn("Reject", () -> showRejectClaimDialog(claim)));
        } else if ("APPROVED".equals(status)) {
            if (claim.getPickupCode() != null && !claim.getPickupCode().isBlank()) {
                buttons.add(linkBtn("Copy code", () -> copyPickupCode(claim.getPickupCode())));
            }
            buttons.add(linkBtn("Confirm pickup", () -> showConfirmReturnDialog(claim)));
        }
        return multiActions(buttons.toArray(new Button[0]));
    }

    private void copyPickupCode(String code) {
        if (!ClipboardUtil.copyText(code)) {
            info("No code", "There is no pickup code to copy yet.");
            return;
        }
        Toast.show(stage, "Pickup code copied: " + code.trim());
    }

    private int addInventoryRow(GridPane table, int row, Inventory item) {
        putData(table, 0, row, cell(nullSafe(item.getTrackingNumber()), true));
        putData(table, 1, row, cell(item.getItemName(), false));
        putData(table, 2, row, cell(shortCategory(item.getCategory()), false));
        putData(table, 3, row, statusBadge(prettyStatus(item.getItemStatus())));
        putData(table, 4, row, cell(nullSafe(item.getStorageBox()), false));
        String itemStatus = item.getItemStatus() == null ? "" : item.getItemStatus().toUpperCase(Locale.ENGLISH);
        boolean archived = "ARCHIVED".equals(itemStatus);
        List<Button> actions = new ArrayList<>();
        actions.add(linkBtn("View", () -> showInventoryDetails(item)));
        if (archived) {
            actions.add(linkBtn("Restore", () -> confirmRestoreItem(item)));
        } else {
            // Available / Claimed / Returned can still be filed to Archive for records.
            actions.add(linkBtn("Archive", () -> confirmArchiveItem(item)));
        }
        putData(table, 5, row, multiActions(actions.toArray(new Button[0])));
        return row + 1;
    }

    private int addUserRow(GridPane table, int row, User account) {
        String loginId = account.getStudentNumber() != null && !account.getStudentNumber().isBlank()
                ? account.getStudentNumber()
                : nullSafe(account.getUsername());
        List<Button> actions = new ArrayList<>();
        actions.add(linkBtn("View", () -> showUserDetails(account)));
        actions.add(linkBtn("Reset PW", () -> showAdminResetPasswordDialog(account)));
        if ("STUDENT".equalsIgnoreCase(account.getRole())) {
            if ("Active".equalsIgnoreCase(account.getAccountStatus())) {
                actions.add(linkBtn("Deactivate", () -> deactivateUser(account)));
            } else if ("Inactive".equalsIgnoreCase(account.getAccountStatus())) {
                actions.add(linkBtn("Activate", () -> activateUser(account)));
            }
        }
        putData(table, 0, row, cell(String.valueOf(account.getUserId()), true));
        putData(table, 1, row, cell(account.getFullName(), false));
        putData(table, 2, row, cell(loginId, false));
        putData(table, 3, row, cell(prettyStatus(account.getRole()), false));
        HBox statusCell = new HBox(6);
        statusCell.setAlignment(Pos.CENTER);
        statusCell.getChildren().add(statusBadge(nullSafe(account.getAccountStatus())));
        if (account.isMustChangePassword()) {
            statusCell.getChildren().add(statusBadge("Temp PW"));
        }
        putData(table, 4, row, statusCell);
        putData(table, 5, row, multiActions(actions.toArray(new Button[0])));
        return row + 1;
    }

    private int addActivityRow(GridPane table, int row, ActivityItem item) {
        putData(table, 0, row, cell(item.title(), false));
        putData(table, 1, row, statusBadge(item.status()));
        putData(table, 2, row, cell(DATE_FMT.format(item.date()), false));
        return row + 1;
    }

    // ------------------------------------------------------------------ dialogs

    private static final String WALK_IN_LABEL = "— Walk-in found item (no student report) —";

    private void showRegisterItemDialog(Report preselected) {
        // Hard rule: only Found reports (or walk-ins) may create inventory rows.
        if (preselected != null && preselected.isLostReport()) {
            info("Cannot register a Lost report",
                    "A Lost report means the student is missing an item — nothing was turned in.\n\n"
                    + "• Register inventory from a Found report or a walk-in find.\n"
                    + "• Then use Match on the Lost report to link it to that found item.");
            if (canMatchLostReport(preselected)) {
                showMatchLostToInventoryDialog(preselected);
            }
            return;
        }
        if (preselected != null && !canRegisterFoundTip(preselected)) {
            info("Not open for registration",
                    "Only pending/under-review Found reports can be registered into inventory.");
            return;
        }

        Map<String, Report> openReports = loadFoundTipsByLabel();
        if (preselected != null && preselected.isFoundReport()) {
            openReports.putIfAbsent(reportLabel(preselected), preselected);
        }

        DialogPaneBuilder dialog = new DialogPaneBuilder("Register Found Item");
        dialog.help("Inventory is only for items physically received: Found reports from students, "
                + "or walk-in finds. Lost reports are matched to existing inventory — never registered as new items.");
        dialog.setPrefWidth(520);

        List<String> reportChoices = new ArrayList<>();
        reportChoices.add(WALK_IN_LABEL);
        reportChoices.addAll(openReports.keySet());
        ComboBox<String> reportCombo = dialog.combo(
                "Found report / walk-in",
                reportChoices.toArray(String[]::new)
        );
        if (preselected != null && preselected.isFoundReport()) {
            reportCombo.getSelectionModel().select(reportLabel(preselected));
        } else {
            reportCombo.getSelectionModel().select(WALK_IN_LABEL);
        }

        TextField itemName = dialog.field("Item name");
        ComboBox<String> category = dialog.combo("Category", "Devices", "Personal Belongings");
        ComboBox<String> subCategory = dialog.combo("Subcategory", deviceSubcategories());
        TextField brand = dialog.field("Brand");
        TextField color = dialog.field("Color");
        // Always editable: admin confirms unique ID seen on the physical item.
        // Prefills from the student report so it later appears in claim verification.
        TextArea uniqueId = dialog.area("Unique identification");
        uniqueId.setPromptText(
                "Write marks you see on the item (sticker, scratch, contents, engravings). "
                + "This is saved for claim verification."
        );

        ComboBox<String> shelf = dialog.combo("Storage shelf (size)",
                SHELF_BIG, SHELF_MEDIUM, SHELF_SMALL);
        ComboBox<String> storageBox = dialog.combo("Storage box", boxesForShelf(SHELF_MEDIUM));
        BrandedDialog.PhotoPick photoPick = dialog.photoPicker("Item photo (required)");
        if (preselected != null && preselected.getPhotoPath() != null
                && !preselected.getPhotoPath().isBlank()) {
            photoPick.setPath(preselected.getPhotoPath());
        }

        category.valueProperty().addListener((obs, oldVal, newVal) -> {
            String[] options = "Devices".equals(newVal)
                    ? deviceSubcategories()
                    : personalSubcategories();
            subCategory.getItems().setAll(options);
            subCategory.getSelectionModel().selectFirst();
        });

        Runnable applySource = () -> {
            boolean walkIn = WALK_IN_LABEL.equals(reportCombo.getValue());
            setFieldLocked(itemName, !walkIn);
            category.setDisable(!walkIn);
            subCategory.setDisable(!walkIn);
            setFieldLocked(brand, !walkIn);
            setFieldLocked(color, !walkIn);
            setFieldLocked(uniqueId, false);
            if (walkIn) {
                if (itemName.getText().isBlank()) itemName.clear();
                category.getSelectionModel().selectFirst();
                subCategory.getItems().setAll(deviceSubcategories());
                subCategory.getSelectionModel().selectFirst();
                uniqueId.clear();
                shelf.getSelectionModel().select(SHELF_MEDIUM);
                storageBox.getItems().setAll(boxesForShelf(SHELF_MEDIUM));
                storageBox.getSelectionModel().selectFirst();
                return;
            }
            Report report = openReports.get(reportCombo.getValue());
            if (report == null) return;
            itemName.setText(nullSafe(report.getItemName()));
            selectCategoryAndSubcategory(category, subCategory, report.getCategory(), report.getSubCategory());
            brand.setText(nullSafe(report.getBrand()));
            color.setText(nullSafe(report.getColor()));
            // Auto-load from student report; admin may edit after inspecting the item.
            uniqueId.setText(nullSafe(report.getDescription()));
            if (report.getPhotoPath() != null && !report.getPhotoPath().isBlank()) {
                photoPick.setPath(report.getPhotoPath());
            }
            String suggested = suggestedShelfFor(report.getSubCategory());
            shelf.getSelectionModel().select(suggested);
            storageBox.getItems().setAll(boxesForShelf(suggested));
            storageBox.getSelectionModel().selectFirst();
        };
        reportCombo.valueProperty().addListener((obs, oldVal, newVal) -> applySource.run());
        shelf.valueProperty().addListener((obs, oldVal, newVal) -> {
            String[] boxes = boxesForShelf(newVal);
            storageBox.getItems().setAll(boxes);
            storageBox.getSelectionModel().selectFirst();
        });
        applySource.run();

        dialog.show(() -> {
            boolean walkIn = WALK_IN_LABEL.equals(reportCombo.getValue());
            Report source = walkIn ? null : openReports.get(reportCombo.getValue());
            if ((!walkIn && source == null)
                    || itemName.getText().isBlank()
                    || category.getValue() == null
                    || subCategory.getValue() == null
                    || uniqueId.getText().isBlank()
                    || shelf.getValue() == null
                    || storageBox.getValue() == null) {
                info("Incomplete",
                        "Provide item name, category, subcategory, unique identification, shelf, and storage box"
                        + (walkIn ? "." : ", and select a student report."));
                return false;
            }
            if (photoPick.storedPath == null || photoPick.storedPath.isBlank()) {
                info("Photo required", "Please attach an item photo before registering.");
                return false;
            }

            // Re-check linked report from DB — Found reports only; Lost can never create inventory.
            if (source != null) {
                Optional<Report> fresh = Report.findById(source.getReportId());
                if (fresh.isEmpty() || !canRegisterFoundTip(fresh.get())) {
                    info("Cannot register",
                            "Only open Found reports can be registered into inventory.\n"
                            + "Lost reports must be matched to an item that was already found and logged.");
                    return false;
                }
                source = fresh.get();
            }

            String name = itemName.getText().trim();
            String cat = category.getValue();
            String brandVal = brand.getText().trim();
            String colorVal = color.getText().trim();
            String unique = uniqueId.getText().trim();
            Optional<Inventory> dup = Inventory.findDuplicateActive(
                    name, cat, colorVal, brandVal, unique);
            if (dup.isPresent()) {
                Inventory existing = dup.get();
                info("Duplicate item",
                        "This item looks already registered in inventory.\n"
                        + "Existing tracking #: " + nullSafe(existing.getTrackingNumber())
                        + "\nStatus: " + prettyStatus(existing.getItemStatus())
                        + "\n\nRegistering the same item again is blocked to prevent spam.");
                return false;
            }

            Inventory item = new Inventory();
            item.setAdminUserId(user.getUserId());
            item.setItemName(name);
            item.setCategory(cat);
            item.setSubCategory(subCategory.getValue());
            item.setBrand(brandVal);
            item.setColor(colorVal);
            item.setDescription("");
            item.setPrivateDetails(unique);
            item.setStorageLocation(shelf.getValue());
            item.setStorageBox(storageBox.getValue());
            item.setTrackingNumber(item.generateTrackingNumber());
            item.setItemStatus("AVAILABLE");
            item.setFoundDate(LocalDate.now());
            if (source != null
                    && photoPick.storedPath.equals(source.getPhotoPath())
                    && source.getPhotoData() != null
                    && source.getPhotoData().length > 0) {
                item.setPhotoPath(photoPick.storedPath);
                item.setPhotoData(source.getPhotoData());
            } else {
                item.setPhoto(photoPick.storedPath);
            }
            if (!item.registerItem()) {
                info("Failed",
                        "Could not register item. It may already exist as a duplicate active inventory row.");
                return false;
            }
            item.assignStorageBox();
            AuditLog.record("REGISTER_ITEM", "INVENTORY", item.getTrackingNumber(),
                    (source == null ? "Walk-in" : "From " + source.getDisplayId())
                            + " — " + item.getItemName());

            if (source != null) {
                Report statusUpdate = new Report();
                statusUpdate.setReportId(source.getReportId());
                // Found tip logged into inventory — not "Claimed" (that is owner pickup only).
                statusUpdate.setReportStatus("REGISTERED");
                statusUpdate.updateReportStatus();
                Notification note = new Notification();
                note.setUserId(source.getUserId());
                note.setReportId(source.getReportId());
                note.setMessage("Thank you. Your found-item report (" + source.getItemName()
                        + ") was registered in Lost & Found inventory (" + item.getTrackingNumber()
                        + "). Status: Registered.");
                note.notifyReportUpdate();
            }

            // Lift On Hold Lost reports that now overlap this Found item → Under Review.
            int released = Report.promoteOnHoldLostReportsForItem(item);
            navigate(Nav.INVENTORY);
            String toast = (source == null ? "Walk-in registered — " : "Found report registered — ")
                    + item.getTrackingNumber();
            if (source != null) toast += " · " + source.getDisplayId() + " Registered";
            if (released > 0) {
                toast += " · " + released + " On Hold lost report"
                        + (released == 1 ? "" : "s") + " → Under Review";
            }
            Toast.show(stage, toast);
            // Real-world next step: verify if any open Lost report matches this Found item.
            offerVerifyLostMatchesForFoundItem(item);
            return true;
        });
    }

    /**
     * After a Found item is logged, ask admin to verify overlapping Lost reports
     * (owner reported missing → finder turned item in → staff confirms they match).
     */
    private void offerVerifyLostMatchesForFoundItem(Inventory foundItem) {
        if (foundItem == null) return;
        List<MatchScorer.LostHit> hits = MatchScorer.rankLostReportsForItem(foundItem);
        if (hits.isEmpty()) return;
        MatchScorer.LostHit top = hits.get(0);
        String summary = hits.size() + " open Lost report" + (hits.size() == 1 ? "" : "s")
                + " may match " + foundItem.getTrackingNumber() + ".\n\n"
                + "Top: " + top.scoreLabel() + " " + top.confidenceWord()
                + " — " + top.report().getDisplayId() + " · " + top.report().getItemName()
                + "\nWhy: " + top.reasonLine()
                + "\n\nVerify Lost ↔ Found now? (Owner can claim only after you confirm the match.)";
        if (BrandedDialog.confirm(stage, "Verify Lost ↔ Found?", summary)) {
            showMatchLostToInventoryDialog(top.report(), foundItem);
        }
    }

    /** Open Found reports only — Lost reports are excluded from inventory registration. */
    private Map<String, Report> loadFoundTipsByLabel() {
        Map<String, Report> map = new LinkedHashMap<>();
        for (Report report : Report.listFoundTipsNeedingRegistration()) {
            map.put(reportLabel(report), report);
        }
        return map;
    }

    private static String reportLabel(Report report) {
        String kind = report.isFoundReport() ? "Found report" : "Lost report";
        return report.getDisplayId() + " — " + report.getItemName()
                + " [" + kind + "] (" + nullSafe(report.getSubCategory()) + ")";
    }

    private void showMatchLostToInventoryDialog(Report lostReport) {
        showMatchLostToInventoryDialog(lostReport, null);
    }

    /**
     * Admin verifies: student's Lost report ↔ Found item already in inventory.
     * Does not create inventory — Found report / walk-in registration does that first.
     */
    private void showMatchLostToInventoryDialog(Report lostReport, Inventory preferredFound) {
        Report live = Report.findById(lostReport.getReportId()).orElse(lostReport);
        if (!canMatchLostReport(live)) {
            info("Cannot match",
                    "Only open Lost reports can be matched. Found reports use Register instead.");
            return;
        }

        List<MatchScorer.ScoredMatch> ranked = MatchScorer.rankForReport(live);
        if (ranked.isEmpty() && preferredFound != null) {
            MatchScorer.ScoredMatch preferredScore = MatchScorer.score(live, preferredFound);
            if (preferredScore.isSuggestedMatch()) {
                ranked = List.of(preferredScore);
            }
        }
        if (ranked.isEmpty()) {
            info("No suggested matches (70%–100%)",
                    "Suggested matches require at least "
                    + MatchScorer.MIN_SUGGEST_SCORE + "% confidence.\n\n"
                    + "A Found report (or walk-in) must be registered into inventory first, "
                    + "and details must overlap enough to reach the 70%–100% window.");
            return;
        }

        DialogPaneBuilder dialog = new DialogPaneBuilder("Verify Lost ↔ Found");
        dialog.help("Compare the Lost report and Found inventory item side by side (photo + key fields). "
                + "Confirm they describe the same item. This does not add inventory — it only marks the Lost report Matched so the owner can claim.");
        dialog.setPrefWidth(820);

        Map<String, Inventory> choices = new LinkedHashMap<>();
        String preferredKey = null;
        for (MatchScorer.ScoredMatch m : ranked) {
            Inventory item = m.item();
            String label = m.scoreLabel() + " " + m.confidenceWord()
                    + " — " + item.getTrackingNumber()
                    + " · " + item.getItemName()
                    + " (" + nullSafe(item.getColor()) + " / " + nullSafe(item.getBrand()) + ")";
            choices.put(label, item);
            if (preferredFound != null && item.getItemId() == preferredFound.getItemId()) {
                preferredKey = label;
            }
        }
        ComboBox<String> pick = dialog.combo("Found item (turned in)",
                choices.keySet().toArray(String[]::new));
        if (preferredKey != null) pick.getSelectionModel().select(preferredKey);

        HBox sideBySide = new HBox(14);
        sideBySide.setAlignment(Pos.TOP_CENTER);
        sideBySide.setMaxWidth(Double.MAX_VALUE);
        dialog.addCustom("Compare", sideBySide);

        VBox confidenceBox = new VBox(8);
        confidenceBox.setMaxWidth(Double.MAX_VALUE);
        dialog.addCustom("Match quality", confidenceBox);

        VBox fieldHints = new VBox(6);
        fieldHints.setMaxWidth(Double.MAX_VALUE);
        dialog.addCustom("Field check", fieldHints);

        Runnable refreshFoundSide = () -> {
            Inventory selected = choices.get(pick.getValue());
            sideBySide.getChildren().clear();
            confidenceBox.getChildren().clear();
            fieldHints.getChildren().clear();

            if (selected == null) {
                VBox lostEmpty = matchCompareCard(
                        "Lost (owner)",
                        live.getPhotoPath(),
                        live.getPhotoData(),
                        null,
                        live.getDisplayId() + " — " + live.getItemName(),
                        "Category: " + nullSafe(live.getCategory()) + " / " + nullSafe(live.getSubCategory()),
                        "Color: " + nullSafe(live.getColor()),
                        "Brand: " + nullSafe(live.getBrand()),
                        "Location: " + nullSafe(live.getLostLocation()),
                        "Details: " + nullSafe(live.getDescription())
                );
                VBox foundEmpty = matchCompareCard(
                        "Found (inventory)", null, null, null, "No item selected");
                HBox.setHgrow(lostEmpty, Priority.ALWAYS);
                HBox.setHgrow(foundEmpty, Priority.ALWAYS);
                sideBySide.getChildren().addAll(lostEmpty, foundEmpty);
                confidenceBox.getChildren().add(matchQualityBanner(
                        -1, "Select a Found inventory item to compare.", ""));
                return;
            }

            MatchScorer.ScoredMatch scored = MatchScorer.score(live, selected);
            String glowHex = matchGlowColor(scored.score());

            VBox lostCard = matchCompareCard(
                    "Lost (owner)",
                    live.getPhotoPath(),
                    live.getPhotoData(),
                    glowHex,
                    live.getDisplayId() + " — " + live.getItemName(),
                    "Category: " + nullSafe(live.getCategory()) + " / " + nullSafe(live.getSubCategory()),
                    "Color: " + nullSafe(live.getColor()),
                    "Brand: " + nullSafe(live.getBrand()),
                    "Location: " + nullSafe(live.getLostLocation()),
                    "Details: " + nullSafe(live.getDescription())
            );
            VBox foundCard = matchCompareCard(
                    "Found (inventory)",
                    selected.getPhotoPath(),
                    selected.getPhotoData(),
                    glowHex,
                    selected.getTrackingNumber() + " — " + selected.getItemName(),
                    "Category: " + nullSafe(selected.getCategory())
                            + " / " + nullSafe(selected.getSubCategory()),
                    "Color: " + nullSafe(selected.getColor()),
                    "Brand: " + nullSafe(selected.getBrand()),
                    "Storage: " + nullSafe(selected.getStorageBox())
                            + " · " + nullSafe(selected.getStorageLocation()),
                    "Unique marks: " + nullSafe(selected.getPrivateDetails())
            );
            HBox.setHgrow(lostCard, Priority.ALWAYS);
            HBox.setHgrow(foundCard, Priority.ALWAYS);
            sideBySide.getChildren().addAll(lostCard, foundCard);

            confidenceBox.getChildren().add(matchQualityBanner(
                    scored.score(),
                    scored.scoreLabel() + " · " + matchQualityLabel(scored.score())
                            + " (" + scored.confidenceWord() + ")",
                    scored.reasonLine()
            ));

            fieldHints.getChildren().addAll(
                    matchFieldHint("Item name", live.getItemName(), selected.getItemName()),
                    matchFieldHint("Category",
                            nullSafe(live.getCategory()) + " / " + nullSafe(live.getSubCategory()),
                            nullSafe(selected.getCategory()) + " / " + nullSafe(selected.getSubCategory())),
                    matchFieldHint("Color", live.getColor(), selected.getColor()),
                    matchFieldHint("Brand", live.getBrand(), selected.getBrand())
            );
        };
        pick.valueProperty().addListener((o, a, b) -> refreshFoundSide.run());
        refreshFoundSide.run();

        dialog.show(() -> {
            Inventory selected = choices.get(pick.getValue());
            if (selected == null) {
                info("Incomplete", "Choose the Found inventory item that matches this Lost report.");
                return false;
            }
            Optional<Report> fresh = Report.findById(live.getReportId());
            if (fresh.isEmpty() || !canMatchLostReport(fresh.get())) {
                info("Already handled", "This Lost report is no longer open for matching.");
                return false;
            }
            Optional<Report> conflict = Report.findOtherLostMatchedToItem(
                    selected.getItemId(), live.getReportId());
            if (conflict.isPresent()) {
                if (!BrandedDialog.confirm(stage, "Same Found item already matched",
                        conflict.get().getDisplayId() + " (" + conflict.get().getItemName()
                                + ") is already matched to " + selected.getTrackingNumber() + ".\n\n"
                                + "Match " + live.getDisplayId() + " to the same item anyway?")) {
                    return false;
                }
            }
            if (!BrandedDialog.confirm(stage, "Confirm Lost ↔ Found match?",
                    "Mark " + live.getDisplayId() + " as Matched to "
                            + selected.getTrackingNumber() + "?\n\n"
                            + "The owner can then submit a claim. You will still verify ownership before pickup.")) {
                return false;
            }
            Report statusUpdate = new Report();
            statusUpdate.setReportId(live.getReportId());
            // Sticky link: remember which Found inventory item this Lost report matched.
            if (!statusUpdate.updateMatchToItem(selected.getItemId())) {
                info("Failed", "Could not mark the Lost report as Matched.");
                return false;
            }
            Notification note = new Notification();
            note.setUserId(live.getUserId());
            note.setReportId(live.getReportId());
            note.setMessage("Good news: your Lost report (" + live.getItemName()
                    + ") was matched to a Found item in inventory (" + selected.getTrackingNumber()
                    + "). Submit a claim with ownership answers so staff can verify you are the owner.");
            note.notifyPossibleMatch();
            AuditLog.record("MATCH", "REPORT", live.getDisplayId(),
                    "Matched to inventory " + selected.getTrackingNumber()
                            + " (" + selected.getItemName() + ")");
            Toast.show(stage, "Verified: " + live.getDisplayId() + " ↔ " + selected.getTrackingNumber());
            navigate(Nav.MANAGE_REPORTS);
            return true;
        });
    }

    /**
     * Side-by-side card used in the Match dialog (photo + key fields).
     * {@code glowHex} tints the border and outer glow by match quality (green / yellow / red).
     */
    private VBox matchCompareCard(String title, String photoPath, byte[] photoData,
                                  String glowHex, String... lines) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setMaxWidth(Double.MAX_VALUE);
        String border = glowHex != null ? glowHex : UiTheme.BORDER;
        card.setStyle(
                "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-radius: 12;"
                + "-fx-border-width: " + (glowHex != null ? "2" : "1") + ";"
        );
        if (glowHex != null) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web(glowHex).deriveColor(0, 1, 1, 0.55));
            glow.setRadius(22);
            glow.setSpread(0.28);
            glow.setOffsetY(0);
            card.setEffect(glow);
        }
        Label head = new Label(title);
        head.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: "
                + (glowHex != null ? glowHex : UiTheme.UE_RED) + ";"
        );
        card.getChildren().addAll(head, photoThumb(photoPath, photoData, 148));
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            Label row = new Label(line);
            row.setWrapText(true);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setStyle("-fx-text-fill: " + UiTheme.TEXT + "; -fx-font-size: 12px;");
            card.getChildren().add(row);
        }
        return card;
    }

    /** Green = strong, yellow = suggested (70–89), red = below suggest window. */
    private static String matchGlowColor(int score) {
        if (score >= 90) return "#16A34A";
        if (score >= MatchScorer.MIN_SUGGEST_SCORE) return "#CA8A04";
        return "#DC2626";
    }

    private static String matchQualityLabel(int score) {
        if (score >= 90) return "Strong match";
        if (score >= MatchScorer.MIN_SUGGEST_SCORE) return "Suggested match (70%+)";
        return "Below 70% — not suggested";
    }

    private static String matchGlowSoftBg(int score) {
        if (score < 0) return "rgba(255,255,255,0.78)";
        if (score >= 90) return "rgba(220, 252, 231, 0.92)";
        if (score >= MatchScorer.MIN_SUGGEST_SCORE) return "rgba(254, 249, 195, 0.92)";
        return "rgba(254, 226, 226, 0.92)";
    }

    private Node matchQualityBanner(int score, String title, String detail) {
        VBox banner = new VBox(4);
        banner.setPadding(new Insets(12, 14, 12, 14));
        banner.setMaxWidth(Double.MAX_VALUE);
        String glow = score < 0 ? UiTheme.BORDER : matchGlowColor(score);
        banner.setStyle(
                "-fx-background-color: " + matchGlowSoftBg(score) + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + glow + ";"
                + "-fx-border-radius: 12;"
                + "-fx-border-width: 1.5;"
        );
        if (score >= 0) {
            DropShadow glowFx = new DropShadow();
            glowFx.setColor(Color.web(glow).deriveColor(0, 1, 1, 0.45));
            glowFx.setRadius(16);
            glowFx.setSpread(0.2);
            banner.setEffect(glowFx);
        }
        Label head = new Label(title);
        head.setWrapText(true);
        head.setStyle(
                "-fx-text-fill: " + (score < 0 ? UiTheme.TEXT : glow) + ";"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
        );
        banner.getChildren().add(head);
        if (detail != null && !detail.isBlank()) {
            Label body = new Label(detail);
            body.setWrapText(true);
            body.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
            banner.getChildren().add(body);
        }
        return banner;
    }

    /** Per-field hint: exact = green, similar = yellow, different = red. */
    private Node matchFieldHint(String label, String lostVal, String foundVal) {
        String a = lostVal == null ? "" : lostVal.trim();
        String b = foundVal == null ? "" : foundVal.trim();
        int tier; // 2 exact, 1 similar, 0 different, -1 missing
        String status;
        if (a.isBlank() || b.isBlank() || "—".equals(a) || "—".equals(b)) {
            tier = -1;
            status = "Hard to compare";
        } else if (a.equalsIgnoreCase(b)) {
            tier = 2;
            status = "Exact match";
        } else {
            String x = a.toLowerCase(Locale.ENGLISH);
            String y = b.toLowerCase(Locale.ENGLISH);
            boolean similar = x.contains(y) || y.contains(x);
            if (!similar) {
                for (String token : x.split("[^a-z0-9]+")) {
                    if (token.length() >= 3 && y.contains(token)) {
                        similar = true;
                        break;
                    }
                }
            }
            if (similar) {
                tier = 1;
                status = "Partial match";
            } else {
                tier = 0;
                status = "Does not match";
            }
        }
        String color = tier == 2 ? "#16A34A" : tier == 1 ? "#CA8A04" : tier == 0 ? "#DC2626" : UiTheme.TEXT_MUTED;
        String tint = tier == 2 ? "rgba(220,252,231,0.88)"
                : tier == 1 ? "rgba(254,249,195,0.88)"
                : tier == 0 ? "rgba(254,226,226,0.88)"
                : UiTheme.FIELD_BG;

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle(
                "-fx-background-color: " + tint + ";"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: " + color + "88;"
                + "-fx-border-radius: 10;"
        );
        if (tier >= 0) {
            DropShadow soft = new DropShadow();
            soft.setColor(Color.web(color).deriveColor(0, 1, 1, 0.35));
            soft.setRadius(10);
            soft.setSpread(0.15);
            row.setEffect(soft);
        }
        Circle dot = new Circle(5, Color.web(color));
        Label name = new Label(label);
        name.setMinWidth(78);
        name.setStyle("-fx-text-fill: " + UiTheme.TEXT + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label verdict = new Label(status);
        verdict.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label values = new Label(nullSafe(a) + "  ↔  " + nullSafe(b));
        values.setWrapText(true);
        values.setMaxWidth(Double.MAX_VALUE);
        values.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 11.5px;");
        row.getChildren().addAll(dot, name, verdict, grow, values);
        return row;
    }

    private Node photoThumb(String storedPath, byte[] photoData, double size) {
        StackPane frame = new StackPane();
        frame.setPrefSize(size, size);
        frame.setMinSize(size, size);
        frame.setMaxSize(size, size);
        frame.setStyle(
                "-fx-background-color: " + UiTheme.CARD_BG + ";"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-radius: 10;"
        );
        Image image = PhotoStorage.loadImage(storedPath, photoData, size * 2, size * 2);
        if (image != null) {
            ImageView view = new ImageView(image);
            view.setFitWidth(size - 4);
            view.setFitHeight(size - 4);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            frame.getChildren().add(view);
            frame.setStyle(frame.getStyle() + "-fx-cursor: hand;");
            frame.setOnMouseClicked(e -> showPhotoLightbox(storedPath, photoData, "Photo preview"));
            Label hint = new Label("Click to enlarge");
            hint.setStyle(
                    "-fx-text-fill: white;"
                    + "-fx-font-size: 10px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-background-color: rgba(20,20,22,0.55);"
                    + "-fx-background-radius: 8;"
                    + "-fx-padding: 3 8 3 8;"
            );
            StackPane.setAlignment(hint, Pos.BOTTOM_CENTER);
            StackPane.setMargin(hint, new Insets(0, 0, 6, 0));
            frame.getChildren().add(hint);
        } else {
            Label empty = new Label("No photo");
            empty.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
            frame.getChildren().add(empty);
        }
        return frame;
    }

    /** Full-size photo viewer for side-by-side Lost ↔ Found comparison. */
    private void showPhotoLightbox(String storedPath, byte[] photoData, String title) {
        Image full = PhotoStorage.loadImage(storedPath, photoData, 900, 900);
        if (full == null) {
            info("No photo", "This item has no photo on file.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initOwner(stage);
        alert.setTitle(title == null ? "Photo" : title);
        alert.setHeaderText(null);
        ImageView view = new ImageView(full);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(Math.min(560, Math.max(280, full.getWidth())));
        view.setFitHeight(Math.min(520, Math.max(240, full.getHeight())));
        StackPane wrap = new StackPane(view);
        wrap.setPadding(new Insets(8));
        wrap.setStyle(
                "-fx-background-color: " + UiTheme.CARD_BG + ";"
                + "-fx-background-radius: 12;"
        );
        alert.getDialogPane().setContent(wrap);
        alert.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        alert.getDialogPane().setPrefWidth(620);
        alert.showAndWait();
    }

    /** Colored % chip — green strong / yellow partial / red weak. */
    private Node matchScoreBadge(int score) {
        String color = matchGlowColor(score);
        Label badge = new Label(score + "% · " + matchQualityLabel(score));
        badge.setStyle(
                "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 10px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 4 10 4 10;"
        );
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(color).deriveColor(0, 1, 1, 0.45));
        glow.setRadius(10);
        glow.setSpread(0.2);
        badge.setEffect(glow);
        return badge;
    }

    /** Open the aging case directly instead of only jumping to the list page. */
    private void openAgingItem(CampusInsights.AgingItem a) {
        if (a == null) return;
        if (a.kind() != null && a.kind().startsWith("Claim")) {
            navigate(Nav.MANAGE_CLAIMS);
            Claim.viewAllClaims().stream()
                    .filter(c -> a.id().equals(c.getDisplayId()))
                    .findFirst()
                    .ifPresentOrElse(this::showClaimDetails,
                            () -> Toast.show(stage, "Claim " + a.id() + " not found — check Manage Claims."));
            return;
        }
        navigate(Nav.MANAGE_REPORTS);
        Report probe = new Report();
        probe.setUserId(0);
        probe.filterReports().stream()
                .filter(r -> a.id().equals(r.getDisplayId()))
                .findFirst()
                .ifPresentOrElse(this::showReportDetails,
                        () -> Toast.show(stage, "Report " + a.id() + " not found — check Manage Reports."));
    }

    private static void setFieldLocked(TextField field, boolean locked) {
        field.setEditable(!locked);
        field.setFocusTraversable(!locked);
    }

    private static void setFieldLocked(TextArea area, boolean locked) {
        area.setEditable(!locked);
        area.setFocusTraversable(!locked);
    }

    private static final String SHELF_BIG = "Big Shelf (large items)";
    private static final String SHELF_MEDIUM = "Medium Shelf (medium items)";
    private static final String SHELF_SMALL = "Small Shelf (small items)";

    private static String[] deviceSubcategories() {
        return new String[]{
                "Cellphones",
                "Power banks",
                "Chargers",
                "Adapters",
                "Electric fans",
                "Earphones",
                "Headphones",
                "Laptops / tablets",
                "USB flash drives",
                "Other devices"
        };
    }

    private static String[] personalSubcategories() {
        return new String[]{
                "Wallets",
                "IDs",
                "Keychains",
                "Bags",
                "School supplies",
                "Umbrellas",
                "Eyewear",
                "Clothing / jackets",
                "Water bottles",
                "Other personal belongings"
        };
    }

    private static void selectCategoryAndSubcategory(
            ComboBox<String> category,
            ComboBox<String> subCategory,
            String categoryValue,
            String subCategoryValue
    ) {
        if (categoryValue != null && !categoryValue.isBlank()) {
            if (!category.getItems().contains(categoryValue)) {
                category.getItems().add(categoryValue);
            }
            category.getSelectionModel().select(categoryValue);
        } else {
            category.getSelectionModel().selectFirst();
        }
        String[] options = "Devices".equals(category.getValue())
                ? deviceSubcategories()
                : personalSubcategories();
        subCategory.getItems().setAll(options);
        if (subCategoryValue != null && !subCategoryValue.isBlank()) {
            if (!subCategory.getItems().contains(subCategoryValue)) {
                subCategory.getItems().add(subCategoryValue);
            }
            subCategory.getSelectionModel().select(subCategoryValue);
        } else {
            subCategory.getSelectionModel().selectFirst();
        }
    }

    /** Boxes inside each size shelf. */
    private static String[] boxesForShelf(String shelf) {
        if (SHELF_BIG.equals(shelf)) {
            return new String[]{
                    "Box B-1", "Box B-2", "Box B-3", "Box B-4", "Box B-5"
            };
        }
        if (SHELF_SMALL.equals(shelf)) {
            return new String[]{
                    "Box S-1", "Box S-2", "Box S-3", "Box S-4",
                    "Box S-5", "Box S-6", "Box S-7", "Box S-8"
            };
        }
        return new String[]{
                "Box M-1", "Box M-2", "Box M-3", "Box M-4", "Box M-5", "Box M-6"
        };
    }

    /** Suggest shelf size from the report subcategory. */
    private static String suggestedShelfFor(String subCategory) {
        if (subCategory == null) return SHELF_MEDIUM;
        String key = subCategory.toLowerCase(Locale.ENGLISH);
        if (key.contains("laptop") || key.contains("bag") || key.contains("jacket")
                || key.contains("clothing") || key.contains("fan") || key.contains("umbrella")) {
            return SHELF_BIG;
        }
        if (key.contains("wallet") || key.contains("id") || key.contains("key")
                || key.contains("earphone") || key.contains("usb") || key.contains("charger")
                || key.contains("adapter") || key.contains("eyewear") || key.contains("flash")) {
            return SHELF_SMALL;
        }
        return SHELF_MEDIUM;
    }

    private void showReportDetails(Report report) {
        report.viewReport();
        String eyebrow = report.isFoundReport() ? "Found report" : "Lost report";
        String matchedLine = "";
        if (report.isLostReport() && report.getMatchedItemId() != null) {
            matchedLine = Inventory.findById(report.getMatchedItemId())
                    .map(i -> "\nMatched Found item: " + nullSafe(i.getTrackingNumber())
                            + " — " + i.getItemName())
                    .orElse("\nMatched Found item id: #" + report.getMatchedItemId());
        }
        String body = report.getItemName()
                + "\n\nType: " + (report.isFoundReport() ? "Found (turned in)" : "Lost (owner missing item)")
                + "\nCategory: " + report.getCategory()
                + "\nSubcategory: " + nullSafe(report.getSubCategory())
                + "\nColor: " + nullSafe(report.getColor())
                + "\nBrand: " + nullSafe(report.getBrand())
                + "\nLocation: " + nullSafe(report.getLostLocation())
                + "\nStatus: " + reportStatusLabel(report)
                + matchedLine
                + "\nSubmitted: " + DATE_FMT.format(report.getDateSubmitted())
                + "\n\nDescription:\n" + nullSafe(report.getDescription());
        ButtonType timelineBtn = new ButtonType("Timeline");
        BrandedDialog.details(stage, "Report " + report.getDisplayId(), eyebrow, body,
                        reportPhotoPreview(report), timelineBtn)
                .ifPresent(result -> {
                    if (result == timelineBtn) {
                        List<Claim> related = Claim.viewAllClaims().stream()
                                .filter(c -> c.getReportId() != null && c.getReportId() == report.getReportId())
                                .toList();
                        StatusTimeline.showForReport(stage, report, related,
                                Notification.forUserReport(report.getUserId(), report.getReportId()));
                    }
                });
    }

    private Node reportPhotoPreview(Report report) {
        Image image = PhotoStorage.loadImage(report.getPhotoPath(), report.getPhotoData(), 560, 420);
        if (image == null) {
            Label empty = new Label("No photo attached");
            empty.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12.5px;");
            return empty;
        }
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(280);
        view.setFitHeight(200);
        StackPane frame = new StackPane(view);
        frame.setMaxWidth(Double.MAX_VALUE);
        frame.setPadding(new Insets(8));
        frame.setStyle(
                "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 12;"
        );
        return frame;
    }

    private void showReportMatches(Report report) {
        Report probe = Report.findById(report.getReportId()).orElse(report);
        if (!probe.allowsStaffMutation()) {
            info("Locked",
                    probe.getDisplayId() + " is " + prettyStatus(probe.getReportStatus())
                    + " — matching and status changes are closed for completed cases.");
            return;
        }
        if (probe.isFoundReport()) {
            info("Found report",
                    "Found reports are registered into inventory — they are not matched like Lost reports.\n"
                    + "Use Register if this report is still open.");
            if (canRegisterFoundTip(probe)) {
                showRegisterItemDialog(probe);
            }
            return;
        }
        List<MatchScorer.ScoredMatch> ranked = MatchScorer.rankForReport(probe);
        if (ranked.isEmpty()) {
            info("No found item yet",
                    "No inventory overlap for this Lost report.\n\n"
                    + "Register a Found report or walk-in first, then match this Lost report.");
            return;
        }
        String body = "Confidence-ranked found items already in inventory for "
                + probe.getDisplayId() + ":\n\n"
                + MatchScorer.formatList(ranked)
                + "\n\nMatch this Lost report to one of these items? "
                + "(This will NOT create a new inventory row.)";
        if (BrandedDialog.confirm(stage,
                ranked.size() + " scored match" + (ranked.size() == 1 ? "" : "es"),
                body)) {
            showMatchLostToInventoryDialog(probe);
        }
    }

    private void showUpdateReportStatusDialog(Report report) {
        Report live = Report.findById(report.getReportId()).orElse(report);
        if (!live.allowsStaffMutation()) {
            info("Locked",
                    live.getDisplayId() + " is " + prettyStatus(live.getReportStatus())
                    + " — status can no longer be changed.");
            return;
        }
        DialogPaneBuilder dialog = new DialogPaneBuilder("Update Report Status");
        dialog.help(live.isLostReport()
                ? "Lost with no inventory → On Hold. When a Found item overlaps, use Match "
                + "(or lift to Under Review / Pending) then confirm. Cancelled keeps audit history."
                : "Found reports are usually Resolved by Register into inventory. "
                + "Cancelled / Archive keep history — nothing is wiped.");
        // Lost: MATCHED should go through Match dialog. Found: no MATCHED shortcut.
        // ARCHIVED uses the dedicated Archive action (guards against active claims).
        String[] statuses = live.isLostReport()
                ? new String[]{"PENDING", "UNDER_REVIEW", "ON_HOLD", "CANCELLED"}
                : new String[]{"PENDING", "UNDER_REVIEW", "REGISTERED", "CANCELLED"};
        ComboBox<String> status = dialog.combo("Status", statuses);
        if (live.getReportStatus() != null) {
            String current = live.getReportStatus().toUpperCase(Locale.ENGLISH);
            if (status.getItems().contains(current)) {
                status.getSelectionModel().select(current);
            }
        }
        dialog.show(() -> {
            Report fresh = Report.findById(live.getReportId()).orElse(live);
            if (!fresh.allowsStaffMutation()) {
                info("Locked", "This report was already completed and can no longer be changed.");
                return false;
            }
            String next = status.getValue();
            if ("MATCHED".equalsIgnoreCase(next) && fresh.isLostReport()) {
                showMatchLostToInventoryDialog(fresh);
                return false;
            }
            if ("CANCELLED".equalsIgnoreCase(next) && Claim.hasActiveClaimForReport(fresh.getReportId())) {
                info("Cannot cancel",
                        "This report has an active claim. Resolve or reject the claim first.");
                return false;
            }
            if ("CANCELLED".equalsIgnoreCase(next)) {
                if (!BrandedDialog.confirm(stage, "Confirm cancel?",
                        "Mark " + fresh.getDisplayId() + " as Cancelled?\n\n"
                        + "The report stays in MySQL for audit (soft-cancel).\n"
                        + "You can Reopen it later from the Cancelled filter.")) {
                    return false;
                }
            }
            Report target = new Report();
            target.setReportId(fresh.getReportId());
            target.setReportStatus(next);
            if (!target.updateReportStatus()) {
                info("Failed", "Could not update status.");
                return false;
            }
            if ("CANCELLED".equalsIgnoreCase(next)) {
                AuditLog.record("REPORT_CANCELLED", "REPORT", String.valueOf(fresh.getReportId()),
                        "Admin marked " + fresh.getDisplayId() + " Cancelled (soft-delete)");
            } else {
                AuditLog.record("REPORT_STATUS", "REPORT", String.valueOf(fresh.getReportId()),
                        fresh.getDisplayId() + " → " + next);
            }
            Notification note = new Notification();
            note.setUserId(fresh.getUserId());
            note.setReportId(fresh.getReportId());
            note.setMessage("Your report (" + fresh.getItemName() + ") is now "
                    + prettyStatus(next) + ".");
            note.notifyReportUpdate();
            if ("CANCELLED".equalsIgnoreCase(next)) {
                reportsRecordFilter = "CANCELLED";
            }
            navigate(Nav.MANAGE_REPORTS);
            return true;
        });
    }

    private void confirmArchiveReport(Report report) {
        if (!report.canBeArchived()) {
            info("Cannot archive",
                    "Matched reports or reports with an active claim cannot be archived.\n"
                    + "Archive keeps the row for audit — it does not delete data.");
            return;
        }
        if (!BrandedDialog.confirm(stage, "Archive report",
                "Archive " + report.getDisplayId() + " — " + report.getItemName() + "?\n\n"
                + "It leaves the Active queue and moves to Archived.\n"
                + "The MySQL row is kept for audit (soft-archive).")) {
            return;
        }
        Report target = new Report();
        target.setReportId(report.getReportId());
        if (!target.archiveReport()) {
            info("Failed", "Could not archive this report.");
            return;
        }
        AuditLog.record("REPORT_ARCHIVED", "REPORT", String.valueOf(report.getReportId()),
                "Archived " + report.getDisplayId() + " (soft-delete — row kept)");
        Toast.success(stage, "Report " + report.getDisplayId() + " archived — kept on file.");
        reportsRecordFilter = "ARCHIVED";
        navigate(Nav.MANAGE_REPORTS);
    }

    private void confirmRestoreReport(Report report) {
        if (!report.canBeRestoredFromArchive()) {
            info("Cannot restore", "Only Archived reports can be restored to Pending.");
            return;
        }
        if (!BrandedDialog.confirm(stage, "Restore report",
                "Restore " + report.getDisplayId() + " to Pending in the Active queue?")) {
            return;
        }
        Report target = new Report();
        target.setReportId(report.getReportId());
        if (!target.restoreFromArchive()) {
            info("Failed", "Could not restore this report.");
            return;
        }
        AuditLog.record("REPORT_RESTORED", "REPORT", String.valueOf(report.getReportId()),
                "Restored " + report.getDisplayId() + " from Archive → Pending");
        Toast.success(stage, "Report " + report.getDisplayId() + " restored to Pending.");
        reportsRecordFilter = "ACTIVE";
        navigate(Nav.MANAGE_REPORTS);
    }

    private void confirmReopenCancelledReport(Report report) {
        if (!report.canBeReopenedFromCancelled()) {
            info("Cannot reopen", "Only Cancelled reports can be reopened to Pending.");
            return;
        }
        if (!BrandedDialog.confirm(stage, "Reopen cancelled report",
                "Reopen " + report.getDisplayId() + " as Pending?\n"
                + "Use this when a student cancelled by mistake.")) {
            return;
        }
        Report target = new Report();
        target.setReportId(report.getReportId());
        if (!target.reopenCancelled()) {
            info("Failed", "Could not reopen this report.");
            return;
        }
        AuditLog.record("REPORT_REOPENED", "REPORT", String.valueOf(report.getReportId()),
                "Reopened " + report.getDisplayId() + " from Cancelled → Pending");
        Notification note = new Notification();
        note.setUserId(report.getUserId());
        note.setReportId(report.getReportId());
        note.setMessage("Your cancelled report (" + report.getItemName()
                + ") was reopened by Lost & Found and is Pending again.");
        note.notifyReportUpdate();
        Toast.success(stage, "Report " + report.getDisplayId() + " reopened to Pending.");
        reportsRecordFilter = "ACTIVE";
        navigate(Nav.MANAGE_REPORTS);
    }

    private void showClaimDetails(Claim claim) {
        claim.viewClaimDetails();
        Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
        String student = User.findById(live.getUserId())
                .map(u -> u.getFullName() + " (" + nullSafe(u.getStudentNumber()) + ")")
                .orElse("Unknown");
        String codeLine = live.getPickupCode() == null || live.getPickupCode().isBlank()
                ? ""
                : "\nPickup code: " + live.getPickupCode();
        String noteLine;
        if ("REJECTED".equalsIgnoreCase(live.getClaimStatus())) {
            String reason = live.getAdminNote();
            noteLine = "\nWhy rejected: "
                    + (reason == null || reason.isBlank() ? "(no reason on file)" : reason.trim());
        } else {
            noteLine = "\nAdmin note: " + nullSafe(live.getAdminNote());
        }
        String body = live.getItemName()
                + "\n\nStudent: " + student
                + "\nStatus: " + prettyStatus(live.getClaimStatus())
                + "\nSubmitted: " + DATE_FMT.format(live.getClaimDate())
                + codeLine
                + "\nAdmin result: " + nullSafe(live.getAdminResult())
                + noteLine
                + "\n\nOwnership answer:\n" + nullSafe(live.getOwnershipAnswer());
        ButtonType timelineBtn = new ButtonType("Timeline");
        BrandedDialog.details(stage, "Claim " + live.getDisplayId(), "Claim details", body, timelineBtn)
                .ifPresent(result -> {
                    if (result == timelineBtn) {
                        Report linked = live.getReportId() == null ? null
                                : Report.findById(live.getReportId()).orElse(null);
                        StatusTimeline.showForClaim(stage, live, linked,
                                Notification.forUserClaim(live.getUserId(), live.getClaimId()));
                    }
                });
    }

    private void showApproveClaimDialog(Claim claim) {
        Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
        if (!isClaimOpenForDecision(live)) {
            info("Locked", live.getDisplayId() + " is already "
                    + prettyStatus(live.getClaimStatus())
                    + (live.getReturnDate() != null ? " (picked up)" : "")
                    + " — approve/reject is closed.");
            return;
        }
        DialogPaneBuilder dialog = new DialogPaneBuilder("Verify & Approve Claim");
        dialog.setPrefWidth(600);

        TextArea studentProof = dialog.area("Student's answers");
        studentProof.setText(nullSafe(claim.getOwnershipAnswer()));
        studentProof.setEditable(false);

        // Unique ID from linked report + will refresh when an inventory item is selected.
        String reportUniqueId = "(No linked report. Select a found item below to see its unique ID.)";
        if (claim.getReportId() != null) {
            reportUniqueId = Report.findById(claim.getReportId())
                    .map(r -> nullSafe(r.getDescription()))
                    .filter(s -> !s.isBlank())
                    .orElse("(No unique identification on the linked report.)");
        }
        TextArea uniqueOnFile = dialog.area("Unique identification on file");
        uniqueOnFile.setText(reportUniqueId);
        uniqueOnFile.setEditable(false);

        Map<String, Inventory> linkableItems = new LinkedHashMap<>();
        linkableItems.put("— Choose later —", null);
        Inventory probe = new Inventory();
        for (Inventory item : probe.filterItems()) {
            String status = item.getItemStatus() == null ? "" : item.getItemStatus().toUpperCase(Locale.ENGLISH);
            if ("AVAILABLE".equals(status) || "CLAIM_PENDING".equals(status)) {
                String label = item.getTrackingNumber() + " — " + item.getItemName()
                        + " (" + nullSafe(item.getColor()) + ")";
                linkableItems.put(label, item);
            }
        }
        for (Inventory item : probe.filterItems()) {
            String status = item.getItemStatus() == null ? "" : item.getItemStatus().toUpperCase(Locale.ENGLISH);
            if (!"AVAILABLE".equals(status) && !"CLAIM_PENDING".equals(status)) continue;
            String proof = nullSafe(claim.getOwnershipAnswer()).toLowerCase(Locale.ENGLISH);
            String itemName = nullSafe(item.getItemName()).toLowerCase(Locale.ENGLISH);
            if (!itemName.isBlank() && proof.contains(itemName)) {
                String label = "Suggested: " + item.getTrackingNumber() + " — " + item.getItemName()
                        + " (" + nullSafe(item.getColor()) + ")";
                linkableItems.putIfAbsent(label, item);
            }
        }

        final Report linkedReport = claim.getReportId() == null ? null
                : Report.findById(claim.getReportId()).orElse(null);
        // Pre-select sticky Lost↔Found match if still available for return.
        String stickyKey = null;
        if (linkedReport != null && linkedReport.getMatchedItemId() != null) {
            Inventory sticky = Inventory.findById(linkedReport.getMatchedItemId()).orElse(null);
            if (sticky != null) {
                String st = sticky.getItemStatus() == null ? "" : sticky.getItemStatus().toUpperCase(Locale.ENGLISH);
                if ("AVAILABLE".equals(st) || "CLAIM_PENDING".equals(st) || "CLAIMED".equals(st)) {
                    stickyKey = "Matched: " + sticky.getTrackingNumber() + " — " + sticky.getItemName()
                            + " (" + nullSafe(sticky.getColor()) + ")";
                    linkableItems.put(stickyKey, sticky);
                }
            }
        }

        ComboBox<String> linkItem = dialog.combo(
                "Found item to return",
                linkableItems.keySet().toArray(String[]::new)
        );
        if (stickyKey != null) {
            linkItem.getSelectionModel().select(stickyKey);
        } else {
            linkableItems.keySet().stream()
                    .filter(k -> k.startsWith("Suggested:"))
                    .findFirst()
                    .ifPresent(linkItem.getSelectionModel()::select);
        }

        VBox matchQualityHost = new VBox();
        matchQualityHost.setMaxWidth(Double.MAX_VALUE);
        dialog.addCustom("Match quality", matchQualityHost);

        TextArea comparison = dialog.area("Quick comparison");
        comparison.setEditable(false);
        final String reportUniqueFinal = reportUniqueId;
        Runnable refreshComparison = () -> {
            Inventory selected = linkableItems.get(linkItem.getValue());
            String proof = nullSafe(claim.getOwnershipAnswer());
            StringBuilder sb = new StringBuilder();
            sb.append("Compare the student's answers with the unique identification on file.\n\n");
            matchQualityHost.getChildren().clear();

            String itemUnique = selected == null ? "" : nullSafe(selected.getPrivateDetails());
            if (selected != null) {
                uniqueOnFile.setText(
                        (itemUnique.isBlank() ? reportUniqueFinal : itemUnique)
                );
                if (linkedReport != null) {
                    MatchScorer.ScoredMatch scored = MatchScorer.score(linkedReport, selected);
                    matchQualityHost.getChildren().add(matchQualityBanner(
                            scored.score(),
                            scored.scoreLabel() + " · " + matchQualityLabel(scored.score())
                                    + " (" + scored.confidenceWord() + ")",
                            scored.reasonLine()
                    ));
                    sb.append("Match confidence: ").append(scored.scoreLabel())
                            .append(" (").append(scored.confidenceWord()).append(")\n")
                            .append("Why: ").append(scored.reasonLine()).append("\n\n");
                } else {
                    matchQualityHost.getChildren().add(matchQualityBanner(
                            -1, "No linked Lost report to score against this item.", ""));
                }
                sb.append(plainCompareLine("Item name", nullSafe(selected.getItemName()), proof));
                sb.append(plainCompareLine("Color", nullSafe(selected.getColor()), proof));
                sb.append(plainCompareLine("Brand", nullSafe(selected.getBrand()), proof));
                sb.append(plainCompareLine("Unique identification",
                        itemUnique.isBlank() ? reportUniqueFinal : itemUnique, proof));
            } else {
                uniqueOnFile.setText(reportUniqueFinal);
                matchQualityHost.getChildren().add(matchQualityBanner(
                        -1, "Select a Found item to see green / yellow / red match quality.", ""));
                sb.append(plainCompareLine("Unique identification (from report)", reportUniqueFinal, proof));
                sb.append("\nTip: Choose a found item above to compare name, color, and brand too.\n");
            }
            sb.append("\nYour decision: approve only if the answers convincingly match.");
            comparison.setText(sb.toString());
        };
        linkItem.valueProperty().addListener((obs, o, n) -> refreshComparison.run());
        refreshComparison.run();

        ComboBox<String> result = dialog.combo(
                "Verification result",
                "Full match",
                "Partial match"
        );
        ComboBox<String> note = dialog.combo(
                "Admin note (optional)",
                "(No note)",
                "You may claim your item at the Lost & Found office",
                "Your answers matched our records. You can claim your item now",
                "Verified! Please proceed to the office during office hours to receive your item.",
                "Some of your answers match, but we need a bit more information. Please visit the office with additional proof of ownership."
        );

        dialog.show(() -> {
            String resultCode = "Full match".equals(result.getValue()) ? "MATCH" : "PARTIAL_MATCH";
            Claim target = new Claim();
            target.setClaimId(claim.getClaimId());
            target.setAdminResult(resultCode);
            String adminNote = note.getValue() == null ? "" : note.getValue().trim();
            if ("(No note)".equalsIgnoreCase(adminNote)) {
                adminNote = "";
            }
            target.setAdminNote(adminNote);
            if (!target.verifyOwnership()) {
                info("Failed", "Could not record verification.");
                return false;
            }
            Inventory linked = linkableItems.get(linkItem.getValue());
            if (linked != null) {
                target.setItemId(linked.getItemId());
                target.linkFoundItem();
            } else if (claim.getItemId() != null) {
                target.setItemId(claim.getItemId());
                target.linkFoundItem();
            }
            if (!target.approveClaim()) {
                info("Failed", "Could not approve claim.");
                return false;
            }
            // Do NOT mark the student's report as Claimed yet.
            // Claimed is set only when admin confirms personal pickup.

            Claim refreshed = Claim.findById(claim.getClaimId()).orElse(claim);
            String code = refreshed.getPickupCode() == null ? "" : refreshed.getPickupCode();
            // Digital pickup notice only — no paper slip printing.
            ClaimSlipPrinter.sendPickupNoticeToStudent(refreshed);
            AuditLog.record("APPROVE_CLAIM", "CLAIM", claim.getDisplayId(),
                    "Approved \"" + claim.getItemName() + "\" — pickup code " + code);
            Toast.show(stage, "Approved — pickup code " + code + " sent to student");
            navigate(Nav.MANAGE_CLAIMS);
            return true;
        });
    }

    /** Plain-English comparison line for admin verification (hidden from students). */
    private static String plainCompareLine(String label, String onFile, String studentAnswers) {
        if (onFile == null || onFile.isBlank() || onFile.startsWith("(")) {
            return "• " + label + ": nothing on file yet\n";
        }
        String proof = studentAnswers == null ? "" : studentAnswers.toLowerCase(Locale.ENGLISH);
        String[] tokens = onFile.toLowerCase(Locale.ENGLISH).split("[^a-z0-9]+");
        int hits = 0;
        int checked = 0;
        for (String token : tokens) {
            if (token.length() < 3) continue;
            checked++;
            if (proof.contains(token)) hits++;
        }
        if (checked == 0) {
            return "• " + label + ": hard to compare automatically — please read both sides\n";
        }
        int pct = (int) Math.round((hits * 100.0) / checked);
        String plain = pct >= 60 ? "Looks consistent"
                : pct >= 30 ? "Some matching details"
                : "Little similarity — review carefully";
        return "• " + label + ": " + plain + "\n";
    }

    private void showRejectClaimDialog(Claim claim) {
        Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
        if (!isClaimOpenForDecision(live)) {
            info("Locked", live.getDisplayId() + " is already finished — reject is closed.");
            return;
        }
        DialogPaneBuilder dialog = new DialogPaneBuilder("Reject Claim");
        dialog.help("A clear reason is required. The student will see this on My Claims and in their notification.");
        TextArea note = dialog.area("Reason for rejection (required)");
        note.setPromptText("Example: Ownership answers did not match the unique marks on file.");
        dialog.show(() -> {
            String reason = note.getText() == null ? "" : note.getText().trim();
            if (reason.isBlank()) {
                info("Reason required", "Enter why this claim is being rejected so the student understands the decision.");
                return false;
            }
            if (!BrandedDialog.confirm(stage, "Confirm rejection",
                    "Reject claim " + claim.getDisplayId() + " for \"" + claim.getItemName() + "\"?\n\n"
                            + "Reason: " + reason)) {
                return false;
            }
            Claim target = new Claim();
            target.setClaimId(claim.getClaimId());
            target.setAdminResult("NO_MATCH");
            target.setAdminNote(reason);
            target.verifyOwnership();
            if (!target.rejectClaim()) {
                info("Failed", "Could not reject claim.");
                return false;
            }
            Notification noteOut = new Notification();
            noteOut.setUserId(claim.getUserId());
            noteOut.setClaimId(claim.getClaimId());
            noteOut.setMessage("Your claim for " + claim.getItemName()
                    + " was rejected. Reason: " + reason);
            noteOut.notifyClaimDecision();
            AuditLog.record("REJECT_CLAIM", "CLAIM", claim.getDisplayId(),
                    "Rejected \"" + claim.getItemName() + "\" — " + reason);
            Toast.show(stage, "Claim " + claim.getDisplayId() + " rejected.");
            navigate(Nav.MANAGE_CLAIMS);
            return true;
        });
    }

    private void showHoldClaimDialog(Claim claim) {
        Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
        if (!isClaimOpenForDecision(live)) {
            info("Locked", live.getDisplayId() + " is already finished — hold is closed.");
            return;
        }
        DialogPaneBuilder dialog = new DialogPaneBuilder("Place Claim on Hold");
        dialog.help("Use Hold when you need more time or info. You can Resume, Approve, or Reject later from On Hold.");
        TextArea note = dialog.area("Hold note (optional)");
        note.setPromptText("Example: Waiting for clearer unique marks from the student.");
        dialog.show(() -> {
            String holdNote = note.getText() == null ? "" : note.getText().trim();
            Claim target = new Claim();
            target.setClaimId(claim.getClaimId());
            target.setAdminNote(holdNote.isBlank() ? "Placed on hold by staff." : holdNote);
            if (!target.placeOnHold()) {
                info("Failed", "Could not place claim on hold.");
                return false;
            }
            Notification noteOut = new Notification();
            noteOut.setUserId(claim.getUserId());
            noteOut.setClaimId(claim.getClaimId());
            String msg = "Your claim for \"" + claim.getItemName()
                    + "\" is on hold. You may come to the SAO (Student Affairs Office) "
                    + "Lost & Found desk to verify your claim.";
            if (!holdNote.isBlank()) {
                msg += " Hold note: " + holdNote;
            }
            noteOut.setMessage(msg);
            noteOut.notifyClaimDecision();
            AuditLog.record("HOLD_CLAIM", "CLAIM", claim.getDisplayId(),
                    holdNote.isBlank() ? "Placed on hold" : "Hold note: " + holdNote);
            Toast.show(stage, "Claim " + claim.getDisplayId() + " placed on hold.");
            navigate(Nav.MANAGE_CLAIMS);
            return true;
        });
    }

    private void resumeHeldClaim(Claim claim) {
        if (!BrandedDialog.confirm(stage, "Resume claim?",
                "Move " + claim.getDisplayId() + " back to Pending so you can decide Approve / Reject / Hold again?")) {
            return;
        }
        Claim target = new Claim();
        target.setClaimId(claim.getClaimId());
        String prior = claim.getAdminNote();
        target.setAdminNote((prior == null || prior.isBlank() ? "" : prior.trim() + "\n")
                + "Resumed to Pending by staff.");
        if (!target.resumeFromHold()) {
            info("Failed", "Could not resume this claim. It may no longer be On Hold.");
            return;
        }
        Notification noteOut = new Notification();
        noteOut.setUserId(claim.getUserId());
        noteOut.setClaimId(claim.getClaimId());
        noteOut.setMessage("Your claim for " + claim.getItemName()
                + " is under review again (moved back to Pending).");
        noteOut.notifyClaimDecision();
        AuditLog.record("RESUME_CLAIM", "CLAIM", claim.getDisplayId(),
                "Resumed to Pending");
        Toast.show(stage, "Claim " + claim.getDisplayId() + " resumed to Pending.");
        navigate(Nav.MANAGE_CLAIMS);
    }

    private void showConfirmReturnDialog(Claim claim) {
        Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
        String st = live.getClaimStatus() == null ? "" : live.getClaimStatus().toUpperCase(Locale.ENGLISH);
        if (!"APPROVED".equals(st) || live.getReturnDate() != null) {
            info("Locked", live.getDisplayId()
                    + (live.getReturnDate() != null
                    ? " was already picked up — confirm pickup is closed."
                    : " must be Approved before pickup can be confirmed."));
            return;
        }
        DialogPaneBuilder dialog = new DialogPaneBuilder("Confirm Personal Pickup");
        dialog.help("Ask the student for the pickup code from their in-app notification "
                + "(sent when the claim was approved). Type that code here, then confirm UE ID. Text code only — no printing.");
        // Do not show the expected code on this form — student must provide it (desk check).
        TextField codeField = dialog.field("Pickup code from student", "");
        codeField.setPromptText("e.g. UE-7K2M");
        TextField receivedBy = dialog.field("Released to / received by", user.getFullName());
        ComboBox<String> returnNote = dialog.combo(
                "Pickup note",
                "Item released. No issues reported.",
                "Owner confirmed the item is theirs.",
                "Item released in good condition."
        );
        DatePicker returnDate = new DatePicker(LocalDate.now());
        dialog.addCustom("Pickup date", returnDate);
        dialog.show(() -> {
            String expected = live.getPickupCode() == null ? "" : live.getPickupCode().trim();
            String entered = codeField.getText() == null ? "" : codeField.getText().trim();
            if (!expected.isBlank()) {
                if (entered.isBlank()) {
                    info("Pickup code required",
                            "Ask the student for the pickup code on their slip / notification, then enter it here.");
                    return false;
                }
                if (!expected.equalsIgnoreCase(entered)) {
                    info("Code mismatch",
                            "That pickup code does not match this claim. Ask the student to check their slip.");
                    return false;
                }
            }
            if (receivedBy.getText().isBlank()) {
                info("Incomplete", "Please enter who received the item.");
                return false;
            }
            Claim target = new Claim();
            target.setClaimId(claim.getClaimId());
            target.setReceivedBy(receivedBy.getText().trim());
            String pickupNote = returnNote.getValue() == null ? "" : returnNote.getValue().trim();
            target.setReturnNote(pickupNote);
            target.setReturnDate(returnDate.getValue() != null ? returnDate.getValue() : LocalDate.now());
            if (!target.confirmItemReturn()) {
                info("Failed", "Could not confirm pickup.");
                return false;
            }

            // Only now mark the student's report as Claimed (personal claim completed).
            Integer reportId = claim.getReportId();
            if (reportId == null) {
                Claim stored = Claim.findById(claim.getClaimId()).orElse(null);
                if (stored != null) reportId = stored.getReportId();
            }
            if (reportId != null && reportId > 0) {
                Report update = new Report();
                update.setReportId(reportId);
                update.setReportStatus("CLAIMED");
                update.updateReportStatus();
            }

            Notification noteOut = new Notification();
            noteOut.setUserId(claim.getUserId());
            noteOut.setClaimId(claim.getClaimId());
            if (reportId != null) noteOut.setReportId(reportId);
            noteOut.setMessage("Your item (" + claim.getItemName()
                    + ") was released to you. Your report is now marked Claimed.");
            noteOut.notifyItemReturned();

            // Notify students who submitted a matching FOUND tip that the item was claimed.
            Inventory linkedItem = null;
            Claim storedClaim = Claim.findById(claim.getClaimId()).orElse(null);
            if (storedClaim != null && storedClaim.getItemId() != null) {
                linkedItem = Inventory.findById(storedClaim.getItemId()).orElse(null);
            }
            notifyFoundReportersItemClaimed(claim.getItemName(), linkedItem);

            AuditLog.record("CONFIRM_PICKUP", "CLAIM", claim.getDisplayId(),
                    "Released \"" + claim.getItemName() + "\" to claimant");
            navigate(Nav.MANAGE_CLAIMS);
            return true;
        });
    }

    /**
     * When an item is personally claimed/picked up, notify finders who filed a FOUND report
     * for that item and mark their tip as Claimed.
     */
    private void notifyFoundReportersItemClaimed(String itemName, Inventory linked) {
        String key = itemName == null ? "" : itemName.trim().toLowerCase(Locale.ENGLISH);
        String linkedName = linked == null || linked.getItemName() == null
                ? "" : linked.getItemName().trim().toLowerCase(Locale.ENGLISH);

        Report probe = new Report();
        probe.setUserId(0);
        for (Report report : probe.filterReports()) {
            if (!report.isFoundReport()) continue;
            String status = report.getReportStatus() == null ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);
            if ("CANCELLED".equals(status) || "CLAIMED".equals(status)) continue;

            String reportName = report.getItemName() == null
                    ? "" : report.getItemName().trim().toLowerCase(Locale.ENGLISH);
            boolean nameMatch = !reportName.isEmpty()
                    && (reportName.equals(key) || (!linkedName.isEmpty() && reportName.equals(linkedName)));
            if (!nameMatch) continue;

            Report update = new Report();
            update.setReportId(report.getReportId());
            update.setReportStatus("CLAIMED");
            update.updateReportStatus();

            Notification note = new Notification();
            note.setUserId(report.getUserId());
            note.setReportId(report.getReportId());
            note.setMessage("Good news! The found item you reported (" + report.getItemName()
                    + ") has already been claimed by its owner. Thank you for helping Lost & Found.");
            note.notifyReportUpdate();
        }
    }

    private void showInventoryDetails(Inventory item) {
        item.viewItemDetails();
        String body = item.getItemName()
                + "\n\nTracking #: " + nullSafe(item.getTrackingNumber())
                + "\nCategory: " + item.getCategory()
                + "\nBrand: " + nullSafe(item.getBrand())
                + "\nColor: " + nullSafe(item.getColor())
                + "\nStatus: " + prettyStatus(item.getItemStatus())
                + "\nStorage: " + nullSafe(item.getStorageBox())
                + " @ " + nullSafe(item.getStorageLocation())
                + "\nFound: " + (item.getFoundDate() != null ? DATE_FMT.format(item.getFoundDate()) : "—")
                + "\n\nUnique marks (admin only — hidden from claimants):\n"
                + nullSafe(item.getPrivateDetails());
        BrandedDialog.details(stage, "Item " + nullSafe(item.getTrackingNumber()),
                "Inventory", body, null);
    }

    private void confirmArchiveItem(Inventory item) {
        if (!BrandedDialog.confirm(stage, "Archive item",
                "Archive " + nullSafe(item.getTrackingNumber()) + " — " + item.getItemName() + "?\n"
                + "It will move to the Archived filter (kept for records).")) {
            return;
        }
        Inventory target = new Inventory();
        target.setItemId(item.getItemId());
        if (!target.removeItem()) {
            info("Failed", "Could not archive item.");
            return;
        }
        Toast.show(stage, "Item archived.");
        inventoryStatusFilter = "ARCHIVED";
        navigate(Nav.INVENTORY);
    }

    private void confirmRestoreItem(Inventory item) {
        if (!BrandedDialog.confirm(stage, "Restore item",
                "Restore " + nullSafe(item.getTrackingNumber()) + " to Available inventory?")) {
            return;
        }
        // Restore must not recreate a duplicate of an already-active twin.
        Optional<Inventory> dup = Inventory.findDuplicateActive(
                item.getItemName(), item.getCategory(), item.getColor(),
                item.getBrand(), item.getPrivateDetails());
        if (dup.isPresent() && dup.get().getItemId() != item.getItemId()) {
            info("Cannot restore",
                    "An active inventory row already matches this item "
                    + "(" + nullSafe(dup.get().getTrackingNumber()) + ").");
            return;
        }
        Inventory target = new Inventory();
        target.setItemId(item.getItemId());
        target.setItemStatus("AVAILABLE");
        if (!target.updateItemStatus()) {
            info("Failed", "Could not restore item.");
            return;
        }
        Toast.show(stage, "Item restored to Available.");
        inventoryStatusFilter = "AVAILABLE";
        navigate(Nav.INVENTORY);
    }

    private void showUserDetails(User account) {
        String body = account.getFullName()
                + "\n\nUsername: " + nullSafe(account.getUsername())
                + "\nStudent #: " + nullSafe(account.getStudentNumber())
                + "\nRole: " + prettyStatus(account.getRole())
                + "\nStatus: " + nullSafe(account.getAccountStatus())
                + "\nEmail: " + nullSafe(account.getEmail());
        BrandedDialog.details(stage, "Account #" + account.getUserId(), "User account", body, null);
    }

    private void deactivateUser(User account) {
        if (!BrandedDialog.confirm(stage, "Deactivate account",
                "Deactivate " + account.getFullName() + "?\nThey will not be able to sign in until reactivated.")) {
            return;
        }
        User target = new User();
        target.setUserId(account.getUserId());
        target.deactivateAccount();
        AuditLog.record("DEACTIVATE_USER", "USER", String.valueOf(account.getUserId()),
                "Deactivated " + account.getFullName());
        navigate(Nav.USERS);
    }

    private void activateUser(User account) {
        User target = new User();
        target.setUserId(account.getUserId());
        if (target.activateAccount()) {
            AuditLog.record("ACTIVATE_USER", "USER", String.valueOf(account.getUserId()),
                    "Activated " + account.getFullName());
            navigate(Nav.USERS);
        }
    }

    private void showChangePasswordDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Change Password");
        PasswordField current = dialog.password("Current password");
        PasswordField next = dialog.password("New password");
        PasswordField confirm = dialog.password("Confirm new password");
        dialog.show(() -> {
            if (!user.verifyPassword(current.getText())) {
                info("Incorrect password", "Current password does not match.");
                return false;
            }
            String pairError = PasswordHasher.validateNewPasswordPair(next.getText(), confirm.getText());
            if (pairError != null) {
                Toast.error(stage, pairError);
                return false;
            }
            user.setPasswordHash(next.getText());
            if (!user.changePassword()) {
                info("Failed", "Could not change password.");
                return false;
            }
            AuditLog.record("PASSWORD_CHANGE", "USER", String.valueOf(user.getUserId()),
                    "Admin changed their password");
            info("Password updated", "Your password was changed successfully.");
            return true;
        });
    }

    private void showChangeEmailDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Update Email");
        dialog.help(AccountRules.ueEmailMessage());
        TextField email = dialog.field("New UE email", user.getEmail());
        dialog.show(() -> {
            if (!AccountRules.isUeEmail(email.getText())) {
                info("UE email required", AccountRules.ueEmailMessage());
                return false;
            }
            user.setEmail(AccountRules.normalizeEmail(email.getText()));
            if (!user.updateProfile()) {
                info("Failed", "Could not update email.");
                return false;
            }
            info("Email updated", "Your email was changed to " + user.getEmail());
            navigate(Nav.SETTINGS);
            return true;
        });
    }

    /** Admin sets a temporary password; user must replace it on next login. */
    private void showAdminResetPasswordDialog(User account) {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Reset Password");
        dialog.help("Set a temporary password for " + account.getFullName()
                + ". Tell them once in person — it is stored hashed and cannot be viewed later.\n"
                + "On next login they must choose a new password before using the system.\n"
                + PasswordHasher.policyMessage());
        PasswordField next = dialog.password("Temporary password");
        PasswordField confirm = dialog.password("Confirm temporary password");
        dialog.show(() -> {
            String plain = next.getText() == null ? "" : next.getText();
            String conf = confirm.getText() == null ? "" : confirm.getText();
            // Same order as Register: filled → match → policy (prevents mismatched / weak temps).
            String pairError = PasswordHasher.validateNewPasswordPair(plain, conf);
            if (pairError != null) {
                Toast.error(stage, pairError);
                return false;
            }
            if (!User.adminResetPassword(account.getUserId(), plain)) {
                info("Failed", "Could not reset password for this account.");
                return false;
            }
            AuditLog.record("RESET_PASSWORD", "USER", String.valueOf(account.getUserId()),
                    "Admin reset password for " + account.getFullName()
                            + " (must change on next login)");
            info("Password reset",
                    "Temporary password saved (hashed) for " + account.getFullName() + ".\n\n"
                    + "Give them this password now — they must change it on next login:\n"
                    + plain);
            return true;
        });
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        chooser.setInitialFileName("lost-and-java-found-export.csv");
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        Report reportProbe = new Report();
        reportProbe.setUserId(0);
        List<Report> reports = reportProbe.filterReports();
        List<Claim> claims = Claim.viewAllClaims();
        Inventory invProbe = new Inventory();
        List<Inventory> inventory = invProbe.filterItems();

        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write("REPORTS");
            w.newLine();
            w.write("ID,Item,Category,Status,Date");
            w.newLine();
            for (Report r : reports) {
                w.write(csv(r.getDisplayId(), r.getItemName(), r.getCategory(),
                        r.getReportStatus(), DATE_FMT.format(r.getDateSubmitted())));
                w.newLine();
            }
            w.newLine();
            w.write("CLAIMS");
            w.newLine();
            w.write("ID,Item,Student UserId,Status,Date");
            w.newLine();
            for (Claim c : claims) {
                w.write(csv(c.getDisplayId(), c.getItemName(), String.valueOf(c.getUserId()),
                        c.getClaimStatus(), DATE_FMT.format(c.getClaimDate())));
                w.newLine();
            }
            w.newLine();
            w.write("INVENTORY");
            w.newLine();
            w.write("Tracking,Item,Category,Status,Storage Box");
            w.newLine();
            for (Inventory i : inventory) {
                w.write(csv(i.getTrackingNumber(), i.getItemName(), i.getCategory(),
                        i.getItemStatus(), nullSafe(i.getStorageBox())));
                w.newLine();
            }
            info("Export complete", "Saved to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            info("Export failed", ex.getMessage());
        }
    }

    /** Additive monthly summary CSV — does not replace the full export. */
    private void exportMonthlyCsv() {
        CampusInsights.MonthSummary month = CampusInsights.monthSummary(LocalDate.now());
        List<CampusInsights.LocationCount> hotspots = CampusInsights.topLostLocations(LocalDate.now(), 20);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export monthly summary CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        String safeMonth = month.monthLabel().replace(' ', '-');
        chooser.setInitialFileName("monthly-summary-" + safeMonth + ".csv");
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write("MONTHLY SUMMARY");
            w.newLine();
            w.write(csv("Month", month.monthLabel()));
            w.newLine();
            w.write(csv("Lost reports filed", String.valueOf(month.lostFiled())));
            w.newLine();
            w.write(csv("Found items registered", String.valueOf(month.foundRegistered())));
            w.newLine();
            w.write(csv("Lost reports matched", String.valueOf(month.lostMatched())));
            w.newLine();
            w.write(csv("Items returned", String.valueOf(month.returned())));
            w.newLine();
            w.write(csv("Avg days approved to pickup", formatAvgDays(month.avgDaysToReturn())));
            w.newLine();
            w.newLine();
            w.write("LOST LOCATION HOTSPOTS");
            w.newLine();
            w.write("Location,Count");
            w.newLine();
            for (CampusInsights.LocationCount loc : hotspots) {
                w.write(csv(loc.location(), String.valueOf(loc.count())));
                w.newLine();
            }
            info("Monthly export complete", "Saved to:\n" + file.getAbsolutePath());
            Toast.show(stage, "Monthly summary exported");
        } catch (IOException ex) {
            info("Export failed", ex.getMessage());
        }
    }

    private static String csv(String... cols) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            String col = cols[i] == null ? "" : cols[i];
            if (col.contains(",") || col.contains("\"")) {
                line.append('"').append(col.replace("\"", "\"\"")).append('"');
            } else {
                line.append(col);
            }
        }
        return line.toString();
    }

    private void logout() {
        if (!BrandedDialog.confirm(stage, "Log out",
                "Are you sure you want to log out?")) {
            return;
        }
        performLogout("Manual logout");
    }

    /** Called by IdleSessionGuard — no confirm dialog. */
    void forceLogoutForIdle() {
        performLogout("Idle timeout (" + IdleSessionGuard.IDLE_MINUTES + " min)");
    }

    private void performLogout(String reason) {
        AuditLog.record("LOGOUT", "USER", String.valueOf(user.getUserId()), reason);
        IdleSessionGuard.detach();
        user.logout();
        LoginView loginView = new LoginView(stage);
        stage.setTitle("Lost and Java-Found — Login");
        stage.setScene(UeChrome.scene(loginView.getRoot(), 1100, 680));
    }

    // ------------------------------------------------------------------ widgets

    /**
     * One GridPane per table — header and data share these exact column widths.
     * All columns center-aligned; Actions gets more width so links do not overlap.
     */
    private static final double[] TABLE_COL_PERCENTS = {10, 18, 12, 14, 14, 32};
    private static final double[] ACTIVITY_COL_PERCENTS = {40, 30, 30};
    private static final int ACTIONS_COL = TABLE_COL_PERCENTS.length - 1;

    private static Pos columnAlign(int index) {
        return Pos.CENTER;
    }

    private static HPos columnHPos(int index) {
        return HPos.CENTER;
    }

    private GridPane newDataTable() {
                return newPercentTable(TABLE_COL_PERCENTS);
    }
    private GridPane newActivityTable() {
        return newPercentTable(ACTIVITY_COL_PERCENTS);
    }
    private GridPane newPercentTable(double[] percents) {
        GridPane table = new GridPane();
        table.setMaxWidth(Double.MAX_VALUE);
        table.setMinWidth(0);
        table.setHgap(12);
        table.setVgap(0);
        table.setPadding(new Insets(4, 20, 4, 20));
        for (int i = 0; i < percents.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(percents[i]);
            cc.setMinWidth(0);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            cc.setHalignment(columnHPos(i));
            table.getColumnConstraints().add(cc);
        }
        return table;
    }

    private int addTableHeader(GridPane table, int row, String... cols) {
        int count = Math.min(cols.length, table.getColumnConstraints().size());
        for (int i = 0; i < count; i++) {
            Label label = new Label(cols[i].toUpperCase(Locale.ENGLISH));
            label.setStyle(
                    "-fx-text-fill: " + UiTheme.TEXT_DIM + ";"
                    + "-fx-font-size: 11px;"
                    + "-fx-font-weight: bold;"
            );
            label.setWrapText(true);
            label.setMinWidth(0);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            putHeader(table, i, row, label);
        }
        return row + 1;
    }

    private void putHeader(GridPane table, int col, int row, Node content) {
        StackPane cell = wrapTableCell(content, col, true);
        table.add(cell, col, row);
    }

    private void putData(GridPane table, int col, int row, Node content) {
        StackPane cell = wrapTableCell(content, col, false);
        table.add(cell, col, row);
    }

    private StackPane wrapTableCell(Node content, int col, boolean header) {
        boolean actionsData = !header && col == ACTIONS_COL;
        Pos align = actionsData ? Pos.CENTER_LEFT : Pos.CENTER;
        HPos hPos = actionsData ? HPos.LEFT : HPos.CENTER;
        
        StackPane cell = new StackPane(content);
        cell.setAlignment(align);
        cell.setMinWidth(0);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setPadding(new Insets(header ? 14 : 12, 8, header ? 14 : 12, 8));
        cell.setStyle("-fx-border-color: " + UiTheme.BORDER + "; -fx-border-width: 0 0 1 0;");
        if (!header) {
            cell.setOnMouseEntered(e -> cell.setStyle(
                    "-fx-background-color: " + UiTheme.ROW_HOVER + ";"
                    + "-fx-border-color: " + UiTheme.BORDER + "; -fx-border-width: 0 0 1 0;"
            ));
            cell.setOnMouseExited(e -> cell.setStyle(
                    "-fx-border-color: " + UiTheme.BORDER + "; -fx-border-width: 0 0 1 0;"
            ));
        }
        GridPane.setFillWidth(cell, true);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        GridPane.setHalignment(cell, hPos);
        GridPane.setValignment(cell, javafx.geometry.VPos.CENTER);
        return cell;
    }

    private Label cell(String text, boolean emphasize) {
        Label label = new Label(text == null ? "—" : text);
        label.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 13px;"
                + (emphasize ? "-fx-font-weight: bold;" : "")
        );
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        return label;
    }

    private StackPane statusBadge(String status) {
        String bg;
        String fg;
        String key = status.toLowerCase(Locale.ENGLISH);
        if (key.contains("pending") || key.contains("review") || key.contains("temp")) {
            bg = UiTheme.STATUS_PENDING_BG;
            fg = UiTheme.STATUS_PENDING_FG;
        } else if (key.contains("approved") || key.contains("matched") || key.contains("active")) {
            bg = UiTheme.STATUS_APPROVED_BG;
            fg = UiTheme.STATUS_APPROVED_FG;
        } else if (key.contains("reject") || key.contains("cancel") || key.contains("inactive")
                || key.contains("archiv")) {
            bg = UiTheme.STATUS_REJECTED_BG;
            fg = UiTheme.STATUS_REJECTED_FG;
        } else if (key.contains("hold")) {
            bg = UiTheme.STATUS_HOLD_BG;
            fg = UiTheme.STATUS_HOLD_FG;
        } else if (key.contains("register") || key.contains("return") || key.contains("claimed")
                || key.contains("resolv")) {
            // Registered (found tip) / Claimed (owner pickup) / Returned — completed desk outcomes.
            bg = UiTheme.STATUS_RESOLVED_BG;
            fg = UiTheme.STATUS_RESOLVED_FG;
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
        label.setAlignment(Pos.CENTER);
        StackPane badge = new StackPane(label);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 20;");
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    private HBox multiActions(Button... buttons) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setSpacing(8);
        box.getChildren().addAll(buttons);
        return box;
    }

    private Button linkBtn(String text, Runnable action) {
        return UiButtons.link(text, action);
    }

    private Button filterChip(String text, boolean active, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("ue-chip", active ? "ue-chip-active" : "ue-chip-idle");
        btn.setFocusTraversable(false);
        btn.setOnAction(e -> {
            // Mark pressed chip red immediately, then run filter (rebuild keeps active red).
            btn.getStyleClass().removeAll("ue-chip-idle", "ue-chip-active");
            btn.getStyleClass().add("ue-chip-active");
            btn.setTextFill(javafx.scene.paint.Color.WHITE);
            action.run();
        });
        return btn;
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

    private HBox statCard(String label, String value, String hint, String colorHex, String iconType) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setPrefWidth(180);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle(UiTheme.cardStyle());

        StackPane iconBubble = createIconBubble(iconType, colorHex);
        
        VBox textBox = new VBox(4);
        textBox.getChildren().addAll(
                muted(label),
                labeled(value, UiTheme.TEXT, 24, true),
                muted(hint)
        );
        
        card.getChildren().addAll(iconBubble, textBox);
        return card;
    }
    
        private StackPane createIconBubble(String type, String colorHex) {
        Color baseColor = Color.web(colorHex);
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, baseColor),
                new Stop(1, baseColor.darker())
        );
        Circle bg = new Circle(26);
        bg.setFill(gradient);
        SVGPath path = new SVGPath();
        path.setFill(Color.WHITE);
        path.setScaleX(1.4);
        path.setScaleY(1.4);
        switch (type) {
            case "DOCUMENT" -> path.setContent(
                    "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
            case "SHIELD" -> path.setContent(
                    "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z");
            case "BOX" -> path.setContent(
                    "M21 16.5c0 .38-.21.71-.53.88l-7.9 4.44c-.16.12-.36.18-.57.18s-.41-.06-.57-.18l-7.9-4.44A.991.991 0 0 1 3 16.5v-9c0-.38.21-.71.53-.88l7.9-4.44c.16-.12.36-.18.57-.18s.41.06.57.18l7.9 4.44c.32.17.53.5.53.88v9zM12 4.15L5.21 7.9 12 11.65 18.79 7.9 12 4.15zM4.02 9.8L11 13.65v6.76l-6.98-3.92V9.8zm15.96 0v6.69L13 20.41v-6.76l6.98-3.85z");
            case "USERS" -> path.setContent(
                    "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z");
        }
        StackPane bubble = new StackPane(bg, path);
        bubble.setMinSize(52, 52);
        bubble.setMaxSize(52, 52);
        return bubble;
    }

    private HBox barRow(String label, long count, long max) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(label);
        name.setMinWidth(90);
        name.setStyle("-fx-text-fill: " + UiTheme.TEXT + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        double width = max > 0 ? (count * 220.0 / max) : 0;
        Rectangle bar = new Rectangle(Math.max(4, width), 14);
        bar.setFill(Color.web("#22C55E"));
        bar.setArcWidth(8);
        bar.setArcHeight(8);
        Label value = new Label(String.valueOf(count));
        value.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-weight: bold;");
        row.getChildren().addAll(name, bar, value);
        return row;
    }

    private HBox settingsTile(String iconType, String title, String subtitle, Runnable action) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20, 22, 20, 22));
        row.setStyle(UiTheme.cardStyle() + "-fx-cursor: hand;");

        StackPane icon = new StackPane(settingsGlyphNode(iconType));
        icon.setPrefSize(48, 48);
        icon.setMinSize(48, 48);
        icon.setMaxSize(48, 48);
        icon.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F87171, " + UiTheme.UE_RED + ");"
                + "-fx-background-radius: 12;"
        );

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
    
        private static Node settingsGlyphNode(String type) {
        SVGPath path = new SVGPath();
        path.setFill(Color.WHITE);
        path.setScaleX(1.35);
        path.setScaleY(1.35);
        if ("MAIL".equals(type)) {
            path.setContent(
                    "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z");
        } else if ("DOCUMENT".equals(type)) {
            path.setContent(
                    "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
        } else {
            path.setContent(
                    "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z");
        }
        return path;
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

    // ------------------------------------------------------------------ helpers

    private record ActivityItem(String title, String status, LocalDate date) {}

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
        circle.setFill(Color.web(UiTheme.UE_RED));
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

    private static String shortText(String text, int max) {
        if (text == null || text.isBlank()) return "—";
        String oneLine = text.replaceAll("\\s+", " ").trim();
        if (oneLine.length() <= max) return oneLine;
        return oneLine.substring(0, Math.max(0, max - 1)) + "…";
    }

    /** Formats approved→pickup average; "—" when no approval dates are on file yet. */
    private static String formatAvgDays(double avg) {
        if (Double.isNaN(avg) || avg < 0) return "—";
        return String.format(Locale.ENGLISH, "%.1f", avg);
    }

    /** Report status label aligned to workflow (Found logged → Registered, not Claimed). */
    private static String reportStatusLabel(Report report) {
        if (report == null) return "—";
        return report.displayStatusLabel();
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

    private static String fieldStyle() {
        return "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 8 12 8 12;";
    }

    private void info(String title, String message) {
        Toast.notify(stage, title, message);
    }

    /** Thin wrapper — branded look only; confirm callbacks stay the same. */
    private final class DialogPaneBuilder {
        private final BrandedDialog dialog;

        DialogPaneBuilder(String title) {
            dialog = new BrandedDialog(stage, title).help(
                    "Review the fields carefully, then OK to apply. Cancel closes without changes."
            );
        }

        DialogPaneBuilder help(String text) {
            dialog.help(text);
            return this;
        }

        void setPrefWidth(double width) { dialog.setPrefWidth(width); }
        TextField field(String label) { return dialog.field(label); }
        TextField field(String label, String value) { return dialog.field(label, value); }
        PasswordField password(String label) { return dialog.password(label); }
        TextArea area(String label) { return dialog.area(label); }
        ComboBox<String> combo(String label, String... values) { return dialog.combo(label, values); }
        BrandedDialog.PhotoPick photoPicker(String label) {
            return dialog.photoPicker(label, "item");
        }
        void addCustom(String label, Node node) { dialog.addCustom(label, node); }

        void show(java.util.function.Supplier<Boolean> onConfirm) {
            dialog.show(onConfirm);
        }
    }
}