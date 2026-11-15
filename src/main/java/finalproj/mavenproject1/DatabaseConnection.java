package finalproj.mavenproject1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/lost_and_found_db";

    private static final String USER = "root";

    private static final String PASSWORD = "unocutie123";

    private static boolean prepared;

    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(
                    URL, USER, PASSWORD
            );
            ensureReady(connection);
            return connection;
        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
            return null;
        }
    }

    /** Aligns Workbench schema with the app and seeds demo rows once. */
    private static synchronized void ensureReady(Connection connection) throws SQLException {
        if (prepared) {
            return;
        }

        migrateSchema(connection);
        seedDemoAccounts(connection);
        seedStorageBoxes(connection);
        seedDemoDomainData(connection);
        prepared = true;
        System.out.println("Database connected successfully!");
    }

    private static void migrateSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
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

            // Prefer datetime so notification times are preserved.
            try {
                st.executeUpdate("ALTER TABLE notifications MODIFY sent_date DATETIME NULL");
            } catch (SQLException ignored) {
                // ignore if already compatible
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

    private static void seedDemoAccounts(Connection connection) throws SQLException {
        if (!usernameExists(connection, "student")) {
            insertUser(connection, "student", "student123", "STUDENT");
            int userId = lastInsertId(connection);
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO students
                    (user_id, student_number, full_name, course, year_level, contact_number, email)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """
            )) {
                ps.setInt(1, userId);
                ps.setString(2, "2025-12345");
                ps.setString(3, "Juan Dela Cruz");
                ps.setString(4, "BSCPE");
                ps.setString(5, "3");
                ps.setString(6, "0917-123-4567");
                ps.setString(7, "juan.delacruz@ue.edu.ph");
                ps.executeUpdate();
            }
        }

        if (!usernameExists(connection, "admin")) {
            insertUser(connection, "admin", "admin123", "ADMIN");
            int userId = lastInsertId(connection);
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO admins (user_id, full_name, email, contact_number)
                    VALUES (?, ?, ?, ?)
                    """
            )) {
                ps.setInt(1, userId);
                ps.setString(2, "Demo Admin");
                ps.setString(3, "admin@ue.edu.ph");
                ps.setString(4, "0918-000-0000");
                ps.executeUpdate();
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

    /** Seeds sample reports/items/claims/notifications only when tables are empty. */
    private static void seedDemoDomainData(Connection connection) throws SQLException {
        int studentId = userIdByUsername(connection, "student");
        int adminId = userIdByUsername(connection, "admin");
        if (studentId <= 0 || adminId <= 0) return;

        if (countRows(connection, "lost_reports") == 0) {
            insertReport(connection, studentId, "Black Wallet", "Personal Belongings", "Wallets",
                    "Black", "Unknown", "Leather wallet with UE ID inside",
                    "Library 2F", "2025-04-22", "PENDING", "2025-04-24");
            insertReport(connection, studentId, "Headphones", "Devices", "Headphones",
                    "Black", "Sony", "Over-ear headphones with stickers on left cup",
                    "Engineering Building", "2025-04-18", "MATCHED", "2025-04-20");
            insertReport(connection, studentId, "Water Bottle", "Personal Belongings", "Other",
                    "Blue", "Nalgene", "Blue bottle with name sticker",
                    "Cafeteria", "2025-04-15", "RESOLVED", "2025-04-18");
        }

        if (countRows(connection, "items") == 0) {
            int boxA = boxIdByName(connection, "Box A");
            int boxB = boxIdByName(connection, "Box B");
            int boxC = boxIdByName(connection, "Box C");
            insertItem(connection, adminId, boxA, "TRK-2025-0010", "Headphones", "Devices",
                    "Headphones", "Sony", "Black",
                    "Over-ear headphones found near Eng'g lobby",
                    "Sticker on left cup; frayed right cable",
                    "2025-04-19", "CLAIMED");
            insertItem(connection, adminId, boxB, "TRK-2025-0011", "Water Bottle",
                    "Personal Belongings", "Other", "Nalgene", "Blue",
                    "Blue bottle left in cafeteria",
                    "Name sticker partially peeled",
                    "2025-04-16", "AVAILABLE");
            insertItem(connection, adminId, boxA, "TRK-2025-0012", "Black Wallet",
                    "Personal Belongings", "Wallets", "Unknown", "Black",
                    "Leather wallet found near library",
                    "UE ID card pocket; loyalty card inside",
                    "2025-04-23", "AVAILABLE");
            insertItem(connection, adminId, boxC, "TRK-2025-0013", "Power Bank", "Devices",
                    "Power banks", "Romoss", "White",
                    "Power bank left in engineering hall",
                    "Scratch near USB-C port; initials 'JD' on back",
                    "2025-04-21", "CLAIM_PENDING");
        }

        if (countRows(connection, "claims") == 0) {
            int r1 = firstReportId(connection, studentId, "Black Wallet");
            int r2 = firstReportId(connection, studentId, "Headphones");
            int r3 = firstReportId(connection, studentId, "Water Bottle");
            int headphones = firstItemId(connection, "TRK-2025-0010");
            int bottle = firstItemId(connection, "TRK-2025-0011");

            int c1 = insertClaim(connection, studentId, r1, null, "PENDING",
                    "Has UE ID and loyalty card inside", null, "2025-04-25");
            int c2 = insertClaim(connection, studentId, r2, headphones, "APPROVED",
                    "Sony headphones with sticker on left cup", "Ownership verified", "2025-04-21");
            int c3 = insertClaim(connection, studentId, r3, bottle, "REJECTED",
                    "Blue bottle", "Details did not match private marks", "2025-04-19");
            insertClaim(connection, studentId, r1, null, "ON_HOLD",
                    "Waiting for item registration", "Item not yet registered", "2025-04-17");

            upsertOwnership(connection, c2, "Sony headphones with sticker on left cup", "MATCH", "VERIFIED");
            upsertOwnership(connection, c3, "Blue bottle", "NO_MATCH", "FAILED");
            upsertOwnership(connection, c1, "Has UE ID and loyalty card inside", null, "PENDING");
        }

        if (countRows(connection, "notifications") == 0) {
            int r2 = firstReportId(connection, studentId, "Headphones");
            int r1 = firstReportId(connection, studentId, "Black Wallet");
            int claimApproved = firstClaimId(connection, studentId, "APPROVED");
            int claimPending = firstClaimId(connection, studentId, "PENDING");
            insertNotification(connection, studentId, null, claimApproved,
                    "Your claim for Black Wallet has been approved!", "CLAIM_DECISION", false,
                    "2025-04-24 10:24:00");
            insertNotification(connection, studentId, r2, null,
                    "Your lost item report (Headphones) has been matched with a found item.",
                    "POSSIBLE_MATCH", false, "2025-04-22 14:10:00");
            insertNotification(connection, studentId, r1, null,
                    "A new match may be available for your report.",
                    "ALERT", true, "2025-04-21 09:05:00");
            insertNotification(connection, studentId, null, claimPending,
                    "Your claim has been submitted and is awaiting review.",
                    "CLAIM_UPDATE", true, "2025-04-20 16:40:00");
        }
    }

    private static void insertReport(Connection connection, int studentId, String itemName,
                                     String category, String subCategory, String color, String brand,
                                     String description, String location, String lostDate,
                                     String status, String submitted) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO lost_reports
                (student_user_id, item_name, category, sub_category, color, brand, description,
                 lost_location, lost_date, report_status, date_submitted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """
        )) {
            ps.setInt(1, studentId);
            ps.setString(2, itemName);
            ps.setString(3, category);
            ps.setString(4, subCategory);
            ps.setString(5, color);
            ps.setString(6, brand);
            ps.setString(7, description);
            ps.setString(8, location);
            ps.setString(9, lostDate);
            ps.setString(10, status);
            ps.setString(11, submitted);
            ps.executeUpdate();
        }
    }

    private static void insertItem(Connection connection, int adminId, int boxId, String tracking,
                                   String itemName, String category, String subCategory, String brand,
                                   String color, String description, String privateDetails,
                                   String foundDate, String status) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO items
                (admin_user_id, box_id, tracking_number, item_name, category, sub_category, brand,
                 color, description, private_details, found_date, item_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """
        )) {
            ps.setInt(1, adminId);
            ps.setInt(2, boxId);
            ps.setString(3, tracking);
            ps.setString(4, itemName);
            ps.setString(5, category);
            ps.setString(6, subCategory);
            ps.setString(7, brand);
            ps.setString(8, color);
            ps.setString(9, description);
            ps.setString(10, privateDetails);
            ps.setString(11, foundDate);
            ps.setString(12, status);
            ps.executeUpdate();
        }
    }

    private static int insertClaim(Connection connection, int studentId, int reportId, Integer itemId,
                                   String status, String ownership, String adminNote, String claimDate)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO claims
                (student_user_id, report_id, item_id, claim_status, ownership_answer, admin_note, claim_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
        )) {
            ps.setInt(1, studentId);
            ps.setInt(2, reportId);
            Db.setIntOrNull(ps, 3, itemId);
            ps.setString(4, status);
            ps.setString(5, ownership);
            ps.setString(6, adminNote);
            ps.setString(7, claimDate);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void upsertOwnership(Connection connection, int claimId, String answer,
                                        String result, String status) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO ownership_checks (claim_id, student_answer, admin_result, check_status)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  student_answer = VALUES(student_answer),
                  admin_result = VALUES(admin_result),
                  check_status = VALUES(check_status)
                """
        )) {
            ps.setInt(1, claimId);
            ps.setString(2, answer);
            ps.setString(3, result);
            ps.setString(4, status);
            ps.executeUpdate();
        }
    }

    private static void insertNotification(Connection connection, int userId, Integer reportId,
                                           Integer claimId, String message, String type,
                                           boolean read, String sentAt) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO notifications
                (user_id, report_id, claim_id, message, notification_type, is_read, sent_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """
        )) {
            ps.setInt(1, userId);
            Db.setIntOrNull(ps, 2, reportId);
            Db.setIntOrNull(ps, 3, claimId);
            ps.setString(4, message);
            ps.setString(5, type);
            ps.setBoolean(6, read);
            ps.setString(7, sentAt);
            ps.executeUpdate();
        }
    }

    private static void insertUser(Connection connection, String username, String password, String role)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users (username, password, role, account_status) VALUES (?, ?, ?, 'Active')"
        )) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    private static boolean usernameExists(Connection connection, String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM users WHERE username = ? LIMIT 1"
        )) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
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

    private static int boxIdByName(Connection connection, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT box_id FROM storage_boxes WHERE box_name = ? LIMIT 1"
        )) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int countRows(Connection connection, String table) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int firstReportId(Connection connection, int studentId, String itemName)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT report_id FROM lost_reports WHERE student_user_id = ? AND item_name = ? LIMIT 1"
        )) {
            ps.setInt(1, studentId);
            ps.setString(2, itemName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int firstItemId(Connection connection, String tracking) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT item_id FROM items WHERE tracking_number = ? LIMIT 1"
        )) {
            ps.setString(1, tracking);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int firstClaimId(Connection connection, int studentId, String status)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT claim_id FROM claims
                WHERE student_user_id = ? AND claim_status = ?
                ORDER BY claim_id LIMIT 1
                """
        )) {
            ps.setInt(1, studentId);
            ps.setString(2, status);
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
