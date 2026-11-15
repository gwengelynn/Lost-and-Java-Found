package finalproj.mavenproject1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * NOTIFICATION — System notifications (MySQL: notifications).
 */
public class Notification {

    private int notificationId;
    private int userId;
    private Integer reportId;
    private Integer claimId;
    private String message;
    private String notificationType;
    private boolean isRead;
    private LocalDateTime sentDate;

    public Notification() {}

    public Notification(int notificationId, int userId, Integer reportId, Integer claimId,
                        String message, String notificationType, boolean isRead,
                        LocalDateTime sentDate) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.reportId = reportId;
        this.claimId = claimId;
        this.message = message;
        this.notificationType = notificationType;
        this.isRead = isRead;
        this.sentDate = sentDate;
    }

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getReportId() { return reportId; }
    public void setReportId(Integer reportId) { this.reportId = reportId; }

    public Integer getClaimId() { return claimId; }
    public void setClaimId(Integer claimId) { this.claimId = claimId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }

    public boolean sendNotification() {
        if (userId <= 0 || message == null || message.isBlank()) return false;
        this.sentDate = LocalDateTime.now();
        this.isRead = false;
        if (this.notificationType == null || this.notificationType.isBlank()) {
            this.notificationType = "ALERT";
        }

        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO notifications
                (user_id, report_id, claim_id, message, notification_type, is_read, sent_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
        )) {
            ps.setInt(1, this.userId);
            Db.setIntOrNull(ps, 2, this.reportId);
            Db.setIntOrNull(ps, 3, this.claimId);
            ps.setString(4, this.message);
            ps.setString(5, this.notificationType);
            ps.setBoolean(6, this.isRead);
            Db.setTimestamp(ps, 7, this.sentDate);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) return false;
                this.notificationId = keys.getInt(1);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public void notifyReportUpdate() {
        this.notificationType = "REPORT_UPDATE";
        if (this.message == null || this.message.isBlank()) {
            this.message = "Your lost-item report has been updated.";
        }
        sendNotification();
    }

    public void notifyPossibleMatch() {
        this.notificationType = "POSSIBLE_MATCH";
        if (this.message == null || this.message.isBlank()) {
            this.message = "A possible match was found for your lost-item report.";
        }
        sendNotification();
    }

    public void notifyItemNotRegistered() {
        this.notificationType = "ITEM_NOT_REGISTERED";
        if (this.message == null || this.message.isBlank()) {
            this.message = "The item has not been registered yet. Your claim remains on hold.";
        }
        sendNotification();
    }

    public void notifyClaimUpdate() {
        this.notificationType = "CLAIM_UPDATE";
        if (this.message == null || this.message.isBlank()) {
            this.message = "Your claim status has been updated.";
        }
        sendNotification();
    }

    public void notifyClaimDecision() {
        this.notificationType = "CLAIM_DECISION";
        if (this.message == null || this.message.isBlank()) {
            this.message = "A decision has been made on your claim.";
        }
        sendNotification();
    }

    public void notifyItemReturned() {
        this.notificationType = "RETURN";
        if (this.message == null || this.message.isBlank()) {
            this.message = "Your item has been returned successfully.";
        }
        sendNotification();
    }

    public List<Notification> viewNotifications() {
        return loadForUser(this.userId).stream()
                .sorted(Comparator.comparing(Notification::getSentDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public boolean markAsRead() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE notifications SET is_read = 1 WHERE notification_id = ? AND user_id = ?"
        )) {
            ps.setInt(1, this.notificationId);
            ps.setInt(2, this.userId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) this.isRead = true;
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean markAllAsRead() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE notifications SET is_read = 1 WHERE user_id = ?"
        )) {
            ps.setInt(1, this.userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public int countUnreadNotifications() {
        return (int) viewNotifications().stream().filter(n -> !n.isRead).count();
    }

    public static Optional<Notification> findById(int notificationId) {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return Optional.empty();

        try (PreparedStatement ps = connection.prepareStatement(
                baseSelect() + " WHERE notification_id = ? LIMIT 1"
        )) {
            ps.setInt(1, notificationId);
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

    private static List<Notification> loadForUser(int userId) {
        List<Notification> list = new ArrayList<>();
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return list;

        try (PreparedStatement ps = connection.prepareStatement(
                baseSelect() + " WHERE user_id = ?"
        )) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Db.closeQuietly(connection);
        }
        return list;
    }

    private static String baseSelect() {
        return """
                SELECT notification_id, user_id, report_id, claim_id, message,
                       notification_type, is_read, sent_date
                FROM notifications
                """;
    }

    private static Notification map(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getInt("notification_id"),
                rs.getInt("user_id"),
                Db.getInteger(rs, "report_id"),
                Db.getInteger(rs, "claim_id"),
                rs.getString("message"),
                rs.getString("notification_type"),
                rs.getBoolean("is_read"),
                Db.getTimestamp(rs, "sent_date")
        );
    }
}
