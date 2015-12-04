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
    public static final String API_BASE_PATH = BASE_URL + "api";
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
    public static final String AVATAR_URL_PATTERN = "https://cdn.discordapp.com/avatars/%1$s/%2$s.jpg";

}
