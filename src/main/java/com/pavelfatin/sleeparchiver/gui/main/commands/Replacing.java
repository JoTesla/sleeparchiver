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
import java.util.List;

public class Replacing extends ListCommand<Night> {
    private final List<Night> _replacement;
    private List<Night> _original;

    public Replacing(String name, ObservableList<Night> items, MultipleSelectionModel<Night> selection,
                     List<Night> replacement) {
        super(name, items, selection);
        _replacement = new ArrayList<>(replacement);
    }

    public void doExecute() {
        _original = new ArrayList<>(getItems());
        List<Integer> indices = new ArrayList<>(getSelection().getSelectedIndices());
        getItems().setAll(_replacement);
        getSelection().clearSelection();
        for (int index : indices) {
            if (index >= 0 && index < getItems().size()) {
                getSelection().select(index);
            }
        }
    }

    public void doRevert() {
        getItems().setAll(_original);
    }
}
