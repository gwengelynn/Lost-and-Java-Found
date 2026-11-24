package finalproj.mavenproject1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * CLAIM — Claim management (MySQL: claims + ownership_checks + item_returns).
 */
public class Claim {

    private int claimId;
    private int userId;
    private Integer reportId;
    private Integer itemId;
    private String claimStatus;
    private String ownershipAnswer;
    private String adminResult;
    private String adminNote;
    private LocalDate claimDate;
    /** Set when admin approves/rejects — start of approved→pickup clock. */
    private LocalDate decisionDate;
    private LocalDate returnDate;
    private String receivedBy;
    private String returnNote;
    /** Short text desk code on the claim slip after approval (e.g. UE-7K2M). Not a QR. */
    private String pickupCode;

    public Claim() {}

    public Claim(int claimId, int userId, Integer reportId, Integer itemId, String claimStatus,
                 String ownershipAnswer, String adminResult, String adminNote,
                 LocalDate claimDate, LocalDate returnDate, String receivedBy, String returnNote) {
        this(claimId, userId, reportId, itemId, claimStatus, ownershipAnswer, adminResult, adminNote,
                claimDate, null, returnDate, receivedBy, returnNote, null);
    }

    public Claim(int claimId, int userId, Integer reportId, Integer itemId, String claimStatus,
                 String ownershipAnswer, String adminResult, String adminNote,
                 LocalDate claimDate, LocalDate returnDate, String receivedBy, String returnNote,
                 String pickupCode) {
        this(claimId, userId, reportId, itemId, claimStatus, ownershipAnswer, adminResult, adminNote,
                claimDate, null, returnDate, receivedBy, returnNote, pickupCode);
    }

    public Claim(int claimId, int userId, Integer reportId, Integer itemId, String claimStatus,
                 String ownershipAnswer, String adminResult, String adminNote,
                 LocalDate claimDate, LocalDate decisionDate, LocalDate returnDate,
                 String receivedBy, String returnNote, String pickupCode) {
        this.claimId = claimId;
        this.userId = userId;
        this.reportId = reportId;
        this.itemId = itemId;
        this.claimStatus = claimStatus;
        this.ownershipAnswer = ownershipAnswer;
        this.adminResult = adminResult;
        this.adminNote = adminNote;
        this.claimDate = claimDate;
        this.decisionDate = decisionDate;
        this.returnDate = returnDate;
        this.receivedBy = receivedBy;
        this.returnNote = returnNote;
        this.pickupCode = pickupCode;
    }

