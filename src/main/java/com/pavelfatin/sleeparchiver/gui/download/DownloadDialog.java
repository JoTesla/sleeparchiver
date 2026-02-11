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

package com.pavelfatin.sleeparchiver.gui.download;

import com.pavelfatin.sleeparchiver.model.Device;
import com.pavelfatin.sleeparchiver.model.Night;
import com.pavelfatin.sleeparchiver.model.WatchModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static com.pavelfatin.sleeparchiver.lang.I18n.t;

public class DownloadDialog extends Dialog<Night> {
    public DownloadDialog(Stage owner, int year, String portName, WatchModel model, boolean debugLogging) {
        initOwner(owner);
        setTitle(t("download.title"));
        setResizable(true);

        Label message = new Label(t("download.waiting"));
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(40, 40);

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        VBox content = new VBox(10, message, progress, logArea);
        content.setPadding(new Insets(15, 20, 15, 20));
        content.setPrefSize(500, 400);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        setResultConverter(button -> null);

        setOnShowing(e -> {
            Thread thread = new Thread(() -> {
                Device device = new Device("SleepArchiver", year, model, msg ->
                        Platform.runLater(() -> {
                            logArea.appendText(msg + "\n");
                            logArea.setScrollTop(Double.MAX_VALUE);
                        }), debugLogging);
                Night night = device.readData(portName);
                if (night != null) {
                    Platform.runLater(() -> {
                        setResult(night);
                        close();
                    });
                } else {
                    Platform.runLater(() -> {
                        message.setText(t("download.failed"));
                        progress.setVisible(false);
                    });
                }
            });
            thread.setDaemon(true);
            thread.start();
        });

    }
}
