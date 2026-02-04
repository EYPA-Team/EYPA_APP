package com.eypa.app.utils;

import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeAgoUtils {

    public static String getRelativeTime(Context context, String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }

        try {
            // 解析日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateString);

            if (date == null) {
                return "";
            }

            long timeInMillis = date.getTime();
            long now = System.currentTimeMillis();
            long diff = now - timeInMillis;

            // 计算时间差（毫秒）
            if (diff < 0) {
                return "未来";
            }

            // 计算时间差（秒）
            long diffSeconds = diff / 1000;
            if (diffSeconds < 60) {
                return diffSeconds + "秒前";
            }

            // 计算时间差（分钟）
            long diffMinutes = diffSeconds / 60;
            if (diffMinutes < 60) {
                return diffMinutes + "分钟前";
            }

            // 计算时间差（小时）
            long diffHours = diffMinutes / 60;
            if (diffHours < 24) {
                return diffHours + "小时前";
            }

            // 计算时间差（天）
            long diffDays = diffHours / 24;
            if (diffDays < 30) {
                return diffDays + "天前";
            }

            // 计算时间差（月）
            long diffMonths = diffDays / 30;
            if (diffMonths < 12) {
                return diffMonths + "个月前";
            }

            // 计算时间差（年）
            long diffYears = diffMonths / 12;
            return diffYears + "年前";

        } catch (ParseException e) {
            // 尝试其他格式
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(dateString);

                if (date == null) {
                    return dateString;
                }

                long timeInMillis = date.getTime();
                long now = System.currentTimeMillis();
                long diffDays = TimeUnit.MILLISECONDS.toDays(now - timeInMillis);

                if (diffDays < 365) {
                    if (diffDays == 0) {
                        return "今天";
                    } else if (diffDays == 1) {
                        return "昨天";
                    } else if (diffDays < 30) {
                        return diffDays + "天前";
                    } else {
                        return (diffDays / 30) + "个月前";
                    }
                } else {
                    return (diffDays / 365) + "年前";
                }

            } catch (ParseException ex) {
                return dateString;
            }
        }
    }
}