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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

public class UtilitiesTest {
    @Test
    public void prefixSingleDigits() {
        assertThat(Utilities.prefixSingleDigits(""), equalTo(""));

        assertThat(Utilities.prefixSingleDigits("5"), equalTo("05"));
        assertThat(Utilities.prefixSingleDigits("50"), equalTo("50"));

        assertThat(Utilities.prefixSingleDigits("a5"), equalTo("a05"));
        assertThat(Utilities.prefixSingleDigits("a50"), equalTo("a50"));

        assertThat(Utilities.prefixSingleDigits("5a"), equalTo("05a"));
        assertThat(Utilities.prefixSingleDigits("50a"), equalTo("50a"));

        assertThat(Utilities.prefixSingleDigits("a5b"), equalTo("a05b"));
        assertThat(Utilities.prefixSingleDigits("a50b"), equalTo("a50b"));

        assertThat(Utilities.prefixSingleDigits("a5b3c"), equalTo("a05b03c"));
        assertThat(Utilities.prefixSingleDigits("a5b30c"), equalTo("a05b30c"));
        assertThat(Utilities.prefixSingleDigits("a50b3c"), equalTo("a50b03c"));
        assertThat(Utilities.prefixSingleDigits("a50b30c"), equalTo("a50b30c"));
    }
}
