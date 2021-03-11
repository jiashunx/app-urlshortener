package io.github.jiashunx.app.urlshortener.model;

/**
 * 雪花算法id模型
 * 0 | 0001100 10100010 10111110 10001001 01011100 00 | 10001 | 00001 | 0000 00000000
 * <p>
 * 0          | 0001100 10100010 10111110 10001001 01011100 00 |    10001   |  00001  | 0000 00000000
 * 0          |       timestamp                                |datacenterId| workerId |    sequence
 * 正数(占位) |       时间戳二进制                             | 数据中心ID | 机器ID | 同一机房同一机器相同时间产生的序列
 */
public class SnowflakeId {

    /**
     * 机器ID所占位数 5个bit 最大: 11111(2进制) --> 31(10进制) id最多32
     */
    private final long WORKERID_BITS = 5L;
    /**
     * 数据中心|机房ID所占位数 5个bit 最大: 11111(2进制) --> 31(10进制) id最多32
     */
    private final long DATACENTERID_BITS = 5L;
    /**
     * 同一时间序列所占位数 12个bit 最大: 111111111111(2进制) --> 4095(10进制) 序列号最多4096
     */
    private final long SEQUENCE_BITS = 12L;
    /**
     * workerId的偏移量
     */
    private final long WORKERID_SHIFT = SEQUENCE_BITS;
    /**
     * datacenterId的偏移量
     */
    private final long DATACENTERID_SHIFT = SEQUENCE_BITS + WORKERID_BITS;
    /**
     * timestamp的偏移量
     */
    private final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKERID_BITS + DATACENTERID_BITS;
    /**
     * 最大机器ID
     */
    private final long MAX_WORKERID = -1L ^ (-1L << WORKERID_BITS);
    /**
     * 最大数据中心|机房ID
     */
    private final long MAX_DATACENTERID = -1L ^ (-1L << DATACENTERID_BITS);
    /**
     * 序号掩码(4095 (0b111111111111=0xfff=4095)), 保证序号最大值在
     */
    private final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BITS);
    /**
     * 开始的时间戳(2015-01-01)
     */
    private final long TIMESTAMP_START = 1420041600000L;

    /**
     * 数据中心|机房id
     */
    private final long datacenterId;
    /**
     * 机器id
     */
    private final long workerId;
    /**
     * 同一时间的序列
     */
    private long sequence;
    /**
     * 最近一次获取id的时间戳
     */
    private long lastTimestamp = -1L;

    private SnowflakeId(long workerId, long datacenterId) {
        this(workerId, datacenterId, 0);
    }

    private SnowflakeId(long workerId, long datacenterId, long sequence) {
        if (workerId > MAX_WORKERID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKERID));
        }
        if (datacenterId > MAX_DATACENTERID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTERID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = sequence;
    }

    private static class Inner {
        private static long innerWorkerId;
        private static long innerDatacenterId;
        static {
            String workerIdVal = System.getProperty("workerId", "1");
            String datacenterIdVal = System.getProperty("datacenterId", "1");
            innerWorkerId = Long.parseLong(workerIdVal);
            innerDatacenterId = Long.parseLong(datacenterIdVal);
        }
        private static final SnowflakeId INSTANCE = new SnowflakeId(innerWorkerId , innerDatacenterId);
    }

    public static SnowflakeId getInstance() {
        return Inner.INSTANCE;
    }

    public static String nextHexId() {
        // TODO
        return "" + nextId();
    }

    public static long nextId() {
        return getInstance().nextId0();
    }

    private synchronized long nextId0() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
                    lastTimestamp - timestamp));
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - TIMESTAMP_START) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTERID_SHIFT)
                | (workerId << WORKERID_SHIFT)
                | sequence;
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

}
