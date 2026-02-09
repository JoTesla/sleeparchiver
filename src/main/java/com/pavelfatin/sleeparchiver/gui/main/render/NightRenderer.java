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

package com.pavelfatin.sleeparchiver.gui.main.render;

import com.pavelfatin.sleeparchiver.model.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NightRenderer {
    private static final int H_GAP = 11;
    private static final int ROW_HEIGHT = 100;

    private static final Font FONT_BOLD = Font.font("Arial", FontWeight.BOLD, 12);
    private static final Font FONT_PLAIN = Font.font("Arial", FontWeight.NORMAL, 12);

    private static final Color COLOR_BLUE = Color.rgb(0, 0, 148);
    private static final Color COLOR_GREEN = Color.rgb(0, 110, 0);
    private static final Color COLOR_FRAME = Color.rgb(204, 204, 204);
    private static final Color COLOR_SELECTED_BORDER = Color.rgb(153, 153, 153);
    private static final Color COLOR_SELECTED_BACKGROUND = Color.rgb(255, 255, 233);
    private static final Color COLOR_HOLIDAY = Color.rgb(180, 0, 0);

    private static final Color COLOR_EASE_HARD = Color.web("#FF4040");
    private static final Color COLOR_EASE_NORMAL = Color.web("#BBBBBB");
    private static final Color COLOR_EASE_EASY = Color.web("#40CC40");
    private static final Color COLOR_QUALITY_BAD = Color.web("#FF4040");
    private static final Color COLOR_QUALITY_AVERAGE = Color.web("#BBBBBB");
    private static final Color COLOR_QUALITY_GOOD = Color.web("#40CC40");

    private static final int MIN_MOMENTS_SPACE = 3;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Transform _transform;

    public void setTransform(Transform transform) {
        _transform = transform;
    }

    public void setNights(List<Night> nights) {
        _transform.setNights(completeOf(nights));
    }

    private static List<Night> completeOf(List<Night> nights) {
        List<Night> complete = new ArrayList<>();
        for (Night night : nights) {
            if (night.isComplete()) {
                complete.add(night);
            }
        }
        return complete;
    }

    public void setResolution(double resolution) {
        _transform.setResolution(resolution);
    }

    public int getPreferredHeight() {
        return ROW_HEIGHT;
    }

    public int getPreferredWidth(List<Night> nights) {
        return H_GAP + _transform.getPreferredWidth(nights) + H_GAP;
    }

    private int toX(SleepInstant instant) {
        return H_GAP + _transform.toX(instant);
    }

    public void render(Canvas canvas, Night night, boolean selected, boolean focused) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        g.clearRect(0, 0, w, h);

        drawFrame(g, w, h, selected, focused);

        LocalDate date = night.getDate();
        if (date != null) {
            drawDate(g, 13, 22, date);
        }

        drawConditionsAndComments(g, 313, 22, night);
        drawObservations(g, 125, 13, night);

        if (night.isAlarmWorked()) {
            drawAlarm(g, 190, 16);
        }

        if (night.isComplete()) {
            Metrics metrics = night.getMetrics();

            drawEquation(g, 208, 302, 22, metrics);

            _transform.setNight(night);

            List<SleepSpan> spans = night.getMetrics().getSpans();

            drawBars(g, spans);
            drawGaps(g, spans, selected);
            if (night.hasWindow()) {
                drawWindow(g, night);
            }
            drawMoments(g, spans);
            drawLengths(g, spans);
        }
    }

    private void drawDate(GraphicsContext g, int x, int y, LocalDate date) {
        g.setFont(FONT_BOLD);
        boolean isHoliday = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        g.setFill(isHoliday ? COLOR_HOLIDAY : Color.BLACK);
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        g.fillText(date.format(DATE_FORMATTER) + " " + dayOfWeek, x, y);
    }

    private void drawConditionsAndComments(GraphicsContext g, int x, int y, Night night) {
        g.setFont(FONT_BOLD);
        g.setFill(COLOR_GREEN);
        String conditions = String.join(" | ", night.getConditions());
        g.fillText(conditions, x, y);

        double xx = conditions.isEmpty() ? 0 : textWidth(conditions, FONT_BOLD) + 6;

        g.setFont(FONT_PLAIN);
        g.setFill(COLOR_BLUE);
        String comments = night.getComments().replaceAll("\\n", "; ");
        g.fillText(comments.isEmpty() ? "" : String.format("(%s)", comments), x + xx, y);
    }

    private void drawEquation(GraphicsContext g, int x1, int x2, int y, Metrics metrics) {
        g.setFont(FONT_BOLD);
        g.setFill(COLOR_BLUE);
        SleepInstant durationInstant = new SleepInstant(metrics.getDuration());
        String duration = String.format("%d:%02d",
                durationInstant.time().getHour(), durationInstant.time().getMinute());
        String equation = String.format("%s / %d = %d",
                duration, metrics.getSpansCount(), metrics.getAverage());
        double gaps = x2 - x1 - textWidth(equation, FONT_BOLD);
        double x = x1 + Math.round(gaps / 2.0);
        g.fillText(equation, x, y);
    }

    private void drawAlarm(GraphicsContext g, int x, int y) {
        g.setStroke(Color.BLACK);
        g.setLineWidth(1);
        g.strokeLine(x, y, x + 2, y + 5);
        g.strokeLine(x + 3, y + 3, x + 5, y - 3);
    }

    private void drawFrame(GraphicsContext g, double w, double h, boolean selected, boolean focused) {
        if (selected) {
            g.setFill(COLOR_SELECTED_BACKGROUND);
            g.fillRoundRect(2, 2, w - 5, h - 5, 15, 15);
        }

        g.setStroke(COLOR_FRAME);
        g.setLineWidth(1);

        g.strokeLine(3, 29, w - 4, 29);

        g.strokeLine(114, 6, 114, 6 + 22);
        g.strokeLine(175, 6, 175, 6 + 22);
        g.strokeLine(208, 6, 208, 6 + 22);
        g.strokeLine(302, 6, 302, 6 + 22);

        g.setStroke(focused ? Color.BLACK : COLOR_SELECTED_BORDER);
        g.strokeRoundRect(2, 2, w - 5, h - 5, 15, 15);
    }

    private Color backgroundOf(boolean selected) {
        return selected ? COLOR_SELECTED_BACKGROUND : Color.WHITE;
    }

    private void drawObservations(GraphicsContext g, int x, int y, Night night) {
        Ease asleep = night.getEaseOfFallingAsleep();
        if (asleep.isKnown()) {
            g.setFill(easeColor(asleep));
            g.fillPolygon(new double[]{x, x + 9, x + 9}, new double[]{y, y, y + 9}, 3);
        }

        Quality quality = night.getQualityOfSleep();
        if (quality.isKnown()) {
            g.setFill(qualityColor(quality));
            g.fillRect(x + 12, y, 16, 9);
        }

        Ease waking = night.getEaseOfWakingUp();
        if (waking.isKnown()) {
            g.setFill(easeColor(waking));
            g.fillPolygon(new double[]{x + 31, x + 31 + 9, x + 31}, new double[]{y, y, y + 9}, 3);
        }
    }

    private void drawMoments(GraphicsContext g, List<SleepSpan> spans) {
        g.setFont(FONT_PLAIN);
        g.setFill(Color.BLACK);
        for (SleepSpan span : spans) {
            String moment = String.format("%d:%02d",
                    span.begin().time().getHour(), span.begin().time().getMinute());
            double[] r = rectangleOf(span);
            if (r[2] > (textWidth(moment, FONT_PLAIN) + MIN_MOMENTS_SPACE)) {
                g.fillText(moment, r[0], r[1] - 4);
            }
        }
    }

    private void drawLengths(GraphicsContext g, List<SleepSpan> spans) {
        g.setFont(FONT_PLAIN);
        g.setFill(Color.BLACK);
        for (SleepSpan span : spans) {
            String length = String.format("%d", span.toMinutes());
            double tw = textWidth(length, FONT_PLAIN);
            double th = textHeight(length, FONT_PLAIN);
            double[] r = rectangleOf(span);
            if (r[2] > tw) {
                g.fillText(length,
                        r[0] + Math.round((r[2] - tw) / 2),
                        r[1] + r[3] + th + 4);
            }
        }
    }

    private void drawBars(GraphicsContext g, List<SleepSpan> spans) {
        for (SleepSpan span : spans) {
            double[] r = rectangleOf(span);
            g.setFill(colorOf(span));
            g.fillRect(r[0], r[1], r[2], r[3]);
        }
    }

    private void drawGaps(GraphicsContext g, List<SleepSpan> spans, boolean selected) {
        g.setFill(backgroundOf(selected));
        for (SleepSpan span : spans) {
            double[] r = rectangleOf(span);
            g.fillRect(r[0] - 1, r[1], 3, r[3]);
        }
    }

    private void drawWindow(GraphicsContext g, Night night) {
        g.setStroke(Color.RED);
        g.setLineWidth(1);
        g.setLineDashes(2);
        double[] alarm = alarmRectangleOf(night);
        g.strokeRect(alarm[0], alarm[1], alarm[2] - 1, alarm[3] - 1);
        g.setLineDashes(null);
    }

    private double[] alarmRectangleOf(Night night) {
        double[] result = rectangleOf(night.getMetrics().getTotalSpan());
        int width = _transform.toWidth(night.getWindow());
        result[0] = result[0] + result[2] - width;
        result[2] = width;
        return result;
    }

    private double[] rectangleOf(SleepSpan span) {
        int x1 = toX(span.begin());
        int x2 = toX(span.end());
        int y1 = 53;
        int y2 = y1 + 22;
        return new double[]{x1, y1, x2 - x1, y2 - y1};
    }

    private static Color colorOf(SleepSpan span) {
        int c = Math.min(span.toMinutes() / 3, 127);
        return Color.rgb(127 - c, 212 - c, 255 - c);
    }

    private static Color easeColor(Ease ease) {
        return switch (ease) {
            case Hard -> COLOR_EASE_HARD;
            case Normal -> COLOR_EASE_NORMAL;
            case Easy -> COLOR_EASE_EASY;
            default -> Color.TRANSPARENT;
        };
    }

    private static Color qualityColor(Quality quality) {
        return switch (quality) {
            case Bad -> COLOR_QUALITY_BAD;
            case Average -> COLOR_QUALITY_AVERAGE;
            case Good -> COLOR_QUALITY_GOOD;
            default -> Color.TRANSPARENT;
        };
    }

    private static double textWidth(String text, Font font) {
        Text t = new Text(text);
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
    }

    private static double textHeight(String text, Font font) {
        Text t = new Text(text);
        t.setFont(font);
        return t.getLayoutBounds().getHeight();
    }
}
