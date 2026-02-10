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

            // Читаем все пакеты в цикле (часы могут отдавать массив по частям)
            java.util.List<byte[]> packets = new java.util.ArrayList<>();
            int packetNum = 0;

            while (true) {
                byte[] rawBuf = new byte[256];
                int total = port.readBytes(rawBuf, rawBuf.length);

                if (total <= 0) {
                    if (packetNum == 0) {
                        throw new IOException("No response from device");
                    }
                    log.accept("No more data. Total packets received: " + packetNum);
                    break;
                }

                packetNum++;
                byte[] packet = java.util.Arrays.copyOf(rawBuf, total);
                packets.add(packet);
                log.accept("=== PACKET " + packetNum + " (" + total + " bytes) ===");

                // Сохраняем каждый пакет отдельно
                try {
                    String timestamp = java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String filename = "packet_" + packetNum + "_" + timestamp + ".dat";
                    java.nio.file.Path path = java.nio.file.Paths.get(filename);
                    java.nio.file.Files.write(path, packet);
                    log.accept("Packet " + packetNum + " saved to: " + filename);
                } catch (Exception e) {
                    log.accept("Failed to save packet: " + e.getMessage());
                }

                // Показываем сырые данные пакета
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < total; i++) {
                    hex.append(String.format("%02X ", packet[i] & 0xFF));
                }
                log.accept("Hex: " + hex.toString().trim());

                // Если пришло мало данных, возможно это последний пакет
                if (total < 10) {
                    log.accept("Small packet, might be end. Waiting for more...");
                    sleep(100); // Небольшая пауза перед следующей попыткой
                }
            }

            // Объединяем все пакеты
            int totalBytes = packets.stream().mapToInt(p -> p.length).sum();
            byte[] rawBuf = new byte[totalBytes];
            int offset = 0;
            for (byte[] packet : packets) {
                System.arraycopy(packet, 0, rawBuf, offset, packet.length);
                offset += packet.length;
            }
            int total = totalBytes;

            log.accept("=== COMBINED DATA: " + total + " bytes ===");

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

            // Сохранение сырых данных в файл для анализа
            try {
                String timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String filename = "raw_data_" + timestamp + ".dat";
                java.nio.file.Path path = java.nio.file.Paths.get(filename);
                java.nio.file.Files.write(path, java.util.Arrays.copyOf(rawBuf, total));
                log.accept("Raw data saved to: " + path.toAbsolutePath());
            } catch (Exception e) {
                log.accept("Failed to save raw data: " + e.getMessage());
            }

            if (allZeros) {
                throw new IOException("No recorded sleep data on the watch.");
            }

            // Ищем 0x56 в данных (может быть не в начале)
            int hsIdx = -1;
            for (int i = 0; i < total; i++) {
                if ((rawBuf[i] & 0xFF) == HANDSHAKE) {
                    hsIdx = i;
                    log.accept("Found 0x56 at position " + i);
                    break;
                }
            }

            // Проверяем, начинаются ли данные с handshake response (0x56)
            InputStream in;
            DeviceReader reader;

            if (hsIdx == 0) {
                // Стандартный протокол: данные начинаются с handshake
                log.accept("Handshake response found (0x56). Parsing data...");
                byte[] remaining = new byte[total - 1];
                System.arraycopy(rawBuf, 1, remaining, 0, remaining.length);
                in = new java.io.SequenceInputStream(
                        new java.io.ByteArrayInputStream(remaining),
                        port.getInputStream());
                reader = new DeviceReader(new BufferedInputStream(in), year);
                reader._sum = 0;
            } else if (hsIdx > 0) {
                // Handshake найден, но не в начале - сдвигаем данные
                log.accept("Handshake found at offset " + hsIdx + ". Shifting data...");
                byte[] remaining = new byte[total - hsIdx - 1];
                System.arraycopy(rawBuf, hsIdx + 1, remaining, 0, remaining.length);
                in = new java.io.SequenceInputStream(
                        new java.io.ByteArrayInputStream(remaining),
                        port.getInputStream());
                reader = new DeviceReader(new BufferedInputStream(in), year);
                reader._sum = 0;
            } else {
                // Handshake не найден вообще
                log.accept("WARNING: No handshake (0x56) found anywhere in data!");
                log.accept("First byte: 0x" + String.format("%02X", rawBuf[0] & 0xFF));
                log.accept("Trying different baud rates might help:");
                log.accept("- Original protocol used 2400 baud");
                log.accept("- Try: 2400, 9600, 19200");
                log.accept("Attempting to parse without handshake (alternative protocol)...");

                // Парсим данные как есть, с начала
                byte[] data = new byte[total];
                System.arraycopy(rawBuf, 0, data, 0, total);
                in = new java.io.SequenceInputStream(
                        new java.io.ByteArrayInputStream(data),
                        port.getInputStream());
                reader = new DeviceReader(new BufferedInputStream(in), year);
                // Не вызываем readHandshake(), парсим сразу
            }

            // Детальный анализ байтов
            log.accept("=== BYTE-BY-BYTE ANALYSIS ===");
            for (int i = 0; i < Math.min(total, 20); i++) {
                log.accept(String.format("[%02d] 0x%02X (%3d)", i, rawBuf[i] & 0xFF, rawBuf[i] & 0xFF));
            }
            log.accept("=== END ANALYSIS ===");

            try {
                var date = reader.readDate();
                log.accept("Date parsed: " + date);

                reader.skip();
                int window = reader.readByte();
                log.accept("Window: " + window + " min" + (window > 90 ? " (INVALID!)" : ""));

                var toBed = reader.readTime();
                log.accept("To bed: " + toBed);

                var alarm = reader.readTime();
                log.accept("Alarm: " + alarm);

                int count = reader.readByte();
                log.accept("Moments count: " + count + (count > 50 ? " (SUSPICIOUS!)" : ""));

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
            } catch (ProtocolException e) {
                log.accept("Parse error: " + e.getMessage());
                log.accept("This might indicate:");
                log.accept("1. Alarm was OFF when sleeping");
                log.accept("2. Data corruption or sync issue");
                log.accept("3. Different watch model/firmware");
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
