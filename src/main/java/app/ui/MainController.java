package app.ui;

import app.core.BatchItem;
import app.core.BatchRunner;
import app.core.CloudConvertFacade;
import app.core.CloudConvertFacadeImpl;
import app.core.ConversionProfile;
import app.core.Profiles;
import app.core.Validation;
import app.persistence.AppSettings;
import app.persistence.JsonSettingsStore;
import app.persistence.SettingsStore;
import app.ui.model.BatchItemFx;
import app.ui.model.ConversionProfileFx;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main controller for the File Converter UI.
 */
public class MainController implements Initializable {

    @FXML
    private ComboBox<ConversionProfileFx> profileCombo;
    @FXML
    private TextField outputDirField;
    @FXML
    private Button outputDirButton;
    @FXML
    private Spinner<Integer> concurrencySpinner;
    @FXML
    private Button addFilesButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button startButton;
    @FXML
    private Button cancelButton;
    @FXML
    private CheckBox skipIneligibleCheck;
    @FXML
    private TableView<BatchItemFx> batchTable;
    @FXML
    private TableColumn<BatchItemFx, String> fileColumn;
    @FXML
    private TableColumn<BatchItemFx, String> typeColumn;
    @FXML
    private TableColumn<BatchItemFx, String> profileColumn;
    @FXML
    private TableColumn<BatchItemFx, String> statusColumn;
    @FXML
    private TableColumn<BatchItemFx, Number> progressColumn;
    @FXML
    private TableColumn<BatchItemFx, String> outputColumn;
    @FXML
    private TableColumn<BatchItemFx, String> messageColumn;
    @FXML
    private Button saveLogButton;
    @FXML
    private TextArea logArea;

    private final ObservableList<BatchItemFx> batchItems = FXCollections.observableArrayList();
    private final SettingsStore settingsStore = new JsonSettingsStore();
    private BatchRunner batchRunner;
    private Path outputDir;
    private final ExecutorService uiExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        profileCombo.setItems(FXCollections.observableArrayList(
                new ConversionProfileFx(Profiles.MOD_TO_MOV),
                new ConversionProfileFx(Profiles.JPEG_TO_WEBP),
                new ConversionProfileFx(Profiles.DOCX_TO_PDF)));
        profileCombo.getSelectionModel().selectFirst();
        concurrencySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, 2));
        batchTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        batchTable.setItems(batchItems);
        fileColumn.setCellValueFactory(cell -> {
            Path p = cell.getValue().getItem().input;
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> p != null ? p.getFileName().toString() : "");
        });
        typeColumn.setCellValueFactory(cell -> {
            Path p = cell.getValue().getItem().input;
            return javafx.beans.binding.Bindings.createStringBinding(() -> {
                if (p == null) {
                    return "";
                }
                String n = p.getFileName().toString();
                int i = n.lastIndexOf('.');
                return i >= 0 ? n.substring(i + 1) : "";
            });
        });
        profileColumn.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
                () -> cell.getValue().getItem().profile.displayName()));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        outputColumn.setCellValueFactory(new PropertyValueFactory<>("outputPath"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        loadSettings();
        outputDirButton.setOnAction(e -> chooseOutputDir());
        addFilesButton.setOnAction(e -> addFiles());
        removeButton.setOnAction(e -> removeSelected());
        clearButton.setOnAction(e -> clearAll());
        startButton.setOnAction(e -> startBatch());
        cancelButton.setOnAction(e -> cancelBatch());
        saveLogButton.setOnAction(e -> saveLog());
    }

    private void loadSettings() {
        AppSettings s = settingsStore.load();
        if (s.lastOutputDir() != null) {
            outputDir = s.lastOutputDir();
            outputDirField.setText(outputDir.toString());
        }
        if (s.lastProfileId() != null) {
            for (ConversionProfileFx pf : profileCombo.getItems()) {
                if (s.lastProfileId().equals(pf.getProfile().id())) {
                    profileCombo.getSelectionModel().select(pf);
                    break;
                }
            }
        }
    }

    private void saveSettings() {
        ConversionProfileFx selected = profileCombo.getSelectionModel().getSelectedItem();
        settingsStore.save(new AppSettings(
                settingsStore.load().apiKey(),
                outputDir,
                selected != null ? selected.getProfile().id() : "mod-mov"));
    }

    private void chooseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        if (outputDir != null) {
            chooser.setInitialDirectory(outputDir.toFile());
        }
        File dir = chooser.showDialog(outputDirButton.getScene().getWindow());
        if (dir != null) {
            outputDir = dir.toPath();
            outputDirField.setText(outputDir.toString());
            saveSettings();
        }
    }

    private void addFiles() {
        if (outputDir == null) {
            showAlert("Select output directory first");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select files to convert");
        List<File> files = chooser.showOpenMultipleDialog(addFilesButton.getScene().getWindow());
        if (files != null) {
            ConversionProfile profile = getSelectedProfile();
            for (File f : files) {
                BatchItem item = new BatchItem(f.toPath(), outputDir, profile);
                Validation.ValidationResult vr = Validation.validate(item);
                if (!vr.valid() && skipIneligibleCheck.isSelected()) {
                    item.status = "Skipped";
                    item.message = vr.message();
                }
                batchItems.add(new BatchItemFx(item));
            }
            log("Added " + files.size() + " file(s)");
        }
    }

    private void removeSelected() {
        List<BatchItemFx> selected = batchTable.getSelectionModel().getSelectedItems();
        batchItems.removeAll(selected);
    }

    private void clearAll() {
        batchItems.clear();
        logArea.clear();
    }

    private void startBatch() {
        if (outputDir == null) {
            showAlert("Select output directory first");
            return;
        }
        String apiKey = settingsStore.load().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("CLOUDCONVERT_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            showAlert("Set CLOUDCONVERT_API_KEY or configure API key in ~/.file-converter/settings.json");
            return;
        }
        try {
            CloudConvertFacade facade = new CloudConvertFacadeImpl(apiKey);
            batchRunner = new BatchRunner(facade, concurrencySpinner.getValue());
            List<BatchItem> items = batchItems.stream()
                    .map(BatchItemFx::getItem)
                    .filter(i -> !"Skipped".equals(i.status) && !"Failed".equals(i.status))
                    .toList();
            saveSettings();
            uiExecutor.submit(() -> {
                batchRunner.run(items);
                Platform.runLater(() -> {
                    batchItems.forEach(BatchItemFx::syncFromItem);
                    log("Batch completed");
                });
            });
        } catch (Exception e) {
            showAlert("Failed to start: " + e.getMessage());
        }
    }

    private void cancelBatch() {
        if (batchRunner != null) {
            batchRunner.cancel();
        }
    }

    private void saveLog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save log");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
        File f = chooser.showSaveDialog(saveLogButton.getScene().getWindow());
        if (f != null) {
            try {
                java.nio.file.Files.writeString(f.toPath(), logArea.getText());
            } catch (Exception e) {
                showAlert("Failed to save: " + e.getMessage());
            }
        }
    }

    private ConversionProfile getSelectedProfile() {
        ConversionProfileFx pf = profileCombo.getSelectionModel().getSelectedItem();
        return pf != null ? pf.getProfile() : Profiles.MOD_TO_MOV;
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    private void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setContentText(msg);
            a.show();
        });
    }
}
