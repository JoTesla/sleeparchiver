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

import com.pavelfatin.sleeparchiver.model.Metrics;
import com.pavelfatin.sleeparchiver.model.Night;
import com.pavelfatin.sleeparchiver.model.SleepInstant;

import java.util.List;

public abstract class Transform {
    protected double _resolution;
    protected int _min;
    protected int _first;

    private final String _name;

    protected Transform(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public void setResolution(double resolution) {
        _resolution = resolution;
    }

    public int toX(SleepInstant instant) {
        return toX(instant.toMinutes());
    }

    public int toWidth(int minutes) {
        return (int) Math.round(_resolution * minutes);
    }

    public void setNights(List<Night> nights) {
        _min = minMinuteOf(nights);
    }

    public void setNight(Night night) {
        Metrics metrics = night.getMetrics();
        _first = metrics.getFirstInstant().toMinutes();
    }

    protected static int maxMinuteOf(List<Night> nights) {
        int max = 0;
        for (Night night : nights) {
            if (night.isComplete()) {
                int edge = night.getMetrics().getLastInstant().toMinutes();
                max = Math.max(max, edge);
            }
        }
        return max;
    }

    private static int minMinuteOf(List<Night> nights) {
        int min = nights.isEmpty() ? 0 : Integer.MAX_VALUE;
        for (Night night : nights) {
            if (night.isComplete()) {
                int edge = night.getMetrics().getFirstInstant().toMinutes();
                min = Math.min(min, edge);
            }
        }
        return min;
    }

    protected static int maxWidthOf(List<Night> nights) {
        int max = 0;
        for (Night night : nights) {
            if (night.isComplete()) {
                int edge = night.getMetrics().getTotalSpan().toMinutes();
                max = Math.max(max, edge);
            }
        }
        return max;
    }

    @Override
    public String toString() {
        return _name;
    }

    public abstract int toX(int minutes);

    public abstract int getPreferredWidth(List<Night> nights);
}
