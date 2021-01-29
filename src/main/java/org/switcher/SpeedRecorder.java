package org.switcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SpeedRecorder {
    private final static Logger logger = LoggerFactory.getLogger(SpeedRecorder.class);
    public final static int DEFAULT_SIZE = 3;

    /**
     * 记录最近n秒的速度
     */
    private final AtomicReference<SpeedUnsafeRecorder> speedRecordsReference;

    /**
     * 父recorder，自身的操作会影响到父节点
     */
    final SpeedRecorder parent;

    SpeedRecorder() {
        this(DEFAULT_SIZE);
    }

    SpeedRecorder(int size) {
        this(size, null);
    }

    SpeedRecorder(SpeedRecorder parent) {
        this(DEFAULT_SIZE, parent);
    }

    SpeedRecorder(int size, SpeedRecorder parent) {
        speedRecordsReference = new AtomicReference<>();
        setSize(size);
        this.parent = parent;
    }

    public int getSize() {
        return speedRecordsReference.get().speedRecords.size();
    }

    public void setSize(int size) {
        // 限制size为正整数
        if (size <= 0) {
            logger.warn("非法参数size={}，将修改为默认值{}", size, DEFAULT_SIZE);
            size = DEFAULT_SIZE;
        }
        speedRecordsReference.set(new SpeedUnsafeRecorder(size));
    }

    public void record(int numberOfBytes) {
        speedRecordsReference.updateAndGet(speedUnsafeRecorder -> {
            speedUnsafeRecorder.record(numberOfBytes);
            // 同时更新parent
            if (parent != null && !speedUnsafeRecorder.stopped) {
                parent.record(numberOfBytes);
            }
            return speedUnsafeRecorder;
        });
    }

    public long getSpeed() {
        // 使用AtomicLong是为了能在updateAndGet设置speed的值，也可用new long[1]代替
        AtomicLong speed = new AtomicLong();
        speedRecordsReference.updateAndGet(speedUnsafeRecorder -> {
            speed.set(speedUnsafeRecorder.getSpeed());
            return speedUnsafeRecorder;
        });
        return speed.get();
    }

    // 停止这个计速器
    void tearDown() {
        if (parent != null) {
            speedRecordsReference.updateAndGet(speedUnsafeRecorder -> {
                speedUnsafeRecorder.stopped = true;
                parent.reduce(speedUnsafeRecorder);
                return speedUnsafeRecorder;
            });
        }
    }

    /**
     * {@link SpeedUnsafeRecorder#get(int)}
     *
     * @param toReduce 每个对应时间需要减少的值
     */
    void reduce(SpeedUnsafeRecorder toReduce) {
        speedRecordsReference.updateAndGet(speedUnsafeRecorder -> {
            // 用speedUnsafeRecorder.get(int)方法顺序获取的speedRecorder的timestamp也是顺序的
            List<SpeedRecord> iSpeedRecords = speedUnsafeRecorder.speedRecords;
            List<SpeedRecord> jSpeedRecords = toReduce.speedRecords;
            for (int i = 0, j = 0; i < iSpeedRecords.size() && j < jSpeedRecords.size(); ) {
                SpeedRecord iSpeedRecord = iSpeedRecords.get(i);
                SpeedRecord jSpeedRecord = jSpeedRecords.get(j);
                if (iSpeedRecord.timestamp < jSpeedRecord.timestamp) {
                    ++i;
                } else if (iSpeedRecord.timestamp > jSpeedRecord.timestamp) {
                    ++j;
                } else {
                    // 如果iSpeedRecord.timestamp==jSpeedRecord.timestamp
                    // 那么需要减去对应值
                    iSpeedRecord.value -= jSpeedRecord.value;
                    ++i;
                    ++j;
                }
            }
            // 同时更新parent
            if (parent != null) {
                parent.reduce(toReduce);
            }
            return speedUnsafeRecorder;
        });
    }
}
