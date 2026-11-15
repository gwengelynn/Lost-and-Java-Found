
package finalproj.mavenproject1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
    private String reportsSearchKey = "";
    private String inventorySearchKey = "";
    private String inventoryStatusFilter = null;

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
        Label role = new Label("Administrator");
        role.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 11px;");
        userMeta.getChildren().addAll(name, role);
        userRow.getChildren().addAll(avatar, userMeta);

        Button logout = new Button("Logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setStyle(UiTheme.primaryButton());
        logout.setOnAction(e -> logout());

        sidebar.getChildren().addAll(brand, brandRule, section, navButtonsBox, spacer, userRow, logout);
        VBox.setMargin(logout, new Insets(12, 0, 0, 0));
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
                        "University of the East — Admin"
                );
                setContent(buildDashboardPage());
            }
            case MANAGE_REPORTS -> {
                setHeader("Manage Reports", "Review and update all lost-item reports.");
                setContent(buildManageReportsPage());
            }
            case MANAGE_CLAIMS -> {
                setHeader("Manage Claims", "Approve, reject, or confirm returns for student claims.");
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
                statCard("Total Reports", String.valueOf(totalReports), "All filed reports"),
                statCard("Total Claims", String.valueOf(totalClaims), "Student ownership requests"),
                statCard("Returned Items", String.valueOf(returnedItems), "Confirmed handovers"),
                statCard("Total Users", String.valueOf(totalUsers), "Registered accounts")
        );

        // Mid row: clickable pie overview + recent activity
        HBox mid = new HBox(14);
        Node pieCard = buildReportsOverviewPie(totalReports, returnedItems);
        HBox.setHgrow(pieCard, Priority.ALWAYS);

        VBox recent = sectionCard("Recent activity", "Latest claim and report updates.");
        HBox.setHgrow(recent, Priority.ALWAYS);
        VBox list = new VBox(8);
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
                    prettyStatus(report.getReportStatus()),
                    report.getDateSubmitted()
            ));
        }
        items.sort(Comparator.comparing(ActivityItem::date).reversed());
        items.stream().limit(8).forEach(item ->
                list.getChildren().add(activityRow(item)));
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(muted("No activity yet."));
        }
        recent.getChildren().add(list);

        mid.getChildren().addAll(pieCard, recent);

        // Quick Actions sit below Reports Overview (mockup layout)
        Node quickActions = buildQuickActionsPanel();

        return new VBox(18, stats, mid, quickActions);
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

        double lost = Math.max(lostReports, 0);
        double found = Math.max(foundItems, 0);
        double returned = Math.max(returnedItems, 0);
        if (lost + found + returned <= 0) {
            lost = 1;
        }
        double total = lost + found + returned;

        double lostPct = lost * 100 / total;
        double foundPct = found * 100 / total;
        double returnedPct = returned * 100 / total;

        double radius = 78;
        double cx = 90;
        double cy = 90;
        Group pieGroup = new Group();

        double angle = 90; // start at top
        angle = addPieSlice(pieGroup, cx, cy, radius, angle, lost / total * 360, Color.web("#E41135"));
        angle = addPieSlice(pieGroup, cx, cy, radius, angle, found / total * 360, Color.web("#E8C04A"));
        addPieSlice(pieGroup, cx, cy, radius, angle, returned / total * 360, Color.web("#3B82F6"));

        // Soft center hole for a cleaner donut-like look
        Circle hole = new Circle(cx, cy, 34);
        hole.setFill(Color.web("#FFF8F8"));
        pieGroup.getChildren().add(hole);

        StackPane pie = new StackPane(pieGroup);
        pie.setPrefSize(180, 180);
        pie.setMinSize(180, 180);

        VBox legend = new VBox(10);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
                pieLegendRow("#E41135", "Lost Items", lostPct, (long) lost),
                pieLegendRow("#E8C04A", "Found Items", foundPct, (long) found),
                pieLegendRow("#3B82F6", "Returned Items", returnedPct, (long) returned)
        );

        HBox chartRow = new HBox(18, pie, legend);
        chartRow.setAlignment(Pos.CENTER_LEFT);
        chartRow.setPadding(new Insets(8, 4, 4, 4));

        VBox card = sectionCard(
                "Reports Overview",
                "Click to open Reports & Analytics."
        );
        card.setStyle(card.getStyle() + "-fx-cursor: hand;");
        card.getChildren().add(chartRow);
        card.setOnMouseClicked(e -> navigate(Nav.ANALYTICS));
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
        arc.setStrokeWidth(2);
        pie.getChildren().add(arc);
        return startAngle - length;
    }

    private static HBox pieLegendRow(String colorHex, String label, double pct, long count) {
        Circle dot = new Circle(6, Color.web(colorHex));
        Label name = new Label(label);
        name.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
        );
        Label meta = new Label(String.format(Locale.ENGLISH, "%.0f%%  (%d)", pct, count));
        meta.setStyle(
                "-fx-text-fill: " + UiTheme.TEXT_MUTED + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
        );
        VBox text = new VBox(2, name, meta);
        HBox row = new HBox(10, dot, text);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node buildManageReportsPage() {
        VBox page = new VBox(14);

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

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.getChildren().add(tableHeader(
                "ID", "Item Name", "Category", "Date Reported", "Status", "Actions"
        ));

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

        if (reports.isEmpty()) {
            tableCard.getChildren().add(emptyState("No reports found.", "Adjust search or wait for new submissions."));
        } else {
            for (Report report : reports) {
                tableCard.getChildren().add(adminReportRow(report));
            }
        }

        page.getChildren().addAll(toolbar, tableCard,
                muted("Showing " + reports.size() + " report" + (reports.size() == 1 ? "" : "s") + "."));
        return page;
    }

    private Node buildManageClaimsPage() {
        VBox page = new VBox(14);

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

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.getChildren().add(tableHeader(
                "Claim ID", "Item Name", "Student", "Date", "Status", "Actions"
        ));

        List<Claim> claims = Claim.filterAllByStatus(claimsFilter);
        if (claims.isEmpty()) {
            tableCard.getChildren().add(emptyState("No claims in this filter.", "New claims appear after students submit."));
        } else {
            for (Claim claim : claims) {
                tableCard.getChildren().add(adminClaimRow(claim));
            }
        }

        page.getChildren().addAll(filters, tableCard,
                muted("Showing " + claims.size() + " claim" + (claims.size() == 1 ? "" : "s") + "."));
        return page;
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
        Button register = new Button("+ Register Item");
        register.setStyle(UiTheme.primaryButton());
        register.setOnAction(e -> showRegisterItemDialog());
        toolbar.getChildren().addAll(search, searchBtn, grow, register);

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
                })
        );

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.getChildren().add(tableHeader(
                "Tracking #", "Item Name", "Category", "Status", "Storage Box", "Actions"
        ));

        List<Inventory> items;
        if (!inventorySearchKey.isBlank()) {
            Inventory probe = new Inventory();
            probe.setItemName(inventorySearchKey);
            items = probe.searchItem();
        } else {
            Inventory probe = new Inventory();
            probe.setItemStatus(inventoryStatusFilter);
            items = probe.filterItems().stream()
                    .filter(i -> !"ARCHIVED".equalsIgnoreCase(i.getItemStatus()))
                    .toList();
        }

        if (items.isEmpty()) {
            tableCard.getChildren().add(emptyState("No inventory items found.", "Register a found item to get started."));
        } else {
            for (Inventory item : items) {
                tableCard.getChildren().add(inventoryRow(item));
            }
        }

        page.getChildren().addAll(toolbar, statusFilters, tableCard,
                muted("Showing " + items.size() + " item" + (items.size() == 1 ? "" : "s") + "."));
        return page;
    }

    private Node buildUsersPage() {
        VBox page = new VBox(14);
        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.getChildren().add(tableHeader(
                "ID", "Full Name", "Username / Student #", "Role", "Status", "Actions"
        ));

        List<User> accounts = User.listAccounts();
        if (accounts.isEmpty()) {
            tableCard.getChildren().add(emptyState("No accounts.", "Accounts appear after registration."));
        } else {
            for (User account : accounts) {
                tableCard.getChildren().add(userRow(account));
            }
        }

        page.getChildren().addAll(tableCard,
                muted("Showing " + accounts.size() + " account" + (accounts.size() == 1 ? "" : "s") + "."));
        return page;
    }

    private Node buildAnalyticsPage() {
        Report reportProbe = new Report();
        reportProbe.setUserId(0);
        long totalReports = reportProbe.filterReports().size();
        long pendingClaims = Claim.countAllByStatus("PENDING");
        long approvedClaims = Claim.countAllByStatus("APPROVED");
        long rejectedClaims = Claim.countAllByStatus("REJECTED");
        long onHoldClaims = Claim.countAllByStatus("ON_HOLD");

        Inventory inv = new Inventory();
        long avail = inv.filterItems().stream().filter(i -> "AVAILABLE".equalsIgnoreCase(i.getItemStatus())).count();
        long claimed = inv.filterItems().stream().filter(i -> "CLAIMED".equalsIgnoreCase(i.getItemStatus())).count();
        inv.setItemStatus("RETURNED");
        long returned = inv.filterItems().size();

        VBox summary = sectionCard("Summary counts", "Live counts from blueprint data.");
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        int row = 0;
        grid.add(muted("Total reports"), 0, row);
        grid.add(labeled(String.valueOf(totalReports), UiTheme.TEXT, 16, true), 1, row++);
        grid.add(muted("Pending claims"), 0, row);
        grid.add(labeled(String.valueOf(pendingClaims), UiTheme.TEXT, 16, true), 1, row++);
        grid.add(muted("Approved claims"), 0, row);
        grid.add(labeled(String.valueOf(approvedClaims), UiTheme.TEXT, 16, true), 1, row++);
        grid.add(muted("Returned items"), 0, row);
        grid.add(labeled(String.valueOf(returned), UiTheme.TEXT, 16, true), 1, row++);
        summary.getChildren().add(grid);

        VBox claimsBreakdown = sectionCard("Claims by status", "Relative volume per status.");
        long maxClaim = Math.max(1, Math.max(Math.max(pendingClaims, approvedClaims), Math.max(rejectedClaims, onHoldClaims)));
        claimsBreakdown.getChildren().addAll(
                barRow("Pending", pendingClaims, maxClaim),
                barRow("Approved", approvedClaims, maxClaim),
                barRow("Rejected", rejectedClaims, maxClaim),
                barRow("On hold", onHoldClaims, maxClaim)
        );

        VBox invBreakdown = sectionCard("Inventory by status", "Available vs claimed vs returned.");
        long maxInv = Math.max(1, Math.max(avail, Math.max(claimed, returned)));
        invBreakdown.getChildren().addAll(
                barRow("Available", avail, maxInv),
                barRow("Claimed", claimed, maxInv),
                barRow("Returned", returned, maxInv)
        );

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button export = new Button("Export CSV");
        export.setStyle(UiTheme.primaryButton());
        export.setOnAction(e -> exportCsv());
        footer.getChildren().add(export);

        return new VBox(14, summary, claimsBreakdown, invBreakdown, footer);
    }

    private Node buildSettingsPage() {
        VBox page = new VBox(14);
        page.getChildren().add(settingsTile(
                "🔒",
                "Change Password",
                "Update the password used to sign in to your administrator account.",
                this::showChangePasswordDialog
        ));
        page.getChildren().add(settingsTile(
                "✉",
                "Update Email Address",
                "Change the email linked to your admin account.",
                this::showChangeEmailDialog
        ));
        return page;
    }

    // ------------------------------------------------------------------ table rows

    private HBox adminReportRow(Report report) {
        HBox row = dataRow();
        row.getChildren().addAll(
                cell(report.getDisplayId(), true),
                cell(report.getItemName(), false),
                cell(shortCategory(report.getCategory()), false),
                cell(DATE_FMT.format(report.getDateSubmitted()), false),
                statusBadge(prettyStatus(report.getReportStatus())),
                multiActions(
                        linkBtn("View", () -> showReportDetails(report)),
                        linkBtn("Matches", () -> showReportMatches(report)),
                        linkBtn("Status", () -> showUpdateReportStatusDialog(report))
                )
        );
        return row;
    }

    private HBox adminClaimRow(Claim claim) {
        String student = User.findById(claim.getUserId())
                .map(User::getFullName)
                .orElse("User #" + claim.getUserId());
        HBox row = dataRow();
        row.getChildren().addAll(
                cell(claim.getDisplayId(), true),
                cell(claim.getItemName(), false),
                cell(student, false),
                cell(DATE_FMT.format(claim.getClaimDate()), false),
                statusBadge(prettyStatus(claim.getClaimStatus())),
                claimActionCell(claim)
        );
        return row;
    }

    private HBox claimActionCell(Claim claim) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(linkBtn("View", () -> showClaimDetails(claim)));
        String status = claim.getClaimStatus() == null ? "" : claim.getClaimStatus().toUpperCase(Locale.ENGLISH);
        if ("PENDING".equals(status)) {
            buttons.add(linkBtn("Approve", () -> showApproveClaimDialog(claim)));
            buttons.add(linkBtn("Reject", () -> showRejectClaimDialog(claim)));
            buttons.add(linkBtn("Hold", () -> showHoldClaimDialog(claim)));
        } else if ("APPROVED".equals(status) && claim.getReturnDate() == null) {
            buttons.add(linkBtn("Return", () -> showConfirmReturnDialog(claim)));
        }
        return multiActions(buttons.toArray(new Button[0]));
    }

    private HBox inventoryRow(Inventory item) {
        HBox row = dataRow();
        row.getChildren().addAll(
                cell(nullSafe(item.getTrackingNumber()), true),
                cell(item.getItemName(), false),
                cell(shortCategory(item.getCategory()), false),
                statusBadge(prettyStatus(item.getItemStatus())),
                cell(nullSafe(item.getStorageBox()), false),
                multiActions(
                        linkBtn("View", () -> showInventoryDetails(item)),
                        linkBtn("Archive", () -> confirmArchiveItem(item))
                )
        );
        return row;
    }

    private HBox userRow(User account) {
        String loginId = account.getStudentNumber() != null && !account.getStudentNumber().isBlank()
                ? account.getStudentNumber()
                : nullSafe(account.getUsername());
        HBox row = dataRow();
        List<Button> actions = new ArrayList<>();
        actions.add(linkBtn("View", () -> showUserDetails(account)));
        if ("STUDENT".equalsIgnoreCase(account.getRole())) {
            if ("Active".equalsIgnoreCase(account.getAccountStatus())) {
                actions.add(linkBtn("Deactivate", () -> deactivateUser(account)));
            } else if ("Inactive".equalsIgnoreCase(account.getAccountStatus())) {
                actions.add(linkBtn("Activate", () -> activateUser(account)));
            }
        }
        row.getChildren().addAll(
                cell(String.valueOf(account.getUserId()), true),
                cell(account.getFullName(), false),
                cell(loginId, false),
                cell(prettyStatus(account.getRole()), false),
                statusBadge(nullSafe(account.getAccountStatus())),
                multiActions(actions.toArray(new Button[0]))
        );
        return row;
    }

    private HBox activityRow(ActivityItem item) {
        HBox row = dataRow();
        row.getChildren().addAll(
                cell(item.title(), false),
                statusBadge(item.status()),
                cell(DATE_FMT.format(item.date()), false)
        );
        return row;
    }

    // ------------------------------------------------------------------ dialogs

    private void showRegisterItemDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Register Found Item");
        TextField itemName = dialog.field("Item name");
        ComboBox<String> category = dialog.combo("Category", "Devices", "Personal Belongings");
        ComboBox<String> subCategory = dialog.combo("Subcategory", deviceSubcategories());
        TextField brand = dialog.field("Brand");
        TextField color = dialog.field("Color");
        TextArea description = dialog.area("Description");
        TextArea privateDetails = dialog.area("Private details (admin only)");
        ComboBox<String> storageBox = dialog.combo("Storage box", storageBoxesAtoT());
        TextField storageLocation = dialog.field("Storage location");

        // Keep subcategory list in sync with main category (school lost-item types)
        category.valueProperty().addListener((obs, oldVal, newVal) -> {
            String[] options = "Devices".equals(newVal)
                    ? deviceSubcategories()
                    : personalSubcategories();
            subCategory.getItems().setAll(options);
            subCategory.getSelectionModel().selectFirst();
        });

        dialog.show(() -> {
            if (itemName.getText().isBlank()
                    || category.getValue() == null
                    || subCategory.getValue() == null
                    || storageBox.getValue() == null) {
                info("Incomplete", "Item name, category, subcategory, and storage box are required.");
                return false;
            }
            Inventory item = new Inventory();
            item.setAdminUserId(user.getUserId());
            item.setItemName(itemName.getText().trim());
            item.setCategory(category.getValue());
            item.setSubCategory(subCategory.getValue());
            item.setBrand(brand.getText().trim());
            item.setColor(color.getText().trim());
            item.setDescription(description.getText().trim());
            item.setPrivateDetails(privateDetails.getText().trim());
            item.setStorageBox(storageBox.getValue());
            item.setStorageLocation(storageLocation.getText().trim());
            item.setTrackingNumber(item.generateTrackingNumber());
            item.setItemStatus("AVAILABLE");
            item.setFoundDate(LocalDate.now());
            if (!item.registerItem()) {
                info("Failed", "Could not register item.");
                return false;
            }
            item.assignStorageBox();
            info("Item registered",
                    "Tracking #: " + item.getTrackingNumber()
                    + "\nStored in: " + item.getStorageBox());
            navigate(Nav.INVENTORY);
            return true;
        });
    }

    /** Common school lost devices (from project classification). */
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

    /** Common personal belongings lost on campus. */
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

    /** Physical storage boxes Box A through Box T. */
    private static String[] storageBoxesAtoT() {
        String[] boxes = new String[20];
        for (int i = 0; i < 20; i++) {
            boxes[i] = "Box " + (char) ('A' + i);
        }
        return boxes;
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
                + "\n\nDescription:\n" + nullSafe(report.getDescription()));
    }

    private void showReportMatches(Report report) {
        Report probe = new Report();
        probe.setReportId(report.getReportId());
        Report.findById(report.getReportId()).ifPresent(stored -> {
            probe.setCategory(stored.getCategory());
            probe.setColor(stored.getColor());
            probe.setBrand(stored.getBrand());
            probe.setItemName(stored.getItemName());
        });
        List<Inventory> matches = probe.findPossibleMatches();
        if (matches.isEmpty()) {
            info("Possible matches", "No inventory matches found for this report.");
            return;
        }
        StringBuilder sb = new StringBuilder("Possible inventory matches:\n\n");
        for (Inventory item : matches) {
            sb.append("• ").append(item.getTrackingNumber())
                    .append(" — ").append(item.getItemName())
                    .append(" (").append(prettyStatus(item.getItemStatus())).append(")\n")
                    .append("  Private: ").append(nullSafe(item.getPrivateDetails())).append("\n\n");
        }
        info("Matches for " + report.getDisplayId(), sb.toString());
    }

    private void showUpdateReportStatusDialog(Report report) {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Update Report Status");
        ComboBox<String> status = dialog.combo("Status",
                "PENDING", "UNDER_REVIEW", "MATCHED", "RESOLVED", "CANCELLED");
        if (report.getReportStatus() != null) {
            String current = report.getReportStatus().toUpperCase(Locale.ENGLISH);
            status.getItems().forEach(s -> {
                if (s.equalsIgnoreCase(current)) {
                    status.getSelectionModel().select(s);
                }
            });
        }
        dialog.show(() -> {
            Report target = new Report();
            target.setReportId(report.getReportId());
            target.setReportStatus(status.getValue());
            if (!target.updateReportStatus()) {
                info("Failed", "Could not update status.");
                return false;
            }
            Notification note = new Notification();
            note.setUserId(report.getUserId());
            note.setReportId(report.getReportId());
            note.setMessage("Your report (" + report.getItemName() + ") is now "
                    + prettyStatus(status.getValue()) + ".");
            note.notifyReportUpdate();
            navigate(Nav.MANAGE_REPORTS);
            return true;
        });
    }

    private void showClaimDetails(Claim claim) {
        claim.viewClaimDetails();
        String student = User.findById(claim.getUserId())
                .map(u -> u.getFullName() + " (" + nullSafe(u.getStudentNumber()) + ")")
                .orElse("Unknown");
        info("Claim " + claim.getDisplayId(),
                "Item: " + claim.getItemName()
                + "\nStudent: " + student
                + "\nStatus: " + prettyStatus(claim.getClaimStatus())
                + "\nSubmitted: " + DATE_FMT.format(claim.getClaimDate())
                + "\nAdmin result: " + nullSafe(claim.getAdminResult())
                + "\nAdmin note: " + nullSafe(claim.getAdminNote())
                + "\nOwnership answer:\n" + nullSafe(claim.getOwnershipAnswer()));
    }

    private void showApproveClaimDialog(Claim claim) {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Approve Claim");
        ComboBox<String> result = dialog.combo("Verification result", "MATCH", "PARTIAL_MATCH");
        TextArea note = dialog.area("Admin note");
        TextField itemIdField = dialog.field("Link item ID (optional)");
        dialog.show(() -> {
            Claim target = new Claim();
            target.setClaimId(claim.getClaimId());
            target.setAdminResult(result.getValue());
            target.setAdminNote(note.getText().trim());
            if (!target.verifyOwnership()) {
                info("Failed", "Could not record verification.");
                return false;
            }
            String rawId = itemIdField.getText().trim();
            if (!rawId.isBlank()) {
                try {
                    int itemId = Integer.parseInt(rawId);
                    target.setItemId(itemId);
                    target.linkFoundItem();
                } catch (NumberFormatException ex) {
                    info("Invalid item ID", "Item ID must be a number.");
                    return false;
                }
            } else if (claim.getItemId() != null) {
                target.setItemId(claim.getItemId());
                target.linkFoundItem();
            }
            if (!target.approveClaim()) {
                info("Failed", "Could not approve claim.");
                return false;
            }
            Notification noteOut = new Notification();
            noteOut.setUserId(claim.getUserId());
            noteOut.setClaimId(claim.getClaimId());
            noteOut.setMessage("Your claim for " + claim.getItemName() + " has been approved.");
            noteOut.notifyClaimDecision();
            navigate(Nav.MANAGE_CLAIMS);
            return true;
        });
    }

    private void showRejectClaimDialog(Claim claim) {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Reject Claim");
        TextArea note = dialog.area("Reason for rejection");
        dialog.show(() -> {
            Claim target = new Claim();
            target.setClaimId(claim.getClaimId());
            target.setAdminResult("NO_MATCH");
            target.setAdminNote(note.getText().trim());
            target.verifyOwnership();
            if (!target.rejectClaim()) {
                info("Failed", "Could not reject claim.");
                return false;
            }
            Notification noteOut = new Notification();
            noteOut.setUserId(claim.getUserId());
            noteOut.setClaimId(claim.getClaimId());
            noteOut.setMessage("Your claim for " + claim.getItemName() + " was rejected.");
            noteOut.notifyClaimDecision();
            navigate(Nav.MANAGE_CLAIMS);
            return true;
        });
    }

    private void showHoldClaimDialog(Claim claim) {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Place Claim on Hold");
        TextArea note = dialog.area("Hold note");
        dialog.show(() -> {
            Claim target = new Claim();
            target.setClaimId(claim.getClaimId());
            target.setAdminNote(note.getText().trim());
            if (!target.placeOnHold()) {
                info("Failed", "Could not place claim on hold.");
                return false;
            }
            Notification noteOut = new Notification();
            noteOut.setUserId(claim.getUserId());
            noteOut.setClaimId(claim.getClaimId());
            noteOut.setMessage("Your claim for " + claim.getItemName() + " is on hold.");
            noteOut.notifyClaimDecision();
            navigate(Nav.MANAGE_CLAIMS);
            return true;
        });
    }

    private void showConfirmReturnDialog(Claim claim) {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Confirm Item Return");
        TextField receivedBy = dialog.field("Received by", user.getFullName());
        TextArea returnNote = dialog.area("Return note");
        DatePicker returnDate = new DatePicker(LocalDate.now());
        dialog.addCustom("Return date", returnDate);
        dialog.show(() -> {
            if (receivedBy.getText().isBlank()) {
                info("Incomplete", "Please enter who received the item.");
                return false;
            }
            Claim target = new Claim();
            target.setClaimId(claim.getClaimId());
            target.setReceivedBy(receivedBy.getText().trim());
            target.setReturnNote(returnNote.getText().trim());
            target.setReturnDate(returnDate.getValue() != null ? returnDate.getValue() : LocalDate.now());
            if (!target.confirmItemReturn()) {
                info("Failed", "Could not confirm return.");
                return false;
            }
            Notification noteOut = new Notification();
            noteOut.setUserId(claim.getUserId());
            noteOut.setClaimId(claim.getClaimId());
            noteOut.setMessage("Your item (" + claim.getItemName() + ") was returned successfully.");
            noteOut.notifyItemReturned();
            navigate(Nav.MANAGE_CLAIMS);
            return true;
        });
    }

    private void showInventoryDetails(Inventory item) {
        item.viewItemDetails();
        info("Item " + item.getTrackingNumber(),
                "Name: " + item.getItemName()
                + "\nCategory: " + item.getCategory()
                + "\nBrand: " + nullSafe(item.getBrand())
                + "\nColor: " + nullSafe(item.getColor())
                + "\nStatus: " + prettyStatus(item.getItemStatus())
                + "\nStorage: " + nullSafe(item.getStorageBox())
                + " @ " + nullSafe(item.getStorageLocation())
                + "\nFound: " + (item.getFoundDate() != null ? DATE_FMT.format(item.getFoundDate()) : "—")
                + "\n\nDescription:\n" + nullSafe(item.getDescription())
                + "\n\nPrivate details (admin only):\n" + nullSafe(item.getPrivateDetails()));
    }

    private void confirmArchiveItem(Inventory item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Archive item");
        confirm.setHeaderText("Archive " + item.getTrackingNumber() + "?");
        confirm.setContentText("This marks the item as archived in inventory.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                Inventory target = new Inventory();
                target.setItemId(item.getItemId());
                target.removeItem();
                navigate(Nav.INVENTORY);
            }
        });
    }

    private void showUserDetails(User account) {
        info("Account #" + account.getUserId(),
                "Name: " + account.getFullName()
                + "\nUsername: " + nullSafe(account.getUsername())
                + "\nStudent #: " + nullSafe(account.getStudentNumber())
                + "\nRole: " + prettyStatus(account.getRole())
                + "\nStatus: " + nullSafe(account.getAccountStatus())
                + "\nEmail: " + nullSafe(account.getEmail()));
    }

    private void deactivateUser(User account) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate account");
        confirm.setHeaderText("Deactivate " + account.getFullName() + "?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                User target = new User();
                target.setUserId(account.getUserId());
                target.deactivateAccount();
                navigate(Nav.USERS);
            }
        });
    }

    private void activateUser(User account) {
        User target = new User();
        target.setUserId(account.getUserId());
        if (target.activateAccount()) {
            navigate(Nav.USERS);
        }
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
        user.logout();
        LoginView loginView = new LoginView(stage);
        stage.setTitle("Lost and Java-Found — Login");
        stage.setScene(new javafx.scene.Scene(loginView.getRoot(), 1100, 680));
    }

    // ------------------------------------------------------------------ widgets

    private HBox tableHeader(String... cols) {
        HBox row = new HBox();
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle("-fx-border-color: " + UiTheme.BORDER + "; -fx-border-width: 0 0 1 0;");
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

    private StackPane statusBadge(String status) {
        String bg;
        String fg;
        String key = status.toLowerCase(Locale.ENGLISH);
        if (key.contains("pending") || key.contains("review")) {
            bg = UiTheme.STATUS_PENDING_BG;
            fg = UiTheme.STATUS_PENDING_FG;
        } else if (key.contains("approved") || key.contains("matched") || key.contains("active")) {
            bg = UiTheme.STATUS_APPROVED_BG;
            fg = UiTheme.STATUS_APPROVED_FG;
        } else if (key.contains("reject") || key.contains("cancel") || key.contains("inactive")) {
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

    private HBox multiActions(Button... buttons) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.getChildren().addAll(buttons);
        return box;
    }

    private Button linkBtn(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(UiTheme.linkButton());
        btn.setOnAction(e -> action.run());
        return btn;
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

    private HBox barRow(String label, long count, long max) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(label);
        name.setMinWidth(90);
        name.setStyle("-fx-text-fill: " + UiTheme.TEXT + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        double width = max > 0 ? (count * 220.0 / max) : 0;
        Rectangle bar = new Rectangle(Math.max(4, width), 14);
        bar.setFill(Color.web(UiTheme.UE_RED));
        bar.setArcWidth(8);
        bar.setArcHeight(8);
        Label value = new Label(String.valueOf(count));
        value.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-weight: bold;");
        row.getChildren().addAll(name, bar, value);
        return row;
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

        void addCustom(String label, Node node) {
            add(label, node);
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