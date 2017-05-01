package co.phoenixlab.discord.commands.fun;

import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Member;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import co.phoenixlab.discord.util.RateLimiter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SignCommand implements Command {
    public static final int COOLDOWN_PERIOD = 20;
    public static final int COOLDOWN_CHARGES = 3;
    public static final String SIGN = "|￣￣￣￣￣￣￣￣￣￣￣￣￣￣￣￣|\n" +
            "%s\n" +
            "|＿＿＿＿＿＿＿＿＿＿＿＿＿＿＿＿|\n" +
            "              (__/)   ||\n" +
            "              (•ㅅ•) ||   ~%s\n" +
            "             / 　 づ";
    public static final int MESSAGE_AREA_WIDTH = 64;
    private final LoadingCache<String, RateLimiter> rateLimiters;

    public SignCommand() {
        rateLimiters = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, RateLimiter>() {
                    @Override
                    public RateLimiter load(String key) throws Exception {
                        return new RateLimiter(
                                key,
                                TimeUnit.SECONDS.toMillis(COOLDOWN_PERIOD),
                                COOLDOWN_CHARGES
                        );
                    }
                });
    }

    @Override
    public void handleCommand(MessageContext context, String args) {
        if (args.isEmpty()) {
            return;
        }
        Server server = context.getServer();
        if (server == null || server == DiscordApiClient.NO_SERVER) {
            return;
        }
        if (checkRateLimit(context, context.getAuthor(), context.getChannel())) {
            return;
        }
        String[] lines = new String[args.length() / MESSAGE_AREA_WIDTH + 1];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = args.substring(i * MESSAGE_AREA_WIDTH,
                    Math.min((i + 1) * MESSAGE_AREA_WIDTH, args.length()));
        }
        //  center the last line
        String lastLine = lines[lines.length - 1];
        int numPad = MESSAGE_AREA_WIDTH - lastLine.length();
        if (numPad > 1) {
            numPad /= 3;
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < numPad; i++) {
                builder.append(' ');
            }
            builder.append(lastLine);
            lines[lines.length - 1] = builder.toString();
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (String line : lines) {
            joiner.add(line);
        }
        User author = context.getAuthor();
        Member authorMember = context.getApiClient().getUserMember(author, server);
        String output = String.format(SIGN, joiner.toString(), getUserDisplayName(author, authorMember));
        context.getApiClient().sendMessage(output, context.getChannel());
        context.getApiClient().deleteMessage(context.getChannel().getId(), context.getMessage().getId());
    }

    private String getUserDisplayName(User user, Member member) {
        return isNullOrEmpty(member.getNick()) ?
            user.getUsername() : member.getNick();
    }

    private boolean checkRateLimit(MessageContext context, User user, Channel channel) {
        if (!context.getBot().getConfig().isAdmin(user.getId())) {
            RateLimiter limiter = rateLimiters.getUnchecked(channel.getId());
            if (limiter.tryMark() > 0) {
                return true;
            }
        }
        return false;
    }
}
