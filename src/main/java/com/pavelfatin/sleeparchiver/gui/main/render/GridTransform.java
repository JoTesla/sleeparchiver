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

package com.pavelfatin.sleeparchiver.gui.main.render;

import com.pavelfatin.sleeparchiver.model.Night;

import java.util.List;

public class GridTransform extends Transform {
    private int _gridStartMinutes;
    private int _gridEndMinutes;
    private int _effectiveStartMinutes;
    private int _effectiveEndMinutes;
    private final boolean _manual;

    public GridTransform(String name, int startHour, int endHour, boolean manual) {
        super(name);
        _manual = manual;
        _gridStartMinutes = startHour * 60;
        _gridEndMinutes = (24 + endHour) * 60;
        _effectiveStartMinutes = _gridStartMinutes;
        _effectiveEndMinutes = _gridEndMinutes;
    }

    public GridTransform(String name, int startHour, int endHour) {
        this(name, startHour, endHour, true);
    }

    public void setGridHours(int startHour, int endHour) {
        _gridStartMinutes = startHour * 60;
        _gridEndMinutes = (24 + endHour) * 60;
        _effectiveStartMinutes = _gridStartMinutes;
        _effectiveEndMinutes = _gridEndMinutes;
    }

    @Override
    public void setNights(List<Night> nights) {
        int dataMin = Integer.MAX_VALUE;
        int dataMax = 0;
        boolean hasData = false;
        for (Night night : nights) {
            if (night.isComplete()) {
                int first = night.getMetrics().getFirstInstant().toMinutes();
                int last = night.getMetrics().getLastInstant().toMinutes();
                dataMin = Math.min(dataMin, first);
                dataMax = Math.max(dataMax, last);
                hasData = true;
            }
        }

        if (_manual) {
            // Manual: use fixed grid, expand if data goes beyond
            _effectiveStartMinutes = hasData ? Math.min(_gridStartMinutes, (dataMin / 60) * 60) : _gridStartMinutes;
            _effectiveEndMinutes = hasData ? Math.max(_gridEndMinutes, ((dataMax + 59) / 60) * 60) : _gridEndMinutes;
        } else {
            // Auto: grid = rounded data range, fallback to 22-10 if no data
            if (hasData) {
                _effectiveStartMinutes = (dataMin / 60) * 60;
                _effectiveEndMinutes = ((dataMax + 59) / 60) * 60;
            } else {
                _effectiveStartMinutes = _gridStartMinutes;
                _effectiveEndMinutes = _gridEndMinutes;
            }
        }
        _min = _effectiveStartMinutes;
    }

    @Override
    public int toX(int minutes) {
        return toWidth(minutes - _min);
    }

    @Override
    public int getPreferredWidth(List<Night> nights) {
        return toWidth(_effectiveEndMinutes - _effectiveStartMinutes);
    }

    public int getEffectiveStartMinutes() {
        return _effectiveStartMinutes;
    }

    public int getEffectiveEndMinutes() {
        return _effectiveEndMinutes;
    }

    public int getGridRangeMinutes() {
        return _effectiveEndMinutes - _effectiveStartMinutes;
    }
}
