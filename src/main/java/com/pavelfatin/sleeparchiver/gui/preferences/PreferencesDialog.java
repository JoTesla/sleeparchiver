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
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
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

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

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
        generalPane.setContent(general);

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

        content.getChildren().addAll(generalPane, historyPane);

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
        try {
            _preferences.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
