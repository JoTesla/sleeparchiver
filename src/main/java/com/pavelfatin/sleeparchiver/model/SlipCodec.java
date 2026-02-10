package com.pavelfatin.sleeparchiver.model;

import java.io.ByteArrayOutputStream;

public class SlipCodec {
    public static final byte FRAME_END = (byte) 0xC0;
    public static final byte FRAME_ESC = (byte) 0xDB;
    public static final byte FRAME_ESC_END = (byte) 0xDC;
    public static final byte FRAME_ESC_ESC = (byte) 0xDD;

    public static byte[] decodeEscapes(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        for (int i = 0; i < data.length; i++) {
            if (data[i] == FRAME_ESC && i + 1 < data.length) {
                if (data[i + 1] == FRAME_ESC_END) {
                    out.write(FRAME_END & 0xFF);
                    i++;
                } else if (data[i + 1] == FRAME_ESC_ESC) {
                    out.write(FRAME_ESC & 0xFF);
                    i++;
                } else {
                    out.write(data[i] & 0xFF);
                }
            } else {
                out.write(data[i] & 0xFF);
            }
        }
        return out.toByteArray();
    }

    public static byte[] encodeEscapes(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 16);
        for (byte b : data) {
            if (b == FRAME_END) {
                out.write(FRAME_ESC & 0xFF);
                out.write(FRAME_ESC_END & 0xFF);
            } else if (b == FRAME_ESC) {
                out.write(FRAME_ESC & 0xFF);
                out.write(FRAME_ESC_ESC & 0xFF);
            } else {
                out.write(b & 0xFF);
            }
        }
        return out.toByteArray();
    }

    public static byte[] makeCommand(int cmd) {
        return new byte[]{FRAME_END, (byte) cmd, 0x00, FRAME_END};
    }

    public static int readWordLE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    public static void validateFrame(byte[] data) throws ProtocolException {
        if (data.length < 2) {
            throw new ProtocolException("Frame too short: " + data.length + " bytes");
        }
        if ((data[0] & 0xFF) != 0xC0) {
            throw new ProtocolException(String.format(
                    "Frame does not start with 0xC0, got: 0x%02X", data[0] & 0xFF));
        }
        // Фрейм заканчивается на C0 00 (или просто C0)
        if ((data[data.length - 1] & 0xFF) == 0x00 && data.length >= 3
                && (data[data.length - 2] & 0xFF) == 0xC0) {
            return; // C0 ... C0 00
        }
        if ((data[data.length - 1] & 0xFF) == 0xC0) {
            return; // C0 ... C0
        }
        throw new ProtocolException(String.format(
                "Frame does not end properly, last bytes: 0x%02X 0x%02X",
                data[data.length - 2] & 0xFF, data[data.length - 1] & 0xFF));
    }
}
