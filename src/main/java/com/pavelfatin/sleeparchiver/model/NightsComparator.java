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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

public class NightsComparator implements Comparator<Night> {
    private static final Comparator<LocalDate> DATE_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());
    private static final Comparator<LocalTime> TIME_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

    public int compare(Night n1, Night n2) {
        return compare(n1.getDate(), n2.getDate(), n1.getToBed(), n2.getToBed());
    }

    static int compare(LocalDate d1, LocalDate d2, LocalTime t1, LocalTime t2) {
        int dateComparison = DATE_COMPARATOR.compare(d1, d2);
        return dateComparison == 0 ? TIME_COMPARATOR.compare(t1, t2) : dateComparison;
    }
}
