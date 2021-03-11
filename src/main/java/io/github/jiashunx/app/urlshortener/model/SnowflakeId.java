package io.github.jiashunx.app.urlshortener.model;

/**
 * 雪花算法id模型
 * @author jiashunx
 */
/**
 * 0 | 0001100 10100010 10111110 10001001 01011100 00 | 10001 | 00001 | 0000 00000000
 * <p>
 * 0          | 0001100 10100010 10111110 10001001 01011100 00 |    10001   |  00001  | 0000 00000000
 * 0          |       timestamp                                |datacenterId| workerId |    sequence
 * 正数(占位) |       时间戳二进制                             | 数据中心ID | 机器ID | 同一机房同一机器相同时间产生的序列
 */
public class SnowflakeId {
    /**
     * 数据中心|机房id
     */
    private long datacenterId;
    /**
     * 机器id
     */
    private long workerId;
    /**
     * 同一时间的序列
     */
    private long sequence;

    private SnowflakeId(long workerId, long datacenterId) {
        this(workerId, datacenterId, 0);
    }

    public SnowflakeId(long workerId, long datacenterId, long sequence) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = sequence;
    }

    /**
     * 机器ID所占位数 5个bit 最大: 11111(2进制) --> 31(10进制) id最多32
     */
    private final long workerIdBits = 5L;
    /**
     * 数据中心|机房ID所占位数 5个bit 最大: 11111(2进制) --> 31(10进制) id最多32
     */
    private final long datacenterIdBits = 5L;
    /**
     * 同一时间序列所占位数 12个bit 最大: 111111111111(2进制) --> 4095(10进制) 序列号最多4096
     */
    private final long sequenceBits = 12L;
    /**
     * workerId的偏移量
     */
    private final long workerIdLeftShift = sequenceBits;
    /**
     * datacenterId的偏移量
     */
    private final long datacenterIdLeftShift = sequenceBits + workerIdBits;
    /**
     * timestamp的偏移量
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    /**
     * 最大机器ID
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    /**
     * 最大数据中心|机房ID
     */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    /**
     * 序号掩码(4095 (0b111111111111=0xfff=4095)), 保证序号最大值在
     */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 开始时间戳(2015-01-01)
     */
    private final long twepoch = 1420041600000L;
    /**
     * 最近一次获取id的时间戳
     */
    private long lastTimestamp = -1L;

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
                    lastTimestamp - timestamp));
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - lastTimestamp) << timestampLeftShift)
                | (datacenterId << datacenterIdLeftShift)
                | (workerId << workerIdLeftShift)
                | sequence;
    }

    public static void main(String[] args)
    {
        SnowflakeId worker = new SnowflakeId(1, 1);
        long timer = System.currentTimeMillis();
        for (int i = 0 ; i < 2600000 ; i++)
        {
            worker.nextId();
        }
        System.out.println(System.currentTimeMillis());
        System.out.println(System.currentTimeMillis() - timer);
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }
}
