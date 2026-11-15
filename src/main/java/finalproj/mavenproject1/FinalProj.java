package finalproj.mavenproject1;
import java.sql.Connection;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FinalProj extends Application {

    @Override
    public void start(Stage stage) {
        LoginView loginView = new LoginView(stage);
        Scene scene = new Scene(loginView.getRoot(), 1100, 680);

        stage.setTitle("Lost and Java-Found — Login");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.show();
        
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Connection test successful!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Warning: could not open MySQL connection at startup.");
        }
    }
    

    public static void main(String[] args) {
        launch(args);
    }
}