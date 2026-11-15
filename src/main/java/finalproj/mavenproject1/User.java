package finalproj.mavenproject1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * USER — Account management backed by MySQL (users + students/admins).
 */
public class User {

    private static User currentSession;

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role;
    private String studentNumber;
    private String course;
    private String yearLevel;
    private String contactNumber;
    private String email;
    private String accountStatus;

    public User() {}

    public User(int userId, String username, String passwordHash, String fullName, String role,
                String studentNumber, String course, String yearLevel, String contactNumber,
                String email, String accountStatus) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.studentNumber = studentNumber;
        this.course = course;
        this.yearLevel = yearLevel;
        this.contactNumber = contactNumber;
        this.email = email;
        this.accountStatus = accountStatus;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getYearLevel() { return yearLevel; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    /** Creates a STUDENT account in MySQL (users + students). */
    public boolean register() {
        if (!validateAccount()) return false;
        if (usernameExists(this.username)) return false;

        if (this.role == null || this.role.isBlank()) {
            this.role = "STUDENT";
        }
        if (!"STUDENT".equalsIgnoreCase(this.role)) {
            return false; // signup only creates students
        }
        if (this.studentNumber == null || this.studentNumber.isBlank()) {
            return false;
        }
        if (this.accountStatus == null || this.accountStatus.isBlank()) {
            this.accountStatus = "Active";
        }

        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement userPs = connection.prepareStatement(
                    "INSERT INTO users (username, password, role, account_status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                userPs.setString(1, this.username.trim());
                userPs.setString(2, this.passwordHash);
                userPs.setString(3, "STUDENT");
                userPs.setString(4, this.accountStatus);
                userPs.executeUpdate();

                try (ResultSet keys = userPs.getGeneratedKeys()) {
                    if (!keys.next()) {
                        connection.rollback();
                        return false;
                    }
                    this.userId = keys.getInt(1);
                }
            }

            try (PreparedStatement studentPs = connection.prepareStatement(
                    """
                    INSERT INTO students
                    (user_id, student_number, full_name, course, year_level, contact_number, email)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """
            )) {
                studentPs.setInt(1, this.userId);
                studentPs.setString(2, this.studentNumber.trim());
                studentPs.setString(3, this.fullName.trim());
                studentPs.setString(4, blankToNull(this.course));
                studentPs.setString(5, blankToNull(this.yearLevel));
                studentPs.setString(6, blankToNull(this.contactNumber));
                studentPs.setString(7, blankToNull(this.email));
                studentPs.executeUpdate();
            }

            connection.commit();
            this.role = "STUDENT";
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // ignore
            }
            System.out.println("Register failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(oldAutoCommit);
                connection.close();
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }

    /** Checks login credentials against MySQL. */
    public boolean login() {
        Optional<User> found = findByLoginKey(this.username);
        if (found.isEmpty()) return false;
        User account = found.get();
        if (!"Active".equalsIgnoreCase(nullSafe(account.accountStatus, "Active"))) return false;
        if (!account.passwordHash.equals(this.passwordHash)) return false;
        if (this.role != null && !this.role.isBlank()
                && !account.role.equalsIgnoreCase(this.role)) {
            return false;
        }
        currentSession = account;
        copyFrom(account);
        return true;
    }

    public void logout() {
        if (currentSession != null && currentSession.userId == this.userId) {
            currentSession = null;
        }
    }

    public boolean updateProfile() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try {
            if ("STUDENT".equalsIgnoreCase(this.role)) {
                try (PreparedStatement ps = connection.prepareStatement(
                        """
                        UPDATE students
                        SET full_name = ?, student_number = ?, course = ?, year_level = ?,
                            contact_number = ?, email = ?
                        WHERE user_id = ?
                        """
                )) {
                    ps.setString(1, this.fullName);
                    ps.setString(2, this.studentNumber);
                    ps.setString(3, blankToNull(this.course));
                    ps.setString(4, blankToNull(this.yearLevel));
                    ps.setString(5, blankToNull(this.contactNumber));
                    ps.setString(6, blankToNull(this.email));
                    ps.setInt(7, this.userId);
                    if (ps.executeUpdate() == 0) return false;
                }
            } else if ("ADMIN".equalsIgnoreCase(this.role)) {
                try (PreparedStatement ps = connection.prepareStatement(
                        """
                        UPDATE admins
                        SET full_name = ?, email = ?, contact_number = ?
                        WHERE user_id = ?
                        """
                )) {
                    ps.setString(1, this.fullName);
                    ps.setString(2, blankToNull(this.email));
                    ps.setString(3, blankToNull(this.contactNumber));
                    ps.setInt(4, this.userId);
                    if (ps.executeUpdate() == 0) return false;
                }
            } else {
                return false;
            }

            if (currentSession != null && currentSession.userId == this.userId) {
                currentSession.copyFrom(this);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(connection);
        }
    }

    public boolean changePassword() {
        if (this.passwordHash == null || this.passwordHash.isBlank()) return false;
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET password = ? WHERE user_id = ?"
        )) {
            ps.setString(1, this.passwordHash);
            ps.setInt(2, this.userId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok && currentSession != null && currentSession.userId == this.userId) {
                currentSession.passwordHash = this.passwordHash;
            }
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(connection);
        }
    }

    public String getUserRole() {
        return role;
    }

    public boolean validateAccount() {
        if (username == null || username.isBlank()) return false;
        if (passwordHash == null || passwordHash.isBlank()) return false;
        if (fullName == null || fullName.isBlank()) return false;
        if (email == null || email.isBlank() || !email.contains("@")) return false;
        return true;
    }

    public boolean deactivateAccount() {
        return setAccountStatusInDb("Inactive");
    }

    public boolean activateAccount() {
        return setAccountStatusInDb("Active");
    }

    private boolean setAccountStatusInDb(String status) {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET account_status = ? WHERE user_id = ?"
        )) {
            ps.setString(1, status);
            ps.setInt(2, this.userId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                this.accountStatus = status;
                if (currentSession != null && currentSession.userId == this.userId) {
                    currentSession.accountStatus = status;
                }
            }
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(connection);
        }
    }

    public static Optional<User> findById(int userId) {
        String sql = baseSelectSql() + " WHERE u.user_id = ? LIMIT 1";
        return queryOne(sql, ps -> ps.setInt(1, userId));
    }

    public static Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        String sql = baseSelectSql() + " WHERE u.username = ? LIMIT 1";
        return queryOne(sql, ps -> ps.setString(1, username.trim()));
    }

    public static Optional<User> findByLoginKey(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        String value = key.trim();
        String sql = baseSelectSql() + """
                 WHERE u.username = ?
                    OR s.email = ?
                    OR a.email = ?
                    OR s.student_number = ?
                LIMIT 1
                """;
        return queryOne(sql, ps -> {
            ps.setString(1, value);
            ps.setString(2, value);
            ps.setString(3, value);
            ps.setString(4, value);
        });
    }

    public static boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    public static User getCurrentSession() {
        return currentSession;
    }

    public static List<User> listAccounts() {
        String sql = baseSelectSql() + " ORDER BY u.user_id";
        List<User> accounts = new ArrayList<>();
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return accounts;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accounts.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }
        return accounts;
    }

    private static String baseSelectSql() {
        return """
                SELECT u.user_id, u.username, u.password, u.role, u.account_status,
                       COALESCE(s.full_name, a.full_name) AS full_name,
                       s.student_number, s.course, s.year_level,
                       COALESCE(s.contact_number, a.contact_number) AS contact_number,
                       COALESCE(s.email, a.email) AS email
                FROM users u
                LEFT JOIN students s ON s.user_id = u.user_id
                LEFT JOIN admins a ON a.user_id = u.user_id
                """;
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private static Optional<User> queryOne(String sql, Binder binder) {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return Optional.empty();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(connection);
        }
        return Optional.empty();
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"),
                rs.getString("role"),
                rs.getString("student_number"),
                rs.getString("course"),
                rs.getString("year_level"),
                rs.getString("contact_number"),
                rs.getString("email"),
                nullSafe(rs.getString("account_status"), "Active")
        );
    }

    private void copyFrom(User other) {
        this.userId = other.userId;
        this.username = other.username;
        this.passwordHash = other.passwordHash;
        this.fullName = other.fullName;
        this.role = other.role;
        this.studentNumber = other.studentNumber;
        this.course = other.course;
        this.yearLevel = other.yearLevel;
        this.contactNumber = other.contactNumber;
        this.email = other.email;
        this.accountStatus = other.accountStatus;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
