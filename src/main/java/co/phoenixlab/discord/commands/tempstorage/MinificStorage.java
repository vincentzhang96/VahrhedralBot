package co.phoenixlab.discord.commands.tempstorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MinificStorage {

    private final Set<String> authorizedAuthorUids;
    private final List<Minific> minifics;

    public MinificStorage() {
        authorizedAuthorUids = new HashSet<>();
        minifics = new ArrayList<>();
    }

    public Set<String> getAuthorizedAuthorUids() {
        return authorizedAuthorUids;
    }

    public List<Minific> getMinifics() {
        return minifics;
    }
}
