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

import com.pavelfatin.sleeparchiver.lang.Utilities;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class DeviceTest {
    @Test
    public void normal() throws IOException {
        assertThat(readNight("normal.dat"),
                equalTo(new Night(LocalDate.of(2009, 6, 14),
                        LocalTime.of(10, 10),
                        30,
                        LocalTime.of(2, 16),
                        Utilities.newList(
                                LocalTime.of(2, 42),
                                LocalTime.of(3, 15),
                                LocalTime.of(4, 48),
                                LocalTime.of(5, 16),
                                LocalTime.of(5, 53),
                                LocalTime.of(6, 27),
                                LocalTime.of(6, 38),
                                LocalTime.of(8, 11),
                                LocalTime.of(8, 27),
                                LocalTime.of(8, 58),
                                LocalTime.of(9, 11),
                                LocalTime.of(9, 33),
                                LocalTime.of(9, 40)))));
    }

    @Test
    public void empty() throws IOException {
        assertThat(readNight("empty.dat"),
                equalTo(new Night(LocalDate.of(2009, 6, 14),
                        LocalTime.of(10, 10),
                        30,
                        LocalTime.of(2, 16),
                        new ArrayList<LocalTime>())));
    }

    @Test
    public void checksum() {
        assertThrows(ProtocolException.class, () -> readNight("checksum.dat"));
    }

    @Test
    public void handshake() {
        assertThrows(ProtocolException.class, () -> readNight("handshake.dat"));
    }

    @Test
    public void ending() {
        assertThrows(ProtocolException.class, () -> readNight("ending.dat"));
    }

    @Test
    public void incomplete() {
        assertThrows(ProtocolException.class, () -> readNight("incomplete.dat"));
    }

    private Night readNight(String file) throws IOException {
        URL url = getClass().getResource("device/" + file);
        try (BufferedInputStream stream = new BufferedInputStream(url.openStream())) {
            return Device.readNight(stream, 2009);
        }
    }
}
