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
 * REPORT — Lost/found reporting (MySQL: lost_reports).
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
    /** LOST = student lost an item; FOUND = student turned in / reported a found item. */
    private String reportType;
    private String photoPath;
    /** Raw image bytes stored in MySQL (LONGBLOB photo_data). */
    private byte[] photoData;
    /** Inventory item this Lost report was matched to (null until admin verifies Lost↔Found). */
    private Integer matchedItemId;

    public Report() {}

    public Report(int reportId, int userId, String itemName, String category, String subCategory,
                  String color, String brand, String description, String lostLocation,
                  LocalDate lostDate, String reportStatus, LocalDate dateSubmitted) {
        this(reportId, userId, itemName, category, subCategory, color, brand, description,
                lostLocation, lostDate, reportStatus, dateSubmitted, "LOST", null, null);
    }

    public Report(int reportId, int userId, String itemName, String category, String subCategory,
                  String color, String brand, String description, String lostLocation,
                  LocalDate lostDate, String reportStatus, LocalDate dateSubmitted, String reportType) {
        this(reportId, userId, itemName, category, subCategory, color, brand, description,
                lostLocation, lostDate, reportStatus, dateSubmitted, reportType, null, null);
    }

    public Report(int reportId, int userId, String itemName, String category, String subCategory,
                  String color, String brand, String description, String lostLocation,
                  LocalDate lostDate, String reportStatus, LocalDate dateSubmitted,
                  String reportType, String photoPath) {
        this(reportId, userId, itemName, category, subCategory, color, brand, description,
                lostLocation, lostDate, reportStatus, dateSubmitted, reportType, photoPath, null);
    }

    public Report(int reportId, int userId, String itemName, String category, String subCategory,
                  String color, String brand, String description, String lostLocation,
                  LocalDate lostDate, String reportStatus, LocalDate dateSubmitted,
                  String reportType, String photoPath, Integer matchedItemId) {
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
        this.reportType = reportType == null || reportType.isBlank() ? "LOST" : reportType;
        this.photoPath = photoPath;
        this.matchedItemId = matchedItemId;
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

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) {
        this.reportType = reportType == null || reportType.isBlank() ? "LOST" : reportType.trim().toUpperCase(Locale.ENGLISH);
    }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public byte[] getPhotoData() { return photoData; }
    public void setPhotoData(byte[] photoData) { this.photoData = photoData; }

    /** Sets path and loads file bytes for MySQL BLOB storage. */
    public void setPhoto(String storedPath) {
        this.photoPath = storedPath;
        this.photoData = PhotoStorage.readBytes(storedPath);
    }

    public Integer getMatchedItemId() { return matchedItemId; }
    public void setMatchedItemId(Integer matchedItemId) { this.matchedItemId = matchedItemId; }

    public boolean isFoundReport() {
        return "FOUND".equalsIgnoreCase(reportType);
    }

    public boolean isLostReport() {
        return !isFoundReport();
    }

    public String getDisplayId() {
        return String.format("LTF-%03d", reportId);
    }

    public boolean submitReport() {
        if (userId <= 0 || itemName == null || itemName.isBlank()) return false;
        if (category == null || category.isBlank()) return false;
        this.dateSubmitted = LocalDate.now();
        if (this.lostDate == null) this.lostDate = LocalDate.now();
        if (this.reportType == null || this.reportType.isBlank()) {
            this.reportType = "LOST";
        }
        // Lost with nothing in inventory yet → On Hold until a found item appears.
        // Lost with scored inventory overlap → Pending for admin match review.
        // Found tips stay Pending for registration.
        if (this.reportStatus == null || this.reportStatus.isBlank()
                || "PENDING".equalsIgnoreCase(this.reportStatus)) {
            if (isLostReport() && MatchScorer.rankForReport(this).isEmpty()) {
                this.reportStatus = "ON_HOLD";
            } else {
                this.reportStatus = "PENDING";
            }
        }
        // Block exact open duplicates (same student + name + type still active).
        if (hasOpenDuplicate(this.userId, this.itemName, this.reportType)) {
            return false;
        }
        // Anti-fraud: same student cannot file Lost+Found for the same item.
        if (crossTypeSameItemBlockReason(
                this.userId, this.reportType, this.itemName, this.category, this.color, this.brand
        ) != null) {
            return false;
        }

        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO lost_reports
                (student_user_id, item_name, category, sub_category, color, brand, description,
                 lost_location, lost_date, report_status, date_submitted, report_type, photo_path, photo_data)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            this.lostLocation = LocationNormalizer.display(this.lostLocation);
            ps.setString(8, Db.blankToNull(this.lostLocation));
            Db.setDate(ps, 9, this.lostDate);
            ps.setString(10, this.reportStatus);
            Db.setDate(ps, 11, this.dateSubmitted);
            ps.setString(12, this.reportType);
            ps.setString(13, Db.blankToNull(this.photoPath));
            bindPhotoData(ps, 14, this.photoData);
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
                    description = ?, lost_location = ?, lost_date = ?, photo_path = ?, photo_data = ?
                WHERE report_id = ? AND student_user_id = ?
                  AND UPPER(COALESCE(report_status, '')) IN ('PENDING', 'UNDER_REVIEW', 'ON_HOLD')
                """
        )) {
            ps.setString(1, this.itemName);
            ps.setString(2, this.category);
            ps.setString(3, Db.blankToNull(this.subCategory));
            ps.setString(4, Db.blankToNull(this.color));
            ps.setString(5, Db.blankToNull(this.brand));
            ps.setString(6, Db.blankToNull(this.description));
            this.lostLocation = LocationNormalizer.display(this.lostLocation);
            ps.setString(7, Db.blankToNull(this.lostLocation));
            Db.setDate(ps, 8, this.lostDate);
            ps.setString(9, Db.blankToNull(this.photoPath));
            bindPhotoData(ps, 10, this.photoData);
            ps.setInt(11, this.reportId);
            ps.setInt(12, this.userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    /** Open LOST reports (Pending / Under Review / On Hold) that overlap inventory — match, do not register. */
    public static List<Report> listLostReportsNeedingMatch() {
        return loadAll().stream()
                .filter(Report::isLostReport)
                .filter(Report::isMatchQueueOpen)
                .filter(r -> !MatchScorer.rankForReport(r).isEmpty())
                .sorted(Comparator.comparing(Report::getDateSubmitted,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Pending/under-review FOUND tips — these are the only student reports that may be
     * registered into Manage Inventory (student turned an item in).
     */
    public static List<Report> listFoundTipsNeedingRegistration() {
        return loadAll().stream()
                .filter(Report::isFoundReport)
                .filter(Report::isOpenForStaffAction)
                .sorted(Comparator.comparing(Report::getDateSubmitted,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    /** Early queue: Pending / Under Review (Found tips + Lost ready for desk work). */
    public boolean isOpenForStaffAction() {
        String s = statusKey();
        return "PENDING".equals(s) || "UNDER_REVIEW".equals(s);
    }

    /** Lost report waiting because no inventory overlap existed at submit time. */
    public boolean isOnHoldAwaitingInventory() {
        return isLostReport() && "ON_HOLD".equals(statusKey());
    }

    /**
     * Lost reports that may still be matched: Pending, Under Review, or On Hold
     * (On Hold unlocks once inventory candidates appear).
     */
    public boolean isMatchQueueOpen() {
        if (!isLostReport()) return false;
        String s = statusKey();
        return "PENDING".equals(s) || "UNDER_REVIEW".equals(s) || "ON_HOLD".equals(s);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(statusKey());
    }

    public boolean isArchived() {
        return "ARCHIVED".equals(statusKey());
    }

    /** Soft-closed for desk queues — still kept in MySQL for audit. */
    public boolean isSoftClosed() {
        return isCancelled() || isArchived();
    }

    /**
     * Case finished or past staff matching — Matches / Status / Register must not reopen it.
     * Includes Matched (awaiting claim), Claimed (owner pickup), Registered (found tip logged),
     * legacy Resolved, Cancelled, Archived.
     */
    public boolean isLifecycleLocked() {
        if (isSoftClosed()) return true;
        String s = statusKey();
        return "MATCHED".equals(s) || "CLAIMED".equals(s)
                || "REGISTERED".equals(s) || "RESOLVED".equals(s);
    }

    /** Found tip already logged into inventory (REGISTERED, or legacy RESOLVED). */
    public boolean isFoundRegistered() {
        if (!isFoundReport()) return false;
        String s = statusKey();
        return "REGISTERED".equals(s) || "RESOLVED".equals(s);
    }

    /**
     * Human-readable status aligned to workflow:
     * Found tip logged → Registered; Lost owner pickup → Claimed.
     */
    public String displayStatusLabel() {
        String s = statusKey();
        if (s.isEmpty()) return "—";
        if (isFoundRegistered()) return "Registered";
        if ("CLAIMED".equals(s)) return "Claimed";
        if ("ON_HOLD".equals(s)) return "On Hold";
        if ("UNDER_REVIEW".equals(s)) return "Under Review";
        if ("MATCHED".equals(s)) return "Matched";
        if ("PENDING".equals(s)) return "Pending";
        if ("CANCELLED".equals(s)) return "Cancelled";
        if ("ARCHIVED".equals(s)) return "Archived";
        if ("RESOLVED".equals(s)) return "Registered";
        String[] parts = s.toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /**
     * Admin may Match / Status:
     * — Found: Pending / Under Review (Register path)
     * — Lost Pending / Under Review: always
     * — Lost On Hold: only when scored inventory candidates exist
     */
    public boolean allowsStaffMutation() {
        if (isLifecycleLocked()) return false;
        if (isFoundReport()) return isOpenForStaffAction();
        if (!isLostReport()) return false;
        if (isOpenForStaffAction()) return true;
        return isOnHoldAwaitingInventory() && !MatchScorer.rankForReport(this).isEmpty();
    }

    private String statusKey() {
        return reportStatus == null ? "" : reportStatus.toUpperCase(Locale.ENGLISH);
    }

    /**
     * Student may cancel while still early (Pending / Under Review / On Hold),
     * and only when no active claim is tied to the report.
     */
    public boolean canBeCancelledByOwner() {
        String s = statusKey();
        if (!("PENDING".equals(s) || "UNDER_REVIEW".equals(s) || "ON_HOLD".equals(s))) return false;
        return !Claim.hasActiveClaimForReport(this.reportId);
    }

    /**
     * Admin archive — hide from active queues but keep the row.
     * Blocked while Matched (awaiting claim) or while an active claim exists.
     * Allowed after completion (Claimed / Registered) so desk can file records.
     */
    public boolean canBeArchived() {
        String s = statusKey();
        if ("ARCHIVED".equals(s)) return false;
        // Mid-case: owner still needs to claim — keep in active queue.
        if ("MATCHED".equals(s)) return false;
        return !Claim.hasActiveClaimForReport(this.reportId);
    }

    public boolean canBeRestoredFromArchive() {
        return isArchived();
    }

    public boolean canBeReopenedFromCancelled() {
        return isCancelled();
    }

    /** Open Lost reports waiting for a Found item match (includes On Hold). */
    public static List<Report> loadOpenLostReports() {
        return loadAll().stream()
                .filter(Report::isLostReport)
                .filter(Report::isMatchQueueOpen)
                .sorted(Comparator.comparing(Report::getDateSubmitted,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    /**
     * When a Found inventory item is registered, lift matching On Hold Lost reports
     * to Under Review so admin can verify Match / Status. Notifies each student.
     *
     * @return how many On Hold reports were promoted
     */
    public static int promoteOnHoldLostReportsForItem(Inventory foundItem) {
        if (foundItem == null) return 0;
        int promoted = 0;
        for (Report lost : loadAll()) {
            if (!lost.isOnHoldAwaitingInventory()) continue;
            MatchScorer.ScoredMatch scored = MatchScorer.score(lost, foundItem);
            if (!scored.isSuggestedMatch()) continue;
            Report target = new Report();
            target.setReportId(lost.getReportId());
            target.setReportStatus("UNDER_REVIEW");
            if (!target.updateReportStatus()) continue;
            promoted++;
            Notification note = new Notification();
            note.setUserId(lost.getUserId());
            note.setReportId(lost.getReportId());
            note.setMessage("Good news: a possible match for your lost item (\""
                    + lost.getItemName()
                    + "\") is now in Lost & Found inventory ("
                    + nullSafeTracking(foundItem)
                    + "). Staff will verify — status is Under Review.");
            note.notifyReportUpdate();
            AuditLog.record("LOST_HOLD_RELEASED", "REPORT", lost.getDisplayId(),
                    "On Hold → Under Review — candidate " + nullSafeTracking(foundItem)
                            + " (" + scored.score() + "%)");
        }
        return promoted;
    }

    private static String nullSafeTracking(Inventory item) {
        if (item == null) return "inventory";
        String t = item.getTrackingNumber();
        return t == null || t.isBlank() ? item.getItemName() : t;
    }

    /**
     * Soft-cancel by the owning student — never deletes the MySQL row.
     * Prefer {@link #cancelByOwner(int)}; this name is kept for older call sites.
     */
    public boolean deleteReport() {
        return cancelByOwner(this.userId);
    }

    /** Soft-cancel: status → CANCELLED (history retained). */
    public boolean cancelByOwner(int ownerUserId) {
        Optional<Report> stored = findById(this.reportId);
        if (stored.isEmpty()) return false;
        Report report = stored.get();
        if (report.userId != ownerUserId) return false;
        if (!report.canBeCancelledByOwner()) return false;
        this.userId = report.userId;
        this.reportStatus = "CANCELLED";
        return updateReportStatus();
    }

    /** Soft-archive for SAO desk — status → ARCHIVED (history retained). */
    public boolean archiveReport() {
        Optional<Report> stored = findById(this.reportId);
        if (stored.isEmpty()) return false;
        Report report = stored.get();
        if (!report.canBeArchived()) return false;
        this.reportStatus = "ARCHIVED";
        return updateReportStatus();
    }

    /** Soft-archive by the owning student — same rules as {@link #canBeArchived()}. */
    public boolean archiveByOwner(int ownerUserId) {
        Optional<Report> stored = findById(this.reportId);
        if (stored.isEmpty()) return false;
        Report report = stored.get();
        if (report.userId != ownerUserId) return false;
        if (!report.canBeArchived()) return false;
        this.userId = report.userId;
        this.reportStatus = "ARCHIVED";
        return updateReportStatus();
    }

    /** Restore archived report into the active queue as PENDING. */
    public boolean restoreFromArchive() {
        Optional<Report> stored = findById(this.reportId);
        if (stored.isEmpty()) return false;
        if (!stored.get().canBeRestoredFromArchive()) return false;
        this.reportStatus = "PENDING";
        return updateReportStatus();
    }

    /** Reopen a cancelled report as PENDING (admin correction). */
    public boolean reopenCancelled() {
        Optional<Report> stored = findById(this.reportId);
        if (stored.isEmpty()) return false;
        if (!stored.get().canBeReopenedFromCancelled()) return false;
        this.reportStatus = "PENDING";
        return updateReportStatus();
    }

    /** Record-filter helper: ACTIVE hides cancelled/archived; others match status. */
    public static boolean matchesRecordFilter(Report report, String recordFilter) {
        if (report == null) return false;
        String filter = recordFilter == null ? "ACTIVE" : recordFilter.trim().toUpperCase(Locale.ENGLISH);
        String s = report.getReportStatus() == null ? "" : report.getReportStatus().toUpperCase(Locale.ENGLISH);
        return switch (filter) {
            case "ALL" -> true;
            case "CANCELLED" -> "CANCELLED".equals(s);
            case "ARCHIVED" -> "ARCHIVED".equals(s);
            default -> !"CANCELLED".equals(s) && !"ARCHIVED".equals(s); // ACTIVE
        };
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

    /**
     * Marks a Lost report as MATCHED and stores the Found inventory item id (sticky link).
     * Does not create inventory — only records which found item admin verified against.
     */
    public boolean updateMatchToItem(int itemId) {
        if (this.reportId <= 0 || itemId <= 0) return false;
        Optional<Report> live = findById(this.reportId);
        if (live.isEmpty() || !live.get().allowsStaffMutation() || !live.get().isLostReport()) {
            return false;
        }
        this.reportStatus = "MATCHED";
        this.matchedItemId = itemId;
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                """
                UPDATE lost_reports SET report_status = ?, matched_item_id = ?
                WHERE report_id = ?
                  AND UPPER(COALESCE(report_status, '')) IN ('PENDING', 'UNDER_REVIEW', 'ON_HOLD')
                """
        )) {
            ps.setString(1, "MATCHED");
            ps.setInt(2, itemId);
            ps.setInt(3, this.reportId);
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
                    // Blank fields must NOT count as matches (that caused whole-category false hits).
                    boolean categoryOk = notBlank(category)
                            && category.equalsIgnoreCase(item.getCategory());
                    boolean colorOk = notBlank(color)
                            && color.equalsIgnoreCase(item.getColor());
                    boolean brandOk = notBlank(brand)
                            && brand.equalsIgnoreCase(item.getBrand());
                    boolean nameOk = notBlank(itemName) && notBlank(item.getItemName())
                            && (item.getItemName().toLowerCase(Locale.ENGLISH)
                            .contains(itemName.toLowerCase(Locale.ENGLISH))
                            || itemName.toLowerCase(Locale.ENGLISH)
                            .contains(item.getItemName().toLowerCase(Locale.ENGLISH)));
                    // Require same category plus at least one real attribute overlap.
                    return categoryOk && (colorOk || brandOk || nameOk);
                })
                .collect(Collectors.toList());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** Another open Lost report already sticky-matched to this inventory item. */
    public static Optional<Report> findOtherLostMatchedToItem(int itemId, int excludeReportId) {
        if (itemId <= 0) return Optional.empty();
        return loadAll().stream()
                .filter(Report::isLostReport)
                .filter(r -> r.reportId != excludeReportId)
                .filter(r -> r.matchedItemId != null && r.matchedItemId == itemId)
                .filter(r -> {
                    String s = r.reportStatus == null ? "" : r.reportStatus.toUpperCase(Locale.ENGLISH);
                    return "MATCHED".equals(s) || "PENDING".equals(s)
                            || "UNDER_REVIEW".equals(s) || "ON_HOLD".equals(s);
                })
                .findFirst();
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

    /**
     * Same student already has an open report with the same item name + type
     * (PENDING / UNDER_REVIEW / ON_HOLD / MATCHED) — used to stop spam submissions.
     */
    public static boolean hasOpenDuplicate(int userId, String itemName, String reportType) {
        if (userId <= 0 || itemName == null || itemName.isBlank()) return false;
        String nameKey = itemName.trim().toLowerCase(Locale.ENGLISH);
        String typeKey = reportType == null || reportType.isBlank()
                ? "LOST" : reportType.trim().toUpperCase(Locale.ENGLISH);
        return loadAll().stream()
                .filter(r -> r.userId == userId)
                .filter(r -> typeKey.equalsIgnoreCase(
                        r.reportType == null || r.reportType.isBlank() ? "LOST" : r.reportType))
                .filter(r -> r.itemName != null
                        && r.itemName.trim().toLowerCase(Locale.ENGLISH).equals(nameKey))
                .anyMatch(r -> {
                    String s = r.reportStatus == null ? "" : r.reportStatus.toUpperCase(Locale.ENGLISH);
                    return "PENDING".equals(s) || "UNDER_REVIEW".equals(s)
                            || "ON_HOLD".equals(s) || "MATCHED".equals(s);
                });
    }

    /**
     * Active for Lost↔Found same-item checks. Ignores Claimed / Returned / Cancelled / Archived.
     */
    public static boolean isActiveForCrossTypeCheck(Report report) {
        if (report == null) return false;
        String s = report.reportStatus == null ? "" : report.reportStatus.toUpperCase(Locale.ENGLISH);
        return !"CLAIMED".equals(s)
                && !"RETURNED".equals(s)
                && !"CANCELLED".equals(s)
                && !"ARCHIVED".equals(s);
    }

    /**
     * Why this student cannot file the new report as the opposite type for the same item,
     * or {@code null} if allowed. Different items (e.g. lost earbud + found jacket) are OK.
     */
    public static String crossTypeSameItemBlockReason(
            int userId,
            String newReportType,
            String itemName,
            String category,
            String color,
            String brand
    ) {
        if (userId <= 0 || itemName == null || itemName.isBlank()) return null;
        String typeKey = newReportType == null || newReportType.isBlank()
                ? "LOST" : newReportType.trim().toUpperCase(Locale.ENGLISH);
        String opposite = "FOUND".equals(typeKey) ? "LOST" : "FOUND";

        Optional<Report> conflict = loadAll().stream()
                .filter(r -> r.userId == userId)
                .filter(r -> opposite.equalsIgnoreCase(
                        r.reportType == null || r.reportType.isBlank() ? "LOST" : r.reportType))
                .filter(Report::isActiveForCrossTypeCheck)
                .filter(r -> looksLikeSameItem(r, itemName, category, color, brand))
                .findFirst();

        if (conflict.isEmpty()) return null;

        Report other = conflict.get();
        if ("LOST".equals(typeKey)) {
            return "You already have an active Found report for a similar item ("
                    + other.getDisplayId() + " — " + nullSafe(other.getItemName()) + ").\n\n"
                    + "You cannot also file a Lost report for the same item on this account. "
                    + "That stops turning an item in and then claiming it as your own.";
        }
        return "You already have an active Lost report for a similar item ("
                + other.getDisplayId() + " — " + nullSafe(other.getItemName()) + ").\n\n"
                + "You cannot also file a Found report for the same item on this account.";
    }

    /** Same-item heuristic: name overlap plus matching category / color / brand when present. */
    static boolean looksLikeSameItem(
            Report existing,
            String itemName,
            String category,
            String color,
            String brand
    ) {
        if (existing == null || !nameOverlaps(existing.itemName, itemName)) {
            return false;
        }
        boolean exactName = existing.itemName != null
                && existing.itemName.trim().equalsIgnoreCase(itemName.trim());
        boolean categoryOk = blank(existing.category) || blank(category)
                || existing.category.trim().equalsIgnoreCase(category.trim());
        boolean colorOk = blank(existing.color) || blank(color)
                || existing.color.trim().equalsIgnoreCase(color.trim())
                || containsEither(existing.color, color);
        boolean brandOk = blank(existing.brand) || blank(brand)
                || existing.brand.trim().equalsIgnoreCase(brand.trim())
                || containsEither(existing.brand, brand);

        if (exactName && categoryOk) return true;
        // Fuzzy name: need category match and at least one of color/brand (when both sides set).
        if (!categoryOk) return false;
        boolean colorBothSet = !blank(existing.color) && !blank(color);
        boolean brandBothSet = !blank(existing.brand) && !blank(brand);
        if (colorBothSet || brandBothSet) {
            return (colorBothSet && colorOk) || (brandBothSet && brandOk) || exactName;
        }
        // Names overlap and category matches — treat as same item.
        return true;
    }

    private static boolean nameOverlaps(String a, String b) {
        if (blank(a) || blank(b)) return false;
        String x = a.trim().toLowerCase(Locale.ENGLISH);
        String y = b.trim().toLowerCase(Locale.ENGLISH);
        if (x.equals(y) || x.contains(y) || y.contains(x)) return true;
        for (String token : x.split("[^a-z0-9]+")) {
            if (token.length() < 3) continue;
            if (y.contains(token)) return true;
        }
        return KeywordSynonyms.sharesMeaning(a, b);
    }

    private static boolean containsEither(String a, String b) {
        if (blank(a) || blank(b)) return false;
        String x = a.trim().toLowerCase(Locale.ENGLISH);
        String y = b.trim().toLowerCase(Locale.ENGLISH);
        return x.contains(y) || y.contains(x);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullSafe(String s) {
        return blank(s) ? "—" : s;
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
                       description, lost_location, lost_date, report_status, date_submitted,
                       COALESCE(report_type, 'LOST') AS report_type, photo_path, photo_data, matched_item_id
                FROM lost_reports
                """;
    }

    private static void bindPhotoData(PreparedStatement ps, int index, byte[] data) throws SQLException {
        if (data == null || data.length == 0) {
            ps.setBytes(index, null);
        } else {
            ps.setBytes(index, data);
        }
    }

    private static Report map(ResultSet rs) throws SQLException {
        String photo = null;
        try {
            photo = rs.getString("photo_path");
        } catch (SQLException ignored) {
            // column may not exist yet on first connection before migrate
        }
        Integer matchedItem = null;
        try {
            matchedItem = Db.getInteger(rs, "matched_item_id");
        } catch (SQLException ignored) {
            // column added by migrateSchema
        }
        Report report = new Report(
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
                Db.getDate(rs, "date_submitted"),
                rs.getString("report_type"),
                photo,
                matchedItem
        );
        try {
            report.setPhotoData(rs.getBytes("photo_data"));
        } catch (SQLException ignored) {
            // column added by migrateSchema
        }
        return report;
    }
}
