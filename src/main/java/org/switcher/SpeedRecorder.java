package org.switcher;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class SpeedRecorder {
    public final static int NANO = 1000000000;

    /**
     * 用于分阶段的记录速度
     */
    private AtomicReferenceArray<SpeedRecord> periodSpeedRecords;

    SpeedRecorder() {
        this(10);
    }

    SpeedRecorder(int numberOfPeriods) {
        setPeriodCount(numberOfPeriods);

    }

    public int getNumberOfPeriods() {
        return periodSpeedRecords.length();
    }

    public void setPeriodCount(int numberOfPeriods) {
        if (numberOfPeriods <= 0) {
            numberOfPeriods = 1;
        }
        if (this.periodSpeedRecords == null || periodSpeedRecords.length() != numberOfPeriods) {
            periodSpeedRecords = new AtomicReferenceArray<>(numberOfPeriods);
            for (int i = 0; i < periodSpeedRecords.length(); i++) {
                periodSpeedRecords.set(i, new SpeedRecord(-1, 0));
            }
        }
    }

    /**
     * 记录上传/下载流量
     * 不要使用this.periodSpeedRecords，因为setPeriodCount可能会使其发生变化
     *
     * @param numberOfBytes 字节数
     */
    public void record(int numberOfBytes) {
        Holder holder = new Holder();
        holder.periodSpeedRecords.updateAndGet(holder.pos, speedRecord -> {
            if (holder.timestamp > speedRecord.timestamp) {
                // 如果目前的时间比当前speedRecord的时间要大，那么需要更新speedRecord的值
                return new SpeedRecord(holder.timestamp, numberOfBytes);
            } else if (holder.timestamp == speedRecord.timestamp) {
                // 如果时间相同，那么直接增加到speedRecord中
                speedRecord.value += numberOfBytes;
                return speedRecord;
            } else {
                // 否则当前时间比speedRecord的时间要小，不做处理
                return speedRecord;
            }
        });
    }

    /**
     * 获取速度
     * 不要使用this.periodSpeedRecords，因为setPeriodCount可能会使其发生变化
     */
    public long getSpeed() {
        Holder holder = new Holder();
        long totalValue = 0;
        int validCount = 0;
        for (int i = 0; i < holder.periodSpeedRecords.length(); i++) {
            int delta = i <= holder.pos ? 0 : 1;
            SpeedRecord speedRecord = holder.periodSpeedRecords.get(i);
            if (holder.timestamp - speedRecord.timestamp == delta) {
                // 对于每一个位置i<=holder.pos，如果holder.timestamp==speedRecord.timestamp，说明这个位置的记录是最新的
                // 同样对于每一个i>holder.pos且holder.timestamp-speedRecord.timestamp==1的位置，这个记录也是最新的
                totalValue += speedRecord.value;
                validCount += 1;
            }
        }
        return validCount == 0 ? 0 : totalValue / validCount;
    }


    private class Holder {
        private final AtomicReferenceArray<SpeedRecord> periodSpeedRecords;
        private final long timestamp;
        private final int pos;

        private Holder() {
            // 保证periodSpeedRecords的一致性
            periodSpeedRecords = SpeedRecorder.this.periodSpeedRecords;
            int periodLong = NANO / periodSpeedRecords.length();
            long nano = System.nanoTime();
            timestamp = nano / NANO;
            pos = (int) (nano % NANO / periodLong);
        }
    }
}
