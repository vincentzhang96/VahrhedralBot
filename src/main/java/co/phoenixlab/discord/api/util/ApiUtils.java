package co.phoenixlab.discord.api.util;

import java.net.MalformedURLException;
import java.net.URL;

public class ApiUtils {

    /**
     * Utility class
     */
    private ApiUtils() {}

    /**
     * Constructs a URL from the given String. Instead of throwing MalformedURLException
     * this method will wrap it and throw it as a RuntimeException instead.
     * @param url
     * @return
     */
    public static URL url(String url) throws RuntimeException {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
