package com.pavelfatin.sleeparchiver.model;

import com.fazecast.jSerialComm.SerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EliteProtocol {
    private static final int CMD_DATE = 0x02;
    private static final int CMD_ALARM = 0x04;
    private static final int CMD_EVENTS = 0x05;
    private static final int CMD_DEVICE_NAME = 0x07;
    private static final int CMD_TIME = 0x09;
    private static final int CMD_FLASH_LOG = 0x0A;

    private static final int POLL_TIMEOUT = 3000;
    private static final int POLL_INTERVAL = 50;

    private final SerialPort port;
    private final int year;
    private final Consumer<String> log;

    public EliteProtocol(SerialPort port, int year, Consumer<String> log) {
        this.port = port;
        this.year = year;
        this.log = log;
    }

    public record AlarmInfo(LocalTime alarmTime, int windowMinutes, LocalTime toBed) {}

    private String hexDump(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    /**
     * Формат ответа: C0 [cmd] [sizeLE 2 bytes] [data...] C0 00
     * extractPayload убирает фрейм и size prefix, возвращает чистые данные.
     */

    public LocalDate readDate() throws IOException {
        log.accept("CMD: readDate (0x02)");
        byte[] data = sendCommand(CMD_DATE, 10);

        // Формат: day(1) month(1) yearLE(2)
        if (data.length < 4) {
            throw new ProtocolException("Date data too short: " + data.length);
        }
        int day = data[0] & 0xFF;
        int month = data[1] & 0xFF;
        int y = SlipCodec.readWordLE(data, 2);
        if (y < 2000 || y > 2100) y = year;
        log.accept("  Date: " + y + "-" + month + "-" + day);

        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new ProtocolException("Invalid date: " + y + "-" + month + "-" + day);
        }
        return LocalDate.of(y, month, day);
    }

    public LocalTime readTime() throws IOException {
        log.accept("CMD: readTime (0x09)");
        byte[] data = sendCommand(CMD_TIME, 10);

        // Формат: seconds(1) minutes(1) hours(1) ???(1)
        if (data.length < 3) {
            throw new ProtocolException("Time data too short: " + data.length);
        }
        int s = data[0] & 0xFF;
        int m = data[1] & 0xFF;
        int h = data[2] & 0xFF;
        log.accept("  Time: " + h + ":" + m + ":" + s);
        return LocalTime.of(h, m, s);
    }

    public AlarmInfo readAlarm() throws IOException {
        log.accept("CMD: readAlarm (0x04)");
        byte[] data = sendCommand(CMD_ALARM, 20);

        // Формат: window(1) 00 alarmMin(1) 00 ??(1) 00 alarmH(1) ??(1) ??(1) 00 00 00 toBedH(1) toBedM(1)
        if (data.length < 14) {
            throw new ProtocolException("Alarm data too short: " + data.length);
        }

        for (int i = 0; i < data.length; i++) {
            log.accept(String.format("  alarm[%d] = 0x%02X (%d)", i, data[i] & 0xFF, data[i] & 0xFF));
        }

        int window = data[0] & 0xFF;
        int alarmMin = data[2] & 0xFF;
        int alarmH = data[6] & 0xFF;
        int toBedH = data[12] & 0xFF;
        int toBedM = data[13] & 0xFF;

        log.accept("  Alarm: " + alarmH + ":" + alarmMin + " window=" + window + " toBed=" + toBedH + ":" + toBedM);

        LocalTime alarmTime = LocalTime.of(alarmH, alarmMin);
        LocalTime toBed = (toBedH < 24 && toBedM < 60) ? LocalTime.of(toBedH, toBedM) : null;
        return new AlarmInfo(alarmTime, window, toBed);
    }

    public List<LocalTime> readEvents() throws IOException {
        log.accept("CMD: readEvents (0x05)");
        byte[] data = sendCommand(CMD_EVENTS, 150);

        // Формат: timeToAlarmLE(2) count(1) [hour(1) minute(1) second(1)] × count
        if (data.length < 3) {
            throw new ProtocolException("Events data too short: " + data.length);
        }

        int timeToAlarmSec = SlipCodec.readWordLE(data, 0);
        int timeToAlarmMin = timeToAlarmSec / 60;
        int timeToAlarmS = timeToAlarmSec % 60;
        log.accept("  TimeToAlarm: " + timeToAlarmMin + ":" + String.format("%02d", timeToAlarmS)
                + " (" + timeToAlarmSec + " sec)");

        int count = data[2] & 0xFF;
        log.accept("  Events count: " + count);

        List<LocalTime> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int offset = 3 + i * 3;
            if (offset + 2 >= data.length) break;
            int h = data[offset] & 0xFF;
            int m = data[offset + 1] & 0xFF;
            int s = data[offset + 2] & 0xFF;
            log.accept(String.format("  event[%d] = %d:%02d:%02d", i + 1, h, m, s));
            if (h < 24 && m < 60 && s < 60) {
                events.add(LocalTime.of(h, m, s));
            }
        }
        return events;
    }

    public byte[] readFlashLog() throws IOException {
        log.accept("CMD: readFlashLog (0x0A)");
        byte[] data = sendCommand(CMD_FLASH_LOG, 150);

        if (data.length <= 2) {
            log.accept("  Flash log empty or not supported");
            return null;
        }
        log.accept("  Flash log: " + data.length + " bytes");
        return data;
    }

    public String readDeviceName() throws IOException {
        log.accept("CMD: readDeviceName (0x07)");
        byte[] data = sendCommand(CMD_DEVICE_NAME, 36);

        int len = data.length;
        while (len > 0 && data[len - 1] == 0) len--;
        String name = new String(data, 0, len);
        log.accept("  Device name: '" + name + "'");
        return name;
    }

    /**
     * Отправляет команду, получает ответ, извлекает данные после size prefix.
     */
    private byte[] sendCommand(int cmd, int expectedBytes) throws IOException {
        byte[] cmdBytes = SlipCodec.makeCommand(cmd);
        log.accept("  Sending: " + hexDump(cmdBytes));
        byte[] frame = sendAndReceive(cmdBytes, expectedBytes);
        log.accept("  Raw frame (" + frame.length + "): " + hexDump(frame));
        byte[] payload = extractPayload(frame);
        log.accept("  Full payload (" + payload.length + "): " + hexDump(payload));

        // Payload начинается с 2-byte LE size, затем данные
        if (payload.length < 2) {
            return payload;
        }
        int size = SlipCodec.readWordLE(payload, 0);
        log.accept("  Size field: " + size);
        byte[] data = new byte[Math.min(size, payload.length - 2)];
        System.arraycopy(payload, 2, data, 0, data.length);
        log.accept("  Data (" + data.length + "): " + hexDump(data));
        return data;
    }

    /**
     * Как в оригинале: ждём expectedBytes в буфере (до 2 сек, 20 × 100ms),
     * затем читаем всё что накопилось.
     */
    private byte[] sendAndReceive(byte[] cmd, int expectedBytes) throws IOException {
        port.writeBytes(cmd, cmd.length);

        // Оригинал: 20 итераций по 100ms = 2 секунды макс
        for (int i = 0; i < 20; i++) {
            if (port.bytesAvailable() >= expectedBytes) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted", e);
            }
        }

        int available = port.bytesAvailable();
        if (available <= 0) {
            throw new IOException("No response from device (timeout)");
        }

        byte[] data = new byte[available];
        int read = port.readBytes(data, available);
        if (read < available) {
            byte[] trimmed = new byte[read];
            System.arraycopy(data, 0, trimmed, 0, read);
            return trimmed;
        }
        return data;
    }

    private byte[] extractPayload(byte[] frame) throws IOException {
        SlipCodec.validateFrame(frame);
        int start = 2; // пропускаем C0 и байт команды
        int end = frame.length - 1;
        if ((frame[end] & 0xFF) == 0x00 && end > 0 && (frame[end - 1] & 0xFF) == 0xC0) {
            end = end - 1;
        }
        if (start >= end) {
            return new byte[0];
        }
        byte[] payload = new byte[end - start];
        System.arraycopy(frame, start, payload, 0, payload.length);
        return SlipCodec.decodeEscapes(payload);
    }
}
