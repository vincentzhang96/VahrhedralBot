package co.phoenixlab.discord.cfg;

import co.phoenixlab.discord.classdiscussion.EmojiRoleBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassDiscussionConfig {

    private List<EmojiRoleBinding> emojiRoleBindings;
    private String serverId;
    private String channelId;


    public ClassDiscussionConfig() {
        emojiRoleBindings = new ArrayList<>();
    }

    public List<EmojiRoleBinding> getEmojiRoleBindings() {
        return Collections.unmodifiableList(emojiRoleBindings);
    }

    public String getServerId() {
        return serverId;
    }

    public String getChannelId() {
        return channelId;
    }
}