    public int getClaimId() { return claimId; }
    public void setClaimId(int claimId) { this.claimId = claimId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getReportId() { return reportId; }
    public void setReportId(Integer reportId) { this.reportId = reportId; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public String getClaimStatus() { return claimStatus; }
    public void setClaimStatus(String claimStatus) { this.claimStatus = claimStatus; }

    public String getOwnershipAnswer() { return ownershipAnswer; }
    public void setOwnershipAnswer(String ownershipAnswer) { this.ownershipAnswer = ownershipAnswer; }

    public String getAdminResult() { return adminResult; }
    public void setAdminResult(String adminResult) { this.adminResult = adminResult; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public LocalDate getClaimDate() { return claimDate; }
    public void setClaimDate(LocalDate claimDate) { this.claimDate = claimDate; }

    public LocalDate getDecisionDate() { return decisionDate; }
    public void setDecisionDate(LocalDate decisionDate) { this.decisionDate = decisionDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    /**
     * Start date for approved→pickup lag: decision/approval date when known,
     * otherwise claim filed date (legacy rows).
     */
    public LocalDate pickupClockStart() {
        if (decisionDate != null) return decisionDate;
        return claimDate;
    }

    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }

    public String getReturnNote() { return returnNote; }
    public void setReturnNote(String returnNote) { this.returnNote = returnNote; }

    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }

    public String getDisplayId() {
        return String.format("CLM-%03d", claimId);
    }

    /** Generates a short spoken desk code: UE-XXXX (no QR / no ambiguous 0/O/1/I). */
    public static String generatePickupCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("UE-");
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 4; i++) {
            sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    public String getItemName() {
        if (reportId != null && reportId > 0) {
            Optional<String> fromReport = Report.findById(reportId).map(Report::getItemName);
            if (fromReport.isPresent() && fromReport.get() != null && !fromReport.get().isBlank()) {
                return fromReport.get();
            }
        }
        return itemNameFromOwnership(ownershipAnswer);
    }

    private static String itemNameFromOwnership(String ownershipAnswer) {
        if (ownershipAnswer == null || ownershipAnswer.isBlank()) return "Unknown item";
        for (String line : ownershipAnswer.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith("item:")) {
                String name = trimmed.substring(5).trim();
                if (!name.isEmpty()) return name;
            }
        }
        return "Unknown item";
    }

    /**
     * Why this claim must be blocked, or {@code null} if it may proceed.
     * Own lost reports may only receive an ownership claim after staff marks them MATCHED.
     */
    public static String claimBlockReason(int studentUserId, Integer reportId) {
        if (studentUserId <= 0) {
            return "Sign in as a student to submit a claim.";
        }
        if (reportId == null || reportId <= 0) {
            return "Select a matched lost report before submitting a claim.";
        }
        Optional<Report> linked = Report.findById(reportId);
        if (linked.isEmpty()) {
            return "That report was not found.";
        }
        Report report = linked.get();
        if (report.isFoundReport()) {
            return "You are not allowed to claim a found-item report. "
                    + "Items you turn in are registered by staff — only the rightful owner "
                    + "may submit an ownership claim after a match.";
        }
        if (report.getUserId() != studentUserId) {
            return "You can only submit ownership claims for your own matched lost reports.";
        }
        // Own lost report: block unless staff has already matched it to found inventory.
        String status = report.getReportStatus() == null
                ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);
        if (!"MATCHED".equals(status)) {
            return "You are not allowed to claim your own lost item report.\n\n"
                    + "Lost & Found staff must first match it to a found item. "
                    + "After you receive a match notification, you may submit an ownership claim "
                    + "from My Claims or that notification.";
        }
        if (hasOpenClaimForReport(studentUserId, reportId)) {
            return "You already have an open claim for " + report.getDisplayId() + ".";
        }
        return null;
    }

