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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Removal extends ListCommand<Night> {
    private final List<Integer> _removalIndices;
    private List<Night> _backup;

    public Removal(String name, ObservableList<Night> items, MultipleSelectionModel<Night> selection) {
        super(name, items, selection);
        _removalIndices = new ArrayList<>(selection.getSelectedIndices());
    }

    public void doExecute() {
        _backup = new ArrayList<>();
        for (int index : _removalIndices) {
            _backup.add(getItems().get(index));
        }

        for (int index : reversed(_removalIndices)) {
            getItems().remove(index);
        }

        int min = _removalIndices.getFirst();
        int lastIndex = getItems().size() - 1;
        int index = min <= lastIndex ? min : lastIndex;
        if (index >= 0) {
            getSelection().clearAndSelect(index);
        }
    }

    public void doRevert() {
        Iterator<Night> it = _backup.iterator();
        for (int index : _removalIndices) {
            getItems().add(index, it.next());
        }
    }
}
