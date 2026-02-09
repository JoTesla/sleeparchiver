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

package com.pavelfatin.sleeparchiver.gui.conditions;

import com.pavelfatin.sleeparchiver.model.Night;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;

import static com.pavelfatin.sleeparchiver.lang.I18n.t;

public class ConditionsDialog extends Dialog<List<Night>> {
    private final ListView<String> _listView;
    private final ObservableList<String> _conditions;
    private List<Night> _nights;

    public ConditionsDialog(Stage owner, List<Night> nights) {
        initOwner(owner);
        setTitle(t("conditions.title"));
        setResizable(true);

        _nights = new ArrayList<>(nights);

        _conditions = FXCollections.observableArrayList();
        _listView = new ListView<>(_conditions);
        _listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        _listView.setPrefSize(350, 250);

        _listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && _listView.getSelectionModel().getSelectedItem() != null) {
                editCondition();
            }
        });

        Button editBtn = new Button(t("action.edit"));
        editBtn.setMaxWidth(Double.MAX_VALUE);
        editBtn.disableProperty().bind(
                _listView.getSelectionModel().selectedItemProperty().isNull());
        editBtn.setOnAction(e -> editCondition());

        Button removeBtn = new Button(t("action.remove"));
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.disableProperty().bind(
                _listView.getSelectionModel().selectedItemProperty().isNull());
        removeBtn.setOnAction(e -> removeConditions());

        VBox buttons = new VBox(5, editBtn, removeBtn);
        buttons.setPadding(new Insets(0, 0, 0, 5));

        HBox main = new HBox(5, _listView, buttons);
        HBox.setHgrow(_listView, Priority.ALWAYS);
        main.setPadding(new Insets(10));

        getDialogPane().setContent(main);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        updateConditions();

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return Collections.unmodifiableList(_nights);
            }
            return null;
        });
    }

    private void updateConditions() {
        Set<String> unique = new TreeSet<>();
        for (Night night : _nights) {
            unique.addAll(night.getConditions());
        }
        _conditions.setAll(unique);
        if (!_conditions.isEmpty()) {
            _listView.getSelectionModel().selectFirst();
        }
    }

    private void editCondition() {
        String selected = _listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog input = new TextInputDialog(selected);
        input.initOwner(getDialogPane().getScene().getWindow());
        input.setTitle(t("conditions.editTitle"));
        input.setHeaderText(null);
        input.setContentText(t("conditions.editLabel"));

        Optional<String> result = input.showAndWait();
        result.ifPresent(replacement -> {
            if (!replacement.trim().isEmpty() && !replacement.equals(selected)) {
                String trimmed = replacement.trim();
                if (isSafeToReplace(selected, trimmed)) {
                    _nights = doEdit(_nights, selected, trimmed);
                    updateConditions();
                    _listView.getSelectionModel().select(trimmed);
                }
            }
        });
    }

    private boolean isSafeToReplace(String condition, String replacement) {
        if (_conditions.contains(replacement)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    t("conditions.existsMessage", replacement, condition, replacement),
                    ButtonType.YES, ButtonType.NO);
            alert.setTitle(t("conditions.existsTitle"));
            alert.setHeaderText(null);
            alert.initOwner(getDialogPane().getScene().getWindow());
            Optional<ButtonType> r = alert.showAndWait();
            return r.isPresent() && r.get() == ButtonType.YES;
        }
        return true;
    }

    private List<Night> doEdit(List<Night> nights, String condition, String replacement) {
        List<Night> result = new ArrayList<>();
        for (Night night : nights) {
            List<String> conditions = new ArrayList<>(night.getConditions());
            if (conditions.contains(condition)) {
                conditions.set(conditions.indexOf(condition), replacement);
            }
            Set<String> unique = new TreeSet<>(conditions);
            result.add(night.with(new ArrayList<>(unique)));
        }
        return result;
    }

    private void removeConditions() {
        List<String> selected = new ArrayList<>(_listView.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;

        int idx = _listView.getSelectionModel().getSelectedIndex();
        _nights = doRemove(_nights, selected);
        updateConditions();

        int size = _conditions.size();
        if (size > 0) {
            _listView.getSelectionModel().select(Math.min(idx, size - 1));
        }
    }

    private List<Night> doRemove(List<Night> nights, List<String> conditions) {
        List<Night> result = new ArrayList<>();
        for (Night night : nights) {
            List<String> copy = new ArrayList<>(night.getConditions());
            copy.removeAll(conditions);
            result.add(night.with(copy));
        }
        return result;
    }
}
