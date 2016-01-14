package co.phoenixlab.discord.ncs.cdtimer;

import java.util.Arrays;

public class ChargedCooldownTimer implements CooldownTimer {

    public static final int MAX_CHARGES = 20;

    private final long[] chargeTimes;

    private long chargeCooldownMillis;

    public ChargedCooldownTimer(long chargeCooldown, int charges) throws IllegalArgumentException {
        if (charges <= 2 || charges > MAX_CHARGES) {
            throw new IllegalArgumentException("Charges must be between 2 and " + MAX_CHARGES);
        }
        chargeTimes = new long[charges];
        setCooldown(chargeCooldown);
    }

    @Override
    public long getCooldown() {
        return chargeCooldownMillis / 1000L;
    }

    @Override
    public void setCooldown(long cooldown) throws IllegalArgumentException {
        chargeCooldownMillis = cooldown * 1000L;
    }

    @Override
    public int getCharges() {
        long time = System.currentTimeMillis();
        int count = 0;
        for (long chargeTime : chargeTimes) {
            if (!isOnCdTime(time, chargeTime)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getMaxCharges() {
        return chargeTimes.length;
    }

    @Override
    public void reset() {
        Arrays.fill(chargeTimes, 0L);
    }

    @Override
    public boolean checkAndSet() {
        long time = System.currentTimeMillis();
        for (int i = 0; i < chargeTimes.length; i++) {
            if (!isChargeOnCD(i, time)) {
                chargeTimes[i] = time;
                return false;
            }
        }
        return true;
    }

    @Override
    public void set() {
        checkAndSet();
    }

    @Override
    public void forceSet() {
        if (checkAndSet()) {
            long time = System.currentTimeMillis();
            int youngest = -1;
            long youngTime = Long.MIN_VALUE;
            for (int i = 0; i < chargeTimes.length; i++) {
                long chargeTime = chargeTimes[i];
                if (youngTime < chargeTime) {
                    youngest = i;
                    youngTime = chargeTime;
                }
            }
            assert youngest != -1 : "Should have found a youngest charge";
            chargeTimes[youngest] = time;
        }
    }


    @Override
    public void forceCooldown() {
        Arrays.fill(chargeTimes, System.currentTimeMillis());
    }

    private boolean isChargeOnCD(int charge) {
        return isChargeOnCD(charge, System.currentTimeMillis());
    }

    private boolean isChargeOnCD(int charge, long now) {
        return isOnCdTime(chargeTimes[charge], now);
    }

    private boolean isOnCdTime(long lastUseTime, long now) {
        return now - lastUseTime < chargeCooldownMillis;
    }
}
