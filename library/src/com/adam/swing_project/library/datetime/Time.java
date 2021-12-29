package com.adam.swing_project.library.datetime;

import java.util.Objects;

public class Time implements Copyable<Time>{
    private int hour;
    private int minute;
    private int second;

    public Time(int hour, int minute, int second) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public int getHour() {
        return hour;
    }
    public int getMinute() {
        return minute;
    }
    public int getSecond() {
        return second;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public void setAllField(int hour, int minute, int second) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    //todo 迁移到util
    public void addSecond() {
        if(second < 59) {
            second++;
        } else if(minute < 59) {
            second = 0;
            minute++;
        } else if(hour < 23) {
            second = 0;
            minute = 0;
            hour++;
        } else {
            second = 0;
            minute = 0;
            hour = 0;
        }
    }

    public void minusSecond() {
        if(second > 0) {
            second--;
        } else if(minute > 0) {
            minute--;
            second = 59;
        } else if(hour > 0) {
            hour--;
            minute = second = 59;
        } else {
            hour = 23;
            minute = second = 59;
        }
    }

    @Override
    public void copyFrom(Time another) {
        setAllField(another.hour, another.minute, another.second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Time time = (Time) o;
        return hour == time.hour && minute == time.minute && second == time.second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hour, minute, second);
    }
}