package com.pavelfatin.sleeparchiver.model;

import com.fazecast.jSerialComm.SerialPort;
import com.pavelfatin.sleeparchiver.lang.Utilities;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Device {
    private static final int HANDSHAKE = 86;

    private static final int TIMEOUT = 2000;
    private static final int BUFFER = 250;
    private static final int DELAY = 500;

    private String _app;
    private int _year;

    public Device(String app, int year) {
        _app = app;
        _year = year;
    }

    public Night readData() {
        List<SerialPort> ports = findFreeSerialPorts();

        for (SerialPort port : ports) {
            try {
                return readNight(port, _year);
            } catch ( IOException e) {
                // Пропускаем порт
            }
        }

        return null;
    }

    private static Night readNight(SerialPort port, int year) throws IOException {
        port.setBaudRate(19200);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, TIMEOUT, 0);

        if (!port.openPort()) {
            throw new IOException("Cannot open port: " + port.getSystemPortName());
        }

        try {
            port.clearRTS();
            port.setDTR();

            sendHandshake(port);
            sleep(DELAY);

            return readNightFromPort(port, year);
        } finally {
            port.closePort();
        }
    }

    private static void sendHandshake(SerialPort port) throws IOException {
        OutputStream out = port.getOutputStream();
        try {
            out.write(HANDSHAKE);
            out.flush();
        } finally {
            Utilities.close(out);
        }
    }

    private static Night readNightFromPort(SerialPort port, int year) throws IOException {
        InputStream in = new BufferedInputStream(port.getInputStream());
        try {
            return readNight(new DeviceReader(in, year));
        } finally {
            Utilities.close(in);
        }
    }

    static Night readNight(InputStream stream, int year) throws IOException {
        return readNight(new DeviceReader(stream, year));
    }

    private static Night readNight(DeviceReader reader) throws IOException {
        reader.readHandshake();

        Date date = reader.readDate();
        reader.skip();
        int window = reader.readByte();
        Time toBed = reader.readTime();
        Time alarm = reader.readTime();

        int count = reader.readByte();
        List<Time> moments = new ArrayList<>();
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

    private static List<SerialPort> findFreeSerialPorts() {
        List<SerialPort> result = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (!port.isOpen()) {
                result.add(port);
            }
        }
        return result;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
