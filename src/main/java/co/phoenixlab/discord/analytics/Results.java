package co.phoenixlab.discord.analytics;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Results {

    private final TreeMap<String, LongAdder> days;
    private ReentrantReadWriteLock lock;

    public Results() {
        days = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        lock = new ReentrantReadWriteLock();
    }

    public void accumulate(String date, long amount) {
        LongAdder adder;
        try {
            lock.readLock().lock();
            adder = days.get(date);
        } finally {
            lock.readLock().unlock();
        }
        if (adder == null) {
            adder = new LongAdder();
            try {
                lock.writeLock().lock();
                LongAdder adder2 = days.get(date);
                if (adder2 == null) {
                    days.put(date, adder);
                } else {
                    adder = adder2;
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        adder.add(amount);
    }

    public SortedMap<String, LongAdder> getDays() {
        return days;
    }
}
