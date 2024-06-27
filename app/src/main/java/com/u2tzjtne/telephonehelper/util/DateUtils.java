package com.u2tzjtne.telephonehelper.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
public class DateUtils {

    public static String convertTimestamp(long timestamp, boolean needTime) {
        try {
            Instant instant = Instant.ofEpochMilli(timestamp);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            LocalDate today = LocalDate.now();
            LocalDate date = dateTime.toLocalDate();
            if (date.equals(today)) {
                // 时间戳在当天内，返回时间
                return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                // 时间戳不在当天内，返回日期
                return dateTime.format(DateTimeFormatter.ofPattern(needTime ? "M月d日" : "M月d日 HH:mm"));
            }
        }catch (Exception e){
            return "";
        }
    }

    public static String getCallDuration(long duration) {
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration - TimeUnit.DAYS.toMillis(days));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes));
        if (days > 0) {
            return days + "天" + hours + "小时" + minutes + "分钟" + seconds + "秒";
        } else if (hours > 0) {
            return hours + "小时" + minutes + "分钟" + seconds + "秒";
        } else if (minutes > 0) {
            return minutes + "分钟" + seconds + "秒";
        } else {
            return seconds + "秒";
        }
    }
}


