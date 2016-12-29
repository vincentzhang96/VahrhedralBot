package co.phoenixlab.discord.cfg;

import java.util.concurrent.TimeUnit;

public class DiscordApiClientConfig {

    private InfluxDbConfig apiClientInfluxConfig;
    private long reportingIntervalMsec;
    private boolean enableMetrics;

    public DiscordApiClientConfig() {
        this(new InfluxDbConfig(), TimeUnit.SECONDS.toMillis(60), false);
    }

    public DiscordApiClientConfig(InfluxDbConfig apiClientInfluxConfig,
                                  long reportingIntervalMsec,
                                  boolean enableMetrics) {
        this.apiClientInfluxConfig = apiClientInfluxConfig;
        this.reportingIntervalMsec = reportingIntervalMsec;
        this.enableMetrics = enableMetrics;
    }

    public InfluxDbConfig getApiClientInfluxConfig() {
        return apiClientInfluxConfig;
    }

    public void setApiClientInfluxConfig(InfluxDbConfig apiClientInfluxConfig) {
        this.apiClientInfluxConfig = apiClientInfluxConfig;
    }

    public long getReportingIntervalMsec() {
        return reportingIntervalMsec;
    }

    public void setReportingIntervalMsec(long reportingIntervalMsec) {
        this.reportingIntervalMsec = reportingIntervalMsec;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
}
