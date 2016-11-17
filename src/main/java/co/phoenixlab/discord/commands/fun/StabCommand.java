package co.phoenixlab.discord.commands.fun;

import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.api.entities.Channel;
import co.phoenixlab.discord.api.entities.Member;
import co.phoenixlab.discord.api.entities.Server;
import co.phoenixlab.discord.api.entities.User;
import co.phoenixlab.discord.commands.CommandUtil;
import co.phoenixlab.discord.util.RateLimiter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StabCommand implements Command {

    public float regularStabChance = 0.95F;

    private LoadingCache<String, RateLimiter> rateLimiters;
    private Random random;

    public StabCommand() {
        rateLimiters = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, RateLimiter>() {
                    @Override
                    public RateLimiter load(String key) throws Exception {
                        return new RateLimiter(
                                key,
                                TimeUnit.SECONDS.toMillis(30),
                                3
                        );
                    }
                });
        random = new Random();
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
        if (args.startsWith("$admin")) {
            performStabAdmin(context, args);
        } else {
            performStab(context, args);
        }
    }

    private void performStabAdmin(MessageContext context, String args) {

    }

    private void performStab(MessageContext context, String args) {
        DiscordApiClient api = context.getApiClient();
        Server server = context.getServer();
        User stabbedUser = CommandUtil.findUser(context, args, false);
        if (stabbedUser == DiscordApiClient.NO_USER || stabbedUser == null) {
            return;
        }
        User stabbingUser = context.getAuthor();
        //  Rate limiting
        Channel channel = context.getChannel();
        if (checkRateLimit(context, stabbingUser, channel)) {
            return;
        }
        String msg = getStabMessage(context, stabbingUser, stabbedUser);
        api.sendMessage("*" + msg + "*", channel);
    }

    private String getStabMessage(MessageContext context, User stabbingUser, User stabbedUser) {
        DiscordApiClient api = context.getApiClient();
        Server server = context.getServer();
        Member stabbingMember = api.getUserMember(stabbingUser, server);
        Member stabbedMember = api.getUserMember(stabbedUser, server);
        String stabbingName = getUserDisplayName(stabbingUser, stabbingMember);
        String stabbedName = getUserDisplayName(stabbedUser, stabbedMember);
        float rand = random.nextFloat();
        String msg = "";
        if (rand < regularStabChance) {
            msg = getRegularStabMessage(context, stabbedUser, stabbingUser, stabbedName);
        } else {
            msg = getAlternativeStabbingMessage(stabbingName, stabbedName);
        }
        return msg;
    }

    private String getAlternativeStabbingMessage(String stabbingName, String stabbedName) {
        int randi = random.nextInt(4);
        String msg;
        String stabbingAndStabbed = stabbingName + " and " + stabbedName;
        switch (randi) {
            case 0:
                msg = stabbingAndStabbed + " make love! :heart:";
                break;
            case 1:
                msg = stabbingAndStabbed + " kiss passionately! :kissing_heart:";
                break;
            case 2:
                msg = stabbingAndStabbed + " glance at each other, blushing. D'aww! :blush:";
                break;
            case 3:
                msg = "Takes out camera :smirk:";
                break;
            default:
                msg = "Bot is out of order, wtf Vahr you suck at coding.";
                break;
        }
        return msg;
    }

    private String getRegularStabMessage(MessageContext context, User stabbedUser, User stabbingUser,
                                         String stabbedName) {
        DiscordApiClient api = context.getApiClient();
        Server server = context.getServer();
        String msg;
        String stabbingName;
        if (api.getClientUser().equals(stabbedUser)) {
            //  Bot stabbing itself
            msg = stabbedName + " stabs itself!";
        } else if (stabbingUser.equals(stabbedUser) ||
                context.getBot().getConfig().isAdmin(stabbedUser.getId())) {
            //  User attempting to stab themselves or an admin
            //  They get stabbed by the bot instead
            Member selfMember = api.getUserMember(api.getClientUser(), server);
            stabbingName = getUserDisplayName(api.getClientUser(), selfMember);
            msg = stabbedName + " stabs " + stabbingName + "!";
        } else {
            msg = stabbedName + " gets stabbed!";
        }
        return msg;
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
