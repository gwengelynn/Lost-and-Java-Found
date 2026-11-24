package finalproj.mavenproject1;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/** Copies short desk codes / IDs to the system clipboard. */
final class ClipboardUtil {

    private ClipboardUtil() {}

    static boolean copyText(String text) {
        if (text == null || text.isBlank()) return false;
        ClipboardContent content = new ClipboardContent();
        content.putString(text.trim());
        Clipboard.getSystemClipboard().setContent(content);
        return true;
    }
}
