package finalproj.mavenproject1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only desk activity log — who did what, and when.
 * Does not change report / claim / inventory workflows.
 */
final class AuditLog {

    record Entry(
            long id,
            Integer actorUserId,
            String actorName,
            String actorRole,
            String action,
            String entityType,
            String entityId,
            String details,
            LocalDateTime createdAt
    ) {}

    private AuditLog() {}

    /** Record an action for the current session user (or anonymous if logged out). */
    static void record(String action, String entityType, String entityId, String details) {
        User actor = User.getCurrentSession();
        Integer userId = actor == null ? null : actor.getUserId();
        String name = actor == null ? "(system)" : nullSafe(actor.getFullName(), actor.getUsername());
        String role = actor == null ? "" : nullSafe(actor.getUserRole(), "");
        write(userId, name, role, action, entityType, entityId, details);
    }

    static void write(Integer actorUserId, String actorName, String actorRole,
                      String action, String entityType, String entityId, String details) {
        if (action == null || action.isBlank()) return;
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO audit_log
                    (actor_user_id, actor_name, actor_role, action, entity_type, entity_id, details)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """
        )) {
            if (actorUserId == null) ps.setObject(1, null);
            else ps.setInt(1, actorUserId);
            ps.setString(2, trim(actorName, 150));
            ps.setString(3, trim(actorRole, 20));
            ps.setString(4, trim(action, 80));
            ps.setString(5, trim(entityType, 40));
            ps.setString(6, trim(entityId, 40));
            ps.setString(7, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }
    }

    static List<Entry> recent(int limit) {
        int cap = Math.max(1, Math.min(limit, 300));
        List<Entry> list = new ArrayList<>();
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT audit_id, actor_user_id, actor_name, actor_role, action,
                       entity_type, entity_id, details, created_at
                FROM audit_log
                ORDER BY created_at DESC, audit_id DESC
                LIMIT ?
                """
        )) {
            ps.setInt(1, cap);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer uid = rs.getObject("actor_user_id") == null
                            ? null : rs.getInt("actor_user_id");
                    Timestamp ts = rs.getTimestamp("created_at");
                    list.add(new Entry(
                            rs.getLong("audit_id"),
                            uid,
                            rs.getString("actor_name"),
                            rs.getString("actor_role"),
                            rs.getString("action"),
                            rs.getString("entity_type"),
                            rs.getString("entity_id"),
                            rs.getString("details"),
                            ts == null ? null : ts.toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }
        return list;
    }

    private static String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String trim(String value, int max) {
        if (value == null) return null;
        String t = value.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException ignored) {
            // ignore
        }
    }
}
