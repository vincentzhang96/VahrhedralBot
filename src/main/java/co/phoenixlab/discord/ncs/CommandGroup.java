package co.phoenixlab.discord.ncs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CommandGroup {

    private final CommandGroup parent;
    private final String groupName;
    private final Map<String, CommandGroup> childrenGroups;

    public CommandGroup(CommandGroup parent, String groupName) {
        this.parent = parent;
        this.groupName = groupName;
        this.childrenGroups = new HashMap<>();
    }

    public CommandGroup getParent() {
        return parent;
    }

    public String getGroupName() {
        return groupName;
    }

    public Map<String, CommandGroup> getChildrenGroups() {
        return Collections.unmodifiableMap(childrenGroups);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CommandGroup that = (CommandGroup) o;
        return Objects.equals(groupName, that.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName);
    }

}
