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

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@XmlJavaTypeAdapter(Ease.Adapter.class)
public enum Ease {
    Unknown,
    Hard,
    Normal,
    Easy;


    public boolean isUnknown() {
        return Unknown.equals(this);
    }

    public boolean isKnown() {
        return !isUnknown();
    }

    public static List<Ease> members() {
        ArrayList<Ease> list = new ArrayList<Ease>();
        list.addAll(Arrays.asList(Ease.values()));
        list.remove(Unknown);
        return list;
    }

    public String format() {
        return isUnknown() ? "" : toString().toLowerCase();
    }

    public static Ease parse(String s) {
        return s.isEmpty()
                    ? Ease.Unknown
                    : Ease.valueOf(Ease.class, capitalize(s));
    }

    private static String capitalize(String text) {
        return text.length() > 1
                ? text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase()
                : text.toUpperCase();
    }


    static class Adapter extends XmlAdapter<String, Ease> {
        @Override
        public Ease unmarshal(String v) throws Exception {
            return v == null ? Ease.Unknown : Ease.parse(v);
        }

        @Override
        public String marshal(Ease v) throws Exception {
            return v.isUnknown() ? null : v.format();
        }
    }
}
