package me.blackout.orcrist.utils.misc;

import org.apache.commons.lang3.time.DurationFormatUtils;

public class Maths {

    public static long msPassed(Long start) {
        return System.currentTimeMillis() - start;
    }

    public static String timeElapsed(Long start) {
        return DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, "HH:mm:ss");
    }

    public static String hoursElapsed(Long start) {
        return DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, "HH");
    }

    public static String minutesElapsed(Long start) {
        return DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, "mm");
    }

    public static String secondsElapsed(Long start) {
        return DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, "ss");
    }

    public static int randomNum(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    public static String millisElapsed(Long start) {
        return Math.round(msPassed(start) * 100) / 100 + "ms";
    }
}
