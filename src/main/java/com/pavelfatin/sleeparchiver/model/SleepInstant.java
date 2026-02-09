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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public record SleepInstant(int days, LocalTime time) implements Comparable<SleepInstant> {
    private static final int MINUTES_IN_HOUR = 60;
    private static final int MINUTES_IN_DAY = MINUTES_IN_HOUR * 24;

    private static final LocalTime DAY_BOUNDARY = LocalTime.of(16, 0);

    public SleepInstant {
        if (days < 0) {
            throw new IllegalArgumentException("Days value is negative: " + days);
        }
    }

    public SleepInstant(LocalTime time) {
        this(0, time);
    }

    public SleepInstant(int minutes) {
        this(minutes / MINUTES_IN_DAY,
                LocalTime.of((minutes % MINUTES_IN_DAY) / MINUTES_IN_HOUR,
                        (minutes % MINUTES_IN_DAY) % MINUTES_IN_HOUR));
    }

    public int toMinutes() {
        return MINUTES_IN_DAY * days +
                MINUTES_IN_HOUR * time.getHour() +
                time.getMinute();
    }

    @Override
    public int compareTo(SleepInstant other) {
        int result = Integer.compare(this.days, other.days);
        if (result != 0) {
            return result;
        }
        return this.time.compareTo(other.time);
    }

    @Override
    public String toString() {
        return String.format("%d:%02d:%02d", days, time.getHour(), time.getMinute());
    }

    public static List<SleepInstant> toInstants(List<LocalTime> moments) {
        List<SleepInstant> instants = new ArrayList<>();
        int days = 0;
        for (int i = 0; i < moments.size(); i++) {
            LocalTime time = moments.get(i);
            if (i == 0 && time.isBefore(DAY_BOUNDARY)) {
                days++;
            }
            if (i > 0 && time.isBefore(moments.get(i - 1))) {
                days++;
            }
            instants.add(new SleepInstant(days, time));
        }
        return instants;
    }
}
