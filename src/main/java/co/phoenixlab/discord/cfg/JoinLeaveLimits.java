package co.phoenixlab.discord.cfg;

public class JoinLeaveLimits {

    private long cacheEvictionTimeMs;
    private long rlPeriodMs;
    private int rmMaxCharges;

    public JoinLeaveLimits() {
    }

    public JoinLeaveLimits(long cacheEvictionTimeMs, long rlPeriodMs, int rmMaxCharges) {
        this.cacheEvictionTimeMs = cacheEvictionTimeMs;
        this.rlPeriodMs = rlPeriodMs;
        this.rmMaxCharges = rmMaxCharges;
    }

    public long getCacheEvictionTimeMs() {
        return cacheEvictionTimeMs;
    }

    public void setCacheEvictionTimeMs(long cacheEvictionTimeMs) {
        this.cacheEvictionTimeMs = cacheEvictionTimeMs;
    }

    public long getRlPeriodMs() {
        return rlPeriodMs;
    }

    public void setRlPeriodMs(long rlPeriodMs) {
        this.rlPeriodMs = rlPeriodMs;
    }

    public int getRmMaxCharges() {
        return rmMaxCharges;
    }

    public void setRmMaxCharges(int rmMaxCharges) {
        this.rmMaxCharges = rmMaxCharges;
    }
}
