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

package com.pavelfatin.sleeparchiver;

import com.pavelfatin.sleeparchiver.gui.main.MainView;
import com.pavelfatin.sleeparchiver.lang.I18n;
import com.pavelfatin.sleeparchiver.model.Document;
import com.pavelfatin.sleeparchiver.model.Language;
import com.pavelfatin.sleeparchiver.model.Preferences;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class SleepArchiver extends Application {
    private static final File SETTINGS = new File("settings");
    private static final String PREFERENCES = "preferences.xml";

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(stage));

        File prefsDir = SETTINGS.exists() && SETTINGS.isDirectory()
                ? SETTINGS
                : getDefaultStorageDir();
        createIfNotExists(prefsDir);

        File prefsFile = new File(prefsDir, PREFERENCES);
        Preferences preferences = Preferences.loadOrCreateDefault(prefsFile, Language.getDefault());
        preferences.getLanguage().apply();
        I18n.reload();

        MainView mainView = new MainView(stage, new Document(), preferences);

        Scene scene = new Scene(mainView, 1000, 700);
        stage.setScene(scene);
        stage.show();

        List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) {
            mainView.doOpen(new File(String.join(" ", args)));
        } else if (preferences.isOpenRecentEnabled() && preferences.hasRecentFiles()) {
            File file = new File(preferences.getRecentFile());
            if (file.exists()) {
                mainView.doOpen(file);
            }
        }
    }

    private File getDefaultStorageDir() {
        String userHome = System.getProperty("user.home");
        return new File(userHome, ".sleeparchiver");
    }

    private static void createIfNotExists(File directory) {
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (!success) {
                throw new RuntimeException("Can't create directory: " + directory.getPath());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
