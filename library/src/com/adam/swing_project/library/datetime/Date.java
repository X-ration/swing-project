package com.adam.swing_project.library.datetime;

import java.util.Calendar;
import java.util.Objects;

public class Date implements Copyable<Date>, Comparable<Date>{
    private int year;
    private int month;
    private int day;

    public Date(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public Date(java.util.Date utilDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(utilDate);
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH) + 1;
        this.day = calendar.get(Calendar.DAY_OF_MONTH);
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setAllField(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    @Override
    public void copyFrom(Date another) {
        setAllField(another.year, another.month, another.day);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Date date = (Date) o;
        return year == date.year && month == date.month && day == date.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }

    @Override
    public int compareTo(Date o) {
        int cmp = Integer.compare(year, o.year);
        if(cmp != 0)
            return cmp;
        cmp = Integer.compare(month, o.month);
        if(cmp != 0)
            return cmp;
        return Integer.compare(day, o.day);
    }

    @Override
    public String toString() {
        return "Date{" +
                "year=" + year +
                ", month=" + month +
                ", day=" + day +
                '}';
    }
}