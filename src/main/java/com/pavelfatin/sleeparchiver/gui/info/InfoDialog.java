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

package com.pavelfatin.sleeparchiver.gui.info;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;

public class InfoDialog extends Dialog<Void> {

    public InfoDialog(Stage owner, String title, String resourcePath, boolean scrollable) {
        initOwner(owner);
        setTitle(title);
        setResizable(true);

        WebView webView = new WebView();
        webView.setPrefSize(500, scrollable ? 400 : 300);

        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }
        webView.getEngine().load(url.toExternalForm());

        getDialogPane().setContent(webView);
        getDialogPane().getButtonTypes().add(ButtonType.OK);

        setResultConverter(button -> null);
    }
}
