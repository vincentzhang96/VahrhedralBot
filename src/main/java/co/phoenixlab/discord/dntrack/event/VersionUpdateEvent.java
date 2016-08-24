package co.phoenixlab.discord.dntrack.event;

import java.time.Instant;

public class VersionUpdateEvent {

    private final RegionDescriptor region;
    private final int oldVersion;
    private final int newVersion;
    private final Instant timestamp;

    public VersionUpdateEvent(RegionDescriptor region, int oldVersion, int newVersion, Instant timestamp) {
        this.region = region;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
        this.timestamp = timestamp;
    }

    public RegionDescriptor getRegion() {
        return region;
    }

    public int getOldVersion() {
        return oldVersion;
    }

    public int getNewVersion() {
        return newVersion;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
