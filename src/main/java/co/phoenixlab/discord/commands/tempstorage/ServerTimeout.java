package co.phoenixlab.discord.commands.tempstorage;

import java.time.Duration;
import java.time.Instant;

public class ServerTimeout {

    private final String serverId;
    private final String userId;
    private final Instant startTime;
    private final Duration duration;
    private final Instant endTime;
    private final String issuedByUserId;

    public ServerTimeout(Duration duration, Instant startTime, String userId, String serverId, String issuedByUserId) {
        this.duration = duration;
        this.startTime = startTime;
        this.userId = userId;
        this.serverId = serverId;
        this.endTime = startTime.plus(duration);
        this.issuedByUserId = issuedByUserId;
    }

    public String getServerId() {
        return serverId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getIssuedByUserId() {
        return issuedByUserId;
    }
}
