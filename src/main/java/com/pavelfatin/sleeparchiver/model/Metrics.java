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
import java.util.Collections;
import java.util.List;

public class Metrics {
    private List<LocalTime> _moments;
    private List<SleepInstant> _instants;
    private List<SleepSpan> _spans;


    Metrics(Night night) {
        _moments = night.getCompleteMoments();
        _instants = SleepInstant.toInstants(_moments);
        _spans = SleepSpan.toSpans(_instants);
    }

    public Integer getAverage() {
        return getDuration() / getBreaksCount();
    }

    public int getDeepSleepMinutes() {
        return _spans.stream()
                .mapToInt(SleepSpan::toMinutes)
                .filter(m -> m > 45)
                .sum();
    }

    public int getBreaksCount() {
        return _moments.size() - 2;
    }

    public SleepInstant getFirstInstant() {
        return _instants.getFirst();
    }

    public SleepInstant getLastInstant() {
        return _instants.getLast();
    }

    public int getSpansCount() {
        return _spans.size();
    }

    public SleepSpan getFirstSpan() {
        return _spans.getFirst();
    }

    public SleepSpan getLastSpan() {
        return _spans.getLast();
    }

    private static final int WAKE_THRESHOLD_MINUTES = 15;

    public int getDuration() {
        return new SleepSpan(getFirstSpan().begin(), findEffectiveWakeUp()).toMinutes();
    }

    // Finds the effective wake-up time: the end of the last "real" sleep span (>= threshold).
    // If all trailing intervals are short (< threshold), it means the person was already awake,
    // so we count sleep only up to the start of that short-interval series.
    private SleepInstant findEffectiveWakeUp() {
        for (int i = _spans.size() - 1; i >= 0; i--) {
            if (_spans.get(i).toMinutes() >= WAKE_THRESHOLD_MINUTES) {
                return _spans.get(i).end();
            }
        }
        return getLastSpan().begin();
    }

    public SleepSpan getTotalSpan() {
        return new SleepSpan(getFirstInstant(), getLastInstant());
    }

    public List<SleepInstant> getInstants() {
        return Collections.unmodifiableList(_instants);
    }

    public List<SleepSpan> getSpans() {
        return Collections.unmodifiableList(_spans);
    }
}
