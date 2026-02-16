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

package com.pavelfatin.sleeparchiver.gui.preferences;

import com.pavelfatin.sleeparchiver.model.Language;
import com.pavelfatin.sleeparchiver.model.Preferences;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

import static com.pavelfatin.sleeparchiver.lang.I18n.t;

public class PreferencesDialog extends Dialog<Void> {
    private final Preferences _preferences;
    private final ComboBox<Language> _language;
    private final CheckBox _backups;
    private final CheckBox _prefill;
    private final CheckBox _historyEnabled;
    private final Spinner<Integer> _historyLimit;
    private final CheckBox _openRecent;

    private final CheckBox _debugLogging;
    private final CheckBox _manualGrid;
    private final ComboBox<Integer> _gridFrom;
    private final ComboBox<Integer> _gridTo;

    private final ToggleGroup _displayGroup;
    private final RadioButton _displayMonthRadio;
    private final RadioButton _displayDaysRadio;

    private final ToggleGroup _sortGroup;
    private final RadioButton _sortDescRadio;
    private final RadioButton _sortAscRadio;

    public PreferencesDialog(Stage owner, Preferences preferences) {
        _preferences = preferences;
        initOwner(owner);
        setTitle(t("preferences.title"));
        setResizable(false);

        _language = new ComboBox<>();
        _language.getItems().addAll(Language.values());
        _language.setValue(preferences.getLanguage());

        _backups = new CheckBox(t("preferences.backups"));
        _backups.setSelected(preferences.isBackupsEnabled());

        _prefill = new CheckBox(t("preferences.prefill"));
        _prefill.setSelected(preferences.isPrefillEnabled());

        _historyEnabled = new CheckBox(t("preferences.historyEnabled"));
        _historyEnabled.setSelected(preferences.isHistoryEnabled());

        _historyLimit = new Spinner<>(1, 15, preferences.getHistoryLimit());
        _historyLimit.setPrefWidth(70);

        _openRecent = new CheckBox(t("preferences.openRecent"));
        _openRecent.setSelected(preferences.isOpenRecentEnabled());

        _historyEnabled.selectedProperty().addListener((obs, oldV, newV) -> {
            _historyLimit.setDisable(!newV);
            _openRecent.setDisable(!newV);
        });
        _historyLimit.setDisable(!preferences.isHistoryEnabled());
        _openRecent.setDisable(!preferences.isHistoryEnabled());

        // Debug logging
        _debugLogging = new CheckBox(t("preferences.debugLogging"));
        _debugLogging.setSelected(preferences.isDebugLogging());

        // Grid section
        _manualGrid = new CheckBox(t("preferences.manualGrid"));
        _manualGrid.setSelected(preferences.isManualGrid());

        _gridFrom = new ComboBox<>(FXCollections.observableArrayList(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23));
        _gridFrom.setValue(preferences.getGridStartHour());
        _gridFrom.setPrefWidth(70);

        _gridTo = new ComboBox<>(FXCollections.observableArrayList(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23));
        _gridTo.setValue(preferences.getGridEndHour());
        _gridTo.setPrefWidth(70);

        _manualGrid.selectedProperty().addListener((obs, oldV, newV) -> {
            _gridFrom.setDisable(!newV);
            _gridTo.setDisable(!newV);
        });
        _gridFrom.setDisable(!preferences.isManualGrid());
        _gridTo.setDisable(!preferences.isManualGrid());

        // Display section
        _displayGroup = new ToggleGroup();
        _displayMonthRadio = new RadioButton(t("preferences.displayMonth"));
        _displayMonthRadio.setToggleGroup(_displayGroup);
        _displayDaysRadio = new RadioButton(t("preferences.displayDays"));
        _displayDaysRadio.setToggleGroup(_displayGroup);

        if ("days".equals(preferences.getDisplayMode())) {
            _displayDaysRadio.setSelected(true);
        } else {
            _displayMonthRadio.setSelected(true);
        }

        // Sort order section
        _sortGroup = new ToggleGroup();
        _sortDescRadio = new RadioButton(t("preferences.sortDesc"));
        _sortDescRadio.setToggleGroup(_sortGroup);
        _sortAscRadio = new RadioButton(t("preferences.sortAsc"));
        _sortAscRadio.setToggleGroup(_sortGroup);

        if ("asc".equals(preferences.getSortOrder())) {
            _sortAscRadio.setSelected(true);
        } else {
            _sortDescRadio.setSelected(true);
        }

        // Layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // General pane
        TitledPane generalPane = new TitledPane();
        generalPane.setText(t("preferences.general"));
        generalPane.setCollapsible(false);
        GridPane general = new GridPane();
        general.setHgap(10);
        general.setVgap(5);
        general.setPadding(new Insets(5));
        general.add(new Label(t("preferences.language")), 0, 0);
        general.add(_language, 1, 0);
        general.add(new Label(t("preferences.restart")), 2, 0);
        general.add(_backups, 0, 1, 3, 1);
        general.add(_prefill, 0, 2, 3, 1);
        general.add(_debugLogging, 0, 3, 3, 1);
        generalPane.setContent(general);

        // History pane
        TitledPane historyPane = new TitledPane();
        historyPane.setText(t("preferences.history"));
        historyPane.setCollapsible(false);
        VBox history = new VBox(5);
        history.setPadding(new Insets(5));
        GridPane historyGrid = new GridPane();
        historyGrid.setHgap(10);
        historyGrid.add(new Label(t("preferences.historyLimit")), 0, 0);
        historyGrid.add(_historyLimit, 1, 0);
        history.getChildren().addAll(_historyEnabled, _openRecent, historyGrid);
        historyPane.setContent(history);

        // Grid pane
        TitledPane gridPane = new TitledPane();
        gridPane.setText(t("preferences.grid"));
        gridPane.setCollapsible(false);
        VBox gridBox = new VBox(5);
        gridBox.setPadding(new Insets(5));
        HBox gridHours = new HBox(10,
                new Label(t("preferences.gridFrom")), _gridFrom,
                new Label(t("preferences.gridTo")), _gridTo);
        gridBox.getChildren().addAll(_manualGrid, gridHours);
        gridPane.setContent(gridBox);

        // Display pane
        TitledPane displayPane = new TitledPane();
        displayPane.setText(t("preferences.display"));
        displayPane.setCollapsible(false);
        VBox displayBox = new VBox(5);
        displayBox.setPadding(new Insets(5));
        displayBox.getChildren().addAll(_displayMonthRadio, _displayDaysRadio,
                new Separator(),
                new Label(t("preferences.sortOrder")), _sortDescRadio, _sortAscRadio);
        displayPane.setContent(displayBox);

        content.getChildren().addAll(generalPane, historyPane, gridPane, displayPane);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                applyPreferences();
            }
            return null;
        });
    }

    private void applyPreferences() {
        _preferences.setLanguage(_language.getValue());
        _preferences.setBackupsEnabled(_backups.isSelected());
        _preferences.setPrefillEnabled(_prefill.isSelected());
        _preferences.setHistoryEnabled(_historyEnabled.isSelected());
        _preferences.setHistoryLimit(_historyLimit.getValue());
        _preferences.setOpenRecentEnabled(_openRecent.isSelected());

        _preferences.setDebugLogging(_debugLogging.isSelected());
        _preferences.setManualGrid(_manualGrid.isSelected());
        _preferences.setGridStartHour(_gridFrom.getValue());
        _preferences.setGridEndHour(_gridTo.getValue());

        _preferences.setDisplayMode(_displayDaysRadio.isSelected() ? "days" : "month");
        _preferences.setSortOrder(_sortAscRadio.isSelected() ? "asc" : "desc");

        try {
            _preferences.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
