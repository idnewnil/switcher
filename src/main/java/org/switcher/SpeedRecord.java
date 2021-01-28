package org.switcher;

/**
 * 速度记录
 */
class SpeedRecord {
    /**
     * 一个标志，用于区分这个记录的创建时间，值为 {@link System#nanoTime()}/{@link SpeedRecorder#NANO}
     */
    final long timestamp;
    int value;

    public SpeedRecord(long timestamp, int value) {
        this.timestamp = timestamp;
        this.value = value;
    }
}
