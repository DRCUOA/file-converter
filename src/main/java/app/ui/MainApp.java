package app.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.awt.Taskbar;
import java.net.URL;
import java.util.Locale;

/**
 * JavaFX application entry point for Converto.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("Converto");
        try (InputStream iconStream = getClass().getResourceAsStream("/icons/app-icon.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        }
        configureDockIcon();
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("mac")) {
            System.setProperty("apple.awt.application.name", "Converto");
        }
        launch(args);
    }

    private void configureDockIcon() {
        try {
            if (!Taskbar.isTaskbarSupported()) {
                return;
            }
            Taskbar taskbar = Taskbar.getTaskbar();
            if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                return;
            }
            URL iconUrl = getClass().getResource("/icons/app-icon.png");
            if (iconUrl != null) {
                taskbar.setIconImage(ImageIO.read(iconUrl));
            }
        } catch (Exception ignored) {
            // Non-mac platforms or restricted environments may not support Taskbar icon updates.
        }
    }
}
