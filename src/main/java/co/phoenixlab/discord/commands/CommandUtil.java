package co.phoenixlab.discord.commands;

import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.User;

public class CommandUtil {

    public static User findUser(MessageContext context, String username) {
        return findUser(context, username, false);
    }

    public static User findUser(MessageContext context, String username, boolean global) {
        Message message = context.getMessage();
        User user;
        //  Attempt to find the given user
        //  If the user is @mentioned, try that first
        if (message.getMentions() != null && message.getMentions().length > 0) {
            user = message.getMentions()[0];
        } else {
            //  Try exact match, frontal match, ID match, and fuzzy match
            if (global) {
                user = context.getApiClient().findUser(username);
            } else {
                user = context.getApiClient().findUser(username, context.getServer());
            }
        }
        return user;
    }
}
