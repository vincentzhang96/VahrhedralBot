package co.phoenixlab.discord.api.event;

public class WebSocketCloseEvent {

    private final int code;
    private final String reason;
    private final boolean isRemoteError;

    public WebSocketCloseEvent(int code, String reason, boolean isRemoteError) {
        this.code = code;
        this.reason = reason;
        this.isRemoteError = isRemoteError;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public boolean isRemoteError() {
        return isRemoteError;
    }
}
