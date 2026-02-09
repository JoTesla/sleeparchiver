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

package com.pavelfatin.sleeparchiver.model;

import java.util.ArrayList;
import java.util.List;

public record SleepSpan(SleepInstant begin, SleepInstant end) implements Comparable<SleepSpan> {

    public SleepSpan {
        if (end.compareTo(begin) < 0) {
            throw new IllegalArgumentException("Begin is less than end: " + begin + ", " + end);
        }
    }

    public int toMinutes() {
        return end.toMinutes() - begin.toMinutes();
    }

    @Override
    public int compareTo(SleepSpan other) {
        return Integer.compare(this.toMinutes(), other.toMinutes());
    }

    @Override
    public String toString() {
        return String.format("%s-%s", begin.toString(), end.toString());
    }

    public static List<SleepSpan> toSpans(List<SleepInstant> instants) {
        List<SleepSpan> spans = new ArrayList<>();
        for (int i = 1; i < instants.size(); i++) {
            spans.add(new SleepSpan(instants.get(i - 1), instants.get(i)));
        }
        return spans;
    }
}
