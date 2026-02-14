package app.ui.model;

import app.core.ConversionProfile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * JavaFX property wrapper for ConversionProfile (for dropdown binding).
 */
public class ConversionProfileFx {

    private final ConversionProfile profile;
    private final StringProperty displayName = new SimpleStringProperty();

    public ConversionProfileFx(ConversionProfile profile) {
        this.profile = profile;
        displayName.set(profile.displayName());
    }

    public ConversionProfile getProfile() {
        return profile;
    }

    public StringProperty displayNameProperty() {
        return displayName;
    }

    @Override
    public String toString() {
        return profile.displayName();
    }
}