    public boolean submitClaim() {
        if (userId <= 0) return false;
        if (ownershipAnswer == null || ownershipAnswer.isBlank()) return false;
        // Own lost reports: ownership claim only after MATCHED. Never claim found tips / others' reports.
        if (reportId != null && reportId > 0) {
            if (claimBlockReason(this.userId, this.reportId) != null) {
                return false;
            }
            // Prefer sticky matched inventory from the Lost report when claim has no item yet.
            if (this.itemId == null) {
                Report.findById(this.reportId).ifPresent(r -> {
                    if (r.getMatchedItemId() != null) this.itemId = r.getMatchedItemId();
                });
            }
        }
        this.claimDate = LocalDate.now();
        // Auto On Hold when there is no usable registered inventory item for this claim.
        // Admin can still Resume / Approve / Reject from Manage Claims.
        if (!hasUsableRegisteredItem(this.itemId)) {
            this.itemId = null;
            this.claimStatus = "ON_HOLD";
            if (this.adminNote == null || this.adminNote.isBlank()) {
                this.adminNote = "Auto-held: no matching registered inventory item on file yet.";
            }
        } else if (this.claimStatus == null || this.claimStatus.isBlank()
                || "ON_HOLD".equalsIgnoreCase(this.claimStatus)) {
            this.claimStatus = "PENDING";
        }

        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO claims
                    (student_user_id, report_id, item_id, claim_status, ownership_answer, admin_note, claim_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                ps.setInt(1, this.userId);
                Db.setIntOrNull(ps, 2, this.reportId);
                Db.setIntOrNull(ps, 3, this.itemId);
                ps.setString(4, this.claimStatus);
                ps.setString(5, this.ownershipAnswer);
                ps.setString(6, Db.blankToNull(this.adminNote));
                Db.setDate(ps, 7, this.claimDate);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        connection.rollback();
                        return false;
                    }
                    this.claimId = keys.getInt(1);
                }
            }

            String ownershipState = "ON_HOLD".equalsIgnoreCase(this.claimStatus) ? "ON_HOLD" : "PENDING";
            upsertOwnership(connection, this.claimId, this.ownershipAnswer, this.adminResult, ownershipState);
            connection.commit();
            return true;
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            e.printStackTrace();
            return false;
        } finally {
            try { connection.setAutoCommit(oldAutoCommit); } catch (SQLException ignored) { }
            Db.closeQuietly(connection);
        }
    }

    public List<Claim> viewMyClaims() {
        return loadAll().stream()
                .filter(c -> c.userId == this.userId)
                .sorted(Comparator.comparing(Claim::getClaimDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public void viewClaimDetails() {
        System.out.println(getDisplayId() + " | " + getItemName() + " | " + claimStatus);
    }

    /** True when inventory id exists and is still Available / Claim-pending for desk handling. */
    public static boolean hasUsableRegisteredItem(Integer itemId) {
        if (itemId == null || itemId <= 0) return false;
        return Inventory.findById(itemId).map(item -> {
            String status = item.getItemStatus();
            if (status == null) return false;
            String key = status.toUpperCase(Locale.ENGLISH);
            return "AVAILABLE".equals(key) || "CLAIM_PENDING".equals(key);
        }).orElse(false);
    }

    public boolean placeOnHold() {
        this.claimStatus = "ON_HOLD";
        return persistStatusAndNote();
    }

    /** Admin resumes an On Hold claim back to Pending for decision. */
    public boolean resumeFromHold() {
        Optional<Claim> stored = findById(this.claimId);
        if (stored.isEmpty()) return false;
        if (!"ON_HOLD".equalsIgnoreCase(stored.get().claimStatus)) return false;
        this.claimStatus = "PENDING";
        if (this.adminNote == null || this.adminNote.isBlank()) {
            this.adminNote = stored.get().adminNote;
        }
        return persistStatusAndNote();
    }

    /**
     * Auto-holds Pending claims older than {@code days} without a staff decision.
     * Returns how many claims were moved to On Hold.
     */
    public static int autoHoldStalePending(int days) {
        if (days < 1) return 0;
        LocalDate today = LocalDate.now();
        int moved = 0;
        for (Claim claim : viewAllClaims()) {
            if (claim.claimStatus == null || !"PENDING".equalsIgnoreCase(claim.claimStatus)) continue;
            if (claim.claimDate == null) continue;
            if (ChronoUnit.DAYS.between(claim.claimDate, today) < days) continue;

            Claim target = new Claim();
            target.setClaimId(claim.claimId);
            target.setAdminNote("Auto-held after " + days + " days without a staff decision.");
            if (!target.placeOnHold()) continue;

            Notification note = new Notification();
            note.setUserId(claim.userId);
            note.setClaimId(claim.claimId);
            note.setMessage("Your claim for " + claim.getItemName()
                    + " is on hold while staff continue review.");
            note.notifyClaimDecision();
            moved++;
        }
        return moved;
    }

    public boolean linkFoundItem() {
        if (this.itemId == null) return false;
        if (Inventory.findById(this.itemId).isEmpty()) return false;
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE claims SET item_id = ? WHERE claim_id = ?"
        )) {
            ps.setInt(1, this.itemId);
            ps.setInt(2, this.claimId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean recordOwnershipAnswer() {
        if (this.ownershipAnswer == null || this.ownershipAnswer.isBlank()) return false;
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE claims SET ownership_answer = ? WHERE claim_id = ?"
            )) {
                ps.setString(1, this.ownershipAnswer);
                ps.setInt(2, this.claimId);
                if (ps.executeUpdate() == 0) return false;
            }
            upsertOwnership(connection, this.claimId, this.ownershipAnswer, this.adminResult, "PENDING");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean verifyOwnership() {
        if (this.adminResult == null || this.adminResult.isBlank()) return false;
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE claims SET admin_note = ? WHERE claim_id = ?"
            )) {
                ps.setString(1, Db.blankToNull(this.adminNote));
                ps.setInt(2, this.claimId);
                if (ps.executeUpdate() == 0) return false;
            }
            upsertOwnership(connection, this.claimId, this.ownershipAnswer, this.adminResult, "VERIFIED");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean approveClaim() {
        this.claimStatus = "APPROVED";
        if (this.adminResult == null || this.adminResult.isBlank()) {
            this.adminResult = "MATCH";
        }
        this.decisionDate = LocalDate.now();
        Optional<Claim> existing = findById(this.claimId);
        String existingCode = existing.map(Claim::getPickupCode).orElse(null);
        if (existingCode != null && !existingCode.isBlank()) {
            this.pickupCode = existingCode;
        } else if (this.pickupCode == null || this.pickupCode.isBlank()) {
            this.pickupCode = generatePickupCode();
        }
        if (!persistDecision()) return false;
        Optional<Claim> stored = findById(this.claimId);
        if (stored.isPresent() && stored.get().itemId != null) {
            Inventory item = new Inventory();
            item.setItemId(stored.get().itemId);
            item.setItemStatus("CLAIMED");
            item.updateItemStatus();
        }
        return true;
    }

    /** Lookup by desk pickup code (case-insensitive). */
    public static Optional<Claim> findByPickupCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String key = code.trim().toUpperCase(Locale.ENGLISH);
        return loadAll().stream()
                .filter(c -> c.pickupCode != null && key.equals(c.pickupCode.trim().toUpperCase(Locale.ENGLISH)))
                .findFirst();
    }

    /**
     * Ensures an approved claim has a pickup code (generates one for older approvals
     * that predate the pickup_code column). Does not change claim status.
     */
    public boolean ensurePickupCode() {
        Optional<Claim> stored = findById(this.claimId);
        if (stored.isEmpty()) return false;
        Claim live = stored.get();
        if (live.claimStatus == null || !"APPROVED".equalsIgnoreCase(live.claimStatus)) return false;
        if (live.returnDate != null) return false;
        if (live.pickupCode != null && !live.pickupCode.isBlank()) {
            this.pickupCode = live.pickupCode;
            return true;
        }
        String code = generatePickupCode();
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE claims SET pickup_code = ? WHERE claim_id = ?"
        )) {
            ps.setString(1, code);
            ps.setInt(2, this.claimId);
            if (ps.executeUpdate() == 0) return false;
            this.pickupCode = code;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean rejectClaim() {
        this.claimStatus = "REJECTED";
        if (this.adminResult == null || this.adminResult.isBlank()) {
            this.adminResult = "NO_MATCH";
        }
        this.decisionDate = LocalDate.now();
        return persistDecision();
    }

    public boolean updateClaimStatus() {
        if (this.claimStatus == null || this.claimStatus.isBlank()) return false;
        return persistStatusAndNote();
    }

    /**
     * Student may soft-archive finished or waiting claims (not approved-for-pickup).
     * Soft-archive keeps the MySQL row for audit.
     */
    public boolean canBeArchived() {
        String s = claimStatus == null ? "" : claimStatus.toUpperCase(Locale.ENGLISH);
        if ("ARCHIVED".equals(s)) return false;
        // Still needs desk pickup — keep in active My Claims.
        if ("APPROVED".equals(s) && returnDate == null) return false;
        return "PENDING".equals(s) || "ON_HOLD".equals(s) || "REJECTED".equals(s)
                || ("APPROVED".equals(s) && returnDate != null);
    }

    public boolean canBeRestoredFromArchive() {
        return claimStatus != null && "ARCHIVED".equalsIgnoreCase(claimStatus);
    }

    /** Soft-archive by the owning student — status → ARCHIVED. */
    public boolean archiveByOwner(int ownerUserId) {
        Optional<Claim> stored = findById(this.claimId);
        if (stored.isEmpty()) return false;
        Claim claim = stored.get();
        if (claim.userId != ownerUserId) return false;
        if (!claim.canBeArchived()) return false;
        this.userId = claim.userId;
        this.claimStatus = "ARCHIVED";
        this.adminNote = claim.adminNote;
        return persistStatusAndNote();
    }

    /** Restore archived claim to Pending (owner may re-open for staff review). */
    public boolean restoreFromArchive(int ownerUserId) {
        Optional<Claim> stored = findById(this.claimId);
        if (stored.isEmpty()) return false;
        Claim claim = stored.get();
        if (claim.userId != ownerUserId) return false;
        if (!claim.canBeRestoredFromArchive()) return false;
        this.userId = claim.userId;
        this.claimStatus = "PENDING";
        this.adminNote = claim.adminNote;
        return persistStatusAndNote();
    }

    public boolean confirmItemReturn() {
        Optional<Claim> storedOpt = findById(this.claimId);
        if (storedOpt.isEmpty()) return false;
        Claim claim = storedOpt.get();
        if (!"APPROVED".equalsIgnoreCase(claim.claimStatus)) return false;

        LocalDate when = this.returnDate != null ? this.returnDate : LocalDate.now();
        // Pickup cannot predate the approval/claim clock start (bad DatePicker / demo data).
        LocalDate earliest = claim.pickupClockStart();
        if (earliest != null && when.isBefore(earliest)) {
            when = earliest;
        }
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO item_returns (claim_id, return_date, received_by, return_note)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      return_date = VALUES(return_date),
                      received_by = VALUES(received_by),
                      return_note = VALUES(return_note)
                    """
            )) {
                ps.setInt(1, this.claimId);
                Db.setDate(ps, 2, when);
                ps.setString(3, Db.blankToNull(this.receivedBy));
                ps.setString(4, Db.blankToNull(this.returnNote));
                ps.executeUpdate();
            }
            this.returnDate = when;

            if (claim.itemId != null) {
                Inventory item = new Inventory();
                item.setItemId(claim.itemId);
                item.setItemStatus("RETURNED");
                item.updateItemStatus();
            }
            // Report status stays as-is here; admin UI sets CLAIMED on personal pickup confirm.
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public List<Claim> viewClaimHistory() {
        return viewMyClaims();
    }

    public static Optional<Claim> findById(int claimId) {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return Optional.empty();

        try (PreparedStatement ps = connection.prepareStatement(
                baseSelect() + " WHERE c.claim_id = ? LIMIT 1"
        )) {
            ps.setInt(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Db.closeQuietly(connection);
        }
        return Optional.empty();
    }

    /** True when any student already has a PENDING / ON_HOLD / APPROVED claim for this report. */
    public static boolean hasActiveClaimForReport(int reportId) {
        if (reportId <= 0) return false;
        return loadAll().stream()
                .filter(c -> c.reportId != null && c.reportId == reportId)
                .anyMatch(c -> {
                    String s = c.claimStatus == null ? "" : c.claimStatus.toUpperCase(Locale.ENGLISH);
                    return "PENDING".equals(s) || "ON_HOLD".equals(s) || "APPROVED".equals(s);
                });
    }

    /** True when the student already has a PENDING / ON_HOLD / APPROVED claim for this report. */
    public static boolean hasOpenClaimForReport(int userId, int reportId) {
        if (userId <= 0 || reportId <= 0) return false;
        return loadAll().stream()
                .filter(c -> c.userId == userId)
                .filter(c -> c.reportId != null && c.reportId == reportId)
                .anyMatch(c -> {
                    String s = c.claimStatus == null ? "" : c.claimStatus.toUpperCase(Locale.ENGLISH);
                    return "PENDING".equals(s) || "ON_HOLD".equals(s) || "APPROVED".equals(s);
                });
    }

    public List<Claim> filterByStatus(String statusOrNull) {
        return viewMyClaims().stream()
                .filter(c -> statusOrNull == null || statusOrNull.isBlank()
                        || statusOrNull.equalsIgnoreCase(c.claimStatus))
                .collect(Collectors.toList());
    }

    public long countByStatus(String status) {
        return viewMyClaims().stream()
                .filter(c -> status.equalsIgnoreCase(c.claimStatus))
                .count();
    }

    public static List<Claim> viewAllClaims() {
        return loadAll().stream()
                .sorted(Comparator.comparing(Claim::getClaimDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public static List<Claim> filterAllByStatus(String statusOrNull) {
        return viewAllClaims().stream()
                .filter(c -> statusOrNull == null || statusOrNull.isBlank()
                        || statusOrNull.equalsIgnoreCase(c.claimStatus))
                .collect(Collectors.toList());
    }

    public static long countAllByStatus(String status) {
        return viewAllClaims().stream()
                .filter(c -> status.equalsIgnoreCase(c.claimStatus))
                .count();
    }

    private boolean persistStatusAndNote() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE claims SET claim_status = ?, admin_note = ? WHERE claim_id = ?"
        )) {
            ps.setString(1, this.claimStatus);
            ps.setString(2, Db.blankToNull(this.adminNote));
            ps.setInt(3, this.claimId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    private boolean persistDecision() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try {
            if (this.decisionDate == null) {
                this.decisionDate = LocalDate.now();
            }
            boolean approved = "APPROVED".equalsIgnoreCase(this.claimStatus);
            String sql = approved
                    ? "UPDATE claims SET claim_status = ?, admin_note = ?, pickup_code = ?, decision_date = ? WHERE claim_id = ?"
                    : "UPDATE claims SET claim_status = ?, admin_note = ?, decision_date = ? WHERE claim_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, this.claimStatus);
                ps.setString(2, Db.blankToNull(this.adminNote));
                if (approved) {
                    ps.setString(3, Db.blankToNull(this.pickupCode));
                    Db.setDate(ps, 4, this.decisionDate);
                    ps.setInt(5, this.claimId);
                } else {
                    Db.setDate(ps, 3, this.decisionDate);
                    ps.setInt(4, this.claimId);
                }
                if (ps.executeUpdate() == 0) return false;
            }
            Optional<Claim> stored = findById(this.claimId);
            String answer = stored.map(c -> c.ownershipAnswer).orElse(this.ownershipAnswer);
            upsertOwnership(connection, this.claimId, answer, this.adminResult,
                    "APPROVED".equalsIgnoreCase(this.claimStatus) ? "MATCH" : "NO_MATCH");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    private static void upsertOwnership(Connection connection, int claimId, String answer,
                                        String result, String status) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO ownership_checks (claim_id, student_answer, admin_result, check_status)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  student_answer = COALESCE(VALUES(student_answer), student_answer),
                  admin_result = COALESCE(VALUES(admin_result), admin_result),
                  check_status = VALUES(check_status)
                """
        )) {
            ps.setInt(1, claimId);
            ps.setString(2, Db.blankToNull(answer));
            ps.setString(3, Db.blankToNull(result));
            ps.setString(4, status);
            ps.executeUpdate();
        }
    }

    private static List<Claim> loadAll() {
        List<Claim> list = new ArrayList<>();
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return list;

        try (PreparedStatement ps = connection.prepareStatement(baseSelect());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Db.closeQuietly(connection);
        }
        return list;
    }

    private static String baseSelect() {
        return """
                SELECT c.claim_id, c.student_user_id, c.report_id, c.item_id, c.claim_status,
                       c.ownership_answer, c.admin_note, c.claim_date, c.decision_date, c.pickup_code,
                       o.admin_result, r.return_date, r.received_by, r.return_note
                FROM claims c
                LEFT JOIN ownership_checks o ON o.claim_id = c.claim_id
                LEFT JOIN item_returns r ON r.claim_id = c.claim_id
                """;
    }

    private static Claim map(ResultSet rs) throws SQLException {
        String code = null;
        try {
            code = rs.getString("pickup_code");
        } catch (SQLException ignored) {
            // Column may be missing on very old DBs before migrateSchema runs.
        }
        LocalDate decided = null;
        try {
            decided = Db.getDate(rs, "decision_date");
        } catch (SQLException ignored) {
            // Column may be missing on very old DBs before migrateSchema runs.
        }
        return new Claim(
                rs.getInt("claim_id"),
                rs.getInt("student_user_id"),
                Db.getInteger(rs, "report_id"),
                Db.getInteger(rs, "item_id"),
                rs.getString("claim_status"),
                rs.getString("ownership_answer"),
                rs.getString("admin_result"),
                rs.getString("admin_note"),
                Db.getDate(rs, "claim_date"),
                decided,
                Db.getDate(rs, "return_date"),
                rs.getString("received_by"),
                rs.getString("return_note"),
                code
        );
    }
}
