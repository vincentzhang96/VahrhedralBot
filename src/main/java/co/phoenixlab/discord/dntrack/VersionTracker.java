package co.phoenixlab.discord.dntrack;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.common.lang.number.ParseInt;
import co.phoenixlab.discord.api.util.ApiUtils;
import co.phoenixlab.discord.dntrack.event.RegionDescriptor;
import co.phoenixlab.discord.dntrack.event.VersionUpdateEvent;
import com.google.common.eventbus.EventBus;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class VersionTracker implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger("DnTrack");

    private final RegionDescriptor region;
    private final EventBus bus;
    private final AtomicInteger version;
    private final AtomicReference<VersionUpdateEvent> lastChangeEvent;
    private final AtomicReference<Instant> lastCheckTime;

    public VersionTracker(RegionDescriptor region, EventBus bus) {
        this.region = region;
        this.bus = bus;
        this.version = new AtomicInteger(-1);
        this.lastChangeEvent = new AtomicReference<>(null);
        this.lastCheckTime = new AtomicReference<>(null);
        LOGGER.info("Created version tracker for " + region.getRegionCode());
    }

    @Override
    public void run() {
        try {
            LOGGER.debug("Performing version check for {}", region.getRegionCode());
            HttpResponse<String> resp = Unirest.get(region.getVersionCheckUrl())
                .asString();
            //  Use the same "now" for check and update (if we do update)
            Instant now = Instant.now();
            lastCheckTime.set(now);
            if (resp.getStatus() == 200) {
                //  Response is OK
                String verStr = resp.getBody();
                int remoteVersion = parseVersion(verStr);
                int lastVersion = version.get();
                if (remoteVersion > lastVersion) {
                    //  New version!
                    if (lastVersion != -1) {
                        LOGGER.info("DN {} version updated from {} to {}",
                            region.getRegionCode(), lastVersion, remoteVersion);
                        VersionUpdateEvent event = new VersionUpdateEvent(false,
                            region, lastVersion, remoteVersion, now);
                        lastChangeEvent.set(event);
                        //  Post the event
                        bus.post(event);
                    } else {
                        LOGGER.info("DN {} initial version for this session is {}",
                            region.getRegionCode(), remoteVersion);
                        VersionUpdateEvent event = new VersionUpdateEvent(true,
                            region, lastVersion, remoteVersion, now);
                        //  Post the event
                        bus.post(event);
                    }
                    version.set(remoteVersion);
                }
            } else {
                throw new IOException("Error " + resp.getStatus() + ": " + resp.getStatusText()
                    + ", " + resp.getBody());
            }
        } catch (Exception e) {
            if (e instanceof UnirestException) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof ConnectTimeoutException) {
                        LOGGER.warn("Connection to {} timed out", region.getVersionCheckUrl());
                        return;
                    } else if (cause instanceof UnknownHostException) {
                        LOGGER.warn("Unable to look up hostname {} (this usually temporary)",
                            ApiUtils.url(region.getVersionCheckUrl()).getHost());
                        return;
                    }
                }
            }
            //  Ignored (kinda)
            LOGGER.warn("Failed version check for " + region.getRegionCode(), e);
        }
    }

    private int parseVersion(String verStr) {
        //  Version strings come in as "version #" sometimes with other junk coming after
        String[] split = verStr.split(" ");
        if (split.length < 2) {
            throw new NumberFormatException(verStr);
        }
        if (!split[0].equalsIgnoreCase("version")) {
            throw new NumberFormatException(verStr);
        }
        try {
            return ParseInt.parse(split[1]);
        } catch (Exception e) {
            throw new NumberFormatException(verStr);
        }
    }

    public int getCurrentVersion() {
        return version.get();
    }

    public RegionDescriptor getRegion() {
        return region;
    }

    public Instant getLastCheckTime() {
        return lastCheckTime.get();
    }

    public Instant getLastVersionChangeTime() {
        return SafeNav.of(lastChangeEvent.get()).get(VersionUpdateEvent::getTimestamp);
    }

    public AtomicInteger currentVersion() {
        return version;
    }

    public AtomicReference<Instant> lastCheckTime() {
        return lastCheckTime;
    }

    public AtomicReference<VersionUpdateEvent> lastChangeEvent() {
        return lastChangeEvent;
    }
}
