package co.phoenixlab.discord.commands.tempstorage;

public class DnTrackInfo {

    private long lastStatusChangeTime;
    private long lastPatchTime;
    private int patchVersion;
    private int serverStatus;

    public DnTrackInfo() {
    }

    public long getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }

    public void setLastStatusChangeTime(long lastStatusChangeTime) {
        this.lastStatusChangeTime = lastStatusChangeTime;
    }

    public long getLastPatchTime() {
        return lastPatchTime;
    }

    public void setLastPatchTime(long lastPatchTime) {
        this.lastPatchTime = lastPatchTime;
    }

    public int getPatchVersion() {
        return patchVersion;
    }

    public void setPatchVersion(int patchVersion) {
        this.patchVersion = patchVersion;
    }

    public int getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(int serverStatus) {
        this.serverStatus = serverStatus;
    }
}
