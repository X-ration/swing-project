package com.adam.swing_project.library.util;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;

import java.util.Calendar;
import java.util.regex.Pattern;

public class DateTimeUtil {

    public static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-[01]\\d-[0-3]\\d")
            , TIME_PATTERN = Pattern.compile("\\d{2}:[0-5]\\d:[0-5]\\d");

    public static Time getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        return new Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }
    public static Time getCurrentTimePlus(int hour, int minute, int second) {
        java.util.Date date = new java.util.Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, hour);
        calendar.add(Calendar.MINUTE, minute);
        calendar.add(Calendar.SECOND, second);
        return new Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }

    public static void timeSelfPlusTime(Time thisTime, Time otherTime) {
        int hour = thisTime.getHour() + otherTime.getHour(),
                minute = thisTime.getMinute() + otherTime.getMinute(),
                second = thisTime.getSecond() + otherTime.getSecond();
        minute += (second / 60);
        second = second % 60;
        hour += (minute / 60);
        minute = minute % 60;
        thisTime.setAllField(hour, minute, second);
    }

    public static Time timeMinusTime(Time time1, Time time2) {
        int hour = time1.getHour() - time2.getHour();
        int minute = time1.getMinute() - time2.getMinute();
        int second = time1.getSecond() - time2.getSecond();
        if(second < 0) {
            minute--;
            second+=60;
        }
        if(minute < 0) {
            hour--;
            minute+=60;
        }
        return new Time(hour, minute, second);
    }

    public static Date datePlusDay(Date date, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
        calendar.add(Calendar.DAY_OF_YEAR, day);
        return new Date(calendar.getTime());
    }

    public static Date getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        return new Date(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static long translateTimeToSeconds(Time time) {
        long result = 0;
        result += (time.getHour() * 3600L);
        result += (time.getMinute() * 60L);
        result += (time.getSecond());
        return result;
    }

    public static java.util.Date translateDateTimeToUtilDate(Date date, Time time) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
        calendar.set(Calendar.HOUR_OF_DAY, time.getHour());
        calendar.set(Calendar.MINUTE, time.getMinute());
        calendar.set(Calendar.SECOND, time.getSecond());
        return calendar.getTime();
    }

    public static String wrapDateYearToDay(Date date) {
        int year = date.getYear(), month = date.getMonth(), day = date.getDay();
        return year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day;
    }

    public static Date unwrapDateYearToDay(String dateString) {
        Assert.isTrue(DATE_PATTERN.matcher(dateString).matches(), "Invalid date string: " + dateString);
        String[] splits = dateString.split("-");
        int year = Integer.parseInt(splits[0]);
        int month = Integer.parseInt(splits[1]);
        int day = Integer.parseInt(splits[2]);
        Assert.isTrue(month >= 1 && month <= 12 && day >= 1 && day <= 31, "Invalid date string: " + dateString);
        return new Date(year, month, day);
    }

    public static Time unwrapTimeHourToSecond(String timeString) {
        Assert.isTrue(TIME_PATTERN.matcher(timeString).matches(), "Invalid time string: " + timeString);
        String[] splits = timeString.split(":");
        int hour = Integer.parseInt(splits[0]);
        int minute = Integer.parseInt(splits[1]);
        int second = Integer.parseInt(splits[2]);
        return new Time(hour, minute, second);
    }

    public static String wrapTimeHourToSecond(Time time) {
        return wrapTimeHourToSecond(time.getHour(),time.getMinute(),time.getSecond());
    }
    public static String wrapTimeHourToMinute(Time time) {
        return wrapTimeHourToMinute(time.getHour(), time.getMinute());
    }
    public static String wrapTimeHourToSecond(int hour, int minute, int second) {
        StringBuilder sb = new StringBuilder();
        sb.append(hour < 10 ? "0" : "").append(hour).append(":")
                .append(minute < 10 ? "0" : "").append(minute).append(":")
                .append(second < 10 ? "0" : "").append(second);
        return sb.toString();
    }
    public static String wrapTimeHourToMinute(int hour, int minute) {
        StringBuilder sb = new StringBuilder();
        sb.append(hour < 10 ? "0" : "").append(hour).append(":")
                .append(minute < 10 ? "0" : "").append(minute);
        return sb.toString();
    }

    public static void main(String[] args) {
        Date date = new Date(2000,1,1);
        Time time = new Time(99,99,99);
        java.util.Date date1 = translateDateTimeToUtilDate(date, time);
        System.out.println(date1);
    }
}
