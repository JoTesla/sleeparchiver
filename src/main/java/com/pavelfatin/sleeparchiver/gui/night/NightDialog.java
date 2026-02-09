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

package com.pavelfatin.sleeparchiver.gui.night;

import com.pavelfatin.sleeparchiver.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.pavelfatin.sleeparchiver.lang.I18n.t;

public class NightDialog extends Dialog<Night> {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final DatePicker _date;
    private final TextField _alarm;
    private final ComboBox<Integer> _window;
    private final TextField _toBed;
    private final TextField _momentField;
    private final ListView<LocalTime> _moments;
    private final ObservableList<LocalTime> _momentsList;
    private final ComboBox<String> _conditionCombo;
    private final ListView<String> _conditions;
    private final ObservableList<String> _conditionsList;
    private final ComboBox<Ease> _easeOfFallingAsleep;
    private final ComboBox<Quality> _qualityOfSleep;
    private final ComboBox<Ease> _easeOfWakingUp;
    private final CheckBox _wakeUpByAlarm;
    private final TextArea _comments;
    private final Label _averageLabel;

    public NightDialog(Stage owner, Night night, boolean isNew, List<String> allConditions) {
        initOwner(owner);
        setTitle(isNew ? t("night.titleAdd") : t("night.titleEdit"));
        setResizable(true);

        _date = new DatePicker(night.getDate());

        _alarm = createTimeField();
        if (night.getAlarm() != null) _alarm.setText(night.getAlarm().format(TIME_FORMAT));

        _window = new ComboBox<>(FXCollections.observableArrayList(Night.getWindows()));
        _window.setValue(night.getWindow());
        _window.setPrefWidth(70);

        _toBed = createTimeField();
        if (night.getToBed() != null) _toBed.setText(night.getToBed().format(TIME_FORMAT));

        _momentField = createTimeField();
        _momentsList = FXCollections.observableArrayList(night.getMoments());
        _moments = new ListView<>(_momentsList);
        _moments.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(TIME_FORMAT));
            }
        });
        _moments.setPrefHeight(120);

        _momentField.setOnAction(e -> {
            LocalTime time = parseTime(_momentField.getText());
            if (time != null) {
                _momentsList.add(time);
                _momentField.clear();
                updateAverage();
            }
        });

        _conditionsList = FXCollections.observableArrayList(night.getConditions());
        _conditions = new ListView<>(_conditionsList);
        _conditions.setPrefHeight(120);

        _conditionCombo = new ComboBox<>(FXCollections.observableArrayList(allConditions));
        _conditionCombo.setEditable(true);
        _conditionCombo.setOnAction(e -> {
            String value = _conditionCombo.getValue();
            if (value != null && !value.trim().isEmpty()) {
                String trimmed = value.trim();
                if (!_conditionsList.contains(trimmed)) {
                    int i = 0;
                    while (i < _conditionsList.size() && _conditionsList.get(i).compareTo(trimmed) < 0) {
                        i++;
                    }
                    _conditionsList.add(i, trimmed);
                }
                _conditions.getSelectionModel().select(trimmed);
                _conditionCombo.setValue(null);
                _conditionCombo.getEditor().clear();
            }
        });

        _easeOfFallingAsleep = new ComboBox<>(FXCollections.observableArrayList(Ease.values()));
        _easeOfFallingAsleep.setValue(night.getEaseOfFallingAsleep());

        _qualityOfSleep = new ComboBox<>(FXCollections.observableArrayList(Quality.values()));
        _qualityOfSleep.setValue(night.getQualityOfSleep());

        _easeOfWakingUp = new ComboBox<>(FXCollections.observableArrayList(Ease.values()));
        _easeOfWakingUp.setValue(night.getEaseOfWakingUp());

        _wakeUpByAlarm = new CheckBox(t("night.wakeUpByAlarm"));
        _wakeUpByAlarm.setSelected(night.isAlarmWorked());

        _comments = new TextArea(night.getComments());
        _comments.setPrefRowCount(3);

        _averageLabel = new Label();

        getDialogPane().setContent(createLayout());
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefSize(700, 500);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return getData();
            }
            return null;
        });

        updateAverage();
    }

    private VBox createLayout() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        GridPane topRow = new GridPane();
        topRow.setHgap(10);
        topRow.setVgap(5);
        topRow.add(new Label(t("night.date")), 0, 0);
        topRow.add(_date, 1, 0);
        topRow.add(new Label(t("night.alarm")), 2, 0);
        topRow.add(_alarm, 3, 0);
        topRow.add(new Label(t("night.window")), 4, 0);
        topRow.add(_window, 5, 0);
        topRow.add(new Label(t("night.toBed")), 6, 0);
        topRow.add(_toBed, 7, 0);

        HBox middle = new HBox(10);
        VBox.setVgrow(middle, Priority.ALWAYS);

        VBox momentsPanel = createMomentsPanel();
        HBox.setHgrow(momentsPanel, Priority.ALWAYS);

        VBox rightSide = new VBox(10);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        VBox conditionsPanel = createConditionsPanel();
        VBox observationsPanel = createObservationsPanel();

        HBox rightTop = new HBox(10, conditionsPanel, observationsPanel);
        HBox.setHgrow(conditionsPanel, Priority.ALWAYS);
        VBox.setVgrow(rightTop, Priority.ALWAYS);

        VBox commentsPanel = new VBox(2, new Label(t("night.comments")), _comments);

        rightSide.getChildren().addAll(rightTop, commentsPanel);
        middle.getChildren().addAll(momentsPanel, rightSide);

        HBox footer = new HBox(_averageLabel);
        footer.setPadding(new Insets(5, 0, 0, 0));

        root.getChildren().addAll(topRow, new Separator(), middle, footer);
        return root;
    }

    private VBox createMomentsPanel() {
        Label title = new Label(t("night.moments"));
        title.setStyle("-fx-font-weight: bold;");

        Button removeBtn = new Button(t("action.remove"));
        removeBtn.disableProperty().bind(
                _moments.getSelectionModel().selectedItemProperty().isNull());
        removeBtn.setOnAction(e -> {
            int idx = _moments.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                _momentsList.remove(idx);
                if (!_momentsList.isEmpty()) {
                    _moments.getSelectionModel().select(Math.min(idx, _momentsList.size() - 1));
                }
                updateAverage();
            }
        });

        HBox addRow = new HBox(5, _momentField, new Label(t("night.momentHint")), removeBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(5, title, addRow, _moments);
        VBox.setVgrow(_moments, Priority.ALWAYS);
        return panel;
    }

    private VBox createConditionsPanel() {
        Label title = new Label(t("night.conditions"));
        title.setStyle("-fx-font-weight: bold;");

        Button removeBtn = new Button(t("action.remove"));
        removeBtn.disableProperty().bind(
                _conditions.getSelectionModel().selectedItemProperty().isNull());
        removeBtn.setOnAction(e -> {
            int idx = _conditions.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                _conditionsList.remove(idx);
                if (!_conditionsList.isEmpty()) {
                    _conditions.getSelectionModel().select(Math.min(idx, _conditionsList.size() - 1));
                }
            }
        });

        HBox addRow = new HBox(5, _conditionCombo, removeBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(5, title, addRow, _conditions);
        VBox.setVgrow(_conditions, Priority.ALWAYS);
        return panel;
    }

    private VBox createObservationsPanel() {
        Label title = new Label(t("night.observations"));
        title.setStyle("-fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.add(new Label(t("night.easeOfFallingAsleep")), 0, 0);
        grid.add(_easeOfFallingAsleep, 1, 0);
        grid.add(new Label(t("night.qualityOfSleep")), 0, 1);
        grid.add(_qualityOfSleep, 1, 1);
        grid.add(new Label(t("night.easeOfWakingUp")), 0, 2);
        grid.add(_easeOfWakingUp, 1, 2);
        grid.add(_wakeUpByAlarm, 0, 3, 2, 1);

        VBox panel = new VBox(5, title, grid);
        return panel;
    }

    private TextField createTimeField() {
        TextField field = new TextField();
        field.setPrefColumnCount(5);
        field.setPromptText("HH:mm");
        return field;
    }

    private LocalTime parseTime(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalTime.parse(text.trim(), TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Night getData() {
        return new Night(
                _date.getValue(),
                parseTime(_alarm.getText()),
                _window.getValue(),
                parseTime(_toBed.getText()),
                _easeOfFallingAsleep.getValue(),
                _qualityOfSleep.getValue(),
                _easeOfWakingUp.getValue(),
                _wakeUpByAlarm.isSelected(),
                _comments.getText().trim(),
                new ArrayList<>(_momentsList),
                new ArrayList<>(_conditionsList));
    }

    private void updateAverage() {
        try {
            Night data = getData();
            if (data.isComplete()) {
                _averageLabel.setText(t("night.average", data.getMetrics().getAverage()));
            } else {
                _averageLabel.setText("");
            }
        } catch (Exception e) {
            _averageLabel.setText("");
        }
    }
}
