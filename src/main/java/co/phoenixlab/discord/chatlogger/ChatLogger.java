package co.phoenixlab.discord.chatlogger;

import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import co.phoenixlab.discord.api.event.ChannelChangeEvent;
import co.phoenixlab.discord.api.event.MessageReceivedEvent;
import com.google.common.eventbus.Subscribe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatLogger {

    private static final Path LOG_PATH = Paths.get("chatlogs");
    public static final DateTimeFormatter DATE_TIME_PARSER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.
            ofPattern("MM/dd/uu HH:mm:ss Z").
            withZone(ZoneId.systemDefault());
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@[0-9]+>");

    private static final Server PRIVATE_MESSAGES = new Server("private_messages", "Private Messages");

    private boolean logFailure = false;

    private DiscordApiClient apiClient;

    public ChatLogger(DiscordApiClient apiClient) {
        this.apiClient = apiClient;
        apiClient.getEventBus().register(this);
    }

    @Subscribe
    public void onMessageRecieved(MessageReceivedEvent messageReceivedEvent) {
        Message message = messageReceivedEvent.getMessage();
        if (message.isPrivateMessage()) {
            logPrivateMessage(message);
        } else {
            Channel parentCh = apiClient.getChannelById(message.getChannelId());
            if (parentCh == null) {
                parentCh = DiscordApiClient.NO_CHANNEL;
            }
            Server parentSrv = parentCh.getParent();
            if (parentSrv == null) {
                parentSrv = DiscordApiClient.NO_SERVER;
            }
            logMessage(message, parentSrv, parentCh);
        }
    }

    @Subscribe
    public void onChannelChanged(ChannelChangeEvent channelChangeEvent) {
        if (channelChangeEvent.getChannelChange() == ChannelChangeEvent.ChannelChange.UPDATED) {
            Channel channel = channelChangeEvent.getChannel();
            logMessage(DATE_TIME_FORMATTER.format(ZonedDateTime.now()) +
                    " -C- Channel name changed to #" + channel.getName(),
                    channel.getParent(), channel);
        }
    }

    public void logMessage(Message message, Server server, Channel channel) {
        String logMsg;
        if (message.getAttachments() != null && message.getAttachments().length > 0) {
            logMsg = String.format("%s [%20s] \"%s\": %s",
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    message.getAuthor().getId(),
                    message.getAuthor().getUsername(),
                    message.getAttachments()[0].toString());
        } else {
            logMsg = String.format("%s [%20s] \"%s\": \"%s\"",
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    message.getAuthor().getId(),
                    message.getAuthor().getUsername(),
                    resolveMentions(message, server));
        }
        logMessage(logMsg, server, channel);
    }

    public void logPrivateMessage(Message message) {
        String logMsg;
        if (message.getAttachments() != null && message.getAttachments().length > 0) {
            logMsg = String.format("%s [%20s]: %s",
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    message.getAuthor().getId(),
                    message.getAttachments()[0].toString());
        } else {
            logMsg = String.format("%s [%20s]: \"%s\"",
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    message.getAuthor().getId(),
                    message.getContent());
        }
        logMessage(logMsg, PRIVATE_MESSAGES,
                new Channel(message.getAuthor().getId(), message.getAuthor().getUsername()));
    }

    private String resolveMentions(Message message, Server server) {
        String content = message.getContent();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        Map<String, String> replacements = new HashMap<>();
        while (matcher.find()) {
            String mention = matcher.group();
            String uid = mention.substring(2, mention.length() - 1);
            User user = apiClient.getUserById(uid, server);
            String replacement;
            if (user == DiscordApiClient.NO_USER) {
                replacement = "@UNKNOWN-" + uid;
            } else {
                replacement = "@" + user.getUsername();
            }
            replacements.put(mention, replacement);
        }
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }
        return content;
    }

    private void logMessage(String message, Server server, Channel channel) {
        if (logFailure) {
            return;
        }
        String srv = server.getId();
        String ch = channel.getId();
        Path serverDir = LOG_PATH.resolve(srv);
        Path channelLog = serverDir.resolve(ch + ".log");
        boolean newFile = false;
        try {
            if (!Files.isDirectory(serverDir)) {
                Files.deleteIfExists(serverDir);
                Files.createDirectories(serverDir);
            }
            if (!Files.exists(channelLog)) {
                Files.createFile(channelLog);
                newFile = true;
            }
        } catch (IOException e) {
            VahrhedralBot.LOGGER.error("Unable to log chat message - cannot create required files.", e);
            VahrhedralBot.LOGGER.error("Chat logging has stopped due to an error for srv {} ch {}", srv, ch);
            logFailure = true;
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(channelLog, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND)) {
            if (newFile) {
                writer.write("-C- Channel #" + channel.getName() + "\n");
            }
            writer.write(message);
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            VahrhedralBot.LOGGER.error("Unable to log chat message - cannot write to file.", e);
            VahrhedralBot.LOGGER.error("Chat logging has stopped due to an error for srv {} ch {}", srv, ch);
            logFailure = true;
        }
    }

}
