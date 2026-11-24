package finalproj.mavenproject1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Copies selected photos into a local uploads folder and returns a stored relative path. */
final class PhotoStorage {

    private PhotoStorage() {}

    static Path uploadsRoot() {
        Path root = Path.of(System.getProperty("user.home"), "lost-and-java-found", "uploads");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            System.out.println("Could not create uploads folder: " + e.getMessage());
        }
        return root;
    }

    /** Opens a file chooser; returns stored relative path or null if cancelled / failed. */
    static String chooseAndStore(Window owner, String prefix) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        java.io.File selected = chooser.showOpenDialog(owner);
        if (selected == null) return null;
        return store(selected.toPath(), prefix);
    }

    static String store(Path source, String prefix) {
        if (source == null || !Files.isRegularFile(source)) return null;
        String name = source.getFileName().toString();
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            ext = name.substring(dot).toLowerCase(Locale.ENGLISH);
        }
        String safePrefix = (prefix == null || prefix.isBlank()) ? "photo" : prefix.replaceAll("[^a-zA-Z0-9_-]", "");
        String fileName = safePrefix + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path dest = uploadsRoot().resolve(fileName);
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            System.out.println("Photo store failed: " + e.getMessage());
            return null;
        }
    }

    static Path resolve(String storedName) {
        if (storedName == null || storedName.isBlank()) return null;
        Path path = uploadsRoot().resolve(storedName.trim());
        return Files.isRegularFile(path) ? path : null;
    }

    /** Reads bytes for a stored upload file (for saving into MySQL LONGBLOB). */
    static byte[] readBytes(String storedName) {
        Path path = resolve(storedName);
        if (path == null) return null;
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            System.out.println("Photo read failed: " + e.getMessage());
            return null;
        }
    }

    /** Deletes a previously stored upload file if it exists. */
    static void delete(String storedName) {
        Path path = resolve(storedName);
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.out.println("Photo delete failed: " + e.getMessage());
        }
    }

    static Image loadImage(String storedName, double fitW, double fitH) {
        return loadImage(storedName, null, fitW, fitH);
    }

    /** Prefers the local file; falls back to DB blob bytes when the file is missing. */
    static Image loadImage(String storedName, byte[] photoData, double fitW, double fitH) {
        Path path = resolve(storedName);
        if (path != null) {
            try {
                Image image = new Image(path.toUri().toString(), fitW, fitH, true, true);
                if (!image.isError()) return image;
            } catch (Exception ignored) {
                // fall through to blob
            }
        }
        return loadImage(photoData, fitW, fitH);
    }

    static Image loadImage(byte[] photoData, double fitW, double fitH) {
        if (photoData == null || photoData.length == 0) return null;
        try {
            Image image = new Image(new ByteArrayInputStream(photoData), fitW, fitH, true, true);
            if (image.isError()) return null;
            return image;
        } catch (Exception e) {
            return null;
        }
    }
}
