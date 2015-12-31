package co.phoenixlab.discord.util.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public class InstantGsonTypeAdapter extends TypeAdapter<Instant> {

    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.beginObject();
            out.name("sec").value(value.getEpochSecond());
            out.name("nanos").value(value.getNano());
            out.endObject();
        }
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            return null;
        }
        long sec = 0L;
        int nanos = 0;
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "sec":
                    sec = in.nextLong();
                    break;
                case "nanos":
                    nanos = in.nextInt();
                    break;
            }
        }
        in.endObject();
        return Instant.ofEpochSecond(sec, nanos);
    }
}
