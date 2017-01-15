package co.phoenixlab.discord.classdiscussion;

import java.util.Objects;

public class EmojiRoleBinding {

    private final String name;
    private final String emojiId;
    private final String roleId;
    private final String parentMsg;

    public EmojiRoleBinding(String name, String emojiId, String roleId, String parentMsg) {
        this.name = name;
        this.emojiId = emojiId;
        this.roleId = roleId;
        this.parentMsg = parentMsg;
    }

    public EmojiRoleBinding() {
        this("", "", "", "");
    }

    public String getName() {
        return name;
    }

    public String getEmojiId() {
        return emojiId;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getParentMsg() {
        return parentMsg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmojiRoleBinding that = (EmojiRoleBinding) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
