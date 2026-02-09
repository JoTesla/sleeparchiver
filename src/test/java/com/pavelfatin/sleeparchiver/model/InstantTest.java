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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

public class InstantTest {
    @Test
    public void accuracy() {
        SleepInstant instant = new SleepInstant(1, LocalTime.of(2, 3));
        assertThat(instant.days(), equalTo(1));
        assertThat(instant.time(), equalTo(LocalTime.of(2, 3)));
    }

    @Test
    public void testEquals() {
        assertThat(new SleepInstant(1, LocalTime.of(12, 30)),
                equalTo(new SleepInstant(1, LocalTime.of(12, 30))));
    }

    @Test
    public void notEquals() {
        assertThat(new SleepInstant(1, LocalTime.of(14, 30)),
                not(equalTo(new SleepInstant(1, LocalTime.of(12, 30)))));
        assertThat(new SleepInstant(2, LocalTime.of(12, 30)),
                not(equalTo(new SleepInstant(1, LocalTime.of(12, 30)))));
    }

    @Test
    public void toMinutes() {
        assertThat(new SleepInstant(0, LocalTime.of(0, 0)).toMinutes(), equalTo(0));
        assertThat(new SleepInstant(0, LocalTime.of(0, 40)).toMinutes(), equalTo(40));
        assertThat(new SleepInstant(0, LocalTime.of(3, 40)).toMinutes(), equalTo(220));
        assertThat(new SleepInstant(2, LocalTime.of(3, 40)).toMinutes(), equalTo(3100));
    }
}
