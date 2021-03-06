package co.phoenixlab.discord.chatlogger;

import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.*;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Longs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
import java.util.Base64;
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

    private final Gson gson;

    private final DiscordApiClient apiClient;

    public ChatLogger(DiscordApiClient apiClient) {
        this.gson = new GsonBuilder().create();
        this.apiClient = apiClient;
        apiClient.getEventBus().register(this);
    }

    @Subscribe
    public void onMessageRecieved(MessageReceivedEvent messageReceivedEvent) {
        Message message = messageReceivedEvent.getMessage();
        if (message.isPrivateMessage()) {
            Channel parentCh = apiClient.getPrivateChannelById(message.getChannelId());
            logPrivateMessage(message, parentCh.getRecipient());
        } else {
            Channel parentCh = apiClient.getChannelById(message.getChannelId());
            if (parentCh == null) {
                parentCh = DiscordApiClient.NO_CHANNEL;
            }
            Server parentSrv = parentCh.getParent();
            if (parentSrv == null) {
                parentSrv = DiscordApiClient.NO_SERVER;
            }
            log(message, parentSrv, parentCh);
        }
    }

    @Subscribe
    public void onChannelChanged(ChannelChangeEvent channelChangeEvent) {
        if (channelChangeEvent.getChannelChange() == ChannelChangeEvent.ChannelChange.UPDATED) {
            Channel channel = channelChangeEvent.getChannel();
            log(DATE_TIME_FORMATTER.format(ZonedDateTime.now()) +
                            " -C- Channel: " + gson.toJson(channelChangeEvent),
                    channel.getParent(), channel);
        }
    }

    @Subscribe
    public void onMemberChange(MemberChangeEvent event) {
        User user = event.getMember().getUser();
        if (event.getMemberChange() == MemberChangeEvent.MemberChange.ADDED) {
            log(DATE_TIME_FORMATTER.format(ZonedDateTime.now()) +
                    " -M- JOINED: " + user.getUsername() + "#" + user.getDiscriminator() + ":" + user.getId(),
                event.getServer().getId(), "userlog", "userlog");
        } else if (event.getMemberChange() == MemberChangeEvent.MemberChange.DELETED) {
            log(DATE_TIME_FORMATTER.format(ZonedDateTime.now()) +
                    " -M- LEFT: " + user.getUsername() + "#" + user.getDiscriminator() + ":" + user.getId(),
                event.getServer().getId(), "userlog", "userlog");
        }
    }


    public void log(Message message, Server server, Channel channel) {
        String logMsg;
        if (message.getAttachments() != null && message.getAttachments().length > 0) {
            logMsg = String.format("%s [%12s] [%20s] \"%s\": \"%s\", %s",
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    base64Encode(message.getId()),
                    message.getAuthor().getId(),
                    message.getAuthor().getUsername(),
                    resolveMentions(message, server),
                    message.getAttachments()[0].toString());
        } else {
            logMsg = String.format("%s [%12s] [%20s] \"%s\": \"%s\"",
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    base64Encode(message.getId()),
                    message.getAuthor().getId(),
                    message.getAuthor().getUsername(),
                    resolveMentions(message, server));
        }
        log(logMsg, server, channel);
    }

    public void logPrivateMessage(Message message, User partner) {
        String logMsg;
        if (message.getAttachments() != null && message.getAttachments().length > 0) {
            logMsg = String.format("%s [%12s] [%20s]: \"%s\", %s",
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    base64Encode(message.getId()),
                    message.getAuthor().getId(),
                    message.getContent(),
                    message.getAttachments()[0].toString());
        } else {
            logMsg = String.format("%s [%12s] [%20s]: \"%s\"",
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    base64Encode(message.getId()),
                    message.getAuthor().getId(),
                    message.getContent());
        }
        log(logMsg, PRIVATE_MESSAGES,
                new Channel(partner.getId(), partner.getUsername()));
    }

    private String resolveMentions(Message message, Server server) {
        String content = message.getContent();
        if (content == null) {
            return "<NULL MESSAGE??>";
        }
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


    private void log(String message, Server server, Channel channel) {
        log(message, server.getId(), channel.getId(), channel.getName());
    }

    private void log(String message, String serverId, String channelId, String channelName) {
        if (logFailure) {
            return;
        }
        Path serverDir = LOG_PATH.resolve(serverId);
        Path channelLog = serverDir.resolve(channelId + ".log");
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
            VahrhedralBot.LOGGER.error("Chat logging has stopped due to an error for srv {} ch {}", serverId, channelId);
            logFailure = true;
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(channelLog, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND)) {
            if (newFile) {
                writer.write("-C- Channel #" + channelName + "\n");
            }
            writer.write(message);
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            VahrhedralBot.LOGGER.error("Unable to log chat message - cannot write to file.", e);
            VahrhedralBot.LOGGER.error("Chat logging has stopped due to an error for srv {} ch {}", serverId, channelId);
            logFailure = true;
        }
    }

    public static String base64Encode(String id) {
        long l = Long.parseLong(id);
        return base64Encode(l);
    }

    public static String base64Encode(long id) {
        return Base64.getUrlEncoder().encodeToString(Longs.toByteArray(id)).replace("=", "");
    }

    @Subscribe
    public void logMessageDel(MessageDeleteEvent event) {
        Channel channel = apiClient.getChannelById(event.getChannelId());
        if (channel == null) {
            channel = apiClient.getPrivateChannelById(event.getChannelId());
        }
        if (channel == null) {
            return;
        }
        if (channel.isPrivate()) {
            log(String.format("%s -C- Message [%12s] deleted",
                DATE_TIME_FORMATTER.format(ZonedDateTime.now()),
                base64Encode(event.getMessageId())),
                PRIVATE_MESSAGES, channel);
        } else {
            log(String.format("%s -C- Message [%12s] deleted",
                DATE_TIME_FORMATTER.format(ZonedDateTime.now()),
                base64Encode(event.getMessageId())),
                channel.getParent(), channel);
        }
    }

    @Subscribe
    public void logMessageEdit(MessageEditEvent event) {
        Message message = event.getMessage();
        if (message.isPrivateMessage()) {
            Channel parentCh = apiClient.getPrivateChannelById(message.getChannelId());
            logPrivateMessageEdit(message, parentCh.getRecipient());
        } else {
            Channel parentCh = apiClient.getChannelById(message.getChannelId());
            if (parentCh == null) {
                parentCh = DiscordApiClient.NO_CHANNEL;
            }
            Server parentSrv = parentCh.getParent();
            if (parentSrv == null) {
                parentSrv = DiscordApiClient.NO_SERVER;
            }
            logMessageEdit(message, parentSrv, parentCh);
        }
    }

    public void logMessageEdit(Message message, Server server, Channel channel) {
        String logMsg;
        if (message.getAttachments() != null && message.getAttachments().length > 0) {
            //  Disallowed
            throw new UnsupportedOperationException("Cannot log edits on embed messages");
        } else {
            logMsg = String.format("%s -C- Edited: [%12s]: \"%s\"",
                    DATE_TIME_FORMATTER.format(ZonedDateTime.now()),
                    base64Encode(message.getId()),
                    resolveMentions(message, server));
        }
        log(logMsg, server, channel);
    }

    public void logPrivateMessageEdit(Message message, User partner) {
        String logMsg;
        if (message.getAttachments() != null && message.getAttachments().length > 0) {
            logMsg = String.format("%s -C- Edited: [%12s]: \"%s\", %s",
                    DATE_TIME_FORMATTER.format(ZonedDateTime.now()),
                    base64Encode(message.getId()),
                    message.getContent(),
                    message.getAttachments()[0].toString());
        } else {
            logMsg = String.format("%s -C- Edited: %s [%12s] [%20s]: \"%s\"",
                    DATE_TIME_FORMATTER.format(ZonedDateTime.now()),
                    DATE_TIME_FORMATTER.format(DATE_TIME_PARSER.parse(message.getTimestamp())),
                    base64Encode(message.getId()),
                    message.getAuthor().getId(),
                    message.getContent());
        }
        log(logMsg, PRIVATE_MESSAGES,
                new Channel(partner.getId(), partner.getUsername()));
    }
}
