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

import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class ListCommand<T> extends AbstractCommand {
    private final ObservableList<T> _items;
    private final MultipleSelectionModel<T> _selection;

    private List<Integer> _savedIndices;

    protected ListCommand(String name, ObservableList<T> items, MultipleSelectionModel<T> selection) {
        super(name);
        _items = items;
        _selection = selection;
    }

    protected ObservableList<T> getItems() {
        return _items;
    }

    protected MultipleSelectionModel<T> getSelection() {
        return _selection;
    }

    protected static List<Integer> reversed(List<Integer> indices) {
        List<Integer> reversed = new ArrayList<>(indices);
        Collections.reverse(reversed);
        return reversed;
    }

    protected static List<Integer> sorted(List<Integer> indices) {
        List<Integer> sorted = new ArrayList<>(indices);
        Collections.sort(sorted);
        return sorted;
    }

    protected int findIndexFor(T row, Comparator<T> comparator) {
        int i;
        for (i = 0; i < _items.size(); i++) {
            T each = _items.get(i);
            if (comparator.compare(row, each) < 0) {
                break;
            }
        }
        return i;
    }

    public void execute() {
        _savedIndices = new ArrayList<>(_selection.getSelectedIndices());
        doExecute();
    }

    public void revert() {
        doRevert();
        _selection.clearSelection();
        for (int index : _savedIndices) {
            if (index >= 0 && index < _items.size()) {
                _selection.select(index);
            }
        }
    }

    protected abstract void doExecute();

    protected abstract void doRevert();
}
