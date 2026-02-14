package app.persistence;

import java.nio.file.Path;

/**
 * Persists and loads app settings (JSON).
 */
public interface SettingsStore {

    AppSettings load();

    void save(AppSettings settings);

    Path getSettingsPath();
}
