package co.phoenixlab.discord.commands.tempstorage;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;

public class ServerTimeout {

    private final String serverId;
    private final String userId;
    private final String lastUsername;
    private final Instant startTime;
    private final Duration duration;
    private final Instant endTime;
    private final String issuedByUserId;
    private transient ScheduledFuture timerFuture;

    public ServerTimeout(Duration duration, Instant startTime, String userId, String serverId, String lastUsername,
                         String issuedByUserId) {
        this.duration = duration;
        this.startTime = startTime;
        this.userId = userId;
        this.serverId = serverId;
        this.lastUsername = lastUsername;
        Instant end;
        try {
            end = startTime.plus(duration);
        } catch (DateTimeException | ArithmeticException e) {
            end = Instant.MAX.minus(1, ChronoUnit.YEARS);
        }
        this.endTime = end;
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

    public ScheduledFuture getTimerFuture() {
        return timerFuture;
    }

    public void setTimerFuture(ScheduledFuture timerFuture) {
        this.timerFuture = timerFuture;
    }

    public String getLastUsername() {
        return lastUsername;
    }
}
