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

import com.pavelfatin.sleeparchiver.gui.main.render.NightRenderer;
import com.pavelfatin.sleeparchiver.model.Night;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ListCell;

public class NightCell extends ListCell<Night> {
    private final Canvas _canvas;
    private final NightRenderer _renderer;

    public NightCell(NightRenderer renderer, double width) {
        _renderer = renderer;
        _canvas = new Canvas(width, renderer.getPreferredHeight());
        setGraphic(_canvas);
        setPrefHeight(renderer.getPreferredHeight() + 4);
        setStyle("-fx-padding: 0;");
    }

    @Override
    protected void updateItem(Night night, boolean empty) {
        super.updateItem(night, empty);

        if (empty || night == null) {
            setGraphic(null);
        } else {
            _canvas.setWidth(getListView().getWidth() - 20);
            _renderer.render(_canvas, night, isSelected(), isFocused());
            setGraphic(_canvas);
        }
    }
}
