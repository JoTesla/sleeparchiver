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

import com.pavelfatin.sleeparchiver.model.xml.LocalDateAdapter;
import com.pavelfatin.sleeparchiver.model.xml.LocalTimeAdapter;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@XmlRootElement(name = "night")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Night {
    private static final Integer[] WINDOWS = new Integer[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
    private static final NightsComparator NIGHTS_COMPARATOR = new NightsComparator();


    @XmlAttribute(name = "date")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate _date;

    @XmlAttribute(name = "alarm")
    @XmlJavaTypeAdapter(LocalTimeAdapter.class)
    private LocalTime _alarm;

    @XmlAttribute(name = "window")
    private int _window;

    @XmlAttribute(name = "toBed")
    @XmlJavaTypeAdapter(LocalTimeAdapter.class)
    private LocalTime _toBed;

    @XmlAttribute(name = "easeOfFallingAsleep")
    private Ease _easeOfFallingAsleep = Ease.Unknown;

    @XmlAttribute(name = "qualityOfSleep")
    private Quality _qualityOfSleep = Quality.Unknown;

    @XmlAttribute(name = "easeOfWakingUp")
    private Ease _easeOfWakingUp = Ease.Unknown;

    @XmlAttribute(name = "alarmWorked")
    private Boolean _alarmWorked;

    @XmlAttribute(name = "comments")
    private String _comments;

    @XmlElement(name = "moment")
    @XmlElementWrapper(name = "moments")
    @XmlJavaTypeAdapter(LocalTimeAdapter.class)
    private List<LocalTime> _moments = new ArrayList<>();

    @XmlElement(name = "condition")
    @XmlElementWrapper(name = "conditions")
    private List<String> _conditions = new ArrayList<>();

    @XmlTransient
    private Metrics _metrics;


    private Night() {
    }

    public Night(LocalDate date, LocalTime alarm, int window, LocalTime toBed, List<LocalTime> moments) {
        _date = date;
        _alarm = alarm;
        _window = window;
        _toBed = toBed;
        _moments = new ArrayList<>(moments);
    }

    public Night(LocalDate date, LocalTime alarm, int window, LocalTime toBed,
                 Ease easeOfFallingAsleep, Quality qualityOfSleep, Ease easeOfWakingUp,
                 boolean alarmWorked, String comments,
                 List<LocalTime> moments, List<String> conditions) {
        _date = date;
        _alarm = alarm;
        _window = window;
        _toBed = toBed;
        _easeOfFallingAsleep = easeOfFallingAsleep;
        _qualityOfSleep = qualityOfSleep;
        _easeOfWakingUp = easeOfWakingUp;
        _alarmWorked = alarmWorked ? true : null;
        _comments = comments.isEmpty() ? null : comments;
        _moments = new ArrayList<>(moments);
        _conditions = new ArrayList<>(conditions);
    }

    public LocalDate getDate() {
        return _date;
    }

    public LocalTime getAlarm() {
        return _alarm;
    }

    public int getWindow() {
        return _window;
    }

    public boolean hasWindow() {
        return _window > 0;
    }

    public LocalTime getToBed() {
        return _toBed;
    }

    public Ease getEaseOfFallingAsleep() {
        return _easeOfFallingAsleep;
    }

    public Quality getQualityOfSleep() {
        return _qualityOfSleep;
    }

    public Ease getEaseOfWakingUp() {
        return _easeOfWakingUp;
    }

    public boolean isAlarmWorked() {
        return _alarmWorked == null ? false : _alarmWorked;
    }

    public String getComments() {
        return _comments == null ? "" : _comments;
    }

    public List<LocalTime> getMoments() {
        return Collections.unmodifiableList(_moments);
    }

    public int getMomentsCount() {
        return _moments.size();
    }

    public boolean hasMoments() {
        return getMomentsCount() > 0;
    }

    public List<String> getConditions() {
        return Collections.unmodifiableList(_conditions);
    }

    public int getConditionsCount() {
        return _conditions.size();
    }

    public boolean hasConditions() {
        return getConditionsCount() > 0;
    }

    public Night with(List<String> conditions) {
        return new Night(_date, _alarm, _window, _toBed,
                _easeOfFallingAsleep, _qualityOfSleep, _easeOfWakingUp,
                _alarmWorked != null && _alarmWorked, _comments != null ? _comments : "",
                _moments, conditions);
    }

    public boolean isComplete() {
        return _alarm != null
                && _toBed != null
                && hasMoments();
    }

    List<LocalTime> getCompleteMoments() {
        List<LocalTime> moments = new ArrayList<>();
        moments.add(_toBed);
        for (LocalTime m : _moments) {
            // Skip moments after alarm (both in morning range, i.e. before 16:00)
            if (_alarm != null && _alarm.isBefore(LocalTime.of(16, 0))
                    && m.isBefore(LocalTime.of(16, 0)) && m.isAfter(_alarm)) {
                continue;
            }
            moments.add(m);
        }
        moments.add(_alarm);
        return moments;
    }

    public Metrics getMetrics() {
        if (!isComplete()) {
            throw new IllegalStateException("Metrics are unavailable: data is not complete.");
        }
        if (_metrics == null) {
            _metrics = new Metrics(this);
        }
        return _metrics;
    }

    public static Integer[] getWindows() {
        return WINDOWS;
    }

    public static Comparator<Night> getComparator() {
        return NIGHTS_COMPARATOR;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Night other = (Night) obj;
        return _window == other._window
                && Objects.equals(_date, other._date)
                && Objects.equals(_alarm, other._alarm)
                && Objects.equals(_toBed, other._toBed)
                && Objects.equals(_easeOfFallingAsleep, other._easeOfFallingAsleep)
                && Objects.equals(_qualityOfSleep, other._qualityOfSleep)
                && Objects.equals(_easeOfWakingUp, other._easeOfWakingUp)
                && Objects.equals(_alarmWorked, other._alarmWorked)
                && Objects.equals(_comments, other._comments)
                && Objects.equals(_moments, other._moments)
                && Objects.equals(_conditions, other._conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_date, _alarm, _window, _toBed,
                _easeOfFallingAsleep, _qualityOfSleep, _easeOfWakingUp,
                _alarmWorked, _comments, _moments, _conditions);
    }

    @Override
    public String toString() {
        return Objects.toString(_date) + ", " +
                Objects.toString(_alarm) + ", " +
                _window + ", " +
                Objects.toString(_toBed) + ", " +
                _easeOfFallingAsleep + ", " +
                _qualityOfSleep + ", " +
                _easeOfWakingUp + ", " +
                _alarmWorked + ", " +
                _comments + ", " +
                _moments + ", " +
                _conditions;
    }
}
