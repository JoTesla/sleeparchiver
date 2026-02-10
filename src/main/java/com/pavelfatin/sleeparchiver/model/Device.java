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

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Device {
    private static final int HANDSHAKE = 86;

    private static final int TIMEOUT = 2000;
    private static final int DELAY = 500;

    private String _app;
    private int _year;
    private int _baudRate;
    private Consumer<String> _logger;

    public Device(String app, int year, int baudRate) {
        this(app, year, baudRate, null);
    }

    public Device(String app, int year, int baudRate, Consumer<String> logger) {
        _app = app;
        _year = year;
        _baudRate = baudRate;
        _logger = logger;
    }

    private void log(String msg) {
        System.out.println(msg);
        if (_logger != null) {
            _logger.accept(msg);
        }
    }

    public static List<String> listPorts() {
        List<String> result = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            String name = port.getSystemPortName();
            if (!name.startsWith("tty.")) {
                result.add(name + " (" + port.getDescriptivePortName() + ")");
            }
        }
        return result;
    }

    public Night readData(String portName) {
        if (portName != null) {
            SerialPort port = SerialPort.getCommPort(portName);
            try {
                log("Port: " + portName + " @ " + _baudRate);
                return readNight(port, _year, _baudRate, this::log);
            } catch (ProtocolException e) {
                log("Protocol error: " + e.getMessage());
            } catch (IOException e) {
                log("IO error: " + e.getMessage());
            }
            return null;
        }

        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            try {
                return readNight(port, _year, _baudRate, this::log);
            } catch (IOException e) {
                // skip
            }
        }
        return null;
    }

    private static Night readNight(SerialPort port, int year, int baudRate, Consumer<String> log) throws IOException {
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT, 0);

        log.accept("Opening port...");
        if (!port.openPort()) {
            throw new IOException("Unable to open port: " + port.getSystemPortName());
        }
        log.accept("Port opened. CTS=" + port.getCTS() + " DSR=" + port.getDSR());

        try {
            port.clearRTS();
            port.setDTR();
            log.accept("DTR=on, RTS=off.");

            // ОРИГИНАЛЬНЫЙ ПРОТОКОЛ: сначала отправляем handshake, потом читаем ответ
            log.accept("Sending handshake (0x56)...");
            byte[] hs = {(byte) HANDSHAKE};
            port.writeBytes(hs, 1);
            sleep(DELAY);

            // Читаем ответ
            byte[] rawBuf = new byte[256];
            int total = port.readBytes(rawBuf, rawBuf.length);

            if (total <= 0) {
                throw new IOException("No response from device");
            }

            // Дамп сырых данных
            StringBuilder hex = new StringBuilder();
            StringBuilder dec = new StringBuilder();
            boolean allZeros = true;
            for (int i = 0; i < total; i++) {
                int b = rawBuf[i] & 0xFF;
                hex.append(String.format("%02X ", b));
                dec.append(b).append(" ");
                if (b != 0) allZeros = false;
            }
            log.accept("Raw " + total + " bytes:");
            log.accept(hex.toString().trim());
            log.accept("Dec: " + dec.toString().trim());

            if (allZeros) {
                throw new IOException("No recorded sleep data on the watch.");
            }

            // Проверяем, начинаются ли данные с handshake response (0x56)
            if ((rawBuf[0] & 0xFF) != HANDSHAKE) {
                log.accept("WARNING: Expected handshake response 0x56, got: 0x" + String.format("%02X", rawBuf[0] & 0xFF));
                log.accept("Possible reasons:");
                log.accept("1. Alarm was DISABLED - watch does not record sleep data without alarm");
                log.accept("2. Watch is not in Date mode - scroll to Date screen on watch");
                log.accept("3. No sleep data recorded yet");
                log.accept("4. Different protocol version or watch model");
                throw new IOException("Invalid handshake response. Ensure alarm was ON and watch has sleep data.");
            }

            // Создаем stream начиная со второго байта (после handshake)
            byte[] remaining = new byte[total - 1];
            System.arraycopy(rawBuf, 1, remaining, 0, remaining.length);
            InputStream in = new java.io.SequenceInputStream(
                    new java.io.ByteArrayInputStream(remaining),
                    port.getInputStream());

            DeviceReader reader = new DeviceReader(new BufferedInputStream(in), year);
            reader._sum = 0;

            var date = reader.readDate();
            reader.skip();
            int window = reader.readByte();
            var toBed = reader.readTime();
            var alarm = reader.readTime();

            int count = reader.readByte();
            log.accept("Date: " + date + ", moments: " + count);

            List<LocalTime> moments = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                moments.add(reader.readTime());
                reader.skip();
            }

            int minutesLow = reader.readByte();
            int minutesHigh = reader.readByte();

            int dataChecksum = reader.getChecksum();
            int checksum = reader.readByte();
            if (dataChecksum != checksum) {
                log.accept("Checksum mismatch: " + dataChecksum + " vs " + checksum);
                throw new ProtocolException(String.format(
                        "Incorrect checksum: %d, expected: %d", dataChecksum, checksum));
            }

            reader.readEnding();
            log.accept("Data read successfully!");

            return new Night(date, alarm, window, toBed, moments);
        } finally {
            port.closePort();
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static Night readNight(InputStream stream, int year) throws IOException {
        DeviceReader reader = new DeviceReader(stream, year);
        reader.readHandshake();

        var date = reader.readDate();
        reader.skip();
        int window = reader.readByte();
        var toBed = reader.readTime();
        var alarm = reader.readTime();

        int count = reader.readByte();
        List<LocalTime> moments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            moments.add(reader.readTime());
            reader.skip();
        }

        int minutesLow = reader.readByte();
        int minutesHigh = reader.readByte();

        int dataChecksum = reader.getChecksum();
        int checksum = reader.readByte();
        if (dataChecksum != checksum) {
            throw new ProtocolException(String.format(
                    "Incorrect checksum: %d, expected: %d", dataChecksum, checksum));
        }

        reader.readEnding();
        return new Night(date, alarm, window, toBed, moments);
    }
}
