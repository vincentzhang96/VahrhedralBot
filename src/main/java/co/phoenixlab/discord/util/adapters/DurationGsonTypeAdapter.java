package co.phoenixlab.discord.util.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;

public class DurationGsonTypeAdapter extends TypeAdapter<Duration> {
    @Override
    public void write(JsonWriter out, Duration value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.beginObject();
            out.name("sec").value(value.getSeconds());
            out.name("nanos").value(value.getNano());
            out.endObject();
        }
    }

    @Override
    public Duration read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            return null;
        }
        long sec = 0L;
        int nanos = 0;
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
        return Duration.ofSeconds(sec, nanos);
    }
}
