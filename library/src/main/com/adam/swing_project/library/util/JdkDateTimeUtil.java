package com.adam.swing_project.library.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 和日期，时间相关的工具类
 */
public class JdkDateTimeUtil {
    private static final JdkDateTimeUtil instance = new JdkDateTimeUtil();

    public static JdkDateTimeUtil getInstance() {
        return instance;
    }

    public String getDateTimeOfTodayInStandardFormat() {
        return getDateTimeOfTodayInFormat("yyyy-MM-dd HH:mm:ss");
    }
    public String getDateTimeOfTodayInFormat(String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        return simpleDateFormat.format(date);
    }

    public Date getDateInFormat(String dateString, String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        try {
            return simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Date getDateInStandardFormat(String dateString) {
        return getDateInFormat(dateString, "yyyy-MM-dd HH:mm:ss");
    }

}
