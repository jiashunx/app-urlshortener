package io.github.jiashunx.app.urlshortener.model;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法id模型
 * 0 | 0001100 10100010 10111110 10001001 01011100 00 | 10001 | 00001  |  101 | 0000 00000000
 * <p>
 * 0          | 0001100 10100010 10111110 10001001 01011100 00 |    10001   |  00001   |   101   | 0000 00000000
 * 0          |       timestamp                                |datacenterId| workerId |threadId |    sequence
 * 正数(占位) |       时间戳二进制                             | 数据中心ID | 机器ID    |  线程ID | 同一机房同一机器同一相同时间某一线程产生的序列
 */
public class SnowflakeId2 {

    /**
     * 数据中心|机房ID所占位数 5个bit 最大: 11111(2进制) --> 31(10进制) id最多32
     */
    private static final long DATACENTERID_BITS = 5L;
    /**
     * 机器ID所占位数 5个bit 最大: 11111(2进制) --> 31(10进制) id最多32
     */
    private static final long WORKERID_BITS = 5L;
    /**
     * 线程ID所占位数 3bit 最大(111)(2进制) --> 7(10进制) id最多8
     */
    private static final long THREAD_BITS = 3L;
    /**
     * 同一时间序列所占位数 12个bit 最大: 111111111111(2进制) --> 4095(10进制) 序列号最多4096
     */
    private static final long SEQUENCE_BITS = 12L;
    /**
     * threadId偏移量
     */
    private static final long THREADID_SHIFT = SEQUENCE_BITS;
    /**
     * workerId的偏移量
     */
    private static final long WORKERID_SHIFT = SEQUENCE_BITS + THREAD_BITS;
    /**
     * datacenterId的偏移量
     */
    private static final long DATACENTERID_SHIFT = SEQUENCE_BITS + THREAD_BITS + WORKERID_BITS;
    /**
     * timestamp的偏移量
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + THREAD_BITS + WORKERID_BITS + DATACENTERID_BITS;
    /**
     * 最大线程ID
     */
    private static final long MAX_THREADID = -1L ^ (-1L << THREAD_BITS);
    /**
     * 最大机器ID
     */
    private static final long MAX_WORKERID = -1L ^ (-1L << WORKERID_BITS);
    /**
     * 最大数据中心|机房ID
     */
    private static final long MAX_DATACENTERID = -1L ^ (-1L << DATACENTERID_BITS);
    /**
     * 序号掩码(4095 (0b111111111111=0xfff=4095)), 保证序号最大值在
     */
    private static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BITS);
    /**
     * 开始的时间戳(2015-01-01)
     */
    private static final long TIMESTAMP_START = 1420041600000L;

    private static final long innerWorkerId;
    private static final long innerDatacenterId;
    static {
        String workerIdVal = System.getProperty("workerId", "1");
        String datacenterIdVal = System.getProperty("datacenterId", "1");
        innerWorkerId = Long.parseLong(workerIdVal);
        innerDatacenterId = Long.parseLong(datacenterIdVal);
    }

    /**
     * 数据中心|机房id
     */
    private final long datacenterId;
    /**
     * 机器id
     */
    private final long workerId;
    /**
     * 线程id
     */
    private final long threadId;
    /**
     * 同一时间的序列
     */
    private long sequence;
    /**
     * 最近一次获取id的时间戳
     */
    private long lastTimestamp = -1L;

    private SnowflakeId2(long workerId, long datacenterId, long threadId) {
        if (workerId > MAX_WORKERID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKERID));
        }
        if (datacenterId > MAX_DATACENTERID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTERID));
        }
        if (threadId > MAX_THREADID || threadId < 0) {
            throw new IllegalArgumentException(String.format("thread Id can't be greater than %d or less than 0", MAX_THREADID));
        }
        this.threadId = threadId;
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = 0L;
    }

    private static class SFThreadFactory implements ThreadFactory {
        private final ThreadGroup threadGroup;
        private final String namePrefix;
        private final AtomicLong threadNumber = new AtomicLong(0L);

        public SFThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            this.threadGroup = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = "snowflake-pool-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            long number = threadNumber.getAndIncrement();
            if (number > MAX_THREADID) {
                throw new IllegalArgumentException(String.format("thread Id can't be greater than %d", MAX_THREADID));
            }
            SnowflakeId2 SnowflakeId2 = new SnowflakeId2(innerWorkerId, innerDatacenterId, number);
            Thread t = new SFThread(threadGroup, r, namePrefix + number, 0, SnowflakeId2);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    private static class SFThread extends Thread {
        private SnowflakeId2 SnowflakeId2;
        private SFThread(ThreadGroup group, Runnable target, String name, long stackSize, SnowflakeId2 SnowflakeId2) {
            super(group, target, name, stackSize);
            this.SnowflakeId2 = Objects.requireNonNull(SnowflakeId2);
        }
        private static final ThreadLocal<SnowflakeId2> THREAD_LOCAL = new ThreadLocal<>();
        @Override
        public void run() {
            THREAD_LOCAL.set(this.SnowflakeId2);
            super.run();
        }
    }

    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            1 << (int) THREAD_BITS, 1 << (int) THREAD_BITS
            , 365L, TimeUnit.DAYS
            , new LinkedBlockingQueue<>(20000000), new SFThreadFactory());// default RejectHandler

    public static String nextHexId() throws InterruptedException, ExecutionException, TimeoutException {
        // TODO
        return "" + nextId();
    }

    public static long nextId() throws InterruptedException, ExecutionException, TimeoutException {
        Future<Long> future = THREAD_POOL_EXECUTOR.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                SnowflakeId2 SnowflakeId2 = SFThread.THREAD_LOCAL.get();
                return SnowflakeId2.nextId0();
            }
        });
        return future.get(3L, TimeUnit.SECONDS);
    }

    private long nextId0() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
                    lastTimestamp - timestamp));
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = timeGen();
                while (timestamp <= lastTimestamp) {
                    timestamp = timeGen();
                }
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - TIMESTAMP_START) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTERID_SHIFT)
                | (workerId << WORKERID_SHIFT)
                | (threadId << THREADID_SHIFT)
                | sequence;
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
