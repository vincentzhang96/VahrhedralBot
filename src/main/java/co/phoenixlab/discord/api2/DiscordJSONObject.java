package co.phoenixlab.discord.api2;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an object from Discord's JSON data.
 */
public interface DiscordJSONObject {

    /**
     * Populates the object with data from the provided JSONObject.
     *
     * @param jsonObject The JSONObject to deserialize from.
     * @throws JSONException            If the provided JSONObject does not contain mandatory fields or the field is
     *                                  of the wrong type.
     * @throws NullPointerException     If the provided JSONObject is null.
     * @throws IllegalArgumentException If a value from the JSONObject is invalid for the given field.
     */
    void fromJsonObject(JSONObject jsonObject)
            throws JSONException, NullPointerException, IllegalArgumentException;

    /**
     * Populates the provided JSONObject with the object's data.
     *
     * @param jsonObject The JSONObject to populate with the object's data.
     * @throws NullPointerException If the provided JSONObject is null.
     */
    void toJsonObject(JSONObject jsonObject)
            throws NullPointerException;

}
