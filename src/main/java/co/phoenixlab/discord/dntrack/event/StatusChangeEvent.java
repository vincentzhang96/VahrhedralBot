package co.phoenixlab.discord.dntrack.event;

import java.time.Instant;

public class StatusChangeEvent {

    private final boolean initial;
    private final RegionDescriptor region;
    private final StatusChange change;
    private final Instant timestamp;

    public StatusChangeEvent(boolean initial, RegionDescriptor region, StatusChange change, Instant timestamp) {
        this.initial = initial;
        this.region = region;
        this.change = change;
        this.timestamp = timestamp;
    }

    public boolean isInitial() {
        return initial;
    }

    public RegionDescriptor getRegion() {
        return region;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public StatusChange getChange() {
        return change;
    }

    public enum StatusChange {
        WENT_OFFLINE,
        WENT_ONLINE
    }
}
