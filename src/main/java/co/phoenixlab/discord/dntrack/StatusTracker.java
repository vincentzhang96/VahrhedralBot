package co.phoenixlab.discord.dntrack;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.discord.dntrack.event.RegionDescriptor;
import co.phoenixlab.discord.dntrack.event.StatusChangeEvent;
import co.phoenixlab.discord.util.TryingScheduledExecutor;
import com.google.common.eventbus.EventBus;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static co.phoenixlab.discord.dntrack.event.StatusChangeEvent.StatusChange.WENT_OFFLINE;
import static co.phoenixlab.discord.dntrack.event.StatusChangeEvent.StatusChange.WENT_ONLINE;

public class StatusTracker implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger("DnTrack");
    public static final ScheduledExecutorService EXECUTOR_SERVICE = new TryingScheduledExecutor(
        Executors.newScheduledThreadPool(4),
        LOGGER
    );
    private static final int TIMEOUT = 3000;

    private final RegionDescriptor region;
    private final EventBus bus;
    private final AtomicInteger status;
    private final AtomicReference<StatusChangeEvent> lastChangeEvent;
    private final AtomicReference<Instant> lastCheckTime;

    public StatusTracker(RegionDescriptor region, EventBus bus) {
        this.region = region;
        this.bus = bus;
        this.status = new AtomicInteger(-1);
        this.lastChangeEvent = new AtomicReference<>(null);
        this.lastCheckTime = new AtomicReference<>(null);
        LOGGER.info("Created status tracker for " + region.getRegionCode());
    }

    @Override
    public void run() {
        try {
            LOGGER.debug("Performing status check for {}", region.getRegionCode());
            HttpResponse<String> resp = Unirest.get(region.getStatusCheckUrl())
                .asString();
            //  Use the same "now" for check and update (if we do update)
            Instant now = Instant.now();
            lastCheckTime.set(now);
            if (resp.getStatus() == 200) {
                //  Response is OK
                String patchConfigList = resp.getBody().replaceAll("[^\\x20-\\x7e]", "");
                List<IpAndPort> ips = getLoginServerIPs(patchConfigList);
                LOGGER.debug("Got {} login server IPs", ips.size());
                CountDownLatch latch = new CountDownLatch(1);
                int currStatus = 0;
                int lastStatus = status.get();
                List<Future<?>> futures = ips.stream().map(ip -> EXECUTOR_SERVICE.submit(() -> {
                    boolean result = testServer(ip);
                    if (result) {
                        latch.countDown();
                    }
                })).collect(Collectors.toList());
                if (latch.await(TIMEOUT + 500, TimeUnit.MILLISECONDS)) {
                    futures.forEach((f) -> f.cancel(true));
                    currStatus = 1;
                }
                if (lastStatus == -1) {
                    status.compareAndSet(lastStatus, currStatus);
                    LOGGER.info("DN {} initial status for this session is {}",
                        region.getRegionCode(), currStatus);
                    StatusChangeEvent evt = new StatusChangeEvent(true, region,
                        currStatus == 1 ? WENT_ONLINE : WENT_OFFLINE, now);
                    bus.post(evt);
                } else if (lastStatus != currStatus) {
                    if (status.compareAndSet(lastStatus, currStatus)) {
                        LOGGER.info("DN {} status changed from {} to {}",
                            region.getRegionCode(), lastStatus, currStatus);
                        StatusChangeEvent evt = new StatusChangeEvent(false, region,
                            currStatus == 1 ? WENT_ONLINE : WENT_OFFLINE, now);
                        lastChangeEvent.set(evt);
                        bus.post(evt);
                    }
                }
            } else {
                throw new IOException("Error " + resp.getStatus() + ": " + resp.getStatusText()
                    + ", " + resp.getBody());
            }
        } catch (Exception e) {
            //  Ignored (kinda)
            LOGGER.warn("Failed status check for " + region.getRegionCode(), e);
        }
    }

    private List<IpAndPort> getLoginServerIPs(String content) throws Exception {
        List<IpAndPort> ret = new ArrayList<>();
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if ("login".equals(localName)) {
                    ret.add(new IpAndPort(attributes.getValue("addr"), attributes.getValue("port")));
                }
            }
        });
        reader.parse(new InputSource(new StringReader(content)));
        return ret;
    }

    private boolean testServer(IpAndPort ipPort) {
        try (Socket socket = new Socket()) {
            socket.setKeepAlive(false);
            socket.setSoTimeout(TIMEOUT);
            socket.connect(new InetSocketAddress(ipPort.ip, ipPort.port), TIMEOUT);
            InputStream inputStream = socket.getInputStream();
            int r = inputStream.read();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public RegionDescriptor getRegion() {
        return region;
    }

    public Instant getLastCheckTime() {
        return lastCheckTime.get();
    }

    public Instant getLastStatusChangeTime() {
        return SafeNav.of(lastChangeEvent.get()).get(StatusChangeEvent::getTimestamp);
    }

    public AtomicInteger currentStatus() {
        return status;
    }

    public AtomicReference<Instant> lastCheckTime() {
        return lastCheckTime;
    }

    public AtomicReference<StatusChangeEvent> lastChangeEvent() {
        return lastChangeEvent;
    }


    static class IpAndPort {
        final String ip;
        final int port;

        public IpAndPort(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public IpAndPort(String ip, String port) {
            this(ip, Integer.parseInt(port));
        }
    }
}
