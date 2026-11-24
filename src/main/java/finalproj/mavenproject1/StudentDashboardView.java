
package finalproj.mavenproject1;

import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private final StackPane sidebarAvatarHost = new StackPane();

    private Nav currentNav = Nav.DASHBOARD;
    private VBox navButtonsBox;
    private String claimsFilter = null;
    /** My Reports type filter: null = all, LOST, or FOUND (same chips as admin Manage Reports). */
    private String reportsTypeFilter = null;
    /** ACTIVE (default) | CANCELLED | ALL — soft-closed reports stay in MySQL. */
    private String reportsRecordFilter = "ACTIVE";
    private String reportsSearchKey = "";
    private String claimsSearchKey = "";

    /** Date-range filters (additive; null bounds = all dates). */
    private DateRanges.Preset reportsDatePreset = DateRanges.Preset.ALL;
    private LocalDate reportsFrom = null;
    private LocalDate reportsTo = null;
    private DateRanges.Preset claimsDatePreset = DateRanges.Preset.ALL;
    private LocalDate claimsFrom = null;
    private LocalDate claimsTo = null;

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
        // Keep original sidebar avatar size/spacing; photo only when set.
        sidebarAvatarHost.getChildren().setAll(buildUserAvatar(36));
        VBox userMeta = new VBox(2);
        Label name = new Label(user.getFullName());
        name.setStyle("-fx-text-fill: " + UiTheme.TEXT + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label role = new Label(prettyStatus(user.getUserRole()));
        role.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 11px;");
        userMeta.getChildren().addAll(name, role);
        userRow.getChildren().addAll(sidebarAvatarHost, userMeta);

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

        // Keep profile + logout pinned at the bottom (always visible).
        VBox footer = new VBox(12, userRow, logout);
        footer.setMinHeight(Region.USE_PREF_SIZE);

        sidebar.getChildren().addAll(brand, brandRule, section, navScroll, footer);
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
        btn.getStyleClass().removeAll("ue-nav", "ue-nav-idle", "ue-nav-active");
        btn.getStyleClass().addAll("ue-nav", active ? "ue-nav-active" : "ue-nav-idle");
        btn.setFocusTraversable(false);
        btn.setStyle("");

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
 "-fx-background-color: linear-gradient(to bottom right, #E41135, #9C0B24);" // Red gradient
                + "-fx-text-fill: white;" // Makes the bell white
                + "-fx-font-size: 20px;" // Makes the bell larger
                + "-fx-background-radius: 20;"
                + "-fx-border-color: transparent;" // Removes the old border
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
           statCard("Active Reports", String.valueOf(pendingReports), "Open lost-item cases", UiTheme.UE_RED, "DOCUMENT"),
           
           statCard("Pending Claims", String.valueOf(pendingClaims), "Waiting for review", "#EAB308", "SEARCH"),
           
           statCard("Approved", String.valueOf(approvedClaims), "Ready for return", "#22C55E", "CHECK"),
           
           statCard("Unread", String.valueOf(unread), "Notifications", "#3B82F6", "BELL")
   );

        VBox cases = sectionCard("Track Your Cases",
                "Follow each lost-item case from filed → match → claim → Claimed.");
        VBox caseList = new VBox(10);
        List<Report> openLost = reports.stream()
                .filter(r -> !r.isFoundReport())
                .filter(r -> !r.isSoftClosed())
                .limit(3)
                .toList();
        if (openLost.isEmpty()) {
            caseList.getChildren().add(muted("No active lost-item cases yet. File a report to start tracking."));
        } else {
            List<Claim> myClaims = claimsProbe.viewMyClaims();
            for (Report report : openLost) {
                List<Claim> related = myClaims.stream()
                        .filter(c -> c.getReportId() != null && c.getReportId() == report.getReportId())
                        .toList();
                caseList.getChildren().add(CaseTracker.card(report, related, () -> showReportDetails(report)));
            }
        }
        cases.getChildren().add(caseList);

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
        Button reportBtn = UiButtons.primaryIcon("Report Item", "PLUS", this::showReportDialog);
        Button claimBtn = UiButtons.primaryIcon("Submit Claim", "PLUS",
                () -> showSubmitClaimDialog(null));
        Button claimsBtn = UiButtons.primary("View My Claims", () -> navigate(Nav.CLAIMS));
        actions.getChildren().addAll(reportBtn, claimBtn, claimsBtn);

        return new VBox(18, stats, actions, cases, recent);
    }

    private Node buildReportsPage() {
        VBox page = new VBox(14);

        HBox typeFilters = new HBox(8);
        typeFilters.getChildren().addAll(
                filterChip("All Types", reportsTypeFilter == null, () -> {
                    reportsTypeFilter = null;
                    navigate(Nav.REPORTS);
                }),
                filterChip("Lost", "LOST".equals(reportsTypeFilter), () -> {
                    reportsTypeFilter = "LOST";
                    navigate(Nav.REPORTS);
                }),
                filterChip("Found", "FOUND".equals(reportsTypeFilter), () -> {
                    reportsTypeFilter = "FOUND";
                    navigate(Nav.REPORTS);
                })
        );

        HBox recordFilters = new HBox(8);
        recordFilters.getChildren().addAll(
                filterChip("Active", "ACTIVE".equals(reportsRecordFilter), () -> {
                    reportsRecordFilter = "ACTIVE";
                    navigate(Nav.REPORTS);
                }),
                filterChip("Cancelled", "CANCELLED".equals(reportsRecordFilter), () -> {
                    reportsRecordFilter = "CANCELLED";
                    navigate(Nav.REPORTS);
                }),
                filterChip("Archived", "ARCHIVED".equals(reportsRecordFilter), () -> {
                    reportsRecordFilter = "ARCHIVED";
                    navigate(Nav.REPORTS);
                }),
                filterChip("All records", "ALL".equals(reportsRecordFilter), () -> {
                    reportsRecordFilter = "ALL";
                    navigate(Nav.REPORTS);
                })
        );

        Node dateBar = buildDateRangeBar(
                reportsDatePreset, reportsFrom, reportsTo,
                (preset, from, to) -> {
                    reportsDatePreset = preset;
                    reportsFrom = from;
                    reportsTo = to;
                    navigate(Nav.REPORTS);
                }
        );

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField search = new TextField(reportsSearchKey);
        search.setPromptText("Search my reports...");
        search.setStyle(fieldStyleStudent());
        search.setPrefWidth(180);
        Button searchBtn = UiButtons.ghostIcon("Search", "SEARCH", () -> {
            reportsSearchKey = search.getText() == null ? "" : search.getText().trim();
            navigate(Nav.REPORTS);
        });
        Button refreshBtn = UiButtons.ghostIcon("Refresh", "REFRESH", () -> navigate(Nav.REPORTS));
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Button add = UiButtons.primaryIcon("Report Lost / Found", "PLUS", this::showReportDialog);
        toolbar.getChildren().addAll(search, searchBtn, refreshBtn, grow, add);

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.setMaxWidth(Double.MAX_VALUE);

        LocalDate[] bounds = DateRanges.boundsFor(reportsDatePreset, reportsFrom, reportsTo);
        List<Report> reports = filterStudentReports(reportForUser().viewMyReports(), reportsSearchKey);
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
            String emptyTitle = "CANCELLED".equals(recordFilter)
                    ? "No cancelled reports."
                    : "ARCHIVED".equals(recordFilter)
                    ? "No archived reports."
                    : "FOUND".equals(reportsTypeFilter)
                    ? "No Found reports yet."
                    : "LOST".equals(reportsTypeFilter)
                    ? "No Lost reports yet."
                    : "No reports in this filter.";
            String emptyBody = "CANCELLED".equals(recordFilter) || "ARCHIVED".equals(recordFilter)
                    ? "Soft-closed reports stay on file for audit — nothing is wiped from the database."
                    : "FOUND".equals(reportsTypeFilter)
                    ? "File a Found report when you turn an item in to SAO."
                    : "LOST".equals(reportsTypeFilter)
                    ? "File a Lost report when you are missing an item. After a match, claim under My Claims."
                    : "File a Lost or Found report to get started — or clear the date filter.";
            tableCard.getChildren().add(emptyStateWithAction(
                    emptyTitle, emptyBody, "File a report", this::showReportDialog
            ));
        } else {
            GridPane table = newDataTable();
            int r = 0;
            r = addTableHeader(table, r, "ID", "Item Name", "Type", "Date Reported", "Status", "Actions");
            for (Report report : reports) {
                r = addReportRow(table, r, report);
            }
            tableCard.getChildren().add(table);
        }

        String guide = "Cancel or Archive keeps the report on file — nothing is deleted from MySQL (audit history).";
        page.getChildren().addAll(typeFilters, recordFilters, dateBar, toolbar, tableCard,
                muted("Showing " + reports.size() + " report" + (reports.size() == 1 ? "" : "s")
                        + " · " + DateRanges.describe(reportsDatePreset, bounds[0], bounds[1])
                        + ". " + guide));
        return page;
    }

    private Node buildClaimsPage() {
        VBox page = new VBox(14);

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField search = new TextField(claimsSearchKey);
        search.setPromptText("Search my claims...");
        search.setStyle(fieldStyleStudent());
        HBox.setHgrow(search, Priority.ALWAYS);
        Button searchBtn = UiButtons.ghostIcon("Search", "SEARCH", () -> {
            claimsSearchKey = search.getText() == null ? "" : search.getText().trim();
            navigate(Nav.CLAIMS);
        });
        Button refreshBtn = UiButtons.ghostIcon("Refresh", "REFRESH", () -> navigate(Nav.CLAIMS));
        Button submitClaimBtn = UiButtons.primaryIcon("Submit Claim", "PLUS",
                () -> showSubmitClaimDialog(null));
        toolbar.getChildren().addAll(search, searchBtn, refreshBtn, submitClaimBtn);

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
                }),
                filterChip("Archived", "ARCHIVED".equals(claimsFilter), () -> {
                    claimsFilter = "ARCHIVED";
                    navigate(Nav.CLAIMS);
                })
        );

        Node dateBar = buildDateRangeBar(
                claimsDatePreset, claimsFrom, claimsTo,
                (preset, from, to) -> {
                    claimsDatePreset = preset;
                    claimsFrom = from;
                    claimsTo = to;
                    navigate(Nav.CLAIMS);
                }
        );

        VBox tableCard = new VBox(0);
        tableCard.setStyle(UiTheme.cardStyle());
        tableCard.setPadding(new Insets(4, 0, 4, 0));
        tableCard.setMaxWidth(Double.MAX_VALUE);

        LocalDate[] bounds = DateRanges.boundsFor(claimsDatePreset, claimsFrom, claimsTo);
        List<Claim> claims = filterStudentClaims(claimForUser().filterByStatus(claimsFilter), claimsSearchKey)
                .stream()
                // "All Claims" hides archived (use the Archived chip to see them).
                .filter(c -> claimsFilter != null || c.getClaimStatus() == null
                        || !"ARCHIVED".equalsIgnoreCase(c.getClaimStatus()))
                .filter(c -> DateRanges.includes(c.getClaimDate(), bounds[0], bounds[1]))
                .toList();
        if (claims.isEmpty()) {
            String emptyTitle = "ARCHIVED".equals(claimsFilter) ? "No archived claims." : "No claims in this filter.";
            String emptyBody = "ARCHIVED".equals(claimsFilter)
                    ? "Archive a claim from Actions to file it here — rows stay in MySQL for audit."
                    : "After a lost report is Matched, submit a claim with ownership answers — or clear the date filter.";
            tableCard.getChildren().add(emptyStateWithAction(emptyTitle, emptyBody,
                    "Submit claim", () -> showSubmitClaimDialog(null)));
        } else {
            GridPane table = newDataTable();
            int r = 0;
            r = addTableHeader(table, r, "Claim ID", "Item Name", "Date Submitted", "Status", "Details", "Actions");
            for (Claim claim : claims) {
                r = addClaimRow(table, r, claim);
            }
            tableCard.getChildren().add(table);
        }

        page.getChildren().addAll(toolbar, filters, dateBar, tableCard,
                muted("Showing " + claims.size() + " claim" + (claims.size() == 1 ? "" : "s")
                        + " · " + DateRanges.describe(claimsDatePreset, bounds[0], bounds[1])
                        + ". Archive keeps claims on file — nothing is deleted."));
        return page;
    }

    private Node buildNotificationsPage() {
        VBox page = new VBox(14);

        HBox toolbar = new HBox();
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        Button markAll = UiButtons.primaryIcon("Mark All as Read", "CHECK", () -> {
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

        VBox avatarCard = new VBox(12);
        avatarCard.setAlignment(Pos.TOP_CENTER);
        avatarCard.setPadding(new Insets(28, 20, 28, 20));
        avatarCard.setPrefWidth(240);
        avatarCard.setMinWidth(240);
        avatarCard.setMaxWidth(260);
        avatarCard.setStyle(UiTheme.cardStyle());

        Node avatarNode = buildUserAvatar(88);
        if (avatarNode instanceof Region avatarRegion) {
            avatarRegion.setMaxWidth(Region.USE_PREF_SIZE);
        }

        Label nameLabel = centeredLabel(nullSafe(user.getFullName()), UiTheme.TEXT, 15, true);

        Button changeAvatarBtn = softButton("Change Avatar", this::changeAvatar);
        changeAvatarBtn.setMaxWidth(Region.USE_PREF_SIZE);
        HBox changeRow = new HBox(changeAvatarBtn);
        changeRow.setAlignment(Pos.CENTER);
        changeRow.setMaxWidth(Double.MAX_VALUE);

        // Keep avatar card minimal: photo + name + avatar actions (course/year live in info fields).
        avatarCard.getChildren().addAll(avatarNode, nameLabel, changeRow);

        boolean hasPhoto = (user.getAvatarPath() != null && !user.getAvatarPath().isBlank())
                || (user.getAvatarData() != null && user.getAvatarData().length > 0);
        if (hasPhoto) {
            Button removeBtn = softButton("Remove Photo", this::removeAvatar);
            removeBtn.setMaxWidth(Region.USE_PREF_SIZE);
            HBox removeRow = new HBox(removeBtn);
            removeRow.setAlignment(Pos.CENTER);
            removeRow.setMaxWidth(Double.MAX_VALUE);
            avatarCard.getChildren().add(removeRow);
        }

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
                "LOCK",
                "Change Password",
                "Update the password used to sign in to your account.",
                this::showChangePasswordDialog
        ));
        page.getChildren().add(settingsTile(
                "MAIL",
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
    private static final double[] TABLE_COL_PERCENTS = {10, 18, 12, 14, 14, 32};
    private static final int ACTIONS_COL = TABLE_COL_PERCENTS.length - 1;
    private static Pos columnAlign(int index) {
        return Pos.CENTER;
    }
    private static HPos columnHPos(int index) {
        return HPos.CENTER;
    }
    private GridPane newDataTable() {
        GridPane table = new GridPane();
        table.setMaxWidth(Double.MAX_VALUE);
        table.setMinWidth(0);
        table.setHgap(12);
        table.setVgap(0);
        table.setPadding(new Insets(4, 20, 4, 20));
        for (int i = 0; i < TABLE_COL_PERCENTS.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(TABLE_COL_PERCENTS[i]);
            cc.setMinWidth(0);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            cc.setHalignment(columnHPos(i));
            table.getColumnConstraints().add(cc);
        }
        return table;
    }

    private int addTableHeader(GridPane table, int row, String... cols) {
        int count = Math.min(cols.length, TABLE_COL_PERCENTS.length);
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
    private int addReportRow(GridPane table, int row, Report report) {

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button view = new Button("View");
        view.setStyle(UiTheme.linkButton());
        view.setOnAction(e -> showReportDetails(report));
        actions.getChildren().add(view);

        if (canEditPendingReport(report)) {
            Button edit = new Button("Edit");
            edit.setStyle(UiTheme.linkButton());
            edit.setOnAction(e -> showEditReportDialog(report));
            actions.getChildren().add(edit);
        }

        boolean readyToClaim = canClaimMatchedLostReport(report)
                && !hasOpenClaimForReport(report.getReportId());
        if (readyToClaim) {
            Button claim = new Button("Claim now");
            claim.setStyle(UiTheme.linkButton());
            claim.setOnAction(e -> showSubmitClaimDialog(report));
            actions.getChildren().add(claim);
        }

        if (report.canBeCancelledByOwner()) {
            Button cancel = new Button("Cancel");
            cancel.setStyle(
                    "-fx-background-color: transparent;"
                    + "-fx-text-fill: " + UiTheme.TEXT_MUTED + ";"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-cursor: hand;"
            );
            cancel.setOnAction(e -> cancelReport(report));
            actions.getChildren().add(cancel);
        }

        if (report.canBeArchived()) {
            Button archive = new Button("Archive");
            archive.setStyle(UiTheme.linkButton());
            archive.setOnAction(e -> archiveReport(report));
            actions.getChildren().add(archive);
        }

        if (report.canBeRestoredFromArchive()) {
            Button restore = new Button("Restore");
            restore.setStyle(UiTheme.linkButton());
            restore.setOnAction(e -> restoreArchivedReport(report));
            actions.getChildren().add(restore);
        }

        putData(table, 0, row, cell(report.getDisplayId(), true));
        putData(table, 1, row, cell(report.getItemName(), false));
        putData(table, 2, row, cell(report.isFoundReport() ? "Found" : "Lost", false));
        putData(table, 3, row, cell(DATE_FMT.format(report.getDateSubmitted()), false));
        HBox statusCell = new HBox(6);
        statusCell.setAlignment(Pos.CENTER);
        statusCell.getChildren().add(statusBadge(
                readyToClaim ? "Matched — claim" : reportProgressStatus(report)));
        putData(table, 4, row, statusCell);
        putData(table, 5, row, actions);
        return row + 1;
    }

    private int addClaimRow(GridPane table, int row, Claim claim) {
        putData(table, 0, row, cell(claim.getDisplayId(), true));
        putData(table, 1, row, cell(claim.getItemName(), false));
        putData(table, 2, row, cell(DATE_FMT.format(claim.getClaimDate()), false));
        putData(table, 3, row, statusBadge(prettyStatus(claim.getClaimStatus())));
        putData(table, 4, row, cell(claimDetailsPreview(claim), false));

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button view = new Button("View");
        view.setStyle(UiTheme.linkButton());
        view.setOnAction(e -> showClaimDetails(claim));
        actions.getChildren().add(view);
        // Approved + not yet picked up → show Resend even if code was never issued (older claims).
        boolean awaitingPickup = claim.getClaimStatus() != null
                && "APPROVED".equalsIgnoreCase(claim.getClaimStatus())
                && claim.getReturnDate() == null;
        if (awaitingPickup) {
            boolean hasCode = claim.getPickupCode() != null && !claim.getPickupCode().isBlank();
            if (hasCode) {
                Button copy = new Button("Copy code");
                copy.setStyle(UiTheme.linkButton());
                copy.setOnAction(e -> copyPickupCode(claim.getPickupCode()));
                actions.getChildren().add(copy);
            }
            Button resend = new Button(hasCode ? "Resend code" : "Get pickup code");
            resend.setStyle(UiTheme.linkButton());
            resend.setOnAction(e -> resendMyPickupCode(claim));
            actions.getChildren().add(resend);
        }
        if (claim.canBeArchived()) {
            Button archive = new Button("Archive");
            archive.setStyle(UiTheme.linkButton());
            archive.setOnAction(e -> archiveClaim(claim));
            actions.getChildren().add(archive);
        }
        if (claim.canBeRestoredFromArchive()) {
            Button restore = new Button("Restore");
            restore.setStyle(UiTheme.linkButton());
            restore.setOnAction(e -> restoreArchivedClaim(claim));
            actions.getChildren().add(restore);
        }
        putData(table, 5, row, actions);
        return row + 1;
    }

    /** DETAILS column: rejection reason when rejected; otherwise ownership answers. */
    private static String claimDetailsPreview(Claim claim) {
        if (claim.getClaimStatus() != null && "REJECTED".equalsIgnoreCase(claim.getClaimStatus())) {
            String reason = claim.getAdminNote();
            if (reason != null && !reason.isBlank()) {
                return shortText("Rejected: " + reason.trim(), 36);
            }
            return "Rejected — open View for details";
        }
        return shortText(nullSafe(claim.getOwnershipAnswer()), 28);
    }

    private void copyPickupCode(String code) {
        if (!ClipboardUtil.copyText(code)) {
            info("No code", "There is no pickup code to copy yet.");
            return;
        }
        Toast.show(stage, "Pickup code copied: " + code.trim());
    }

    /** Sends (or creates then sends) the digital pickup notice for an approved claim. */
    private void resendMyPickupCode(Claim claim) {
        Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
        if (live.getClaimStatus() == null || !"APPROVED".equalsIgnoreCase(live.getClaimStatus())
                || live.getReturnDate() != null) {
            info("Not available", "Pickup codes are only for approved claims waiting for pickup.");
            return;
        }
        boolean created = live.getPickupCode() == null || live.getPickupCode().isBlank();
        if (!live.ensurePickupCode()) {
            info("Failed", "Could not create or load a pickup code for this claim.");
            return;
        }
        Claim refreshed = Claim.findById(live.getClaimId()).orElse(live);
        ClaimSlipPrinter.sendPickupNoticeToStudent(refreshed);
        Toast.show(stage, (created ? "Pickup code created — " : "Pickup code resent — ")
                + refreshed.getPickupCode()
                + ". Check Notifications"
                + (user.getEmail() != null && user.getEmail().contains("@") ? " / email" : ""));
        navigate(Nav.CLAIMS);
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

    private HBox actionCell(Runnable viewAction, Runnable secondaryAction) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
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
        if (key.contains("pending") || key.contains("review") || key.equals("not claimed")
                || key.contains("for pickup")) {
            bg = UiTheme.STATUS_PENDING_BG;
            fg = UiTheme.STATUS_PENDING_FG;
        } else if (key.contains("approved") || key.contains("matched")
                || key.equals("claimed") || key.contains("resolved")
                || key.contains("register")) {
            bg = UiTheme.STATUS_APPROVED_BG;
            fg = UiTheme.STATUS_APPROVED_FG;
        } else if (key.contains("reject") || key.contains("cancel") || key.contains("archiv")) {
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
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 20;");
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    private HBox notificationRow(Notification note, boolean clickable) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 18, 16, 18));
        row.setStyle(UiTheme.cardStyle()
                + (note.isRead() ? "" : "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"));

        // Always a true circle — SVG must not stretch the host into a tall pill.
        StackPane icon = notifCircleIcon(note.getNotificationType());

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
                // Workflow step 4→5: match notification opens claim for that report.
                if ("POSSIBLE_MATCH".equalsIgnoreCase(note.getNotificationType())
                        && note.getReportId() != null) {
                    Report.findById(note.getReportId()).ifPresentOrElse(
                            report -> showSubmitClaimDialog(report),
                            () -> navigate(Nav.NOTIFICATIONS)
                    );
                } else if ("CLAIM_DECISION".equalsIgnoreCase(note.getNotificationType())
                        || "RETURN".equalsIgnoreCase(note.getNotificationType())) {
                    navigate(Nav.CLAIMS);
                } else {
                    navigate(Nav.NOTIFICATIONS);
                }
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
        } else {
            path.setContent(
                    "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z");
        }
        return path;
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
        return emptyStateWithAction(title, subtitle, null, null);
    }

    private VBox emptyStateWithAction(String title, String subtitle, String actionLabel, Runnable action) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(42, 24, 42, 24));
        box.getChildren().addAll(
                labeled(title, UiTheme.TEXT, 15, true),
                muted(subtitle)
        );
        if (actionLabel != null && action != null) {
            box.getChildren().add(UiButtons.primary(actionLabel, action));
        }
        return box;
    }

    private static String fieldStyleStudent() {
        return "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 8; -fx-background-radius: 8;"
                + "-fx-padding: 8 10 8 10;";
    }

    private List<Report> filterStudentReports(List<Report> source, String key) {
        if (key == null || key.isBlank()) return source;
        String q = key.toLowerCase(Locale.ENGLISH);
        return source.stream().filter(r ->
                (r.getItemName() != null && r.getItemName().toLowerCase(Locale.ENGLISH).contains(q))
                        || (r.getBrand() != null && r.getBrand().toLowerCase(Locale.ENGLISH).contains(q))
                        || (r.getDisplayId() != null && r.getDisplayId().toLowerCase(Locale.ENGLISH).contains(q))
        ).toList();
    }

    private List<Claim> filterStudentClaims(List<Claim> source, String key) {
        if (key == null || key.isBlank()) return source;
        String q = key.toLowerCase(Locale.ENGLISH);
        return source.stream().filter(c ->
                (c.getItemName() != null && c.getItemName().toLowerCase(Locale.ENGLISH).contains(q))
                        || (c.getDisplayId() != null && c.getDisplayId().toLowerCase(Locale.ENGLISH).contains(q))
        ).toList();
    }

    private boolean looksLikeDuplicateReport(String itemName) {
        if (itemName == null || itemName.isBlank()) return false;
        String key = itemName.toLowerCase(Locale.ENGLISH);
        LocalDate cutoff = LocalDate.now().minusDays(14);
        return reportForUser().viewMyReports().stream().anyMatch(r -> {
            if (r.getItemName() == null) return false;
            if (!r.getItemName().toLowerCase(Locale.ENGLISH).contains(key)
                    && !key.contains(r.getItemName().toLowerCase(Locale.ENGLISH))) {
                return false;
            }
            if (r.isSoftClosed()) return false;
            return r.getDateSubmitted() == null || !r.getDateSubmitted().isBefore(cutoff);
        });
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
        fromPick.setStyle(fieldStyleStudent());
        Label toLbl = new Label("To");
        toLbl.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        DatePicker toPick = new DatePicker(to);
        toPick.setPrefWidth(140);
        toPick.setStyle(fieldStyleStudent());
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

    private Button filterChip(String text, boolean active, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("ue-chip", active ? "ue-chip-active" : "ue-chip-idle");
        btn.setFocusTraversable(false);
        btn.setOnAction(e -> {
            btn.getStyleClass().removeAll("ue-chip-idle", "ue-chip-active");
            btn.getStyleClass().add("ue-chip-active");
            btn.setTextFill(javafx.scene.paint.Color.WHITE);
            action.run();
        });
        return btn;
    }

    private Button softButton(String text, Runnable action) {
        return UiButtons.ghost(text, action);
    }

    // ------------------------------------------------------------------ dialogs

    private void showReportDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Report Lost / Found Item");
        dialog.help("Lost = your item is missing (you are the owner looking for it).\n"
                + "Found = you found an item that is not yours and are turning it in to Lost & Found.\n\n"
                + "You may file both Lost and Found for different items (e.g. lost earbud + found jacket).\n"
                + "You cannot file Lost and Found for the same item on one account.\n\n"
                + "Staff register Found items into inventory, then match them to open Lost reports.");
        ComboBox<String> reportType = dialog.combo("Report type",
                "Lost item (mine is missing)",
                "Found item (turning in to L&F)");
        TextField itemName = dialog.field("Item name");
        ComboBox<String> category = dialog.combo("Category",
                "Devices", "Personal Belongings");
        ComboBox<String> subCategory = dialog.combo("Subcategory", deviceSubcategories());
        TextField color = dialog.field("Color");
        TextField brand = dialog.field("Brand (optional)");
        TextField location = dialog.field("Location");
        location.setPromptText("Last known / found location");
        TextArea description = dialog.area("Description");
        BrandedDialog.PhotoPick photoPick = dialog.photoPicker("Item photo (required)");

        category.valueProperty().addListener((obs, oldVal, newVal) -> {
            String[] options = "Devices".equals(newVal)
                    ? deviceSubcategories()
                    : personalSubcategories();
            subCategory.getItems().setAll(options);
            subCategory.getSelectionModel().selectFirst();
        });

        dialog.show(() -> {
            if (itemName.getText().isBlank() || location.getText().isBlank()
                    || description.getText().isBlank()
                    || category.getValue() == null
                    || subCategory.getValue() == null
                    || reportType.getValue() == null) {
                info("Incomplete", "Please fill in type, item name, category, subcategory, location, and description.");
                return false;
            }
            if (photoPick.storedPath == null || photoPick.storedPath.isBlank()) {
                info("Photo required", "Please attach an item photo before submitting.");
                return false;
            }
            boolean found = reportType.getValue() != null
                    && reportType.getValue().toLowerCase(Locale.ENGLISH).startsWith("found");
            String name = itemName.getText().trim();
            String type = found ? "FOUND" : "LOST";
            String cat = category.getValue();
            String colorVal = color.getText().trim();
            String brandVal = brand.getText().trim();
            // Same student + same item as opposite type (active only) is blocked.
            String crossBlock = Report.crossTypeSameItemBlockReason(
                    user.getUserId(), type, name, cat, colorVal, brandVal);
            if (crossBlock != null) {
                info("Not allowed", crossBlock);
                return false;
            }
            // Exact open duplicate is blocked in the model; warn first for near matches.
            if (Report.hasOpenDuplicate(user.getUserId(), name, type)) {
                info("Duplicate report",
                        "You already have an open " + (found ? "found" : "lost")
                        + " report for \"" + name + "\".\n"
                        + "Edit or cancel the existing one instead of submitting again.");
                return false;
            }
            if (looksLikeDuplicateReport(name)) {
                if (!BrandedDialog.confirm(stage, "Similar report found",
                        "You already have a recent report with a similar item name.\n"
                        + "Submit another report anyway?")) {
                    return false;
                }
            }
            Report report = new Report();
            report.setUserId(user.getUserId());
            report.setReportType(type);
            report.setItemName(name);
            report.setCategory(cat);
            report.setSubCategory(subCategory.getValue());
            report.setColor(colorVal);
            report.setBrand(brandVal);
            report.setDescription(description.getText().trim());
            report.setLostLocation(location.getText().trim());
            report.setLostDate(java.time.LocalDate.now());
            // Status chosen inside submitReport (Lost + no inventory → ON_HOLD).
            report.setReportStatus("PENDING");
            report.setPhoto(photoPick.storedPath);
            if (!report.submitReport()) {
                String blocked = Report.crossTypeSameItemBlockReason(
                        user.getUserId(), type, name, cat, colorVal, brandVal);
                if (blocked != null) {
                    info("Not allowed", blocked);
                    return false;
                }
                info("Failed",
                        "Could not submit report. A matching open report may already exist.");
                return false;
            }
            boolean onHold = !found && report.isOnHoldAwaitingInventory();
            Notification note = new Notification();
            note.setUserId(user.getUserId());
            note.setReportId(report.getReportId());
            note.setMessage(found
                    ? "Your found-item report (" + report.getItemName() + ") was submitted for admin review."
                    : onHold
                    ? ("Your lost-item report (" + report.getItemName()
                    + ") is On Hold — no matching item is in inventory yet. "
                    + "When a possible match is registered, staff will review it.")
                    : ("Your lost-item report (" + report.getItemName()
                    + ") was submitted — a possible inventory match was found for staff review."));
            note.notifyReportUpdate();
            navigate(Nav.REPORTS);
            Toast.show(stage, found
                    ? "Found report submitted — " + report.getDisplayId()
                    : onHold
                    ? "Lost report On Hold — " + report.getDisplayId() + " (waiting for inventory)"
                    : "Lost report submitted — " + report.getDisplayId());
            return true;
        });
    }

    private void showEditReportDialog(Report existing) {
        if (!canEditPendingReport(existing)) {
            info("Not editable", "Only Pending / Under review / On Hold reports can be edited.");
            return;
        }
        DialogPaneBuilder dialog = new DialogPaneBuilder("Edit Report " + existing.getDisplayId());
        TextField itemName = dialog.field("Item name", nullSafe(existing.getItemName()));
        ComboBox<String> category = dialog.combo("Category", "Devices", "Personal Belongings");
        if ("Personal Belongings".equalsIgnoreCase(existing.getCategory())) {
            category.getSelectionModel().select("Personal Belongings");
        }
        ComboBox<String> subCategory = dialog.combo("Subcategory",
                "Devices".equals(category.getValue()) ? deviceSubcategories() : personalSubcategories());
        if (existing.getSubCategory() != null) {
            if (!subCategory.getItems().contains(existing.getSubCategory())) {
                subCategory.getItems().add(existing.getSubCategory());
            }
            subCategory.getSelectionModel().select(existing.getSubCategory());
        }
        TextField color = dialog.field("Color", nullSafe(existing.getColor()));
        TextField brand = dialog.field("Brand (optional)", nullSafe(existing.getBrand()));
        TextField location = dialog.field("Location", nullSafe(existing.getLostLocation()));
        TextArea description = dialog.area("Description");
        description.setText(nullSafe(existing.getDescription()));
        BrandedDialog.PhotoPick photoPick = dialog.photoPicker("Item photo (required)");
        if (existing.getPhotoPath() != null && !existing.getPhotoPath().isBlank()) {
            photoPick.setPath(existing.getPhotoPath());
        }

        category.valueProperty().addListener((obs, oldVal, newVal) -> {
            String[] options = "Devices".equals(newVal)
                    ? deviceSubcategories()
                    : personalSubcategories();
            subCategory.getItems().setAll(options);
            subCategory.getSelectionModel().selectFirst();
        });

        dialog.show(() -> {
            if (itemName.getText().isBlank() || location.getText().isBlank()
                    || description.getText().isBlank()
                    || category.getValue() == null
                    || subCategory.getValue() == null) {
                info("Incomplete", "Please fill in item name, category, subcategory, location, and description.");
                return false;
            }
            if (photoPick.storedPath == null || photoPick.storedPath.isBlank()) {
                info("Photo required", "Please attach an item photo before saving.");
                return false;
            }
            existing.setItemName(itemName.getText().trim());
            existing.setCategory(category.getValue());
            existing.setSubCategory(subCategory.getValue());
            existing.setColor(color.getText().trim());
            existing.setBrand(brand.getText().trim());
            existing.setDescription(description.getText().trim());
            existing.setLostLocation(location.getText().trim());
            // Keep existing BLOB if the same file path is still selected.
            if (photoPick.storedPath.equals(existing.getPhotoPath())
                    && existing.getPhotoData() != null && existing.getPhotoData().length > 0) {
                existing.setPhotoPath(photoPick.storedPath);
            } else {
                existing.setPhoto(photoPick.storedPath);
            }
            if (!existing.updateReport()) {
                info("Failed", "Could not update report (it may no longer be editable).");
                return false;
            }
            // If On Hold and details now overlap inventory, lift to Pending for staff match.
            String liftedNote = "";
            if (existing.isLostReport() && existing.isOnHoldAwaitingInventory()
                    && !MatchScorer.rankForReport(existing).isEmpty()) {
                Report lift = new Report();
                lift.setReportId(existing.getReportId());
                lift.setReportStatus("PENDING");
                if (lift.updateReportStatus()) {
                    existing.setReportStatus("PENDING");
                    liftedNote = "\nA possible inventory match was found — status is now Pending for staff review.";
                    Notification note = new Notification();
                    note.setUserId(user.getUserId());
                    note.setReportId(existing.getReportId());
                    note.setMessage("Your lost report (" + existing.getItemName()
                            + ") now has a possible inventory match. Status: Pending — staff will review.");
                    note.notifyReportUpdate();
                }
            }
            info("Report updated", existing.getDisplayId() + " was saved." + liftedNote);
            navigate(Nav.REPORTS);
            return true;
        });
    }

    private static boolean canEditPendingReport(Report report) {
        if (report == null) return false;
        String status = report.getReportStatus() == null ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);
        return "PENDING".equals(status) || "UNDER_REVIEW".equals(status) || "ON_HOLD".equals(status);
    }

    /** Common school lost devices. */
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

    private void showEditProfileDialog() {
        DialogPaneBuilder dialog = new DialogPaneBuilder("Edit Profile");
        dialog.help("Use the same college, course, and year options as registration.");
        TextField fullName = dialog.field("Full name", user.getFullName());
        TextField studentNumber = dialog.field("Student number", nullSafe(user.getStudentNumber()));

        ComboBox<String> college = dialog.combo("College", UePrograms.COLLEGES);
        ComboBox<String> course = dialog.combo("Course", UePrograms.coursesForCollege(college.getValue()));
        ComboBox<String> year = dialog.combo("Year level", UePrograms.YEAR_LEVELS);

        // Prefill from current profile (including legacy short course / year values).
        String matchedCourse = UePrograms.matchCourse(user.getCourse());
        String matchedCollege = matchedCourse != null
                ? UePrograms.collegeForCourse(matchedCourse)
                : UePrograms.collegeForCourse(user.getCourse());
        if (matchedCollege != null) {
            college.getSelectionModel().select(matchedCollege);
            course.getItems().setAll(UePrograms.coursesForCollege(matchedCollege));
            if (matchedCourse != null) {
                course.getSelectionModel().select(matchedCourse);
            } else {
                course.getSelectionModel().selectFirst();
            }
        }
        String matchedYear = UePrograms.matchYearLevel(user.getYearLevel());
        if (matchedYear != null) {
            year.getSelectionModel().select(matchedYear);
        }

        college.valueProperty().addListener((obs, oldVal, newVal) -> {
            String[] courses = UePrograms.coursesForCollege(newVal);
            course.getItems().setAll(courses);
            if (courses.length > 0) {
                course.getSelectionModel().selectFirst();
            } else {
                course.getSelectionModel().clearSelection();
            }
        });

        TextField contact = dialog.field("Contact number", nullSafe(user.getContactNumber()));
        TextField email = dialog.field("Email", user.getEmail());

        dialog.show(() -> {
            if (fullName.getText().isBlank()) {
                info("Invalid", "Full name is required.");
                return false;
            }
            String sn = studentNumber.getText() == null ? "" : studentNumber.getText().trim();
            if (!sn.matches("\\d{11}")) {
                info("Invalid student number",
                        "Student number must be exactly 11 digits (e.g. 20242314567).");
                return false;
            }
            if (college.getValue() == null || college.getValue().isBlank()) {
                info("Invalid", "Please select a college.");
                return false;
            }
            if (course.getValue() == null || course.getValue().isBlank()) {
                info("Invalid", "Please select a course.");
                return false;
            }
            if (year.getValue() == null || year.getValue().isBlank()) {
                info("Invalid", "Please select a year level.");
                return false;
            }
            if (!AccountRules.isUeEmail(email.getText())) {
                info("UE email required", AccountRules.ueEmailMessage());
                return false;
            }
            if (!AccountRules.isValidContactOptional(contact.getText())) {
                info("Invalid contact", AccountRules.contactMessage());
                return false;
            }
            user.setFullName(fullName.getText().trim());
            user.setStudentNumber(sn);
            user.setCourse(course.getValue().trim());
            user.setYearLevel(year.getValue().trim());
            user.setContactNumber(contact.getText().trim());
            user.setEmail(AccountRules.normalizeEmail(email.getText()));
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
                    "Student changed their password");
            info("Password updated", "Your password was changed successfully.");
            return true;
        });
    }

    private void changeAvatar() {
        String stored = PhotoStorage.chooseAndStore(stage, "avatar-" + user.getUserId());
        if (stored == null) return;
        String previousPath = user.getAvatarPath();
        byte[] previousData = user.getAvatarData();
        user.setAvatar(stored);
        if (!user.updateAvatarPath()) {
            user.setAvatarPath(previousPath);
            user.setAvatarData(previousData);
            info("Failed", "Could not save avatar.");
            return;
        }
        if (previousPath != null && !previousPath.isBlank() && !previousPath.equals(stored)) {
            PhotoStorage.delete(previousPath);
        }
        Toast.show(stage, "Profile photo updated.");
        refreshAvatars();
    }

    private void removeAvatar() {
        String previousPath = user.getAvatarPath();
        byte[] previousData = user.getAvatarData();
        if ((previousPath == null || previousPath.isBlank())
                && (previousData == null || previousData.length == 0)) {
            return;
        }
        if (!BrandedDialog.confirm(stage, "Remove photo",
                "Remove your profile photo? Your initials will show instead.")) {
            return;
        }
        user.setAvatarPath(null);
        user.setAvatarData(null);
        if (!user.updateAvatarPath()) {
            user.setAvatarPath(previousPath);
            user.setAvatarData(previousData);
            info("Failed", "Could not remove avatar.");
            return;
        }
        if (previousPath != null && !previousPath.isBlank()) {
            PhotoStorage.delete(previousPath);
        }
        Toast.show(stage, "Profile photo removed.");
        refreshAvatars();
    }

    private void refreshAvatars() {
        sidebarAvatarHost.getChildren().setAll(buildUserAvatar(36));
        if (currentNav == Nav.PROFILE) {
            navigate(Nav.PROFILE);
        }
    }

    private Node buildUserAvatar(double size) {
        Image photo = PhotoStorage.loadImage(user.getAvatarPath(), user.getAvatarData(), size, size);
        if (photo != null) {
            ImageView view = new ImageView(photo);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(false);
            Circle clip = new Circle(size / 2.0, size / 2.0, size / 2.0);
            view.setClip(clip);
            StackPane stack = new StackPane(view);
            stack.setPrefSize(size, size);
            stack.setMaxSize(size, size);
            return stack;
        }
        return circleAvatar(initials(user.getFullName()), size);
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

    /**
     * Workflow step 5 — submit ownership claim after a match notification.
     * Only MATCHED LOST reports. No inventory list (admin verifies privately).
     */
    private void showSubmitClaimDialog(Report preselected) {
        if (preselected != null) {
            String blocked = Claim.claimBlockReason(user.getUserId(), preselected.getReportId());
            if (blocked != null) {
                info("Claim not allowed", blocked);
                return;
            }
        }
        Map<String, Report> matchedLost = loadMatchedLostReportsByLabel();
        if (preselected != null && canClaimMatchedLostReport(preselected)) {
            matchedLost.putIfAbsent(matchedReportLabel(preselected), preselected);
        }
        if (matchedLost.isEmpty()) {
            info("No matched reports",
                    "You are not allowed to claim your own lost item report until staff matches it.\n\n"
                    + "Watch My Reports / Notifications for a match, then submit an ownership claim "
                    + "with proof of ownership.");
            return;
        }

        DialogPaneBuilder dialog = new DialogPaneBuilder("Submit Ownership Claim");
        dialog.setPrefWidth(520);
        ComboBox<String> reportCombo = dialog.combo(
                "Matched lost report",
                matchedLost.keySet().toArray(String[]::new)
        );
        if (preselected != null) {
            reportCombo.getSelectionModel().select(matchedReportLabel(preselected));
        }

        TextField itemName = dialog.field("Item name");
        TextField category = dialog.field("Category");
        TextField subCategory = dialog.field("Subcategory");
        TextField color = dialog.field("Color");
        TextField brand = dialog.field("Brand");
        itemName.setEditable(false);
        category.setEditable(false);
        subCategory.setEditable(false);
        color.setEditable(false);
        brand.setEditable(false);

        // Full-width stacked questions so the text is not clipped in the label column.
        TextArea q1 = dialog.stackedArea(
                "Q1. What unique marks, stickers, scratches, or features does the item have?");
        TextArea q2 = dialog.stackedArea(
                "Q2. What was inside or with the item (cards, case, cables, notes, etc.)?");
        TextArea q3 = dialog.stackedArea(
                "Q3. Any other proof only the owner would know?");

        Runnable applyReport = () -> {
            Report selected = matchedLost.get(reportCombo.getValue());
            if (selected == null) return;
            itemName.setText(nullSafe(selected.getItemName()));
            category.setText(nullSafe(selected.getCategory()));
            subCategory.setText(nullSafe(selected.getSubCategory()));
            color.setText(nullSafe(selected.getColor()));
            brand.setText(nullSafe(selected.getBrand()));
        };
        reportCombo.valueProperty().addListener((obs, o, n) -> applyReport.run());
        applyReport.run();

        dialog.show(() -> {
            Report source = matchedLost.get(reportCombo.getValue());
            String a1 = textOrEmpty(q1);
            String a2 = textOrEmpty(q2);
            String a3 = textOrEmpty(q3);
            if (source == null || (a1.isBlank() && a2.isBlank() && a3.isBlank())) {
                info("Incomplete", "Select your matched report and answer the ownership questions.");
                return false;
            }
            String blocked = Claim.claimBlockReason(user.getUserId(), source.getReportId());
            if (blocked != null) {
                info("Claim not allowed", blocked);
                return false;
            }

            String ownershipPacket = "Item: " + source.getItemName()
                    + "\nCategory: " + nullSafe(source.getCategory())
                    + "\nSubcategory: " + nullSafe(source.getSubCategory())
                    + "\nColor: " + nullSafe(source.getColor())
                    + "\nBrand: " + nullSafe(source.getBrand())
                    + "\nQ1 Unique marks: " + a1
                    + "\nQ2 Contents/with item: " + a2
                    + "\nQ3 Other proof: " + a3;

            Claim claim = new Claim();
            claim.setUserId(user.getUserId());
            claim.setReportId(source.getReportId());
            // Prefer sticky matched inventory; submitClaim auto-holds if none is usable.
            claim.setItemId(source.getMatchedItemId());
            claim.setOwnershipAnswer(ownershipPacket);
            claim.setClaimDate(LocalDate.now());
            if (!claim.submitClaim()) {
                info("Failed",
                        "Could not submit claim. You may already have an open claim for this report.");
                return false;
            }

            boolean held = claim.getClaimStatus() != null
                    && "ON_HOLD".equalsIgnoreCase(claim.getClaimStatus());
            Notification note = new Notification();
            note.setUserId(user.getUserId());
            note.setClaimId(claim.getClaimId());
            note.setReportId(source.getReportId());
            note.setMessage(held
                    ? ("Your ownership claim for \"" + source.getItemName()
                    + "\" was submitted and placed on hold — no matching registered item is on file yet. Staff will continue review.")
                    : ("Your ownership claim for \"" + source.getItemName()
                    + "\" was submitted and is pending admin verification."));
            note.notifyClaimUpdate();

            info("Claim submitted",
                    held
                            ? ("Claim " + claim.getDisplayId() + " is On Hold.\n"
                            + "No matching registered inventory item was on file yet. "
                            + "Staff can Resume, Approve, or Reject it from Manage Claims.")
                            : ("Claim " + claim.getDisplayId() + " is Pending.\n"
                            + "Track it under My Claims. Inventory details stay private until approval."));
            navigate(Nav.CLAIMS);
            return true;
        });
    }

    private static String textOrEmpty(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private Map<String, Report> loadMatchedLostReportsByLabel() {
        Map<String, Report> map = new LinkedHashMap<>();
        for (Report report : reportForUser().viewMyReports()) {
            if (canClaimMatchedLostReport(report) && !hasOpenClaimForReport(report.getReportId())) {
                map.put(matchedReportLabel(report), report);
            }
        }
        return map;
    }

    private static boolean canClaimMatchedLostReport(Report report) {
        if (report == null || report.isFoundReport()) return false;
        String status = report.getReportStatus() == null ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);
        return "MATCHED".equals(status);
    }

    private boolean hasOpenClaimForReport(int reportId) {
        return claimForUser().viewMyClaims().stream()
                .filter(c -> c.getReportId() != null && c.getReportId() == reportId)
                .anyMatch(c -> {
                    String s = c.getClaimStatus() == null ? "" : c.getClaimStatus().toUpperCase(Locale.ENGLISH);
                    return "PENDING".equals(s) || "ON_HOLD".equals(s) || "APPROVED".equals(s);
                });
    }

    private static String matchedReportLabel(Report report) {
        return report.getDisplayId() + " — " + report.getItemName() + " (Matched)";
    }

    /** Student progress statuses for My Reports — aligned to real desk actions. */
    private String reportProgressStatus(Report report) {
        if (report == null) return "Pending";
        String status = report.getReportStatus() == null ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);
        if ("CANCELLED".equals(status)) return "Cancelled";
        if ("ARCHIVED".equals(status)) return "Archived";
        // Found tip logged into inventory (never "Claimed" — that is owner pickup only).
        if (report.isFoundReport() && report.isFoundRegistered()) return "Registered";
        // Lost: Claimed only after admin confirms personal pickup.
        if (isReportClaimed(report)) return "Claimed";
        if (hasApprovedClaimAwaitingPickup(report)) return "Approved — for pickup";
        if ("MATCHED".equals(status)) return "Matched";
        if ("ON_HOLD".equals(status)) return "On Hold — waiting for item";
        if ("UNDER_REVIEW".equals(status)) return "Under review";
        if ("PENDING".equals(status)) return "Pending";
        return report.displayStatusLabel();
    }

    private boolean isReportClaimed(Report report) {
        if (report == null || report.isFoundReport()) return false;
        String status = report.getReportStatus() == null ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);
        return "CLAIMED".equals(status);
    }

    /** Approved claim exists, but admin has not confirmed personal pickup yet. */
    private boolean hasApprovedClaimAwaitingPickup(Report report) {
        if (report == null) return false;
        return claimForUser().viewMyClaims().stream().anyMatch(c ->
                c.getReportId() != null
                        && c.getReportId() == report.getReportId()
                        && c.getClaimStatus() != null
                        && "APPROVED".equalsIgnoreCase(c.getClaimStatus())
                        && c.getReturnDate() == null);
    }

    private void showReportDetails(Report report) {
        report.viewReport();
        List<Claim> related = claimForUser().viewMyClaims().stream()
                .filter(c -> c.getReportId() != null && c.getReportId() == report.getReportId())
                .toList();
        String guidance;
        if (report.isFoundReport()) {
            guidance = report.isFoundRegistered()
                    ? "Your found-item tip was registered into Lost & Found inventory. Thank you — you cannot claim this report."
                    : "Found report: staff may register it into inventory. You cannot claim this report.";
        } else if ("MATCHED".equalsIgnoreCase(report.getReportStatus())) {
            guidance = "A match was found. Submit a claim with ownership answers (My Claims / Claim).";
        } else if ("ON_HOLD".equalsIgnoreCase(report.getReportStatus())) {
            guidance = "On Hold: no matching inventory yet. You’ll be notified when a possible match is registered.";
        } else {
            guidance = "Monitor this report. You’ll be notified when a match is found.";
        }
        String details =
                "Type: " + (report.isFoundReport() ? "Found" : "Lost")
                + "\nCategory: " + report.getCategory()
                + "\nSubcategory: " + nullSafe(report.getSubCategory())
                + "\nColor: " + nullSafe(report.getColor())
                + "\nBrand: " + nullSafe(report.getBrand())
                + "\nLocation: " + nullSafe(report.getLostLocation())
                + "\nStatus: " + reportProgressStatus(report)
                + "\nSubmitted: " + DATE_FMT.format(report.getDateSubmitted())
                + "\n\n" + guidance;
        showCaseDialog(
                "Report " + report.getDisplayId(),
                report.getItemName(),
                report.isFoundReport() ? null : CaseTracker.forReport(report, related),
                details,
                reportPhotoPreview(report),
                () -> StatusTimeline.showForReport(stage, report, related,
                        Notification.forUserReport(user.getUserId(), report.getReportId()))
        );
    }

    private void showClaimDetails(Claim claim) {
        claim.viewClaimDetails();
        Claim live = Claim.findById(claim.getClaimId()).orElse(claim);
        Report linked = live.getReportId() == null ? null
                : Report.findById(live.getReportId()).orElse(null);
        String codeLine = "";
        if ("APPROVED".equalsIgnoreCase(live.getClaimStatus())
                && live.getReturnDate() == null
                && live.getPickupCode() != null && !live.getPickupCode().isBlank()) {
            codeLine = "\n\nPickup code: " + live.getPickupCode()
                    + "\nBring this code + your UE ID to the Lost & Found desk.";
        }
        String adminLine;
        if ("REJECTED".equalsIgnoreCase(live.getClaimStatus())) {
            String reason = live.getAdminNote();
            adminLine = "\nWhy rejected: "
                    + (reason == null || reason.isBlank() ? "(no reason provided)" : reason.trim());
        } else if (live.getAdminNote() != null && !live.getAdminNote().isBlank()) {
            adminLine = "\nAdmin note: " + live.getAdminNote().trim();
        } else {
            adminLine = "";
        }
        String details =
                "Status: " + prettyStatus(live.getClaimStatus())
                + "\nSubmitted: " + DATE_FMT.format(live.getClaimDate())
                + adminLine
                + codeLine
                + "\n\nOwnership answer:\n" + nullSafe(live.getOwnershipAnswer());
        showCaseDialog(
                "Claim " + live.getDisplayId(),
                live.getItemName(),
                CaseTracker.forClaim(live, linked),
                details,
                null,
                () -> StatusTimeline.showForClaim(stage, live, linked,
                        Notification.forUserClaim(user.getUserId(), live.getClaimId()))
        );
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

    /** Details dialog with optional case tracker + Timeline action. */
    private void showCaseDialog(String title, String subtitle, Node tracker, String details,
                                Node photoPreview, Runnable onTimeline) {
        javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.initStyle(javafx.stage.StageStyle.UTILITY);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.initOwner(stage);

        Label titleLbl = new Label(title);
        titleLbl.setWrapText(true);
        titleLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subLbl = new Label("UE Caloocan Lost & Found");
        subLbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.88);");
        VBox heroText = new VBox(3, titleLbl, subLbl);
        HBox.setHgrow(heroText, Priority.ALWAYS);
        HBox hero = new HBox(14, UeImages.dialogBrandMark(), heroText);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(18, 20, 18, 20));
        hero.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F0183C, #C70E2E 55%, #A50C26);"
                + "-fx-background-radius: 14 14 0 0;"
        );

        Label head = new Label(subtitle);
        head.setWrapText(true);
        head.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + ";");
        Label body = new Label(details);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 13px; -fx-text-fill: " + UiTheme.TEXT_MUTED + ";");

        VBox content = new VBox(12, head);
        if (photoPreview != null) {
            Label photoLbl = new Label("Photo");
            photoLbl.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + UiTheme.UE_RED + ";"
            );
            content.getChildren().addAll(photoLbl, photoPreview);
        }
        if (tracker != null) content.getChildren().add(tracker);
        content.getChildren().add(body);
        content.setPadding(new Insets(16, 18, 10, 18));
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #FFF8F9, #FFFFFF);");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox shell = new VBox(hero, scroll);

        var pane = dialog.getDialogPane();
        pane.setGraphic(null);
        pane.setContent(shell);
        pane.setPrefWidth(520);
        pane.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-radius: 14;"
                + "-fx-background-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(165,12,38,0.22), 28, 0.18, 0, 10);"
        );
        ButtonType timelineBtn = new ButtonType("Timeline");
        pane.getButtonTypes().setAll(timelineBtn, ButtonType.CLOSE);
        dialog.setOnShown(e -> {
            Button close = (Button) pane.lookupButton(ButtonType.CLOSE);
            if (close != null) {
                close.setText("Close");
                UiButtons.style(close, UiButtons.Kind.GHOST);
            }
            Button timeline = (Button) pane.lookupButton(timelineBtn);
            if (timeline != null) UiButtons.style(timeline, UiButtons.Kind.PRIMARY);
        });
        dialog.showAndWait().ifPresent(result -> {
            if (result == timelineBtn && onTimeline != null) onTimeline.run();
        });
    }

    private void cancelReport(Report report) {
        if (!report.canBeCancelledByOwner()) {
            info("Cannot cancel",
                    "Only Pending / Under Review reports can be cancelled, and only when no claim is active.\n"
                    + "Cancelled reports stay on file — they are never deleted.");
            return;
        }
        if (!BrandedDialog.confirm(stage, "Cancel report",
                "Cancel " + report.getDisplayId() + " — " + report.getItemName() + "?\n\n"
                + "This marks it Cancelled for your records.\n"
                + "Nothing is wiped from the database (soft-cancel for audit).")) {
            return;
        }
        Report target = new Report();
        target.setReportId(report.getReportId());
        target.setUserId(user.getUserId());
        if (!target.cancelByOwner(user.getUserId())) {
            info("Failed", "Could not cancel this report. It may already be matched or claimed.");
            return;
        }
        AuditLog.record("REPORT_CANCELLED", "REPORT", String.valueOf(report.getReportId()),
                "Student cancelled " + report.getDisplayId() + " (soft-delete — row kept)");
        Toast.success(stage, "Report " + report.getDisplayId() + " cancelled — kept on file.");
        reportsRecordFilter = "CANCELLED";
        navigate(Nav.REPORTS);
    }

    private void archiveReport(Report report) {
        if (!report.canBeArchived()) {
            info("Cannot archive",
                    "Matched reports or reports with an active claim cannot be archived.\n"
                    + "Archive keeps the row for audit — it does not delete data.");
            return;
        }
        if (!BrandedDialog.confirm(stage, "Archive report",
                "Archive " + report.getDisplayId() + " — " + report.getItemName() + "?\n\n"
                + "It leaves Active and moves to the Archived filter.\n"
                + "Nothing is wiped from the database (soft-archive for audit).")) {
            return;
        }
        Report target = new Report();
        target.setReportId(report.getReportId());
        if (!target.archiveByOwner(user.getUserId())) {
            info("Failed", "Could not archive this report. It may already be matched or claimed.");
            return;
        }
        AuditLog.record("REPORT_ARCHIVED", "REPORT", String.valueOf(report.getReportId()),
                "Student archived " + report.getDisplayId() + " (soft-delete — row kept)");
        Toast.success(stage, "Report " + report.getDisplayId() + " archived — kept on file.");
        reportsRecordFilter = "ARCHIVED";
        navigate(Nav.REPORTS);
    }

    private void restoreArchivedReport(Report report) {
        if (!report.canBeRestoredFromArchive()) {
            info("Cannot restore", "Only Archived reports can be restored to Pending.");
            return;
        }
        if (report.getUserId() != user.getUserId()) {
            info("Not allowed", "You can only restore your own reports.");
            return;
        }
        if (!BrandedDialog.confirm(stage, "Restore report",
                "Restore " + report.getDisplayId() + " to Pending in your Active reports?")) {
            return;
        }
        Report target = new Report();
        target.setReportId(report.getReportId());
        if (!target.restoreFromArchive()) {
            info("Failed", "Could not restore this report.");
            return;
        }
        AuditLog.record("REPORT_RESTORED", "REPORT", String.valueOf(report.getReportId()),
                "Student restored " + report.getDisplayId() + " from Archive → Pending");
        Toast.success(stage, "Report " + report.getDisplayId() + " restored to Pending.");
        reportsRecordFilter = "ACTIVE";
        navigate(Nav.REPORTS);
    }

    private void archiveClaim(Claim claim) {
        if (!claim.canBeArchived()) {
            info("Cannot archive",
                    "Approved claims waiting for pickup cannot be archived yet.\n"
                    + "You can archive Pending, On Hold, Rejected, or completed claims.\n"
                    + "Archive keeps the row for audit — it does not delete data.");
            return;
        }
        if (!BrandedDialog.confirm(stage, "Archive claim",
                "Archive " + claim.getDisplayId() + " — " + claim.getItemName() + "?\n\n"
                + "It leaves All Claims and moves to the Archived filter.\n"
                + "Nothing is wiped from the database (soft-archive for audit).")) {
            return;
        }
        Claim target = new Claim();
        target.setClaimId(claim.getClaimId());
        if (!target.archiveByOwner(user.getUserId())) {
            info("Failed", "Could not archive this claim.");
            return;
        }
        AuditLog.record("CLAIM_ARCHIVED", "CLAIM", String.valueOf(claim.getClaimId()),
                "Student archived " + claim.getDisplayId() + " (soft-delete — row kept)");
        Toast.success(stage, "Claim " + claim.getDisplayId() + " archived — kept on file.");
        claimsFilter = "ARCHIVED";
        navigate(Nav.CLAIMS);
    }

    private void restoreArchivedClaim(Claim claim) {
        if (!claim.canBeRestoredFromArchive()) {
            info("Cannot restore", "Only Archived claims can be restored to Pending.");
            return;
        }
        if (claim.getUserId() != user.getUserId()) {
            info("Not allowed", "You can only restore your own claims.");
            return;
        }
        if (!BrandedDialog.confirm(stage, "Restore claim",
                "Restore " + claim.getDisplayId() + " to Pending in My Claims?")) {
            return;
        }
        Claim target = new Claim();
        target.setClaimId(claim.getClaimId());
        if (!target.restoreFromArchive(user.getUserId())) {
            info("Failed", "Could not restore this claim.");
            return;
        }
        AuditLog.record("CLAIM_RESTORED", "CLAIM", String.valueOf(claim.getClaimId()),
                "Student restored " + claim.getDisplayId() + " from Archive → Pending");
        Toast.success(stage, "Claim " + claim.getDisplayId() + " restored to Pending.");
        claimsFilter = "PENDING";
        navigate(Nav.CLAIMS);
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

    /** Centered wrapped label for the profile avatar card. */
    private static Label centeredLabel(String text, String color, double size, boolean bold) {
        Label label = new Label(text == null ? "" : text);
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        label.setMaxWidth(200);
        label.setPrefWidth(200);
        label.setStyle(
                "-fx-text-fill: " + color + ";"
                + "-fx-font-size: " + size + "px;"
                + "-fx-font-weight: " + (bold ? "bold" : "normal") + ";"
                + "-fx-alignment: center;"
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

    private static String notifColorHex(String type) {
        if (type == null) return UiTheme.UE_RED;
        return switch (type.toUpperCase(Locale.ENGLISH)) {
            case "CLAIM_DECISION", "RETURN" -> "#22C55E";
            case "POSSIBLE_MATCH" -> "#EAB308";
            case "ALERT", "ITEM_NOT_REGISTERED" -> "#3B82F6";
            default -> UiTheme.UE_RED;
        };
    }

    /** Fixed 40×40 circle disc + centered glyph (never grows into a capsule). */
    private static StackPane notifCircleIcon(String type) {
        double size = 40;
        Circle disc = new Circle(size / 2.0, Color.web(notifColorHex(type)));
        Node glyph = notifGlyphNode(type);
        StackPane icon = new StackPane(disc, glyph);
        icon.setMinSize(size, size);
        icon.setPrefSize(size, size);
        icon.setMaxSize(size, size);
        icon.setAlignment(Pos.CENTER);
        icon.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        return icon;
    }

    private static Node notifGlyphNode(String type) {
        String iconType;
        if (type == null) {
            iconType = "DOCUMENT";
        } else {
            iconType = switch (type.toUpperCase(Locale.ENGLISH)) {
                case "CLAIM_DECISION", "RETURN" -> "CHECK";
                case "POSSIBLE_MATCH" -> "SEARCH";
                case "ALERT", "ITEM_NOT_REGISTERED" -> "BELL";
                default -> "DOCUMENT";
            };
        }

        SVGPath path = new SVGPath();
        path.setFill(Color.WHITE);
        // Material 24×24 paths → scale down so they sit inside the 40px circle.
        path.setScaleX(0.58);
        path.setScaleY(0.58);
        switch (iconType) {
            case "DOCUMENT" -> path.setContent(
                    "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
            case "SEARCH" -> path.setContent(
                    "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
            case "BELL" -> path.setContent(
                    "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z");
            case "CHECK" -> path.setContent(
                    "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
            default -> path.setContent(
                    "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
        }
        return path;
    }
    private void info(String title, String message) {
        Toast.notify(stage, title, message);
    }


    /** Thin wrapper — branded look only; confirm callbacks stay the same. */
    private final class DialogPaneBuilder {
        private final BrandedDialog dialog;

        DialogPaneBuilder(String title) {
            dialog = new BrandedDialog(stage, title).help(
                    "Fill required fields, then OK to save. Cancel closes without changes."
            );
        }

        DialogPaneBuilder help(String text) {
            dialog.help(text);
            return this;
        }

        TextField field(String label) { return dialog.field(label); }
        TextField field(String label, String value) { return dialog.field(label, value); }
        PasswordField password(String label) { return dialog.password(label); }
        TextArea area(String label) { return dialog.area(label); }
        TextArea stackedArea(String question) { return dialog.stackedArea(question); }
        ComboBox<String> combo(String label, String... values) { return dialog.combo(label, values); }

        BrandedDialog.PhotoPick photoPicker(String label) {
            return dialog.photoPicker(label, "report");
        }

        void setPrefWidth(double width) { dialog.setPrefWidth(width); }

        void show(java.util.function.Supplier<Boolean> onConfirm) {
            dialog.show(onConfirm);
        }
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
            case "DOCUMENT" -> path.setContent("M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
            case "SEARCH" -> path.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
            case "BELL" -> path.setContent("M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z");
            case "CHECK" -> path.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
        }
        
        StackPane bubble = new StackPane(bg, path);
        bubble.setMinSize(52, 52);
        bubble.setMaxSize(52, 52);
        return bubble;
    }
}