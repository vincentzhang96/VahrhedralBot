package co.phoenixlab.discord.commands.tempstorage;

import java.util.HashMap;
import java.util.Map;

public class DnTrackStorage {

    private final Map<String, DnTrackInfo> regions;

    public DnTrackStorage() {
        regions = new HashMap<>();
    }

    public Map<String, DnTrackInfo> getRegions() {
        return regions;
    }
}
