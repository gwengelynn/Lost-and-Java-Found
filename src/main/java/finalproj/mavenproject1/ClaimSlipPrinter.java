package finalproj.mavenproject1;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Builds the pickup identification notice sent to the student (in-app + email log).
 * Printing was removed — the student receives the code digitally instead of a paper slip.
 */
final class ClaimSlipPrinter {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private ClaimSlipPrinter() {}

    /** Slip-style body for notifications (pickup code + claim details). */
    static String pickupNotice(Claim claim) {
        if (claim == null) return "";

        String student = User.findById(claim.getUserId())
                .map(u -> u.getFullName()
                        + (u.getStudentNumber() == null || u.getStudentNumber().isBlank()
                        ? "" : " (" + u.getStudentNumber() + ")"))
                .orElse("Student #" + claim.getUserId());

        Optional<Report> report = claim.getReportId() == null
                ? Optional.empty()
                : Report.findById(claim.getReportId());

        String code = claim.getPickupCode() == null || claim.getPickupCode().isBlank()
                ? "—"
                : claim.getPickupCode().trim().toUpperCase(Locale.ENGLISH);

        return "UE Caloocan — Lost and Java-Found"
                + "\nPickup identification"
                + "\n\nPICKUP CODE: " + code
                + "\nClaim ID: " + claim.getDisplayId()
                + "\nItem: " + claim.getItemName()
                + "\nStudent: " + student
                + "\nStatus: " + nullSafe(claim.getClaimStatus())
                + "\nClaim date: " + (claim.getClaimDate() == null ? "—" : DATE.format(claim.getClaimDate()))
                + "\nReport: " + report.map(Report::getDisplayId).orElse("—")
                + "\n\nBring this pickup code + your UE ID to the Lost & Found desk."
                + " Staff will verify the code before releasing your item."
                + "\n(Issued " + DATE.format(LocalDate.now()) + ")";
    }

    /** Sends the pickup notice to the student (short in-app alert + full slip-style email/log). */
    static void sendPickupNoticeToStudent(Claim claim) {
        if (claim == null) return;
        String code = claim.getPickupCode() == null ? "" : claim.getPickupCode().trim().toUpperCase(Locale.ENGLISH);
        String body = pickupNotice(claim);

        Notification note = new Notification();
        note.setUserId(claim.getUserId());
        note.setClaimId(claim.getClaimId());
        if (claim.getReportId() != null) note.setReportId(claim.getReportId());
        // Keep in-app message compact (fits typical VARCHAR); full slip goes to email/log.
        note.setMessage("Claim approved for " + claim.getItemName()
                + ". Pickup code: " + code
                + ". Bring this code + your UE ID to the Lost & Found desk. "
                + "Full details were also sent to your account email (if on file).");
        note.notifyClaimDecision();

        EmailNotifier.notifyUser(
                claim.getUserId(),
                "Pickup code " + code + " — " + claim.getDisplayId(),
                body
        );
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
