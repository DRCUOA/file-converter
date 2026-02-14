package app.ui.model;

import app.core.BatchItem;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * JavaFX property wrapper for BatchItem.
 */
public class BatchItemFx {

    private final BatchItem item;
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final StringProperty outputPath = new SimpleStringProperty();

    public BatchItemFx(BatchItem item) {
        this.item = item;
        status.set(item.status);
        message.set(item.message);
        progress.set(item.progress);
        outputPath.set(item.outputPath != null ? item.outputPath.toString() : "");
    }

    public BatchItem getItem() {
        return item;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public StringProperty outputPathProperty() {
        return outputPath;
    }

    public void syncFromItem() {
        status.set(item.status);
        message.set(item.message);
        progress.set(item.progress);
        outputPath.set(item.outputPath != null ? item.outputPath.toString() : "");
    }
}
