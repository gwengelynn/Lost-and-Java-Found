package finalproj.mavenproject1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CLAIM — Claim management (MySQL: claims + ownership_checks + item_returns).
 */
public class Claim {

    private int claimId;
    private int userId;
    private int reportId;
    private Integer itemId;
    private String claimStatus;
    private String ownershipAnswer;
    private String adminResult;
    private String adminNote;
    private LocalDate claimDate;
    private LocalDate returnDate;
    private String receivedBy;
    private String returnNote;

    public Claim() {}

    public Claim(int claimId, int userId, int reportId, Integer itemId, String claimStatus,
                 String ownershipAnswer, String adminResult, String adminNote,
                 LocalDate claimDate, LocalDate returnDate, String receivedBy, String returnNote) {
        this.claimId = claimId;
        this.userId = userId;
        this.reportId = reportId;
        this.itemId = itemId;
        this.claimStatus = claimStatus;
        this.ownershipAnswer = ownershipAnswer;
        this.adminResult = adminResult;
        this.adminNote = adminNote;
        this.claimDate = claimDate;
        this.returnDate = returnDate;
        this.receivedBy = receivedBy;
        this.returnNote = returnNote;
    }

    public int getClaimId() { return claimId; }
    public void setClaimId(int claimId) { this.claimId = claimId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

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

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }

    public String getReturnNote() { return returnNote; }
    public void setReturnNote(String returnNote) { this.returnNote = returnNote; }

    public String getDisplayId() {
        return String.format("CLM-%03d", claimId);
    }

    public String getItemName() {
        return Report.findById(reportId)
                .map(Report::getItemName)
                .orElse("Unknown item");
    }

    public boolean submitClaim() {
        if (userId <= 0 || reportId <= 0) return false;
        if (ownershipAnswer == null || ownershipAnswer.isBlank()) return false;
        this.claimDate = LocalDate.now();
        if (this.claimStatus == null || this.claimStatus.isBlank()) {
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
                ps.setInt(2, this.reportId);
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

            upsertOwnership(connection, this.claimId, this.ownershipAnswer, this.adminResult, "PENDING");
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

    public boolean placeOnHold() {
        this.claimStatus = "ON_HOLD";
        return persistStatusAndNote();
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

    public boolean rejectClaim() {
        this.claimStatus = "REJECTED";
        if (this.adminResult == null || this.adminResult.isBlank()) {
            this.adminResult = "NO_MATCH";
        }
        return persistDecision();
    }

    public boolean updateClaimStatus() {
        if (this.claimStatus == null || this.claimStatus.isBlank()) return false;
        return persistStatusAndNote();
    }

    public boolean confirmItemReturn() {
        Optional<Claim> storedOpt = findById(this.claimId);
        if (storedOpt.isEmpty()) return false;
        Claim claim = storedOpt.get();
        if (!"APPROVED".equalsIgnoreCase(claim.claimStatus)) return false;

        LocalDate when = this.returnDate != null ? this.returnDate : LocalDate.now();
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
            Report report = new Report();
            report.setReportId(claim.reportId);
            report.setReportStatus("RESOLVED");
            report.updateReportStatus();
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
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE claims SET claim_status = ?, admin_note = ? WHERE claim_id = ?"
            )) {
                ps.setString(1, this.claimStatus);
                ps.setString(2, Db.blankToNull(this.adminNote));
                ps.setInt(3, this.claimId);
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
                       c.ownership_answer, c.admin_note, c.claim_date,
                       o.admin_result, r.return_date, r.received_by, r.return_note
                FROM claims c
                LEFT JOIN ownership_checks o ON o.claim_id = c.claim_id
                LEFT JOIN item_returns r ON r.claim_id = c.claim_id
                """;
    }

    private static Claim map(ResultSet rs) throws SQLException {
        return new Claim(
                rs.getInt("claim_id"),
                rs.getInt("student_user_id"),
                rs.getInt("report_id"),
                Db.getInteger(rs, "item_id"),
                rs.getString("claim_status"),
                rs.getString("ownership_answer"),
                rs.getString("admin_result"),
                rs.getString("admin_note"),
                Db.getDate(rs, "claim_date"),
                Db.getDate(rs, "return_date"),
                rs.getString("received_by"),
                rs.getString("return_note")
        );
    }
}
