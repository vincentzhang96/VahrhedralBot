/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Vincent Zhang/PhoenixLAB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package co.phoenixlab.discord.util;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A RateLimiter throttles access to a resource, acting as a guard to prevent overutilization of some resource via
 * the {@link #mark(boolean)}, {@link #mark()}, and {@link #tryMark()} methods.
 */
public class RateLimiter {

    /**
     * Name of this RateLimiter.
     */
    private final String label;
    /**
     * ReadWrite lock for charge concurrency.
     */
    private final ReadWriteLock lock;
    /**
     * The maximum number of charges that can be on cooldown at the same time.
     */
    private int maxCharges;
    /**
     * Charge cooldown time.
     */
    private long periodMs;
    /**
     * Charge timestamps.
     */
    private long[] charges;

    /**
     * Constructs a new RateLimiter with no label and the given period time and maximum number of charges.
     *
     * @param periodMs   Cooldown time (in milliseconds) for each charge.
     * @param maxCharges Maximum number of charges that can be on cooldown at the same time.
     */
    public RateLimiter(long periodMs, int maxCharges) {
        this("", periodMs, maxCharges);
    }

    /**
     * Constructs a new RateLimiter with a given label and the given period time and maximum number of charges.
     *
     * @param label      The RateLimiter's label, for display/debug purposes.
     * @param periodMs   Cooldown time (in milliseconds) for each charge.
     * @param maxCharges Maximum number of charges that can be on cooldown at the same time.
     */
    public RateLimiter(String label, long periodMs, int maxCharges) {
        this.label = label;
        this.periodMs = periodMs;
        this.maxCharges = maxCharges;
        this.charges = new long[maxCharges];
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Consumes an available charge. If no charges are available then this method will immediately throw an exception
     * with the number of milliseconds to wait before retrying. This method is equivalent to calling
     * {@link #mark(boolean)} with {@code false}, but does not throw {@link InterruptedException}.
     *
     * @throws RateLimitExceededException If there are no charges available; that is, the rate limit has been
     *                                    exceeded.
     */
    public void mark() throws RateLimitExceededException {
        try {
            //  Will never throw InterruptedException with false param
            mark(false);
        } catch (InterruptedException ignore) {
            //  ignore
        }
    }

    /**
     * Attempts to consume an available charge. If a charge was successfully consumed, then zero is returned.
     * Otherwise, the number of milliseconds to wait before retrying is returned and no charges are consumed.
     *
     * @return 0 if a charge was available and consumed, or a positive number of milliseconds to wait before retrying.
     */
    public long tryMark() {
        try {
            //  Will never throw InterruptedException with false param
            mark(false);
            return 0L;
        } catch (InterruptedException ignore) {
            //  ignore
            return 0L;
        } catch (RateLimitExceededException e) {
            return e.getRetryIn();
        }
    }

    /**
     * Attempts to consume an available charge, optionally waiting for one. If no charges are available and
     * {@code waitFor} is false, then a {@link RateLimitExceededException} is thrown with the number of milliseconds
     * until the next charge is available.
     *
     * @param waitFor Whether or not to wait for an available charge.
     * @throws RateLimitExceededException If {@code waitFor} is false and no charges are available; that is, the
     *                                    rate limit has been exceeded.
     * @throws InterruptedException       If the current Thread has been interrupted while waiting for a charge and
     *                                    {@code waitFor} is true.
     */
    public void mark(boolean waitFor) throws RateLimitExceededException, InterruptedException {
        long now = System.currentTimeMillis();
        long diff;
        try {
            lock.readLock().lock();
            do {
                diff = Long.MAX_VALUE;
                for (int i = 0; i < charges.length; i++) {
                    long delta = now - charges[i];
                    if (delta >= periodMs) {
                        if (tryAcquireCharge(now, i, delta)) {
                            return;
                        }
                    }
                    if (delta < diff) {
                        diff = delta;
                    }
                }
                if (waitFor) {
                    Thread.yield();
                    Thread.sleep(100);
                }
            } while (waitFor);
        } finally {
            lock.readLock().unlock();
        }
        throw new RateLimitExceededException(label, diff);
    }

    private boolean tryAcquireCharge(long now, int i, long delta) {
        lock.readLock().unlock();
        try {
            lock.writeLock().lock();
            //  recheck
            if (delta >= now - charges[i]) {
                charges[i] = now;
                return true;
            }
            //  if not we go on to the next one
        } finally {
            lock.writeLock().unlock();
            lock.readLock().lock();
        }
        return false;
    }

    /**
     * Get the number of charges currently available.
     *
     * @return The number of charges currently available.
     */
    public int getRemainingCharges() {
        int count = 0;
        try {
            lock.readLock().lock();
            for (long charge : charges) {
                if (isTimeOnCd(charge)) {
                    ++count;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return maxCharges - count;
    }

    /**
     * Whether or not there are any charges available.
     *
     * @return If there's any available charges.
     */
    public boolean hasCharges() {
        return getRemainingCharges() > 0;
    }

    private boolean isChargeOnCd(int charge) {
        return charges[charge] != 0 && isTimeOnCd(charges[charge]);
    }

    private boolean isTimeOnCd(long time) {
        return System.currentTimeMillis() - time < periodMs;
    }

    /**
     * Resets this RateLimiter by making all charges available.
     */
    public void reset() {
        try {
            lock.writeLock().lock();
            Arrays.fill(charges, 0);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return String.format("RateLimiter %s: period:%,dms chargeCount:%,d charges:%s",
                label, periodMs, maxCharges, Arrays.toString(charges));
    }

    public String getLabel() {
        return label;
    }

    public int getMaxCharges() {
        return maxCharges;
    }

    public long getPeriodMs() {
        return periodMs;
    }
}
