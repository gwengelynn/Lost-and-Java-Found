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
 * INVENTORY — Found-item management (MySQL: items + storage_boxes).
 */
public class Inventory {

    private int itemId;
    private int adminUserId;
    private String trackingNumber;
    private String itemName;
    private String category;
    private String subCategory;
    private String brand;
    private String color;
    private String description;
    private String privateDetails;
    private LocalDate foundDate;
    private String storageBox;
    private String storageLocation;
    private String itemStatus;
    private String photoPath;
    /** Raw image bytes stored in MySQL (LONGBLOB photo_data). */
    private byte[] photoData;

    public Inventory() {}

    public Inventory(int itemId, int adminUserId, String trackingNumber, String itemName,
                     String category, String subCategory, String brand, String color,
                     String description, String privateDetails, LocalDate foundDate,
                     String storageBox, String storageLocation, String itemStatus) {
        this(itemId, adminUserId, trackingNumber, itemName, category, subCategory, brand, color,
                description, privateDetails, foundDate, storageBox, storageLocation, itemStatus, null);
    }

    public Inventory(int itemId, int adminUserId, String trackingNumber, String itemName,
                     String category, String subCategory, String brand, String color,
                     String description, String privateDetails, LocalDate foundDate,
                     String storageBox, String storageLocation, String itemStatus, String photoPath) {
        this.itemId = itemId;
        this.adminUserId = adminUserId;
        this.trackingNumber = trackingNumber;
        this.itemName = itemName;
        this.category = category;
        this.subCategory = subCategory;
        this.brand = brand;
        this.color = color;
        this.description = description;
        this.privateDetails = privateDetails;
        this.foundDate = foundDate;
        this.storageBox = storageBox;
        this.storageLocation = storageLocation;
        this.itemStatus = itemStatus;
        this.photoPath = photoPath;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getAdminUserId() { return adminUserId; }
    public void setAdminUserId(int adminUserId) { this.adminUserId = adminUserId; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrivateDetails() { return privateDetails; }
    public void setPrivateDetails(String privateDetails) { this.privateDetails = privateDetails; }

    public LocalDate getFoundDate() { return foundDate; }
    public void setFoundDate(LocalDate foundDate) { this.foundDate = foundDate; }

    public String getStorageBox() { return storageBox; }
    public void setStorageBox(String storageBox) { this.storageBox = storageBox; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public byte[] getPhotoData() { return photoData; }
    public void setPhotoData(byte[] photoData) { this.photoData = photoData; }

    /** Sets path and loads file bytes for MySQL BLOB storage. */
    public void setPhoto(String storedPath) {
        this.photoPath = storedPath;
        this.photoData = PhotoStorage.readBytes(storedPath);
    }

    public boolean registerItem() {
        if (adminUserId <= 0 || itemName == null || itemName.isBlank()) return false;
        if (category == null || category.isBlank()) return false;
        if (this.foundDate == null) this.foundDate = LocalDate.now();
        if (this.itemStatus == null || this.itemStatus.isBlank()) {
            this.itemStatus = "AVAILABLE";
        }
        if (this.storageBox == null || this.storageBox.isBlank()) {
            this.storageBox = "Box A";
        }
        // Block spam / redundant inventory rows (same active item already logged).
        if (findDuplicateActive(
                this.itemName, this.category, this.color, this.brand, this.privateDetails
        ).isPresent()) {
            return false;
        }

        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try {
            int boxId = DatabaseConnection.resolveBoxId(connection, this.storageBox, this.storageLocation);
            if (this.trackingNumber == null || this.trackingNumber.isBlank()) {
                this.trackingNumber = generateTrackingNumber(connection);
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO items
                    (admin_user_id, box_id, tracking_number, item_name, category, sub_category, brand,
                     color, description, private_details, found_date, item_status, photo_path, photo_data)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                ps.setInt(1, this.adminUserId);
                ps.setInt(2, boxId);
                ps.setString(3, this.trackingNumber);
                ps.setString(4, this.itemName.trim());
                ps.setString(5, this.category.trim());
                ps.setString(6, Db.blankToNull(this.subCategory));
                ps.setString(7, Db.blankToNull(this.brand));
                ps.setString(8, Db.blankToNull(this.color));
                ps.setString(9, Db.blankToNull(this.description));
                ps.setString(10, Db.blankToNull(this.privateDetails));
                Db.setDate(ps, 11, this.foundDate);
                ps.setString(12, this.itemStatus);
                ps.setString(13, Db.blankToNull(this.photoPath));
                bindPhotoData(ps, 14, this.photoData);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) return false;
                    this.itemId = keys.getInt(1);
                }
            }

            // Keep location text on the box row when provided.
            if (this.storageLocation != null && !this.storageLocation.isBlank()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE storage_boxes SET box_location = ? WHERE box_id = ?"
                )) {
                    ps.setString(1, this.storageLocation.trim());
                    ps.setInt(2, boxId);
                    ps.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean updateItem() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try {
            int boxId = DatabaseConnection.resolveBoxId(connection, this.storageBox, this.storageLocation);
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    UPDATE items
                    SET box_id = ?, item_name = ?, category = ?, sub_category = ?, brand = ?, color = ?,
                        description = ?, private_details = ?, found_date = ?, photo_path = ?, photo_data = ?
                    WHERE item_id = ?
                    """
            )) {
                ps.setInt(1, boxId);
                ps.setString(2, this.itemName);
                ps.setString(3, this.category);
                ps.setString(4, Db.blankToNull(this.subCategory));
                ps.setString(5, Db.blankToNull(this.brand));
                ps.setString(6, Db.blankToNull(this.color));
                ps.setString(7, Db.blankToNull(this.description));
                ps.setString(8, Db.blankToNull(this.privateDetails));
                Db.setDate(ps, 9, this.foundDate);
                ps.setString(10, Db.blankToNull(this.photoPath));
                bindPhotoData(ps, 11, this.photoData);
                ps.setInt(12, this.itemId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean removeItem() {
        this.itemStatus = "ARCHIVED";
        return updateItemStatus();
    }

    public void viewItemDetails() {
        System.out.println(trackingNumber + " | " + itemName + " | " + itemStatus
                + " | private: " + privateDetails);
    }

    public List<Inventory> searchItem() {
        String key = itemName == null ? "" : itemName.trim().toLowerCase(Locale.ENGLISH);
        return loadAll().stream()
                .filter(i -> !"ARCHIVED".equalsIgnoreCase(i.itemStatus))
                .filter(i -> key.isEmpty()
                        || i.itemName.toLowerCase(Locale.ENGLISH).contains(key)
                        || (i.trackingNumber != null
                        && i.trackingNumber.toLowerCase(Locale.ENGLISH).contains(key))
                        || (i.brand != null && i.brand.toLowerCase(Locale.ENGLISH).contains(key)))
                .collect(Collectors.toList());
    }

    public List<Inventory> filterItems() {
        return loadAll().stream()
                .filter(i -> this.category == null || this.category.isBlank()
                        || this.category.equalsIgnoreCase(i.category))
                .filter(i -> this.itemStatus == null || this.itemStatus.isBlank()
                        || this.itemStatus.equalsIgnoreCase(i.itemStatus))
                .filter(i -> this.storageBox == null || this.storageBox.isBlank()
                        || this.storageBox.equalsIgnoreCase(i.storageBox))
                .sorted(Comparator.comparing(Inventory::getFoundDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public String generateTrackingNumber() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) {
            return String.format("TRK-%d-%04d", LocalDate.now().getYear(), (int) (Math.random() * 9999));
        }
        try {
            return generateTrackingNumber(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            return String.format("TRK-%d-%04d", LocalDate.now().getYear(), (int) (Math.random() * 9999));
        } finally {
            Db.closeQuietly(connection);
        }
    }

    private static String generateTrackingNumber(Connection connection) throws SQLException {
        int next = 1;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(item_id), 0) + 1 FROM items")) {
            if (rs.next()) next = rs.getInt(1);
        }
        return String.format("TRK-%d-%04d", LocalDate.now().getYear(), next);
    }

    public boolean assignStorageBox() {
        if (this.storageBox == null || this.storageBox.isBlank()) return false;
        Optional<Inventory> stored = findById(this.itemId);
        if (stored.isEmpty()) return false;
        Inventory item = stored.get();
        item.storageBox = this.storageBox;
        if (this.storageLocation != null) {
            item.storageLocation = this.storageLocation;
        }
        return item.updateItem();
    }

    public boolean updateItemStatus() {
        if (this.itemStatus == null || this.itemStatus.isBlank()) return false;
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE items SET item_status = ? WHERE item_id = ?"
        )) {
            ps.setString(1, this.itemStatus);
            ps.setInt(2, this.itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            Db.closeQuietly(connection);
        }
    }

    public boolean checkItemAvailability() {
        Optional<Inventory> stored = findById(this.itemId);
        return stored.isPresent() && "AVAILABLE".equalsIgnoreCase(stored.get().itemStatus);
    }

    public List<Inventory> compareWithReports() {
        return loadAll().stream()
                .filter(i -> "AVAILABLE".equalsIgnoreCase(i.itemStatus)
                        || "CLAIM_PENDING".equalsIgnoreCase(i.itemStatus))
                .collect(Collectors.toList());
    }

    public static Optional<Inventory> findById(int itemId) {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) return Optional.empty();

        try (PreparedStatement ps = connection.prepareStatement(
                baseSelect() + " WHERE i.item_id = ? LIMIT 1"
        )) {
            ps.setInt(1, itemId);
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
     * Finds an already-active inventory row that matches this physical item.
     * Same name + category + color + brand (ignores unique-mark wording differences)
     * so admins cannot re-register the same item by tweaking private details.
     */
    public static Optional<Inventory> findDuplicateActive(
            String itemName, String category, String color, String brand, String privateDetails
    ) {
        if (itemName == null || itemName.isBlank()) return Optional.empty();
        String nameKey = norm(itemName);
        String catKey = norm(category);
        String colorKey = norm(color);
        String brandKey = norm(brand);

        return loadAll().stream()
                .filter(i -> {
                    String status = i.itemStatus == null ? "" : i.itemStatus.toUpperCase(Locale.ENGLISH);
                    return "AVAILABLE".equals(status)
                            || "CLAIM_PENDING".equals(status)
                            || "CLAIMED".equals(status);
                })
                .filter(i -> nameKey.equals(norm(i.itemName)))
                .filter(i -> catKey.isEmpty() || catKey.equals(norm(i.category)))
                .filter(i -> colorKey.equals(norm(i.color)))
                .filter(i -> brandKey.equals(norm(i.brand)))
                .findFirst();
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    private static List<Inventory> loadAll() {
        List<Inventory> list = new ArrayList<>();
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
                SELECT i.item_id, i.admin_user_id, i.tracking_number, i.item_name, i.category,
                       i.sub_category, i.brand, i.color, i.description, i.private_details,
                       i.found_date, i.item_status, i.photo_path, i.photo_data, b.box_name, b.box_location
                FROM items i
                LEFT JOIN storage_boxes b ON b.box_id = i.box_id
                """;
    }

    private static void bindPhotoData(PreparedStatement ps, int index, byte[] data) throws SQLException {
        if (data == null || data.length == 0) {
            ps.setBytes(index, null);
        } else {
            ps.setBytes(index, data);
        }
    }

    private static Inventory map(ResultSet rs) throws SQLException {
        String photo = null;
        try {
            photo = rs.getString("photo_path");
        } catch (SQLException ignored) {
            // older schema
        }
        Inventory item = new Inventory(
                rs.getInt("item_id"),
                rs.getInt("admin_user_id"),
                rs.getString("tracking_number"),
                rs.getString("item_name"),
                rs.getString("category"),
                rs.getString("sub_category"),
                rs.getString("brand"),
                rs.getString("color"),
                rs.getString("description"),
                rs.getString("private_details"),
                Db.getDate(rs, "found_date"),
                rs.getString("box_name"),
                rs.getString("box_location"),
                rs.getString("item_status"),
                photo
        );
        try {
            item.setPhotoData(rs.getBytes("photo_data"));
        } catch (SQLException ignored) {
            // column added by migrateSchema
        }
        return item;
    }
}
