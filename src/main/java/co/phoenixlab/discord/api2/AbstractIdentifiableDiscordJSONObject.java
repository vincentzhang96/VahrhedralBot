package co.phoenixlab.discord.api2;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Convenience abstract class implementing {@link DiscordJSONObject} that manages ID and handles
 * serialization/deserialization of the ID field.
 */
public class AbstractIdentifiableDiscordJSONObject extends AbstractIdentifiable implements DiscordJSONObject {

    public AbstractIdentifiableDiscordJSONObject() {
        super();
    }

    public AbstractIdentifiableDiscordJSONObject(String id)
            throws NullPointerException, IllegalArgumentException {
        super(id);
    }

    @Override
    public void fromJsonObject(JSONObject jsonObject)
            throws JSONException, NullPointerException {
        Objects.requireNonNull(jsonObject);
        id = jsonObject.getString("id");
        if (!isValid()) {
            throw new IllegalArgumentException("ID is not valid");
        }
    }

    @Override
    public void toJsonObject(JSONObject jsonObject)
            throws NullPointerException {
        Objects.requireNonNull(jsonObject);
        jsonObject.put("id", id);
    }
}
