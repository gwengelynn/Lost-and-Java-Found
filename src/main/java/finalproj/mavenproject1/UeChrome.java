package finalproj.mavenproject1;

import java.net.URL;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

/**
 * Applies shared UE stylesheet. Does <b>not</b> wrap or replace scene roots —
 * layout stays exactly as Login / Signup / Admin / Student built it.
 * Toast overlays an existing StackPane already inside that root.
 */
final class UeChrome {

    static final String TOAST_HOST_ID = "ue-toast-host";

    private static final String STYLESHEET = "/finalproj/mavenproject1/ue-theme.css";

    private UeChrome() {}

    static void apply(Scene scene) {
        if (scene == null) return;
        String url = stylesheetUrl();
        if (url == null) return;
        if (!scene.getStylesheets().contains(url)) {
            scene.getStylesheets().add(url);
        }
    }

    static void apply(DialogPane pane) {
        if (pane == null) return;
        String url = stylesheetUrl();
        if (url == null) return;
        if (!pane.getStylesheets().contains(url)) {
            pane.getStylesheets().add(url);
        }
    }

    /** Scene root is used as-is — no wrapper StackPane. */
    static Scene scene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        apply(scene);
        return scene;
    }

    /**
     * Find an existing StackPane to draw toasts on:
     * Login/Signup root, or Admin/Student {@code layered} center stack.
     * Never inserts a new scene root.
     */
    static StackPane toastHost(Window window) {
        if (window == null) return null;
        Scene scene = window.getScene();
        if (scene == null) return null;
        Parent root = scene.getRoot();
        if (root instanceof StackPane sp) {
            return sp;
        }
        if (root instanceof BorderPane bp) {
            Node center = bp.getCenter();
            if (center instanceof StackPane sp) {
                return sp;
            }
        }
        return findFirstStackPane(root);
    }

    private static StackPane findFirstStackPane(Node node) {
        if (node instanceof StackPane sp) {
            return sp;
        }
        if (node instanceof Pane pane) {
            for (Node child : pane.getChildren()) {
                StackPane found = findFirstStackPane(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String stylesheetUrl() {
        URL res = UeChrome.class.getResource(STYLESHEET);
        return res == null ? null : res.toExternalForm();
    }
}
