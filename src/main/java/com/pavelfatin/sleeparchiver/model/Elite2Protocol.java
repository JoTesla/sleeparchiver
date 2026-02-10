package com.pavelfatin.sleeparchiver.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Elite2Protocol {

    /**
     * Парсинг flash log из Elite2.
     * Формат:
     *   [6]  month
     *   [7]  day
     *   [8-9]  year (LE)
     *   [10-11] timeToAlarm (LE, minutes)
     *   [16-17] window (LE, minutes)
     *   [22] toBed hours
     *   [23] toBed minutes
     *   [24] toBed seconds
     *   [25] event count
     *   [26+] events as 2-byte LE words (seconds since toBed)
     */
    public static Night parseFlashLog(byte[] data) throws ProtocolException {
        if (data.length < 26) {
            throw new ProtocolException("Flash log too short: " + data.length + " bytes");
        }

        int month = data[6] & 0xFF;
        int day = data[7] & 0xFF;
        int yearVal = SlipCodec.readWordLE(data, 8);

        LocalDate date;
        try {
            date = LocalDate.of(yearVal, month, day);
        } catch (Exception e) {
            throw new ProtocolException("Invalid date in flash log: " + yearVal + "-" + month + "-" + day);
        }

        int timeToAlarm = SlipCodec.readWordLE(data, 10);
        int window = SlipCodec.readWordLE(data, 16);

        int toBedH = data[22] & 0xFF;
        int toBedM = data[23] & 0xFF;
        int toBedS = data[24] & 0xFF;

        LocalTime toBed;
        try {
            toBed = LocalTime.of(toBedH, toBedM, toBedS);
        } catch (Exception e) {
            throw new ProtocolException("Invalid toBed time: " + toBedH + ":" + toBedM + ":" + toBedS);
        }

        // Alarm = toBed + timeToAlarm minutes
        LocalTime alarm = toBed.plusMinutes(timeToAlarm);

        int count = data[25] & 0xFF;
        List<LocalTime> moments = new ArrayList<>();
        for (int i = 0; i < count && (26 + i * 2 + 1) < data.length; i++) {
            int secondsSinceToBed = SlipCodec.readWordLE(data, 26 + i * 2);
            LocalTime moment = toBed.plusSeconds(secondsSinceToBed);
            moments.add(moment);
        }

        return new Night(date, alarm, window, toBed, moments);
    }
}
