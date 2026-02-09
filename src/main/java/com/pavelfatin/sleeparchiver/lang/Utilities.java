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

package com.pavelfatin.sleeparchiver.lang;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utilities {
    private Utilities() {
    }

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            // failsafe
        }
    }

    public static String prefixSingleDigits(String s) {
        return s.replaceAll("(?<=\\D|^)\\d(?=\\D|$)", "0$0");
    }

    public static String join(String separator, List<String> parts) {
        return String.join(separator, parts);
    }

    public static String join(String separator, String... parts) {
        return String.join(separator, parts);
    }

    @SafeVarargs
    public static <T> List<T> newList(T... items) {
        return new ArrayList<T>(Arrays.asList(items));
    }

    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static <V> int compare(Comparable<V> o1, V o2) {
        if (o1 == null) {
            return o2 == null ? 0 : -1;
        } else {
            return o2 == null ? 1 : o1.compareTo(o2);
        }
    }

    public static boolean differs(Object o1, Object o2) {
        return !(equals(o1, o2));
    }

    public static String capitalize(String text) {
        return text.length() > 1
                ? text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase()
                : text.toUpperCase();
    }
}
