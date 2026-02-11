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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
    private PrintWriter _fileLog;

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
        if (_fileLog != null) {
            _fileLog.println(msg);
            _fileLog.flush();
        }
    }

    private void openFileLog() {
        try {
            Path logsDir = Paths.get("logs");
            Files.createDirectories(logsDir);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path logFile = logsDir.resolve("acquire_" + ts + ".log");
            _fileLog = new PrintWriter(new FileWriter(logFile.toFile()), true);
            log("Log file: " + logFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
        }
    }

    private void closeFileLog() {
        if (_fileLog != null) {
            _fileLog.close();
            _fileLog = null;
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
        openFileLog();
        try {
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
        } finally {
            closeFileLog();
        }
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
            // Flush входного буфера (могут быть данные от предыдущих попыток)
            byte[] discard = new byte[256];
            while (port.readBytes(discard, discard.length) > 0) { /* drain */ }

            EliteProtocol proto = new EliteProtocol(port, _year, this::log);

            // Порядок как в оригинальном ПО: flashLog -> date -> time -> alarm -> events -> name
            byte[] flashData = null;
            try { flashData = proto.readFlashLog(); } catch (Exception e) { log("readFlashLog failed: " + e.getMessage()); }

            java.time.LocalDate date = null;
            try { date = proto.readDate(); } catch (Exception e) { log("readDate failed: " + e.getMessage()); }

            LocalTime time = null;
            try { time = proto.readTime(); } catch (Exception e) { log("readTime failed: " + e.getMessage()); }

            EliteProtocol.AlarmInfo alarmInfo = null;
            try { alarmInfo = proto.readAlarm(); } catch (Exception e) { log("readAlarm failed: " + e.getMessage()); }

            List<LocalTime> events = null;
            try { events = proto.readEvents(); } catch (Exception e) { log("readEvents failed: " + e.getMessage()); }

            String deviceName = null;
            try { deviceName = proto.readDeviceName(); } catch (Exception e) { log("readDeviceName failed: " + e.getMessage()); }

            // Пробуем собрать Night
            log("--- Building Night ---");

            // Elite2: сначала пробуем flash log
            if (_model == WatchModel.ELITE2 && flashData != null && flashData.length > 26) {
                try {
                    Night night = Elite2Protocol.parseFlashLog(flashData);
                    log("Flash log parsed: " + night.getDate());
                    return night;
                } catch (Exception e) {
                    log("Flash log parse failed: " + e.getMessage());
                }
            }

            // Собираем из отдельных команд
            if (date == null) {
                throw new IOException("Failed to read date from device");
            }
            LocalTime alarm = alarmInfo != null ? alarmInfo.alarmTime() : null;
            int window = alarmInfo != null ? alarmInfo.windowMinutes() : 20;
            LocalTime toBed = alarmInfo != null ? alarmInfo.toBed() : null;
            if (events == null) events = new ArrayList<>();

            log("Result: date=" + date + " alarm=" + alarm + " window=" + window
                    + " toBed=" + toBed + " events=" + events.size());
            return new Night(date, alarm, window, toBed, events);
        } finally {
            port.closePort();
        }
    }

    private Night readNightPro(SerialPort port) throws IOException {
        int baudRate = _model.getBaudRate();

        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT, 0);

        log("Opening port...");
        if (!port.openPort()) {
            throw new IOException("Unable to open port: " + port.getSystemPortName());
        }
        log("Port opened. CTS=" + port.getCTS() + " DSR=" + port.getDSR());

        try {
            port.clearRTS();
            port.setDTR();
            log("DTR=on, RTS=off.");

            log("Sending handshake (0x56)...");
            byte[] hs = {(byte) HANDSHAKE};
            port.writeBytes(hs, 1);
            sleep(DELAY);

            // Читаем все пакеты в цикле
            List<byte[]> packets = new ArrayList<>();
            int packetNum = 0;
            int emptyReads = 0;

            while (emptyReads < 3) {
                byte[] rawBuf = new byte[256];
                int n = port.readBytes(rawBuf, rawBuf.length);

                if (n <= 0) {
                    emptyReads++;
                    if (packetNum == 0 && emptyReads >= 3) {
                        throw new IOException("No response from device");
                    }
                    log("Empty read #" + emptyReads + ", waiting...");
                    sleep(200);
                    continue;
                }

                emptyReads = 0;
                packetNum++;
                byte[] packet = Arrays.copyOf(rawBuf, n);
                packets.add(packet);

                // Лог каждого пакета
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    hex.append(String.format("%02X ", packet[i] & 0xFF));
                }
                log("Packet " + packetNum + " (" + n + " bytes): " + hex.toString().trim());

                if (n < 10) {
                    sleep(100);
                }
            }

            log("Read complete. Packets: " + packetNum);

            // Объединяем пакеты
            int totalBytes = packets.stream().mapToInt(p -> p.length).sum();
            byte[] rawBuf = new byte[totalBytes];
            int offset = 0;
            for (byte[] packet : packets) {
                System.arraycopy(packet, 0, rawBuf, offset, packet.length);
                offset += packet.length;
            }
            int total = totalBytes;

            log("=== RAW DATA: " + total + " bytes ===");

            // Hex dump
            StringBuilder hexAll = new StringBuilder();
            for (int i = 0; i < total; i++) {
                hexAll.append(String.format("%02X ", rawBuf[i] & 0xFF));
            }
            log("HEX: " + hexAll.toString().trim());

            // Побайтовый анализ
            log("=== BYTE-BY-BYTE ===");
            for (int i = 0; i < total; i++) {
                int b = rawBuf[i] & 0xFF;
                log(String.format("[%3d] 0x%02X  dec=%3d  char=%s", i, b, b,
                        (b >= 32 && b < 127) ? "'" + (char) b + "'" : "."));
            }
            log("=== END ===");

            // Сохраняем сырые данные в файл
            saveRawData(rawBuf, total);

            // Проверка на все нули
            boolean allZeros = true;
            for (int i = 0; i < total; i++) {
                if ((rawBuf[i] & 0xFF) != 0) { allZeros = false; break; }
            }
            if (allZeros) {
                throw new IOException("No recorded sleep data on the watch.");
            }

            // Ищем handshake
            int hsIdx = -1;
            for (int i = 0; i < total; i++) {
                if ((rawBuf[i] & 0xFF) == HANDSHAKE) {
                    hsIdx = i;
                    log("Handshake 0x56 found at offset " + i);
                    break;
                }
            }

            // Парсинг
            InputStream in;
            DeviceReader reader;

            if (hsIdx >= 0) {
                log("Parsing from offset " + (hsIdx + 1) + "...");
                byte[] remaining = new byte[total - hsIdx - 1];
                System.arraycopy(rawBuf, hsIdx + 1, remaining, 0, remaining.length);
                in = new ByteArrayInputStream(remaining);
                reader = new DeviceReader(new BufferedInputStream(in), _year);
                reader._sum = 0;
            } else {
                log("WARNING: No handshake found, parsing from offset 0");
                in = new ByteArrayInputStream(Arrays.copyOf(rawBuf, total));
                reader = new DeviceReader(new BufferedInputStream(in), _year);
            }

            try {
                var date = reader.readDate();
                log("Date: " + date);

                reader.skip();
                int window = reader.readByte();
                log("Window: " + window + " min");

                var toBed = reader.readTime();
                log("ToBed: " + toBed);

                var alarm = reader.readTime();
                log("Alarm: " + alarm);

                int count = reader.readByte();
                log("Moments count: " + count);

                List<LocalTime> moments = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    LocalTime m = reader.readTime();
                    reader.skip();
                    moments.add(m);
                    log("  Moment " + (i + 1) + ": " + m);
                }

                int minutesLow = reader.readByte();
                int minutesHigh = reader.readByte();
                int totalMinutes = minutesLow + (minutesHigh << 8);
                log("Total minutes: " + totalMinutes + " (low=" + minutesLow + " high=" + minutesHigh + ")");

                int dataChecksum = reader.getChecksum();
                int checksum = reader.readByte();
                log("Checksum: calculated=" + dataChecksum + " received=" + checksum);

                if (dataChecksum != checksum) {
                    throw new ProtocolException(String.format(
                            "Incorrect checksum: %d, expected: %d", dataChecksum, checksum));
                }

                reader.readEnding();
                log("=== PARSE OK ===");
                log("Date=" + date + " ToBed=" + toBed + " Alarm=" + alarm
                        + " Window=" + window + " Moments=" + count);

                return new Night(date, alarm, window, toBed, moments);
            } catch (ProtocolException e) {
                log("Parse error: " + e.getMessage());
                throw new IOException("Failed to parse watch data: " + e.getMessage(), e);
            }
        } finally {
            port.closePort();
        }
    }

    private void saveRawData(byte[] data, int length) {
        try {
            Path logsDir = Paths.get("logs");
            Files.createDirectories(logsDir);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path rawFile = logsDir.resolve("raw_" + ts + ".dat");
            Files.write(rawFile, Arrays.copyOf(data, length));
            log("Raw data saved: " + rawFile.toAbsolutePath());
        } catch (IOException e) {
            log("Failed to save raw data: " + e.getMessage());
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
