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

package com.pavelfatin.sleeparchiver.gui.main.commands;

import com.pavelfatin.sleeparchiver.model.Night;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

import java.util.Comparator;

public class Addition extends ListCommand<Night> {
    private final Comparator<Night> _order;
    private final Night _night;
    private int _insertionIndex;

    public Addition(String name, ObservableList<Night> items, MultipleSelectionModel<Night> selection,
                    Comparator<Night> order, Night night) {
        super(name, items, selection);
        _order = order;
        _night = night;
    }

    public void doExecute() {
        _insertionIndex = findIndexFor(_night, _order);
        getItems().add(_insertionIndex, _night);
        getSelection().clearAndSelect(_insertionIndex);
    }

    public void doRevert() {
        getItems().remove(_insertionIndex);
    }
}
