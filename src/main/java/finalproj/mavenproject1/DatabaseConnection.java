package finalproj.mavenproject1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static boolean prepared;

    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(
                    AppConfig.dbUrl(), AppConfig.dbUser(), AppConfig.dbPassword()
            );
            ensureReady(connection);
            return connection;
        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Aligns schema, one-time live clean (wipe dummy/demo transactional data),
     * ensures Admin is user_id 1 named "Admin", keeps storage boxes. No demo seed data.
     */
    private static synchronized void ensureReady(Connection connection) throws SQLException {
        if (prepared) {
            return;
        }

        migrateSchema(connection);
        ensureAppMetaTable(connection);
        prepareLiveCleanSlate(connection);
        ensureAdminAsUserOne(connection);
        seedStorageBoxes(connection);
        prepared = true;
        System.out.println("Database connected successfully!");
    }

    private static void migrateSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            // Keep password column wide enough (BCrypt leftovers were ~60 chars).
            try {
                st.executeUpdate("ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NOT NULL");
            } catch (SQLException ignored) {
                // ignore if already compatible
            }

            addColumnIfMissing(connection, st, "users", "account_status",
                    "ALTER TABLE users ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'Active'");
            addColumnIfMissing(connection, st, "students", "email",
                    "ALTER TABLE students ADD COLUMN email VARCHAR(100) NULL");
            addColumnIfMissing(connection, st, "admins", "email",
                    "ALTER TABLE admins ADD COLUMN email VARCHAR(100) NULL");
            addColumnIfMissing(connection, st, "admins", "contact_number",
                    "ALTER TABLE admins ADD COLUMN contact_number VARCHAR(20) NULL");

            addColumnIfMissing(connection, st, "lost_reports", "brand",
                    "ALTER TABLE lost_reports ADD COLUMN brand VARCHAR(100) NULL");
            addColumnIfMissing(connection, st, "lost_reports", "sub_category",
                    "ALTER TABLE lost_reports ADD COLUMN sub_category VARCHAR(50) NULL");
            addColumnIfMissing(connection, st, "lost_reports", "date_submitted",
                    "ALTER TABLE lost_reports ADD COLUMN date_submitted DATE NULL");
            addColumnIfMissing(connection, st, "lost_reports", "report_type",
                    "ALTER TABLE lost_reports ADD COLUMN report_type VARCHAR(20) NOT NULL DEFAULT 'LOST'");

            addColumnIfMissing(connection, st, "items", "tracking_number",
                    "ALTER TABLE items ADD COLUMN tracking_number VARCHAR(50) NULL");
            addColumnIfMissing(connection, st, "items", "sub_category",
                    "ALTER TABLE items ADD COLUMN sub_category VARCHAR(50) NULL");
            addColumnIfMissing(connection, st, "items", "description",
                    "ALTER TABLE items ADD COLUMN description TEXT NULL");

            addColumnIfMissing(connection, st, "notifications", "report_id",
                    "ALTER TABLE notifications ADD COLUMN report_id INT NULL");
            addColumnIfMissing(connection, st, "notifications", "claim_id",
                    "ALTER TABLE notifications ADD COLUMN claim_id INT NULL");

            addColumnIfMissing(connection, st, "lost_reports", "photo_path",
                    "ALTER TABLE lost_reports ADD COLUMN photo_path VARCHAR(255) NULL");
            addColumnIfMissing(connection, st, "items", "photo_path",
                    "ALTER TABLE items ADD COLUMN photo_path VARCHAR(255) NULL");
            addColumnIfMissing(connection, st, "lost_reports", "photo_data",
                    "ALTER TABLE lost_reports ADD COLUMN photo_data LONGBLOB NULL");
            addColumnIfMissing(connection, st, "items", "photo_data",
                    "ALTER TABLE items ADD COLUMN photo_data LONGBLOB NULL");
            addColumnIfMissing(connection, st, "users", "avatar_path",
                    "ALTER TABLE users ADD COLUMN avatar_path VARCHAR(255) NULL");
            addColumnIfMissing(connection, st, "users", "avatar_data",
                    "ALTER TABLE users ADD COLUMN avatar_data LONGBLOB NULL");
            // After SAO admin Reset PW, user must pick a new password on next login.
            addColumnIfMissing(connection, st, "users", "must_change_password",
                    "ALTER TABLE users ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing(connection, st, "claims", "pickup_code",
                    "ALTER TABLE claims ADD COLUMN pickup_code VARCHAR(16) NULL");
            // When admin approved/rejected — used for approved→pickup analytics (not claim filed date).
            addColumnIfMissing(connection, st, "claims", "decision_date",
                    "ALTER TABLE claims ADD COLUMN decision_date DATE NULL");
            // Sticky Lost↔Found link — which inventory item a Lost report was matched to.
            addColumnIfMissing(connection, st, "lost_reports", "matched_item_id",
                    "ALTER TABLE lost_reports ADD COLUMN matched_item_id INT NULL");
            try {
                st.executeUpdate("ALTER TABLE notifications MODIFY COLUMN message TEXT NOT NULL");
            } catch (SQLException ignored) {
                // already TEXT / compatible
            }

            // Allow claims without a linked item yet (app supports ON_HOLD / pending link).
            if (!isColumnNullable(connection, "claims", "item_id")) {
                try {
                    st.executeUpdate("ALTER TABLE claims DROP FOREIGN KEY claims_ibfk_3");
                } catch (SQLException ignored) {
                    // FK name may differ on some installs
                }
                st.executeUpdate("ALTER TABLE claims MODIFY item_id INT NULL");
                try {
                    st.executeUpdate(
                            "ALTER TABLE claims ADD CONSTRAINT claims_ibfk_3 "
                                    + "FOREIGN KEY (item_id) REFERENCES items(item_id)"
                    );
                } catch (SQLException ignored) {
                    // already present
                }
            }

            // Claims are standalone ownership requests — not filed against the claimant's own lost report.
            if (!isColumnNullable(connection, "claims", "report_id")) {
                try {
                    st.executeUpdate("ALTER TABLE claims DROP FOREIGN KEY claims_ibfk_2");
                } catch (SQLException ignored) {
                    // FK name may differ on some installs
                }
                st.executeUpdate("ALTER TABLE claims MODIFY report_id INT NULL");
                try {
                    st.executeUpdate(
                            "ALTER TABLE claims ADD CONSTRAINT claims_ibfk_2 "
                                    + "FOREIGN KEY (report_id) REFERENCES lost_reports(report_id)"
                    );
                } catch (SQLException ignored) {
                    // already present
                }
            }

            // Prefer datetime so notification times are preserved.
            try {
                st.executeUpdate("ALTER TABLE notifications MODIFY sent_date DATETIME NULL");
            } catch (SQLException ignored) {
                // ignore if already compatible
            }

            // Append-only desk audit trail (who matched / approved / logged in, etc.).
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS audit_log (
                        audit_id INT AUTO_INCREMENT PRIMARY KEY,
                        actor_user_id INT NULL,
                        actor_name VARCHAR(150) NULL,
                        actor_role VARCHAR(20) NULL,
                        action VARCHAR(80) NOT NULL,
                        entity_type VARCHAR(40) NULL,
                        entity_id VARCHAR(40) NULL,
                        details TEXT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_audit_created (created_at),
                        INDEX idx_audit_action (action)
                    )
                    """);

            // Merge Library / library-style duplicates into a single display form.
            normalizeExistingLostLocations(connection);
            backfillClaimDecisionDates(connection);
            // Found tips formerly marked RESOLVED → REGISTERED (not owner Claimed).
            normalizeFoundRegisteredStatus(connection);
            ensureAppMetaTable(connection);
        }
    }

    /** Found reports logged into inventory use REGISTERED (legacy rows used RESOLVED). */
    private static void normalizeFoundRegisteredStatus(Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                UPDATE lost_reports
                SET report_status = 'REGISTERED'
                WHERE UPPER(COALESCE(report_type, 'LOST')) = 'FOUND'
                  AND UPPER(COALESCE(report_status, '')) = 'RESOLVED'
                """
        )) {
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // older schemas without report_type — non-fatal
        }
    }

    private static void ensureAppMetaTable(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS app_meta (
                        meta_key VARCHAR(64) PRIMARY KEY,
                        meta_value VARCHAR(255) NOT NULL,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
        }
    }

    private static boolean metaIs(Connection connection, String key, String expected) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT meta_value FROM app_meta WHERE meta_key = ? LIMIT 1"
        )) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && expected.equalsIgnoreCase(rs.getString(1));
            }
        }
    }

    private static void setMeta(Connection connection, String key, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO app_meta (meta_key, meta_value) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value)
                """
        )) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    /**
     * One-time defense/live reset (v2 — re-run for empty demo-ready DB):
     * — clears reports, claims, inventory, notifications, audit (analytics go empty)
     * — removes all student / dummy accounts
     * — rebuilds Admin as user_id = 1, display name "Admin" (not "Demo Admin")
     * Runs only once (app_meta.live_clean_v3 = done).
     */
    private static void prepareLiveCleanSlate(Connection connection) throws SQLException {
        if (metaIs(connection, "live_clean_v3", "done")) {
            // Keep Admin display name clean on later launches.
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    UPDATE admins SET full_name = 'Admin'
                    WHERE full_name LIKE '%Demo%' OR full_name = 'Demo Admin'
                       OR full_name IS NULL OR TRIM(full_name) = ''
                    """
            )) {
                ps.executeUpdate();
            }
            return;
        }

        try (Statement st = connection.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");

            // Transactional / analytics source data
            execIgnoreMissing(st, "DELETE FROM notifications");
            execIgnoreMissing(st, "DELETE FROM item_returns");
            execIgnoreMissing(st, "DELETE FROM ownership_checks");
            execIgnoreMissing(st, "DELETE FROM claims");
            execIgnoreMissing(st, "DELETE FROM lost_reports");
            execIgnoreMissing(st, "DELETE FROM items");
            execIgnoreMissing(st, "DELETE FROM audit_log");

            // Accounts: wipe ALL students + admins so Admin can own user_id 1
            execIgnoreMissing(st, "DELETE FROM students");
            execIgnoreMissing(st, "DELETE FROM admins");
            execIgnoreMissing(st, "DELETE FROM users");

            resetAutoIncrement(st, "users");
            resetAutoIncrement(st, "lost_reports");
            resetAutoIncrement(st, "items");
            resetAutoIncrement(st, "claims");
            resetAutoIncrement(st, "notifications");
            resetAutoIncrement(st, "audit_log");
            resetAutoIncrement(st, "item_returns");
            resetAutoIncrement(st, "ownership_checks");

            st.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        // Admin = user_id 1, name "Admin" only (login: admin / admin123)
        String hash = PasswordHasher.hash("admin123");
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO users (user_id, username, password, role, account_status)
                VALUES (1, 'admin', ?, 'ADMIN', 'Active')
                """
        )) {
            ps.setString(1, hash);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO admins (user_id, full_name, email, contact_number)
                VALUES (1, 'Admin', 'admin@ue.edu.ph', '0918-000-0000')
                """
        )) {
            ps.executeUpdate();
        }

        setMeta(connection, "live_clean_v3", "done");
        setMeta(connection, "live_clean_v2", "done");
        setMeta(connection, "live_clean_v1", "done");
        System.out.println("Live clean v3 complete: all reports/claims/inventory/students wiped; Admin is user_id 1 named Admin.");
    }

    private static void execIgnoreMissing(Statement st, String sql) {
        try {
            st.executeUpdate(sql);
        } catch (SQLException ignored) {
            // table may not exist on very old DBs
        }
    }

    private static void resetAutoIncrement(Statement st, String table) {
        try {
            st.executeUpdate("ALTER TABLE " + table + " AUTO_INCREMENT = 1");
        } catch (SQLException ignored) {
            // ignore if table missing
        }
    }

    /** Ensures Admin account exists as user_id 1 named Admin (never seeds a dummy student). */
    private static void ensureAdminAsUserOne(Connection connection) throws SQLException {
        // Always normalize admin display name — never show "Demo Admin".
        try (PreparedStatement ps = connection.prepareStatement(
                """
                UPDATE admins a
                INNER JOIN users u ON u.user_id = a.user_id
                SET a.full_name = 'Admin'
                WHERE UPPER(u.role) = 'ADMIN'
                   OR u.username = 'admin'
                   OR a.full_name LIKE '%Demo%'
                   OR a.full_name = 'Demo Admin'
                """
        )) {
            ps.executeUpdate();
        }

        int adminId = userIdByUsername(connection, "admin");
        if (adminId > 0) {
            if (adminId != 1) {
                System.out.println("Note: admin user_id is " + adminId + " (expected 1 after live clean).");
            } else {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE admins SET full_name = 'Admin' WHERE user_id = 1"
                )) {
                    ps.executeUpdate();
                }
            }
            return;
        }

        // Fresh DB without admin — create as ID 1 when free, else next id.
        boolean id1Free;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM users WHERE user_id = 1 LIMIT 1"
        ); ResultSet rs = ps.executeQuery()) {
            id1Free = !rs.next();
        }
        if (id1Free) {
            String hash = PasswordHasher.hash("admin123");
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO users (user_id, username, password, role, account_status)
                    VALUES (1, 'admin', ?, 'ADMIN', 'Active')
                    """
            )) {
                ps.setString(1, hash);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO admins (user_id, full_name, email, contact_number)
                    VALUES (1, 'Admin', 'admin@ue.edu.ph', '0918-000-0000')
                    """
            )) {
                ps.executeUpdate();
            }
        } else {
            insertUser(connection, "admin", "admin123", "ADMIN");
            int userId = lastInsertId(connection);
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO admins (user_id, full_name, email, contact_number)
                    VALUES (?, 'Admin', 'admin@ue.edu.ph', '0918-000-0000')
                    """
            )) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        }
    }

    /**
     * Fills claims.decision_date from APPROVE_CLAIM audit rows when missing,
     * so approved→pickup averages are not inflated by long Pending waits.
     */
    private static void backfillClaimDecisionDates(Connection connection) {
        try {
            if (!columnExists(connection, "claims", "decision_date")) return;
            if (!tableExists(connection, "audit_log")) return;
        } catch (SQLException e) {
            return;
        }
        String select = """
                SELECT entity_id, MIN(DATE(created_at)) AS decided
                FROM audit_log
                WHERE action = 'APPROVE_CLAIM'
                  AND entity_type = 'CLAIM'
                  AND entity_id IS NOT NULL
                GROUP BY entity_id
                """;
        String update = """
                UPDATE claims SET decision_date = ?
                WHERE claim_id = ? AND decision_date IS NULL
                  AND UPPER(claim_status) = 'APPROVED'
                """;
        try (PreparedStatement q = connection.prepareStatement(select);
             ResultSet rs = q.executeQuery();
             PreparedStatement u = connection.prepareStatement(update)) {
            while (rs.next()) {
                String entityId = rs.getString("entity_id");
                java.sql.Date decided = rs.getDate("decided");
                if (entityId == null || decided == null) continue;
                Integer claimId = parseClaimId(entityId);
                if (claimId == null) continue;
                u.setDate(1, decided);
                u.setInt(2, claimId);
                u.executeUpdate();
            }
        } catch (SQLException ignored) {
            // audit_log may be empty / older installs — non-fatal
        }
    }

    private static Integer parseClaimId(String displayId) {
        if (displayId == null) return null;
        String t = displayId.trim().toUpperCase(java.util.Locale.ENGLISH);
        if (!t.startsWith("CLM-")) return null;
        try {
            return Integer.parseInt(t.substring(4).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        String sql = """
                SELECT 1 FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** One-pass cleanup of lost_location text for hotspot analytics. */
    private static void normalizeExistingLostLocations(Connection connection) throws SQLException {
        String select = "SELECT report_id, lost_location FROM lost_reports "
                + "WHERE lost_location IS NOT NULL AND TRIM(lost_location) <> ''";
        try (PreparedStatement q = connection.prepareStatement(select);
             ResultSet rs = q.executeQuery();
             PreparedStatement u = connection.prepareStatement(
                     "UPDATE lost_reports SET lost_location = ? WHERE report_id = ?"
             )) {
            while (rs.next()) {
                int id = rs.getInt("report_id");
                String raw = rs.getString("lost_location");
                String clean = LocationNormalizer.display(raw);
                if (clean == null || clean.equals(raw)) continue;
                u.setString(1, clean);
                u.setInt(2, id);
                u.executeUpdate();
            }
        }
    }

    private static void addColumnIfMissing(Connection connection, Statement st,
                                           String table, String column, String ddl)
            throws SQLException {
        if (!columnExists(connection, table, column)) {
            st.executeUpdate(ddl);
        }
    }

    private static boolean columnExists(Connection connection, String table, String column)
            throws SQLException {
        String sql = """
                SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean isColumnNullable(Connection connection, String table, String column)
            throws SQLException {
        String sql = """
                SELECT IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return true;
                return "YES".equalsIgnoreCase(rs.getString(1));
            }
        }
    }

    private static void seedStorageBoxes(Connection connection) throws SQLException {
        ensureBox(connection, "Box A", "Lost & Found Room - Shelf 1");
        ensureBox(connection, "Box B", "Lost & Found Room - Shelf 2");
        ensureBox(connection, "Box C", "Lost & Found Room - Shelf 3");
    }

    private static void ensureBox(Connection connection, String name, String location)
            throws SQLException {
        try (PreparedStatement find = connection.prepareStatement(
                "SELECT box_id FROM storage_boxes WHERE box_name = ? LIMIT 1"
        )) {
            find.setString(1, name);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO storage_boxes (box_name, box_location) VALUES (?, ?)"
        )) {
            insert.setString(1, name);
            insert.setString(2, location);
            insert.executeUpdate();
        }
    }

    private static void insertUser(Connection connection, String username, String password, String role)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users (username, password, role, account_status) VALUES (?, ?, ?, 'Active')"
        )) {
            ps.setString(1, username);
            // Store BCrypt hash only — never plaintext.
            ps.setString(2, PasswordHasher.hash(password));
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    private static int userIdByUsername(Connection connection, String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT user_id FROM users WHERE username = ? LIMIT 1"
        )) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int lastInsertId(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Resolve storage box by name; create it when missing. */
    static int resolveBoxId(Connection connection, String boxName, String location)
            throws SQLException {
        String name = (boxName == null || boxName.isBlank()) ? "Box A" : boxName.trim();
        try (PreparedStatement find = connection.prepareStatement(
                "SELECT box_id FROM storage_boxes WHERE box_name = ? LIMIT 1"
        )) {
            find.setString(1, name);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO storage_boxes (box_name, box_location) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            insert.setString(1, name);
            insert.setString(2, Db.blankToNull(location));
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
}
