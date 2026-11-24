package finalproj.mavenproject1;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Builds a readable status timeline for reports and claims. */
final class StatusTimeline {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);

    record Event(LocalDateTime when, String title, String detail) {}

    private StatusTimeline() {}

    static void showForReport(Window owner, Report report, List<Claim> relatedClaims,
                              List<Notification> relatedNotes) {
        List<Event> events = new ArrayList<>();
        if (report.getDateSubmitted() != null) {
            events.add(new Event(
                    report.getDateSubmitted().atStartOfDay(),
                    "Report submitted",
                    (report.isFoundReport() ? "Found" : "Lost") + " item: " + report.getItemName()
            ));
        }
        String status = report.getReportStatus() == null ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);
        if (!status.isBlank() && !"PENDING".equals(status)) {
            events.add(new Event(
                    LocalDate.now().atStartOfDay(),
                    "Current status: " + pretty(status),
                    "Latest report state in the system"
            ));
        }
        if (relatedNotes != null) {
            for (Notification n : relatedNotes) {
                if (n.getSentDate() == null) continue;
                events.add(new Event(n.getSentDate(), prettyType(n.getNotificationType()), n.getMessage()));
            }
        }
        if (relatedClaims != null) {
            for (Claim c : relatedClaims) {
                if (c.getClaimDate() != null) {
                    events.add(new Event(
                            c.getClaimDate().atStartOfDay(),
                            "Claim " + c.getDisplayId() + " filed",
                            "Status: " + pretty(c.getClaimStatus())
                    ));
                }
                if (c.getReturnDate() != null) {
                    events.add(new Event(
                            c.getReturnDate().atStartOfDay(),
                            "Marked Claimed",
                            nullSafe(c.getReturnNote()).isBlank()
                                    ? "Item released at the desk — report status Claimed"
                                    : c.getReturnNote().trim()
                    ));
                }
            }
        }
        show(owner, "Timeline — " + report.getDisplayId(), events);
    }

    static void showForClaim(Window owner, Claim claim, Report report, List<Notification> relatedNotes) {
        List<Event> events = new ArrayList<>();
        if (report != null && report.getDateSubmitted() != null) {
            events.add(new Event(
                    report.getDateSubmitted().atStartOfDay(),
                    "Linked report " + report.getDisplayId(),
                    report.getItemName()
            ));
        }
        if (claim.getClaimDate() != null) {
            events.add(new Event(
                    claim.getClaimDate().atStartOfDay(),
                    "Claim submitted",
                    "Ownership answers recorded"
            ));
        }
        String status = claim.getClaimStatus() == null ? "" : claim.getClaimStatus().toUpperCase(Locale.ENGLISH);
        if ("APPROVED".equals(status) || "REJECTED".equals(status) || "ON_HOLD".equals(status)) {
            events.add(new Event(
                    LocalDate.now().atStartOfDay(),
                    "Decision: " + pretty(status),
                    "Admin note: " + nullSafe(claim.getAdminNote())
                            + (claim.getAdminResult() == null || claim.getAdminResult().isBlank()
                            ? "" : "\nResult: " + claim.getAdminResult())
            ));
        }
        if (claim.getReturnDate() != null) {
            events.add(new Event(
                    claim.getReturnDate().atStartOfDay(),
                    "Marked Claimed",
                    "Received by: " + nullSafe(claim.getReceivedBy())
            ));
        }
        if (relatedNotes != null) {
            for (Notification n : relatedNotes) {
                if (n.getSentDate() == null) continue;
                events.add(new Event(n.getSentDate(), prettyType(n.getNotificationType()), n.getMessage()));
            }
        }
        show(owner, "Timeline — " + claim.getDisplayId(), events);
    }

    private static void show(Window owner, String title, List<Event> events) {
        events.sort(Comparator.comparing(Event::when, Comparator.nullsLast(Comparator.naturalOrder())));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(javafx.stage.StageStyle.UTILITY);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (owner != null) dialog.initOwner(owner);

        VBox list = new VBox(12);
        list.setPadding(new Insets(4));
        if (events.isEmpty()) {
            Label empty = new Label("No timeline events yet.");
            empty.setStyle("-fx-text-fill: " + UiTheme.TEXT_MUTED + "; -fx-font-size: 13px;");
            list.getChildren().add(empty);
        } else {
            for (Event event : events) {
                list.getChildren().add(row(event));
            }
        }
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Label eye = new Label("ACTIVITY");
        eye.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.82);");
        Label heading = new Label(title);
        heading.setWrapText(true);
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        VBox heroText = new VBox(4, eye, heading);
        HBox.setHgrow(heroText, Priority.ALWAYS);
        HBox heroRow = new HBox(14, UeImages.dialogBrandMark(), heroText);
        heroRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        heroRow.setPadding(new Insets(18, 20, 18, 20));
        heroRow.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F0183C, #C70E2E 55%, #A50C26);"
                + "-fx-background-radius: 14 14 0 0;"
        );

        VBox formShell = new VBox(scroll);
        formShell.setPadding(new Insets(16, 16, 8, 16));
        formShell.setStyle("-fx-background-color: linear-gradient(to bottom, #FFF8F9, #FFFFFF);");

        var pane = dialog.getDialogPane();
        pane.setGraphic(null);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setPrefWidth(480);
        pane.setPrefHeight(460);
        pane.setContent(new VBox(heroRow, formShell));
        pane.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-border-color: " + UiTheme.BORDER_STRONG + ";"
                + "-fx-border-radius: 14;"
                + "-fx-background-radius: 14;"
                + "-fx-effect: dropshadow(gaussian, rgba(165,12,38,0.22), 28, 0.18, 0, 10);"
        );
        dialog.setOnShown(e -> {
            pane.setGraphic(null);
            javafx.scene.control.Button close = (javafx.scene.control.Button) pane.lookupButton(ButtonType.CLOSE);
            if (close != null) {
                close.setText("Close");
                UiButtons.style(close, UiButtons.Kind.GHOST);
            }
            var bar = pane.lookup(".button-bar");
            if (bar != null) {
                bar.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #FFF5F6, #FCE8EC);"
                        + "-fx-padding: 12 16 14 16;"
                        + "-fx-background-radius: 0 0 14 14;"
                );
            }
        });
        dialog.showAndWait();
    }

    private static HBox row(Event event) {
        Region dot = new Region();
        dot.setPrefSize(10, 10);
        dot.setMinSize(10, 10);
        dot.setStyle("-fx-background-color: " + UiTheme.UE_RED + "; -fx-background-radius: 5;");
        VBox text = new VBox(2);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label title = new Label(event.title());
        title.setWrapText(true);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + UiTheme.TEXT + ";");
        Label when = new Label(event.when() == null ? "" : DT.format(event.when()));
        when.setStyle("-fx-text-fill: #71717A; -fx-font-size: 11px;");
        text.getChildren().addAll(title, when);
        if (event.detail() != null && !event.detail().isBlank()) {
            Label detail = new Label(event.detail());
            detail.setWrapText(true);
            detail.setStyle("-fx-text-fill: #3F3F46; -fx-font-size: 12px;");
            text.getChildren().add(detail);
        }
        HBox row = new HBox(12, dot, text);
        row.setPadding(new Insets(6, 4, 6, 4));
        return row;
    }

    private static String pretty(String status) {
        if (status == null || status.isBlank()) return "—";
        String[] parts = status.toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static String prettyType(String type) {
        return pretty(type == null ? "UPDATE" : type);
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
