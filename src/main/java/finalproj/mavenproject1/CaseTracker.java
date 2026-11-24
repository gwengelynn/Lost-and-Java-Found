package finalproj.mavenproject1;

import java.util.List;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Visual case progress for students: Filed → … → Claimed.
 * Label "Claimed" matches Manage Reports status after desk release.
 * Pure UI helper — does not change report/claim workflow logic.
 */
final class CaseTracker {

    private CaseTracker() {}

    enum Step {
        FILED("Filed"),
        REVIEW("Under review"),
        MATCHED("Matched"),
        CLAIMED_IN("Claim filed"),
        APPROVED("Approved"),
        /** Desk confirmed personal release — same outcome as report status CLAIMED. */
        PICKUP("Claimed");

        final String label;
        Step(String label) { this.label = label; }
    }

    static Node forReport(Report report, List<Claim> relatedClaims) {
        int active = resolveStepIndex(report, relatedClaims);
        String headline = headlineFor(active, report, relatedClaims);
        return build(headline, active);
    }

    static Node forClaim(Claim claim, Report linkedReport) {
        List<Claim> one = claim == null ? List.of() : List.of(claim);
        int active = resolveStepIndex(linkedReport, one);
        // Claims without a report still advance from claim filed onward.
        if (linkedReport == null && claim != null) {
            String s = claim.getClaimStatus() == null ? "" : claim.getClaimStatus().toUpperCase(Locale.ENGLISH);
            if (claim.getReturnDate() != null) active = Step.PICKUP.ordinal();
            else if ("APPROVED".equals(s)) active = Step.APPROVED.ordinal();
            else if ("REJECTED".equals(s)) active = Step.CLAIMED_IN.ordinal();
            else active = Step.CLAIMED_IN.ordinal();
        }
        String headline = "Case progress · " + (claim == null ? "—" : claim.getDisplayId());
        if (claim != null && claim.getReturnDate() != null) {
            headline = (claim.getDisplayId() == null ? "Case" : claim.getDisplayId()) + " · Claimed";
        } else if (claim != null && "APPROVED".equalsIgnoreCase(claim.getClaimStatus())
                && claim.getPickupCode() != null && !claim.getPickupCode().isBlank()) {
            headline = "Ready for pickup · code " + claim.getPickupCode();
        }
        return build(headline, active);
    }

    private static int resolveStepIndex(Report report, List<Claim> claims) {
        if (report == null && (claims == null || claims.isEmpty())) return 0;

        String status = report == null || report.getReportStatus() == null
                ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);

        Claim best = pickBestClaim(claims);
        String claimStatus = best == null || best.getClaimStatus() == null
                ? "" : best.getClaimStatus().toUpperCase(Locale.ENGLISH);
        // Pickup = Lost report CLAIMED after desk release — not Found REGISTERED/RESOLVED.
        boolean pickedUp = "CLAIMED".equals(status)
                || (best != null && best.getReturnDate() != null);
        boolean approved = best != null && "APPROVED".equals(claimStatus);
        boolean claimFiled = best != null;
        boolean matched = "MATCHED".equals(status) || claimFiled || pickedUp;
        boolean review = matched || "UNDER_REVIEW".equals(status);

