package me.blackout.orcrist.utils.world;

public final class TimerUtils {
    private long time;
    private long nanoTime = -1L;

    public TimerUtils() {
        time = System.currentTimeMillis();
    }

    public void resetNano() {
        nanoTime = System.nanoTime();
    }

    public void setTicks(long ticks) {
        nanoTime = System.nanoTime() - convertTicksToNano(ticks);
    }

    public void setNano(long time) {
        nanoTime = System.nanoTime() - time;
    }

    public void setMicro(long time) {
        nanoTime = System.nanoTime() - convertMicroToNano(time);
    }

    public void setMillis(long time) {
        nanoTime = System.nanoTime() - convertMillisToNano(time);
    }

    public void setSec(long time) {
        nanoTime = System.nanoTime() - convertSecToNano(time);
    }

    public long getTicks() {
        return convertNanoToTicks(nanoTime);
    }

    public long getNano() {
        return nanoTime;
    }

    public long getMicro() {
        return convertNanoToMicro(nanoTime);
    }

    public long getMillis() {
        return convertNanoToMillis(nanoTime);
    }

    public long getSec() {
        return convertNanoToSec(nanoTime);
    }

    public boolean passedTicks(long ticks) {
        return passedNano(convertTicksToNano(ticks));
    }

    public boolean passedNano(long time) {
        return System.nanoTime() - nanoTime >= time;
    }

    public boolean passedMicro(long time) {
        return passedNano(convertMicroToNano(time));
    }

    public boolean passedMillis(long time) {
        return passedNano(convertMillisToNano(time));
    }

    public boolean passedSec(long time) {
        return passedNano(convertSecToNano(time));
    }

    public long convertMillisToTicks(long time) {
        return time / 50;
    }

    public long convertTicksToMillis(long ticks) {
        return ticks * 50;
    }

    public long convertNanoToTicks(long time) {
        return convertMillisToTicks(convertNanoToMillis(time));
    }

    public long convertTicksToNano(long ticks) {
        return convertMillisToNano(convertTicksToMillis(ticks));
    }

    public long convertSecToMillis(long time) {
        return time * 1000L;
    }

    public long convertSecToMicro(long time) {
        return convertMillisToMicro(convertSecToMillis(time));
    }

    public long convertSecToNano(long time) {
        return convertMicroToNano(convertMillisToMicro(convertSecToMillis(time)));
    }

    public long convertMillisToMicro(long time) {
        return time * 1000L;
    }

    public long convertMillisToNano(long time) {
        return convertMicroToNano(convertMillisToMicro(time));
    }

    public long convertMicroToNano(long time) {
        return time * 1000L;
    }

    public long convertNanoToMicro(long time) {
        return time / 1000L;
    }

    public long convertNanoToMillis(long time) {
        return convertMicroToMillis(convertNanoToMicro(time));
    }

    public long convertNanoToSec(long time) {
        return convertMillisToSec(convertMicroToMillis(convertNanoToMicro(time)));
    }

    public long convertMicroToMillis(long time) {
        return time / 1000L;
    }

    public long convertMicroToSec(long time) {
        return convertMillisToSec(convertMicroToMillis(time));
    }

    public long convertMillisToSec(long time) {
        return time / 1000L;
    }

    public boolean hasPassed(double ms) {
        return System.currentTimeMillis() - time >= ms;
    }

    public void reset() {
        time = System.currentTimeMillis();
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long ms;

    public boolean hasPassed(int ms) {
        return System.currentTimeMillis() - this.ms >= ms;
    }
}
