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
    private WatchModel _model;
    private Consumer<String> _logger;

    public Device(String app, int year, WatchModel model) {
        this(app, year, model, null);
    }

    public Device(String app, int year, WatchModel model, Consumer<String> logger) {
        _app = app;
        _year = year;
        _model = model;
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
                log("Port: " + portName + " @ " + _model.getBaudRate() + " [" + _model.getDisplayName() + "]");
                return readNight(port);
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
                return readNight(port);
            } catch (IOException e) {
                // skip
            }
        }
        return null;
    }

    private Night readNight(SerialPort port) throws IOException {
        switch (_model) {
            case ELITE:
            case ELITE2:
                return readNightElite(port);
            case PRO:
            default:
                return readNightPro(port);
        }
    }

    private Night readNightElite(SerialPort port) throws IOException {
        int baudRate = _model.getBaudRate();
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT, 0);

        log("Opening port (Elite)...");
        if (!port.openPort()) {
            throw new IOException("Unable to open port: " + port.getSystemPortName());
        }

        try {
            EliteProtocol proto = new EliteProtocol(port, _year, this::log);

            if (_model == WatchModel.ELITE2) {
                log("Trying flash log (Elite2)...");
                byte[] flashData = proto.readFlashLog();
                if (flashData != null && flashData.length > 26) {
                    log("Parsing flash log...");
                    Night night = Elite2Protocol.parseFlashLog(flashData);
                    log("Flash log parsed: " + night.getDate());
                    return night;
                }
                log("Flash log empty, falling back to Elite commands...");
            }

            // Стандартный Elite протокол
            var date = proto.readDate();
            var alarmInfo = proto.readAlarm();
            var events = proto.readEvents();

            return new Night(date, alarmInfo.time(), alarmInfo.windowMinutes(),
                    null, events);
        } finally {
            port.closePort();
        }
    }

    private Night readNightPro(SerialPort port) throws IOException {
        int baudRate = _model.getBaudRate();
        Consumer<String> logFn = this::log;

        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT, 0);

        logFn.accept("Opening port...");
        if (!port.openPort()) {
            throw new IOException("Unable to open port: " + port.getSystemPortName());
        }
        logFn.accept("Port opened. CTS=" + port.getCTS() + " DSR=" + port.getDSR());

        try {
            port.clearRTS();
            port.setDTR();
            logFn.accept("DTR=on, RTS=off.");

            logFn.accept("Sending handshake (0x56)...");
            byte[] hs = {(byte) HANDSHAKE};
            port.writeBytes(hs, 1);
            sleep(DELAY);

            java.util.List<byte[]> packets = new java.util.ArrayList<>();
            int packetNum = 0;

            while (true) {
                byte[] rawBuf = new byte[256];
                int total = port.readBytes(rawBuf, rawBuf.length);

                if (total <= 0) {
                    if (packetNum == 0) {
                        throw new IOException("No response from device");
                    }
                    logFn.accept("No more data. Total packets: " + packetNum);
                    break;
                }

                packetNum++;
                byte[] packet = java.util.Arrays.copyOf(rawBuf, total);
                packets.add(packet);
                logFn.accept("Packet " + packetNum + " (" + total + " bytes)");

                if (total < 10) {
                    sleep(100);
                }
            }

            int totalBytes = packets.stream().mapToInt(p -> p.length).sum();
            byte[] rawBuf = new byte[totalBytes];
            int offset = 0;
            for (byte[] packet : packets) {
                System.arraycopy(packet, 0, rawBuf, offset, packet.length);
                offset += packet.length;
            }
            int total = totalBytes;

            logFn.accept("Combined: " + total + " bytes");

            boolean allZeros = true;
            for (int i = 0; i < total; i++) {
                if ((rawBuf[i] & 0xFF) != 0) { allZeros = false; break; }
            }

            if (allZeros) {
                throw new IOException("No recorded sleep data on the watch.");
            }

            int hsIdx = -1;
            for (int i = 0; i < total; i++) {
                if ((rawBuf[i] & 0xFF) == HANDSHAKE) {
                    hsIdx = i;
                    break;
                }
            }

            InputStream in;
            DeviceReader reader;

            if (hsIdx >= 0) {
                byte[] remaining = new byte[total - hsIdx - 1];
                System.arraycopy(rawBuf, hsIdx + 1, remaining, 0, remaining.length);
                in = new java.io.SequenceInputStream(
                        new java.io.ByteArrayInputStream(remaining),
                        port.getInputStream());
                reader = new DeviceReader(new BufferedInputStream(in), _year);
                reader._sum = 0;
            } else {
                logFn.accept("WARNING: No handshake (0x56) found");
                byte[] data = new byte[total];
                System.arraycopy(rawBuf, 0, data, 0, total);
                in = new java.io.SequenceInputStream(
                        new java.io.ByteArrayInputStream(data),
                        port.getInputStream());
                reader = new DeviceReader(new BufferedInputStream(in), _year);
            }

            try {
                var date = reader.readDate();
                logFn.accept("Date: " + date);

                reader.skip();
                int window = reader.readByte();
                var toBed = reader.readTime();
                var alarm = reader.readTime();

                int count = reader.readByte();
                logFn.accept("Moments: " + count);

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
                logFn.accept("Data read successfully!");

                return new Night(date, alarm, window, toBed, moments);
            } catch (ProtocolException e) {
                logFn.accept("Parse error: " + e.getMessage());
                throw new IOException("Failed to parse watch data: " + e.getMessage(), e);
            }
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
