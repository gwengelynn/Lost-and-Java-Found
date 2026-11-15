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
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REPORT — Lost-item reporting (MySQL: lost_reports).
 */
public class Report {

    private int reportId;
    private int userId;
    private String itemName;
    private String category;
    private String subCategory;
    private String color;
    private String brand;
    private String description;
    private String lostLocation;
    private LocalDate lostDate;
    private String reportStatus;
    private LocalDate dateSubmitted;

    public Report() {}

    public Report(int reportId, int userId, String itemName, String category, String subCategory,
                  String color, String brand, String description, String lostLocation,
                  LocalDate lostDate, String reportStatus, LocalDate dateSubmitted) {
        this.reportId = reportId;
        this.userId = userId;
        this.itemName = itemName;
        this.category = category;
        this.subCategory = subCategory;
        this.color = color;
        this.brand = brand;
        this.description = description;
        this.lostLocation = lostLocation;
        this.lostDate = lostDate;
        this.reportStatus = reportStatus;
        this.dateSubmitted = dateSubmitted;
    }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLostLocation() { return lostLocation; }
    public void setLostLocation(String lostLocation) { this.lostLocation = lostLocation; }

    public LocalDate getLostDate() { return lostDate; }
    public void setLostDate(LocalDate lostDate) { this.lostDate = lostDate; }

    public String getReportStatus() { return reportStatus; }
    public void setReportStatus(String reportStatus) { this.reportStatus = reportStatus; }

    public LocalDate getDateSubmitted() { return dateSubmitted; }
    public void setDateSubmitted(LocalDate dateSubmitted) { this.dateSubmitted = dateSubmitted; }

    public String getDisplayId() {
        return String.format("LTF-%03d", reportId);
    }

