/*
 * SleepArchiver - cross-platform data manager for Sleeptracker-series watches.
 * Copyright (C) 2009-2011 Pavel Fatin <http://pavelfatin.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.pavelfatin.sleeparchiver.gui.main;

import com.pavelfatin.sleeparchiver.gui.conditions.ConditionsDialog;
import com.pavelfatin.sleeparchiver.gui.download.DownloadDialog;
import com.pavelfatin.sleeparchiver.gui.info.InfoDialog;
import com.pavelfatin.sleeparchiver.gui.main.commands.*;
import com.pavelfatin.sleeparchiver.gui.main.render.*;
import com.pavelfatin.sleeparchiver.gui.night.NightDialog;
import com.pavelfatin.sleeparchiver.gui.preferences.PreferencesDialog;
import com.pavelfatin.sleeparchiver.model.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import com.pavelfatin.sleeparchiver.model.Device;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static com.pavelfatin.sleeparchiver.lang.I18n.t;

public class MainView extends BorderPane {
    private static final String APP_NAME = "SleepArchiver";
    private static final String APP_VERSION = "2.0.0";

    private final Stage _stage;
    private Preferences _preferences;

    private final ObservableList<Night> _nights = FXCollections.observableArrayList();
    private ListView<Night> _listView;
    private final NightRenderer _renderer = new NightRenderer();

    private final Invoker _invoker = new Invoker();
    private final BooleanProperty _undoEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty _redoEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty _editEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty _removeEnabled = new SimpleBooleanProperty(false);

    private Document _document;
    private StatusBar _statusBar;

    private final List<Transform> _transforms;
    private final List<Zoom> _zooms;
    private Transform _currentTransform;
    private Zoom _currentZoom;
    private int _zoomIndex;

    private MenuItem _undoMenuItem;
    private MenuItem _redoMenuItem;
    private Menu _recentMenu;
    private ComboBox<String> _portCombo;
    private ComboBox<Integer> _baudCombo;

    public MainView(Stage stage, Document document, Preferences preferences) {
        _stage = stage;
        _preferences = preferences;

        _transforms = createTransforms();
        _zooms = createZooms();
        _currentTransform = _transforms.getFirst();
        _zoomIndex = 2; // 100%
        _currentZoom = _zooms.get(_zoomIndex);

        _renderer.setTransform(_currentTransform);
        _renderer.setResolution(_currentZoom.resolution());

        setTop(createMenuAndToolBar());
        _listView = createListView();
        setCenter(_listView);
        _statusBar = new StatusBar();
        setBottom(_statusBar);

        _nights.addListener((ListChangeListener<Night>) c -> {
            updateRenderer();
            updateTitle();
            updateStatusBar();
        });

        _stage.setOnCloseRequest(this::onCloseRequest);

        setDocument(document);
    }

    private javafx.scene.layout.VBox createMenuAndToolBar() {
        javafx.scene.layout.VBox top = new javafx.scene.layout.VBox();
        top.getChildren().addAll(createMenuBar(), createToolBar());
        return top;
    }

    // ---- Menu ----

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu(t("menu.file"));
        fileMenu.getItems().addAll(
                menuItem(t("action.new"), "Ctrl+N", e -> blank()),
                menuItem(t("action.open"), "Ctrl+O", e -> open()),
                createRecentMenu(),
                new SeparatorMenuItem(),
                menuItem(t("action.save"), "Ctrl+S", e -> save()),
                menuItem(t("action.saveAs"), "Ctrl+Shift+S", e -> saveAs()),
                new SeparatorMenuItem(),
                menuItem(t("action.import"), null, e -> importData()),
                menuItem(t("action.export"), null, e -> exportData()),
                new SeparatorMenuItem(),
                menuItem(t("action.exit"), null, e -> exit())
        );

        Menu editMenu = new Menu(t("menu.edit"));
        _undoMenuItem = menuItem(t("action.undo"), "Ctrl+Z", e -> undo());
        _undoMenuItem.disableProperty().bind(_undoEnabled.not());
        _redoMenuItem = menuItem(t("action.redo"), "Ctrl+Y", e -> redo());
        _redoMenuItem.disableProperty().bind(_redoEnabled.not());
        editMenu.getItems().addAll(
                _undoMenuItem,
                _redoMenuItem,
                new SeparatorMenuItem(),
                menuItem(t("action.selectAll"), "Ctrl+A", e -> selectAll())
        );

        Menu nightsMenu = new Menu(t("menu.nights"));
        MenuItem addItem = menuItem(t("action.add"), null, e -> add());
        MenuItem editItem = menuItem(t("action.edit"), null, e -> edit());
        editItem.disableProperty().bind(_editEnabled.not());
        MenuItem removeItem = menuItem(t("action.remove"), null, e -> remove());
        removeItem.disableProperty().bind(_removeEnabled.not());
        nightsMenu.getItems().addAll(
                addItem,
                editItem,
                removeItem,
                new SeparatorMenuItem(),
                menuItem(t("action.acquire"), "Ctrl+I", e -> download()),
                new SeparatorMenuItem(),
                menuItem(t("action.conditions"), "Ctrl+D", e -> conditions()),
                menuItem(t("action.preferences"), "Ctrl+P", e -> preferences())
        );

        Menu helpMenu = new Menu(t("menu.help"));
        helpMenu.getItems().addAll(
                menuItem(t("action.about"), "F1", e -> about()),
                menuItem(t("action.license"), null, e -> license())
        );

        menuBar.getMenus().addAll(fileMenu, editMenu, nightsMenu, helpMenu);
        return menuBar;
    }

    private Menu createRecentMenu() {
        _recentMenu = new Menu(t("menu.reopen"));
        updateRecentMenu();
        return _recentMenu;
    }

    private void updateRecentMenu() {
        _recentMenu.getItems().clear();
        for (String path : _preferences.getRecentFiles()) {
            MenuItem item = new MenuItem(path);
            item.setOnAction(e -> {
                if (isUserDataSafe()) {
                    File file = new File(path);
                    addRecent(file);
                    doOpen(file);
                }
            });
            _recentMenu.getItems().add(item);
        }
        if (!_preferences.getRecentFiles().isEmpty()) {
            _recentMenu.getItems().add(new SeparatorMenuItem());
            _recentMenu.getItems().add(menuItem(t("action.clearList"), null, e -> clearRecent()));
        }
        _recentMenu.setDisable(!_preferences.hasRecentFiles());
    }

    private MenuItem menuItem(String text, String accelerator, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        MenuItem item = new MenuItem(text);
        if (accelerator != null) {
            item.setAccelerator(KeyCombination.valueOf(accelerator));
        }
        item.setOnAction(handler);
        return item;
    }

    // ---- ToolBar ----

    private ToolBar createToolBar() {
        ComboBox<Zoom> zoomCombo = new ComboBox<>(FXCollections.observableArrayList(_zooms));
        zoomCombo.setValue(_currentZoom);
        zoomCombo.setOnAction(e -> {
            _currentZoom = zoomCombo.getValue();
            _zoomIndex = _zooms.indexOf(_currentZoom);
            updateRenderer();
        });

        ComboBox<Transform> transformCombo = new ComboBox<>(FXCollections.observableArrayList(_transforms));
        transformCombo.setValue(_currentTransform);
        transformCombo.setOnAction(e -> {
            _currentTransform = transformCombo.getValue();
            _renderer.setTransform(_currentTransform);
            updateRenderer();
        });

        Button addBtn = new Button(t("toolbar.add"));
        addBtn.setOnAction(e -> add());
        Button editBtn = new Button(t("toolbar.edit"));
        editBtn.disableProperty().bind(_editEnabled.not());
        editBtn.setOnAction(e -> edit());
        Button removeBtn = new Button(t("toolbar.remove"));
        removeBtn.disableProperty().bind(_removeEnabled.not());
        removeBtn.setOnAction(e -> remove());

        _portCombo = new ComboBox<>();
        _portCombo.setPromptText(t("toolbar.serialPort"));
        _portCombo.setPrefWidth(250);
        refreshPorts();

        _baudCombo = new ComboBox<>(FXCollections.observableArrayList(
                2400, 4800, 9600, 19200, 38400, 57600, 115200));
        _baudCombo.setValue(19200);
        _baudCombo.setPrefWidth(90);

        Button refreshBtn = new Button(t("toolbar.refresh"));
        refreshBtn.setOnAction(e -> refreshPorts());

        Button acquireBtn = new Button(t("toolbar.acquire"));
        acquireBtn.setOnAction(e -> download());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(addBtn, editBtn, removeBtn, new Separator(),
                _portCombo, _baudCombo, refreshBtn, acquireBtn, new Separator(),
                spacer, transformCombo, zoomCombo);
        return toolBar;
    }

    private void refreshPorts() {
        String selected = _portCombo.getValue();
        _portCombo.getItems().clear();
        _portCombo.getItems().addAll(Device.listPorts());
        if (selected != null && _portCombo.getItems().contains(selected)) {
            _portCombo.setValue(selected);
        }
    }

    // ---- ListView ----

    private ListView<Night> createListView() {
        ListView<Night> listView = new ListView<>(_nights);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(lv -> new NightCell(_renderer, 900));

        listView.getSelectionModel().getSelectedIndices().addListener(
                (ListChangeListener<Integer>) c -> {
                    updateListActions();
                    updateStatusBar();
                });

        listView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                if (_editEnabled.get()) {
                    edit();
                }
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && _editEnabled.get()) {
                edit();
            } else if (e.getCode() == KeyCode.DELETE && _removeEnabled.get()) {
                remove();
            } else if (e.getCode() == KeyCode.INSERT) {
                add();
            }
        });

        listView.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                if (e.getDeltaY() > 0) {
                    zoomIn();
                } else if (e.getDeltaY() < 0) {
                    zoomOut();
                }
                e.consume();
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem ctxEdit = new MenuItem(t("ctx.edit"));
        ctxEdit.disableProperty().bind(_editEnabled.not());
        ctxEdit.setOnAction(e -> edit());
        MenuItem ctxRemove = new MenuItem(t("ctx.remove"));
        ctxRemove.disableProperty().bind(_removeEnabled.not());
        ctxRemove.setOnAction(e -> remove());
        MenuItem ctxAdd = new MenuItem(t("ctx.add"));
        ctxAdd.setOnAction(e -> add());
        MenuItem ctxDownload = new MenuItem(t("ctx.acquire"));
        ctxDownload.setOnAction(e -> download());
        contextMenu.getItems().addAll(ctxEdit, ctxRemove, new SeparatorMenuItem(), ctxAdd, ctxDownload);
        listView.setContextMenu(contextMenu);

        return listView;
    }

    // ---- Document ----

    void setDocument(Document document) {
        _document = document;

        _nights.setAll(_document.getNights());
        if (!_nights.isEmpty()) {
            _listView.getSelectionModel().selectLast();
            _listView.scrollTo(_nights.size() - 1);
        }

        _invoker.reset();

        updateTitle();
        updateCommandActions();
        updateListActions();
        updateStatusBar();

        _listView.requestFocus();
    }

    private boolean isModified() {
        return !_document.getNights().equals(new ArrayList<>(_nights));
    }

    private void updateTitle() {
        String docName = _document.isNew() ? t("title.untitled") : _document.getName();
        String modification = isModified() ? " *" : "";
        _stage.setTitle(String.format("%s - %s %s%s", docName, APP_NAME, APP_VERSION, modification));
    }

    // ---- Commands ----

    private void invoke(Command command) {
        _invoker.invoke(command);
        updateCommandActions();
        int idx = _listView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            _listView.scrollTo(idx);
        }
    }

    private void updateCommandActions() {
        _undoEnabled.set(_invoker.isUndoAvailable());
        _redoEnabled.set(_invoker.isRedoAvailable());

        String undo = t("action.undo") + (_invoker.isUndoAvailable() ? " " + _invoker.getUndoCommandName() : "");
        _undoMenuItem.setText(undo);

        String redo = t("action.redo") + (_invoker.isRedoAvailable() ? " " + _invoker.getRedoCommandName() : "");
        _redoMenuItem.setText(redo);
    }

    private void updateListActions() {
        MultipleSelectionModel<Night> sel = _listView.getSelectionModel();
        _editEnabled.set(sel.getSelectedIndices().size() == 1);
        _removeEnabled.set(!sel.getSelectedIndices().isEmpty());
    }

    private void updateStatusBar() {
        int count = _nights.size();
        MultipleSelectionModel<Night> sel = _listView.getSelectionModel();
        String status;
        if (count > 0) {
            if (sel.getSelectedIndices().size() == 1) {
                status = t("status.position", sel.getSelectedIndex() + 1, count);
            } else {
                status = t("status.selection", sel.getSelectedIndices().size(), count);
            }
        } else {
            status = t("status.empty");
        }
        _statusBar.setStatus(status);
    }

    private void updateRenderer() {
        _renderer.setTransform(_currentTransform);
        _renderer.setResolution(_currentZoom.resolution());
        _renderer.setNights(new ArrayList<>(_nights));
        _listView.refresh();
    }

    // ---- Zoom ----

    private List<Zoom> createZooms() {
        List<Zoom> result = new ArrayList<>();
        for (int zoom : new int[]{50, 70, 100, 140, 200}) {
            result.add(new Zoom(10.0 * zoom / (60.0 * 10.0), String.format("%d%%", zoom)));
        }
        return result;
    }

    private List<Transform> createTransforms() {
        List<Transform> result = new ArrayList<>();
        result.add(new RelativeTransform(t("transform.relative")));
        result.add(new AbsoluteTransform(t("transform.absolute")));
        return result;
    }

    private void zoomIn() {
        if (_zoomIndex < _zooms.size() - 1) {
            _zoomIndex++;
            _currentZoom = _zooms.get(_zoomIndex);
            updateRenderer();
        }
    }

    private void zoomOut() {
        if (_zoomIndex > 0) {
            _zoomIndex--;
            _currentZoom = _zooms.get(_zoomIndex);
            updateRenderer();
        }
    }

    // ---- Actions ----

    private void blank() {
        if (isUserDataSafe()) {
            setDocument(new Document());
        }
    }

    private void open() {
        if (isUserDataSafe()) {
            FileChooser chooser = createDocumentFileChooser();
            chooser.setTitle(t("dialog.openDatabase"));
            File file = chooser.showOpenDialog(_stage);
            if (file != null) {
                addRecent(file);
                doOpen(file);
            }
        }
    }

    public void doOpen(File file) {
        try {
            setDocument(Document.load(file));
        } catch (FileNotFoundException e) {
            showError(t("error.openDatabase"), t("error.fileNotFound", file.getPath()));
        } catch (IOException e) {
            showError(t("error.openDatabase"), t("error.readFile", file.getPath()));
        }
    }

    private boolean save() {
        if (_document.isNew()) {
            return saveAs();
        } else {
            return doSave(_document.getLocation());
        }
    }

    private boolean saveAs() {
        FileChooser chooser = createDocumentFileChooser();
        chooser.setTitle(t("dialog.saveDatabase"));
        File file = chooser.showSaveDialog(_stage);
        if (file != null) {
            if (isSafeToWrite(file)) {
                addRecent(file);
                return doSave(file);
            }
        }
        return false;
    }

    private boolean doSave(File file) {
        try {
            Document document = new Document(new ArrayList<>(_nights));
            document.saveAs(file, _preferences.isBackupsEnabled());
            _document = document;
            updateTitle();
            return true;
        } catch (FileNotFoundException e) {
            showError(t("error.saveDatabase"), t("error.saveNotFound", file.getPath()));
            return false;
        } catch (IOException e) {
            showError(t("error.saveDatabase"), t("error.writeFile", file.getPath()));
            return false;
        }
    }

    private void importData() {
        FileChooser chooser = createDataFileChooser();
        chooser.setTitle(t("dialog.importData"));
        File file = chooser.showOpenDialog(_stage);
        if (file != null) {
            try {
                List<Night> nights = Document.importData(file);
                invoke(new Importing(t("command.importing"), _nights,
                        _listView.getSelectionModel(), Night.getComparator(), nights));
            } catch (IOException e) {
                showError(t("error.importData"), t("error.importRead", file.getPath()));
            }
        }
    }

    private void exportData() {
        FileChooser chooser = createDataFileChooser();
        chooser.setTitle(t("dialog.exportData"));
        File file = chooser.showSaveDialog(_stage);
        if (file != null) {
            if (isSafeToWrite(file)) {
                try {
                    Document.exportData(file, new ArrayList<>(_nights));
                } catch (IOException e) {
                    showError(t("error.exportData"), t("error.exportWrite", file.getPath()));
                }
            }
        }
    }

    private void add() {
        Night prototype = createNewNight();
        NightDialog dialog = new NightDialog(_stage, prototype, true, getAllConditions());
        Optional<Night> result = dialog.showAndWait();
        result.ifPresent(night ->
                invoke(new Addition(t("command.insertion"), _nights,
                        _listView.getSelectionModel(), Night.getComparator(), night)));
    }

    private void edit() {
        int index = _listView.getSelectionModel().getSelectedIndex();
        if (index < 0) return;
        Night night = _nights.get(index);

        NightDialog dialog = new NightDialog(_stage, night, false, getAllConditions());
        Optional<Night> result = dialog.showAndWait();
        result.ifPresent(data ->
                invoke(new Editing(t("command.editing"), _nights,
                        _listView.getSelectionModel(), Night.getComparator(), data)));
    }

    private void remove() {
        invoke(new Removal(t("command.removal"), _nights, _listView.getSelectionModel()));
    }

    private void download() {
        String selected = _portCombo.getValue();
        String portName = null;
        if (selected != null && !selected.isEmpty()) {
            portName = selected.substring(0, selected.indexOf(' '));
        }
        int baudRate = _baudCombo.getValue() != null ? _baudCombo.getValue() : 19200;
        DownloadDialog dialog = new DownloadDialog(_stage, java.time.LocalDate.now().getYear(), portName, baudRate);
        Optional<Night> result = dialog.showAndWait();
        result.ifPresent(this::doAddNight);
    }

    private void doAddNight(Night prototype) {
        NightDialog dialog = new NightDialog(_stage, prototype, true, getAllConditions());
        Optional<Night> result = dialog.showAndWait();
        result.ifPresent(night ->
                invoke(new Addition(t("command.insertion"), _nights,
                        _listView.getSelectionModel(), Night.getComparator(), night)));
    }

    private void undo() {
        _invoker.undo();
        updateCommandActions();
    }

    private void redo() {
        _invoker.redo();
        updateCommandActions();
    }

    private void selectAll() {
        if (!_nights.isEmpty()) {
            _listView.getSelectionModel().selectAll();
        }
    }

    private void conditions() {
        ConditionsDialog dialog = new ConditionsDialog(_stage, new ArrayList<>(_nights));
        Optional<List<Night>> result = dialog.showAndWait();
        result.ifPresent(data ->
                invoke(new Replacing(t("command.editingConditions"), _nights,
                        _listView.getSelectionModel(), data)));
    }

    private void preferences() {
        PreferencesDialog dialog = new PreferencesDialog(_stage, _preferences);
        dialog.showAndWait();
    }

    private void about() {
        InfoDialog dialog = new InfoDialog(_stage, t("title.about"),
                "/com/pavelfatin/sleeparchiver/resources/about.html", false);
        dialog.showAndWait();
    }

    private void license() {
        InfoDialog dialog = new InfoDialog(_stage, t("title.license"),
                "/com/pavelfatin/sleeparchiver/resources/license.html", true);
        dialog.showAndWait();
    }

    private void exit() {
        if (isUserDataSafe()) {
            _stage.close();
        }
    }

    private void clearRecent() {
        _preferences.clearRecentFiles();
        try {
            _preferences.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        updateRecentMenu();
    }

    // ---- Helpers ----

    private Night createNewNight() {
        LocalTime alarm = null;
        int window = 20;
        LocalTime toBed = null;
        if (_preferences.isPrefillEnabled() && !_nights.isEmpty()) {
            Night last = _nights.getLast();
            alarm = last.getAlarm();
            window = last.getWindow();
            toBed = last.getToBed();
        }
        return new Night(LocalDate.now(), alarm, window, toBed, new ArrayList<>());
    }

    private List<String> getAllConditions() {
        Set<String> unique = new HashSet<>();
        for (Night night : _nights) {
            unique.addAll(night.getConditions());
        }
        List<String> sorted = new ArrayList<>(unique);
        Collections.sort(sorted);
        return sorted;
    }

    private void addRecent(File file) {
        _preferences.addRecentFile(file.getPath());
        try {
            _preferences.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        updateRecentMenu();
    }

    private boolean isUserDataSafe() {
        if (!_nights.isEmpty() && isModified()) {
            String docName = _document.isNew() ? t("title.untitled") : _document.getName();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    t("unsaved.message", docName),
                    ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            alert.setTitle(t("unsaved.title"));
            alert.setHeaderText(null);
            alert.initOwner(_stage);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == ButtonType.YES) return save();
                if (result.get() == ButtonType.NO) return true;
            }
            return false;
        }
        return true;
    }

    private boolean isSafeToWrite(File file) {
        if (file.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    t("fileExists.message", file.getPath()),
                    ButtonType.YES, ButtonType.NO);
            alert.setTitle(t("fileExists.title"));
            alert.setHeaderText(null);
            alert.initOwner(_stage);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.YES;
        }
        return true;
    }

    private FileChooser createDocumentFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(t("file.database"), "*.xmz"));
        if (_preferences.hasRecentFiles()) {
            File dir = _preferences.getRecentDirectory();
            if (dir != null && dir.exists()) {
                chooser.setInitialDirectory(dir);
            }
        }
        return chooser;
    }

    private FileChooser createDataFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(t("file.csv"), "*.csv"));
        return chooser;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(_stage);
        alert.showAndWait();
    }

    private void onCloseRequest(WindowEvent event) {
        if (!isUserDataSafe()) {
            event.consume();
        }
    }
}
