package finalproj.mavenproject1;

import java.util.concurrent.atomic.AtomicLong;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Auto-logout after idle time on shared campus PCs.
 * Attach after login; detach on the login screen.
 */
final class IdleSessionGuard {

    /** Minutes with no mouse/keyboard before forced logout. */
    static final int IDLE_MINUTES = 10;

    private static Timeline watcher;
    private static final AtomicLong lastActivityMs = new AtomicLong(System.currentTimeMillis());
    private static Runnable onIdle;
    private static Stage boundStage;

    private IdleSessionGuard() {}

    static void attach(Stage stage, Runnable idleAction) {
        detach();
        if (stage == null || idleAction == null) return;
        boundStage = stage;
        onIdle = idleAction;
        lastActivityMs.set(System.currentTimeMillis());

        Scene scene = stage.getScene();
        if (scene != null) {
            scene.addEventFilter(MouseEvent.ANY, e -> touch());
            scene.addEventFilter(KeyEvent.ANY, e -> touch());
        }
        stage.sceneProperty().addListener((obs, oldSc, newSc) -> {
            if (newSc != null) {
                newSc.addEventFilter(MouseEvent.ANY, e -> touch());
                newSc.addEventFilter(KeyEvent.ANY, e -> touch());
                touch();
            }
        });

        watcher = new Timeline(new KeyFrame(Duration.seconds(15), e -> checkIdle()));
        watcher.setCycleCount(Timeline.INDEFINITE);
        watcher.play();
    }

    static void touch() {
        lastActivityMs.set(System.currentTimeMillis());
    }

    static void detach() {
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }
        onIdle = null;
        boundStage = null;
    }

    private static void checkIdle() {
        if (onIdle == null) return;
        long idleMs = System.currentTimeMillis() - lastActivityMs.get();
        long limitMs = IDLE_MINUTES * 60_000L;
        if (idleMs < limitMs) return;

        Runnable action = onIdle;
        Stage stage = boundStage;
        detach();
        Platform.runLater(() -> {
            if (stage != null) {
                Toast.info(stage,
                        "Signed out after " + IDLE_MINUTES
                                + " minutes of inactivity — protects shared campus computers.");
            }
            action.run();
        });
    }
}
