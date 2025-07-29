package com.pdf.ai;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    public static String getRelativeTimeSpanString(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        if (seconds < 60) {
            return "just now";
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes < 60) {
            return minutes + " min ago";
        }

        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours < 24) {
            return hours + " hours ago";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days < 30) {
            return days + " days ago";
        }

        if (days < 365) {
            return (days / 30) + " months ago";
        }

        return (days / 365) + " years ago";
    }
}
