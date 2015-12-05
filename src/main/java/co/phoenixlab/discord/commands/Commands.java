package co.phoenixlab.discord.commands;

import co.phoenixlab.discord.CommandDispatcher;
import co.phoenixlab.discord.Configuration;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.User;

import java.util.StringJoiner;

import static co.phoenixlab.discord.api.DiscordApiClient.NO_USER;
import static co.phoenixlab.discord.commands.CommandUtil.findUser;

public class Commands {

    private final AdminCommands adminCommands;

    public Commands(VahrhedralBot bot) {
        adminCommands = new AdminCommands(bot);
    }

    public void register(CommandDispatcher dispatcher) {
        adminCommands.registerAdminCommands();
        dispatcher.registerAlwaysActiveCommand("sudo", this::admin,
                "Administrative commands");
        dispatcher.registerCommand("admins", this::listAdmins,
                "List admins");
        dispatcher.registerCommand("info", this::info,
                "Display information about the caller or the provided name, if present. @Mentions and partial front " +
                        "matches are supported");
        dispatcher.registerCommand("avatar", this::avatar,
                "Display the avatar of the caller or the provided name, if present. @Mentions and partial front " +
                        "matches are supported");
    }

    private void admin(MessageContext context, String args) {
        //  Permission check
        if (!context.getBot().getConfig().getAdmins().contains(context.getMessage().getAuthor().getId())) {
            context.getApiClient().sendMessage(context.getMessage().getAuthor().getUsername() +
                            " is not in the sudoers file. This incident will be reported",
                    context.getMessage().getChannelId());
            return;
        }
        Message original = context.getMessage();
        adminCommands.getAdminCommandDispatcher().
                handleCommand(new Message(original.getAuthor(), original.getChannelId(), args,
                original.getChannelId(), original.getMentions(), original.getTime()));
    }

    private void listAdmins(MessageContext context, String s) {
        DiscordApiClient apiClient = context.getApiClient();
        VahrhedralBot bot = context.getBot();
        StringJoiner joiner = new StringJoiner(", ");
        bot.getConfig().getAdmins().stream().
                map(apiClient::getUserById).
                filter(user -> user != null).
                map(User::getUsername).
                forEach(joiner::add);
        String res = joiner.toString();
        if (res.isEmpty()) {
            res = "None";
        }
        apiClient.sendMessage("Admins: " + res, context.getMessage().getChannelId());
    }

    private void info(MessageContext context, String args) {
        Message message = context.getMessage();
        Configuration config = context.getBot().getConfig();
        User user;
        if (!args.isEmpty()) {
            user = findUser(context, args);
            selfCheck(context, user);
        } else {
            user = message.getAuthor();
        }
        if (user == NO_USER) {
            context.getApiClient().sendMessage("Unable to find user. Try typing their name EXACTLY or" +
                    " @mention them instead", message.getChannelId());
        } else {
            String avatar = (user.getAvatar() == null ? "N/A" : user.getAvatarUrl().toExternalForm());
            String response = String.format("**Username:** %s\n**ID:** %s\n%s%s**Avatar:** %s",
                    user.getUsername(), user.getId(),
                    config.getBlacklist().contains(user.getId()) ? "**Blacklisted**\n" : "",
                    config.getAdmins().contains(user.getId()) ? "**Bot Administrator**\n" : "",
                    avatar);
            context.getApiClient().sendMessage(response, message.getChannelId());
        }
    }



    private void avatar(MessageContext context, String args) {
        Message message = context.getMessage();
        User user;
        if (!args.isEmpty()) {
            user = findUser(context, args);
            selfCheck(context, user);
        } else {
            user = message.getAuthor();
        }
        if (user == NO_USER) {
            context.getApiClient().sendMessage("Unable to find user. Try typing their name EXACTLY or" +
                    " @mention them instead", message.getChannelId());
        } else {
            String avatar = (user.getAvatar() == null ? "No avatar" : user.getAvatarUrl().toExternalForm());
            context.getApiClient().sendMessage(String.format("%s's avatar: %s", user.getUsername(), avatar),
                    message.getChannelId());
        }
    }

    private void selfCheck(MessageContext context, User user) {
        if (context.getMessage().getAuthor().equals(user)) {
            context.getApiClient().sendMessage(user.getUsername() + ", you can omit your name when " +
                            "using this command to refer to yourself.",
                    context.getMessage().getChannelId());
        }
    }
}
