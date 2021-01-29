package org.switcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 非线程安全的recorder
 */
class SpeedUnsafeRecorder {
    public final static int NANO = 1000000000;

    final List<SpeedRecord> speedRecords;

    /**
     * 是否已经停止
     */
    boolean stopped;

    /**
     * 最近更新的位置
     */
    private int latest;

    SpeedUnsafeRecorder(int size) {
        speedRecords = new ArrayList<>(size);
        latest = size - 1;
        stopped = false;
        for (int i = 0; i < size; i++) {
            speedRecords.add(new SpeedRecord(-1, 0));
        }
    }

    /**
     * 按时间从小到大排列，获取第i个位置的SpeedRecord
     *
     * @param i 位置i
     * @return {@link SpeedRecord}
     */
    SpeedRecord get(int i) {
        int realI = latest + 1 + i;
        if (realI > speedRecords.size()) {
            realI -= speedRecords.size();
        }
        return speedRecords.get(realI);
    }

    void record(int numberOfBytes) {
        if (stopped)
            return;
        long timestamp = System.nanoTime() / NANO;
        if (timestamp != speedRecords.get(latest).timestamp) {
            latest += 1;
            if (latest >= speedRecords.size()) {
                latest -= speedRecords.size();
            }
            speedRecords.set(latest, new SpeedRecord(timestamp, numberOfBytes));
        } else {
            speedRecords.get(latest).value += numberOfBytes;
        }
    }

    long getSpeed() {
        if (stopped)
            return 0;
        long timestamp = System.nanoTime() / NANO;
        long totalValue = 0;
        for (int i = 0; i < speedRecords.size(); i++) {
            SpeedRecord speedRecord = speedRecords.get(i);
            if (timestamp - speedRecord.timestamp < speedRecords.size()) {
                totalValue += speedRecord.value;
            }
        }
        return totalValue / speedRecords.size();
    }
}
