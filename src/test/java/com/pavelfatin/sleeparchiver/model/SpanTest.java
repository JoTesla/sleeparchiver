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

import static com.pavelfatin.sleeparchiver.lang.Utilities.newList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Collections;

public class SpanTest {
    private SleepInstant _i1 = new SleepInstant(1, LocalTime.of(14, 10));
    private SleepInstant _i2 = new SleepInstant(2, LocalTime.of(15, 20));
    private SleepInstant _i3 = new SleepInstant(3, LocalTime.of(16, 30));

    @Test
    public void accuracy() {
        SleepSpan span = new SleepSpan(_i1, _i2);
        assertThat(span.begin(), equalTo(_i1));
        assertThat(span.end(), equalTo(_i2));
    }

    @Test
    public void testEquals() {
        assertThat(new SleepSpan(_i1, _i3), equalTo(new SleepSpan(_i1, _i3)));
    }

    @Test
    public void notEquals() {
        assertThat(new SleepSpan(_i1, _i2), not(equalTo(new SleepSpan(_i1, _i3))));
        assertThat(new SleepSpan(_i2, _i3), not(equalTo(new SleepSpan(_i1, _i3))));
    }

    @Test
    public void toMinutes() {
        assertThat(new SleepSpan(_i1, _i3).toMinutes(), equalTo(3020));
    }

    @Test
    public void toMinutesEmpty() {
        assertThat(new SleepSpan(_i1, _i1).toMinutes(), equalTo(0));
    }

    @Test
    public void toSpansSingle() {
        assertThat(SleepSpan.toSpans(newList(_i1, _i3)),
                equalTo(newList(new SleepSpan(_i1, _i3))));
    }

    @Test
    public void toSpansMultiple() {
        assertThat(SleepSpan.toSpans(newList(_i1, _i2, _i3)),
                equalTo(newList(new SleepSpan(_i1, _i2), new SleepSpan(_i2, _i3))));
    }

    @Test
    public void toSpansNone() {
        assertThat(SleepSpan.toSpans(newList(_i1)),
                equalTo(Collections.<SleepSpan>emptyList()));
    }

    @Test
    public void toSpansEmpty() {
        assertThat(SleepSpan.toSpans(Collections.<SleepInstant>emptyList()),
                equalTo(Collections.<SleepSpan>emptyList()));
    }
}