        if (pickedUp) return Step.PICKUP.ordinal();
        if (approved) return Step.APPROVED.ordinal();
        if (claimFiled) return Step.CLAIMED_IN.ordinal();
        if (matched) return Step.MATCHED.ordinal();
        if (review) return Step.REVIEW.ordinal();
        return Step.FILED.ordinal();
    }

    private static Claim pickBestClaim(List<Claim> claims) {
        if (claims == null || claims.isEmpty()) return null;
        Claim best = null;
        int bestRank = -1;
        for (Claim c : claims) {
            int rank = 0;
            String s = c.getClaimStatus() == null ? "" : c.getClaimStatus().toUpperCase(Locale.ENGLISH);
            if (c.getReturnDate() != null) rank = 5;
            else if ("APPROVED".equals(s)) rank = 4;
            else if ("ON_HOLD".equals(s) || "PENDING".equals(s)) rank = 3;
            else if ("REJECTED".equals(s)) rank = 2;
            else rank = 1;
            if (rank > bestRank) {
                bestRank = rank;
                best = c;
            }
        }
        return best;
    }

    private static String headlineFor(int active, Report report, List<Claim> claims) {
        String id = report == null ? "Case" : report.getDisplayId();
        Step step = Step.values()[Math.min(active, Step.values().length - 1)];
        Claim best = pickBestClaim(claims);
        if (best != null && "APPROVED".equalsIgnoreCase(best.getClaimStatus())
                && best.getReturnDate() == null
                && best.getPickupCode() != null && !best.getPickupCode().isBlank()) {
            return id + " · bring code " + best.getPickupCode() + " + UE ID";
        }
        return id + " · " + step.label;
    }

    /** Thin uniform connector — locked height so HBox cannot stretch it to the node size. */
    private static final double LINE_HEIGHT = 3;
    private static final double NODE_RADIUS = 10;
    private static final double NODE_CURRENT_RADIUS = 11;
    private static final String DONE_GREEN = "#22C55E";

    private static Region trackLine(boolean filled) {
        Region line = new Region();
        HBox.setHgrow(line, Priority.ALWAYS);
        line.setMinHeight(LINE_HEIGHT);
        line.setPrefHeight(LINE_HEIGHT);
        line.setMaxHeight(LINE_HEIGHT);
        line.setStyle("-fx-background-color: "
                + (filled ? UiTheme.UE_RED : "rgba(228,17,53,0.18)") + ";"
                + "-fx-background-radius: 2;");
        return line;
    }

    private static Region trackSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMinHeight(LINE_HEIGHT);
        spacer.setPrefHeight(LINE_HEIGHT);
        spacer.setMaxHeight(LINE_HEIGHT);
        return spacer;
    }

    private static Node build(String headline, int activeIndex) {
        Label title = new Label(headline);
        title.setWrapText(true);
        title.setStyle(
                "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + UiTheme.TEXT + ";"
        );

        HBox track = new HBox(0);
        track.setAlignment(Pos.CENTER_LEFT);
        Step[] steps = Step.values();
        // Claimed (desk release) is terminal — every step including the last is a green check.
        boolean caseFinished = activeIndex >= steps.length - 1;
        for (int i = 0; i < steps.length; i++) {
            boolean done = i <= activeIndex;
            boolean current = i == activeIndex && !caseFinished;
            boolean complete = done && !current;
            VBox col = new VBox(6);
            col.setAlignment(Pos.TOP_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setMaxWidth(Double.MAX_VALUE);

            HBox rail = new HBox();
            rail.setAlignment(Pos.CENTER);
            rail.setMinHeight(NODE_CURRENT_RADIUS * 2);
            rail.setPrefHeight(NODE_CURRENT_RADIUS * 2);
            if (i > 0) {
                rail.getChildren().add(trackLine(i <= activeIndex));
            } else {
                rail.getChildren().add(trackSpacer());
            }

            String fillHex = complete ? DONE_GREEN : (done ? UiTheme.UE_RED : "#FFFFFF");
            String strokeHex = complete ? DONE_GREEN : (done ? UiTheme.UE_RED : "rgba(228,17,53,0.35)");
            Circle ring = new Circle(current ? NODE_CURRENT_RADIUS : NODE_RADIUS);
            ring.setFill(Color.web(fillHex));
            ring.setStroke(Color.web(strokeHex));
            ring.setStrokeWidth(2);
            StackPane dot = new StackPane(ring);
            dot.setMinSize(NODE_CURRENT_RADIUS * 2, NODE_CURRENT_RADIUS * 2);
            dot.setPrefSize(NODE_CURRENT_RADIUS * 2, NODE_CURRENT_RADIUS * 2);
            dot.setMaxSize(NODE_CURRENT_RADIUS * 2, NODE_CURRENT_RADIUS * 2);
            if (done) {
                Label mark = new Label(complete || caseFinished ? "✓" : String.valueOf(i + 1));
                mark.setStyle(
                        "-fx-text-fill: white;"
                        + "-fx-font-size: " + (current ? "11px" : "12px") + ";"
                        + "-fx-font-weight: bold;"
                );
                dot.getChildren().add(mark);
            }
            rail.getChildren().add(dot);

            if (i < steps.length - 1) {
                // Fill connector through the final step when the case is fully picked up.
                rail.getChildren().add(trackLine(caseFinished ? i < steps.length - 1 : i < activeIndex));
            } else {
                rail.getChildren().add(trackSpacer());
            }

            Label name = new Label(steps[i].label);
            name.setWrapText(true);
            name.setAlignment(Pos.CENTER);
            name.setMaxWidth(72);
            name.setStyle(
                    "-fx-font-size: 10px;"
                    + "-fx-font-weight: " + (current ? "bold" : "normal") + ";"
                    + "-fx-text-fill: " + (done ? UiTheme.TEXT : UiTheme.TEXT_MUTED) + ";"
                    + "-fx-text-alignment: center;"
            );
            col.getChildren().addAll(rail, name);
            track.getChildren().add(col);
        }

        VBox box = new VBox(10, title, track);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle(
                "-fx-background-color: " + UiTheme.UE_RED_SOFT + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 12;"
        );
        return box;
    }

    /** Compact card for dashboard lists. */
    static Node card(Report report, List<Claim> relatedClaims, Runnable open) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle(
                "-fx-background-color: " + UiTheme.FIELD_BG + ";"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + UiTheme.BORDER + ";"
                + "-fx-border-radius: 12;"
                + "-fx-cursor: hand;"
        );
        Label name = new Label(report.getDisplayId() + " — " + report.getItemName());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: " + UiTheme.TEXT + "; -fx-font-size: 13px;");
        card.getChildren().addAll(name, forReport(report, relatedClaims));
        if (open != null) {
            card.setOnMouseClicked(e -> open.run());
        }
        return card;
    }
}