    public boolean submitReport() {
        if (userId <= 0 || itemName == null || itemName.isBlank()) return false;
        if (category == null || category.isBlank()) return false;
        this.dateSubmitted = LocalDate.now();
        if (this.lostDate == null) this.lostDate = LocalDate.now();
        if (this.reportStatus == null || this.reportStatus.isBlank()) {
            this.reportStatus = "PENDING";
        }

        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO lost_reports
                (student_user_id, item_name, category, sub_category, color, brand, description,
                 lost_location, lost_date, report_status, date_submitted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
        )) {
            ps.setInt(1, this.userId);
            ps.setString(2, this.itemName.trim());
            ps.setString(3, this.category.trim());
            ps.setString(4, Db.blankToNull(this.subCategory));
            ps.setString(5, Db.blankToNull(this.color));
            ps.setString(6, Db.blankToNull(this.brand));
            ps.setString(7, Db.blankToNull(this.description));
            ps.setString(8, Db.blankToNull(this.lostLocation));
            Db.setDate(ps, 9, this.lostDate);
            ps.setString(10, this.reportStatus);
            Db.setDate(ps, 11, this.dateSubmitted);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) return false;
                this.reportId = keys.getInt(1);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean updateReport() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                """
                UPDATE lost_reports
                SET item_name = ?, category = ?, sub_category = ?, color = ?, brand = ?,
                    description = ?, lost_location = ?, lost_date = ?
                WHERE report_id = ? AND student_user_id = ?
                """
        )) {
            ps.setString(1, this.itemName);
            ps.setString(2, this.category);
            ps.setString(3, Db.blankToNull(this.subCategory));
            ps.setString(4, Db.blankToNull(this.color));
            ps.setString(5, Db.blankToNull(this.brand));
            ps.setString(6, Db.blankToNull(this.description));
            ps.setString(7, Db.blankToNull(this.lostLocation));
            Db.setDate(ps, 8, this.lostDate);
            ps.setInt(9, this.reportId);
            ps.setInt(10, this.userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean deleteReport() {
        Optional<Report> stored = findById(this.reportId);
        if (stored.isEmpty()) return false;
        Report report = stored.get();
        if (report.userId != this.userId) return false;
        if ("RESOLVED".equalsIgnoreCase(report.reportStatus)) return false;
        this.reportStatus = "CANCELLED";
        return updateReportStatus();
    }

    public void viewReport() {
        System.out.println(getDisplayId() + " | " + itemName + " | " + reportStatus);
    }

    public List<Report> viewMyReports() {
        return loadAll().stream()
                .filter(r -> r.userId == this.userId)
                .sorted(Comparator.comparing(Report::getDateSubmitted,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public boolean updateReportStatus() {
        if (this.reportStatus == null || this.reportStatus.isBlank()) return false;
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE lost_reports SET report_status = ? WHERE report_id = ?"
        )) {
            ps.setString(1, this.reportStatus);
            ps.setInt(2, this.reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public List<Report> searchReports() {
        String key = itemName == null ? "" : itemName.trim().toLowerCase(Locale.ENGLISH);
        return loadAll().stream()
                .filter(r -> key.isEmpty()
                        || r.itemName.toLowerCase(Locale.ENGLISH).contains(key)
                        || (r.description != null && r.description.toLowerCase(Locale.ENGLISH).contains(key))
                        || (r.brand != null && r.brand.toLowerCase(Locale.ENGLISH).contains(key)))
                .collect(Collectors.toList());
    }

    public List<Report> filterReports() {
        return loadAll().stream()
                .filter(r -> this.userId <= 0 || r.userId == this.userId)
                .filter(r -> this.category == null || this.category.isBlank()
                        || this.category.equalsIgnoreCase(r.category))
                .filter(r -> this.reportStatus == null || this.reportStatus.isBlank()
                        || this.reportStatus.equalsIgnoreCase(r.reportStatus))
                .sorted(Comparator.comparing(Report::getDateSubmitted,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public List<Inventory> findPossibleMatches() {
        Inventory probe = new Inventory();
        probe.setCategory(this.category);
        probe.setColor(this.color);
        probe.setBrand(this.brand);
        probe.setItemName(this.itemName);
        return probe.compareWithReports().stream()
                .filter(item -> {
                    boolean categoryOk = category == null || category.isBlank()
                            || category.equalsIgnoreCase(item.getCategory());
                    boolean colorOk = color == null || color.isBlank()
                            || color.equalsIgnoreCase(item.getColor());
                    boolean brandOk = brand == null || brand.isBlank()
                            || brand.equalsIgnoreCase(item.getBrand());
                    boolean nameOk = itemName == null || itemName.isBlank()
                            || item.getItemName().toLowerCase(Locale.ENGLISH)
                            .contains(itemName.toLowerCase(Locale.ENGLISH))
                            || itemName.toLowerCase(Locale.ENGLISH)
                            .contains(item.getItemName().toLowerCase(Locale.ENGLISH));
                    return categoryOk && (colorOk || brandOk || nameOk);
                })
                .collect(Collectors.toList());
    }

    public static Optional<Report> findById(int reportId) {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return Optional.empty();

        try (PreparedStatement ps = connection.prepareStatement(
                baseSelect() + " WHERE report_id = ? LIMIT 1"
        )) {
            ps.setInt(1, reportId);
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

    private static List<Report> loadAll() {
        List<Report> list = new ArrayList<>();
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
                SELECT report_id, student_user_id, item_name, category, sub_category, color, brand,
                       description, lost_location, lost_date, report_status, date_submitted
                FROM lost_reports
                """;
    }

    private static Report map(ResultSet rs) throws SQLException {
        return new Report(
                rs.getInt("report_id"),
                rs.getInt("student_user_id"),
                rs.getString("item_name"),
                rs.getString("category"),
                rs.getString("sub_category"),
                rs.getString("color"),
                rs.getString("brand"),
                rs.getString("description"),
                rs.getString("lost_location"),
                Db.getDate(rs, "lost_date"),
                rs.getString("report_status"),
                Db.getDate(rs, "date_submitted")
        );
    }
}
