package co.phoenixlab.discord.commands;

import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.User;

public class CommandUtil {

    static User findUser(MessageContext context, String username) {
        Message message = context.getMessage();
        User user;
        Channel channel = context.getApiClient().getChannelById(message.getChannelId());
        //  Attempt to find the given user
        //  If the user is @mentioned, try that first
        if (message.getMentions() != null && message.getMentions().length > 0) {
            user = message.getMentions()[0];
        } else {
            user = context.getApiClient().findUser(username, channel.getParent());
        }
        //  Try matching by ID
        if (user == DiscordApiClient.NO_USER) {
            user = context.getApiClient().getUserById(username);
        }
        return user;
    }
}
