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

    public record AlarmInfo(LocalTime time, int windowMinutes) {}

    public LocalDate readDate() throws IOException {
        byte[] resp = sendAndReceive(SlipCodec.makeCommand(CMD_DATE), 64);
        byte[] decoded = extractPayload(resp);
        if (decoded.length < 4) {
            throw new ProtocolException("Date response too short: " + decoded.length);
        }
        int month = decoded[0] & 0xFF;
        int day = decoded[1] & 0xFF;
        int y = SlipCodec.readWordLE(decoded, 2);
        if (y < 2000) y = year;
        log.accept("Elite date: " + y + "-" + month + "-" + day);
        return LocalDate.of(y, month, day);
    }

    public LocalTime readTime() throws IOException {
        byte[] resp = sendAndReceive(SlipCodec.makeCommand(CMD_TIME), 64);
        byte[] decoded = extractPayload(resp);
        if (decoded.length < 3) {
            throw new ProtocolException("Time response too short: " + decoded.length);
        }
        int h = decoded[0] & 0xFF;
        int m = decoded[1] & 0xFF;
        int s = decoded[2] & 0xFF;
        log.accept("Elite time: " + h + ":" + m + ":" + s);
        return LocalTime.of(h, m, s);
    }

    public AlarmInfo readAlarm() throws IOException {
        byte[] resp = sendAndReceive(SlipCodec.makeCommand(CMD_ALARM), 64);
        byte[] decoded = extractPayload(resp);
        if (decoded.length < 4) {
            throw new ProtocolException("Alarm response too short: " + decoded.length);
        }
        int h = decoded[0] & 0xFF;
        int m = decoded[1] & 0xFF;
        int window = SlipCodec.readWordLE(decoded, 2);
        log.accept("Elite alarm: " + h + ":" + m + " window=" + window);
        return new AlarmInfo(LocalTime.of(h, m), window);
    }

    public List<LocalTime> readEvents() throws IOException {
        byte[] resp = sendAndReceive(SlipCodec.makeCommand(CMD_EVENTS), 256);
        byte[] decoded = extractPayload(resp);
        List<LocalTime> events = new ArrayList<>();
        if (decoded.length < 1) {
            return events;
        }
        int count = decoded[0] & 0xFF;
        log.accept("Elite events count: " + count);
        for (int i = 0; i < count && (1 + i * 2 + 1) < decoded.length; i++) {
            int h = decoded[1 + i * 2] & 0xFF;
            int m = decoded[1 + i * 2 + 1] & 0xFF;
            if (h < 24 && m < 60) {
                events.add(LocalTime.of(h, m));
            }
        }
        return events;
    }

    public byte[] readFlashLog() throws IOException {
        byte[] resp = sendAndReceive(SlipCodec.makeCommand(CMD_FLASH_LOG), 4096);
        byte[] decoded = extractPayload(resp);
        if (decoded.length <= 2) {
            log.accept("Flash log empty or not supported (Elite model)");
            return null;
        }
        log.accept("Flash log: " + decoded.length + " bytes");
        return decoded;
    }

    public String readDeviceName() throws IOException {
        byte[] resp = sendAndReceive(SlipCodec.makeCommand(CMD_DEVICE_NAME), 128);
        byte[] decoded = extractPayload(resp);
        // Trim trailing zeros
        int len = decoded.length;
        while (len > 0 && decoded[len - 1] == 0) len--;
        String name = new String(decoded, 0, len);
        log.accept("Device name: " + name);
        return name;
    }

    private byte[] sendAndReceive(byte[] cmd, int expectedBytes) throws IOException {
        port.writeBytes(cmd, cmd.length);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[expectedBytes];
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT;

        while (System.currentTimeMillis() < deadline) {
            int n = port.readBytes(tmp, tmp.length);
            if (n > 0) {
                buffer.write(tmp, 0, n);
                // Проверяем, получили ли полный фрейм (заканчивается на C0 00 или C0)
                byte[] data = buffer.toByteArray();
                if (data.length >= 4 && isFrameComplete(data)) {
                    return data;
                }
            } else {
                try {
                    Thread.sleep(POLL_INTERVAL);
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted", e);
                }
            }
        }

        byte[] data = buffer.toByteArray();
        if (data.length > 0) {
            return data;
        }
        throw new IOException("No response from device (timeout)");
    }

    private boolean isFrameComplete(byte[] data) {
        if (data.length < 3) return false;
        int last = data[data.length - 1] & 0xFF;
        int prev = data[data.length - 2] & 0xFF;
        return (last == 0x00 && prev == 0xC0) || last == 0xC0;
    }

    private byte[] extractPayload(byte[] frame) throws IOException {
        SlipCodec.validateFrame(frame);
        // Убираем начальный C0, команду, и конечный C0 (00)
        int start = 2; // пропускаем C0 и байт команды
        int end = frame.length - 1;
        if ((frame[end] & 0xFF) == 0x00 && end > 0 && (frame[end - 1] & 0xFF) == 0xC0) {
            end = end - 1; // убираем C0 00
        }
        // end сейчас указывает на завершающий C0
        if (start >= end) {
            return new byte[0];
        }
        byte[] payload = new byte[end - start];
        System.arraycopy(frame, start, payload, 0, payload.length);
        return SlipCodec.decodeEscapes(payload);
    }
}
