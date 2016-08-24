package co.phoenixlab.discord.dntrack.event;

public class RegionDescriptor {

    private String regionCode;
    private String regionNameKey;
    private String versionCheckUrl;
    private String statusCheckClass;
    private String statusCheckUrl;

    public RegionDescriptor() {
    }

    public RegionDescriptor(String regionCode, String regionNameKey, String versionCheckUrl,
                            String statusCheckClass, String statusCheckUrl) {
        this.regionCode = regionCode;
        this.regionNameKey = regionNameKey;
        this.versionCheckUrl = versionCheckUrl;
        this.statusCheckClass = statusCheckClass;
        this.statusCheckUrl = statusCheckUrl;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getRegionNameKey() {
        return regionNameKey;
    }

    public String getVersionCheckUrl() {
        return versionCheckUrl;
    }

    public String getStatusCheckClass() {
        return statusCheckClass;
    }

    public String getStatusCheckUrl() {
        return statusCheckUrl;
    }
}
