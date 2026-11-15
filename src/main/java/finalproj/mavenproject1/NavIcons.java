
package finalproj.mavenproject1;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;

/**
 * Lively outline nav icons for the student sidebar (mockup-style).
 */
public final class NavIcons {

    private NavIcons() {}

    public static Node forNav(String key) {
        return switch (key) {
            case "DASHBOARD" -> dashboard();
            case "REPORTS", "MANAGE_REPORTS" -> reports();
            case "CLAIMS", "MANAGE_CLAIMS" -> claims();
            case "NOTIFICATIONS" -> notifications();
            case "PROFILE", "USERS" -> profile();
            case "INVENTORY" -> inventory();
            case "ANALYTICS" -> analytics();
            case "SETTINGS" -> settings();
            default -> dashboard();
        };
    }

    public static void applyColor(Node iconRoot, boolean active) {
        Color color = active ? Color.WHITE : Color.web(UiTheme.TEXT);
        paint(iconRoot, color);
    }

    private static void paint(Node node, Color color) {
        if (node instanceof Shape shape) {
            shape.setStroke(color);
            // Outline icons stay hollow; lines never fill
            if (!(shape instanceof Line)) {
                shape.setFill(Color.TRANSPARENT);
            }
        } else if (node instanceof Group group) {
            for (Node child : group.getChildren()) {
                paint(child, color);
            }
        } else if (node instanceof StackPane pane) {
            for (Node child : pane.getChildren()) {
                paint(child, color);
            }
        }
    }

    private static Node wrap(Group drawing) {
        StackPane box = new StackPane(drawing);
        box.setMinSize(22, 22);
        box.setPrefSize(22, 22);
        box.setMaxSize(22, 22);
        box.setMouseTransparent(true);
        // Scale drawing slightly to fit the tighter box
        drawing.setScaleX(0.82);
        drawing.setScaleY(0.82);
        return box;
    }

    private static void stroke(Shape shape) {
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(Color.web(UiTheme.TEXT));
        shape.setStrokeWidth(2.2);
        shape.getStrokeDashArray().clear();
        shape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
    }

    private static Node dashboard() {
        // House / home outline
        SVGPath path = new SVGPath();
        path.setContent(
                "M 4 13 L 14 4 L 24 13 "
                + "M 7 12 L 7 23 L 12 23 L 12 16 L 16 16 L 16 23 L 21 23 L 21 12"
        );
        stroke(path);
        return wrap(new Group(path));
    }

    private static Node reports() {
        // Document with lines
        Rectangle page = new Rectangle(7, 3, 14, 20);
        page.setArcWidth(3);
        page.setArcHeight(3);
        stroke(page);

        Line l1 = new Line(11, 9, 19, 9);
        Line l2 = new Line(11, 13, 19, 13);
        Line l3 = new Line(11, 17, 17, 17);
        stroke(l1);
        stroke(l2);
        stroke(l3);
        return wrap(new Group(page, l1, l2, l3));
    }

    private static Node claims() {
        // Clipboard
        Rectangle board = new Rectangle(7, 6, 14, 17);
        board.setArcWidth(3);
        board.setArcHeight(3);
        stroke(board);

        Rectangle clip = new Rectangle(11, 2, 6, 5);
        clip.setArcWidth(2);
        clip.setArcHeight(2);
        stroke(clip);

        Line l1 = new Line(11, 12, 19, 12);
        Line l2 = new Line(11, 16, 17, 16);
        stroke(l1);
        stroke(l2);
        return wrap(new Group(board, clip, l1, l2));
    }

    private static Node notifications() {
        // Bell
        SVGPath bell = new SVGPath();
        bell.setContent(
                "M 14 3 C 11 3 9 6 9 9 L 9 14 L 6 19 L 22 19 L 19 14 L 19 9 "
                + "C 19 6 17 3 14 3 Z "
                + "M 12 21 C 12.5 22.2 13.5 23 14 23 C 14.5 23 15.5 22.2 16 21"
        );
        stroke(bell);
        return wrap(new Group(bell));
    }

    private static Node profile() {
        // Person silhouette outline
        Circle head = new Circle(14, 9, 4.5);
        stroke(head);

        SVGPath body = new SVGPath();
        body.setContent("M 6 23 C 6 17.5 10 15 14 15 C 18 15 22 17.5 22 23");
        stroke(body);
        return wrap(new Group(head, body));
    }

    private static Node settings() {
        // Gear
        SVGPath gear = new SVGPath();
        gear.setContent(
                "M 14 5 L 15.2 8.2 L 18.5 7.2 L 20.8 9.5 L 19.8 12.8 L 23 14 "
                + "L 19.8 15.2 L 20.8 18.5 L 18.5 20.8 L 15.2 19.8 L 14 23 "
                + "L 12.8 19.8 L 9.5 20.8 L 7.2 18.5 L 8.2 15.2 L 5 14 "
                + "L 8.2 12.8 L 7.2 9.5 L 9.5 7.2 L 12.8 8.2 Z"
        );
        stroke(gear);

        Circle hole = new Circle(14, 14, 3.2);
        stroke(hole);
        return wrap(new Group(gear, hole));
    }

    private static Node inventory() {
        // Storage box
        Rectangle box = new Rectangle(6, 9, 16, 13);
        box.setArcWidth(2);
        box.setArcHeight(2);
        stroke(box);
        Line lid = new Line(5, 9, 23, 9);
        stroke(lid);
        Line flap = new Line(14, 5, 14, 9);
        stroke(flap);
        Line seam = new Line(14, 9, 14, 22);
        stroke(seam);
        return wrap(new Group(box, lid, flap, seam));
    }

    private static Node analytics() {
        // Simple bar chart
        Rectangle b1 = new Rectangle(7, 14, 4, 8);
        Rectangle b2 = new Rectangle(12, 9, 4, 13);
        Rectangle b3 = new Rectangle(17, 11, 4, 11);
        stroke(b1);
        stroke(b2);
        stroke(b3);
        Line axis = new Line(6, 22, 22, 22);
        stroke(axis);
        return wrap(new Group(b1, b2, b3, axis));
    }
}