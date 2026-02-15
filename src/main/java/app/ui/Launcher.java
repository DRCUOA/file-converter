package app.ui;

import java.util.Locale;

/**
 * Plain Java entry point for packaged/classpath runs.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("mac")) {
            System.setProperty("apple.awt.application.name", "Converto");
        }
        MainApp.main(args);
    }
}
