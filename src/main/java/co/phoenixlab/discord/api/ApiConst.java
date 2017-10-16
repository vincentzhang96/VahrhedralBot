package co.phoenixlab.discord.api;

/**
 * Contains various useful API URLs and paths
 */
public class ApiConst {

    /**
     * Utility class
     */
    private ApiConst() {
    }

    /**
     * The base URL from which Discord runs
     */
    public static final String BASE_URL = "https://discordapp.com/";
    /**
     * Base API path
     */
    public static final String API_BASE_PATH = BASE_URL + "api/v6";
    /**
     * WebSocket gateway
     */
    public static final String WEBSOCKET_GATEWAY = API_BASE_PATH + "/gateway";
    /**
     * The endpoint for accessing user information
     */
    public static final String USERS_ENDPOINT = API_BASE_PATH + "/users/";
    /**
     * The endpoint for logging in
     */
    public static final String LOGIN_ENDPOINT = API_BASE_PATH + "/auth/login";
    /**
     * The endpoint for logging out
     */
    public static final String LOGOUT_ENDPOINT = API_BASE_PATH + "/auth/logout";
    /**
     * The endpoint for accessing server information
     */
    public static final String SERVERS_ENDPOINT = API_BASE_PATH + "/guilds/";
    /**
     * The endpoint for accessing channel information
     */
    public static final String CHANNELS_ENDPOINT = API_BASE_PATH + "/channels/";
    /**
     * The endpoint for accepting invites
     */
    public static final String INVITE_ENDPOINT = API_BASE_PATH + "/invite";
    /**
     * The format string for avatar URLs
     */
    public static final String AVATAR_URL_PATTERN = "https://cdn.discordapp.com/avatars/%1$s/%2$s.jpg?size=1024";
    /**
     * The format string for Discord's default avatar URLs. Values from 0 to 4 inclusive.
     */
    public static final String NO_AVATAR_URL_PATTERN = "https://cdn.discordapp.com/embed/avatars/%d.png";
    /**
     * The format string for animated avatar URLs
     */
    public static final String ANIMATED_AVATAR_URL_PATTERN = "https://cdn.discordapp.com/avatars/%1$s/%2$s.gif?size=1024";
    /**
     * The format string for icon URLs
     */
    public static final String ICON_URL_PATTERN = "https://cdn.discordapp.com/icons/%1$s/%2$s.jpg";
    /**
     * The endpoint for various random status updates
     */
    public static final String TRACK_ENDPOINT = API_BASE_PATH + "/track";

}
