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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StabCommand implements Command {

    public float regularStabChance = 0.90F;

    private LoadingCache<String, RateLimiter> rateLimiters;
    private Random random;

    private boolean globalEnabled = true;
    private Set<String> disabledServers;
    private List<BiFunction<String, String, String>> alternateStabs;

    public StabCommand() {
        disabledServers = new HashSet<>();
        alternateStabs = new ArrayList<>();
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

        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s and %2$s make love! :heart:",
            stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s and %2$s kiss passionately! :kissing_heart:", stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s and %2$s glance at each other, blushing. D'aww! :blush:",
            stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s passionately plunges their blade into %2$s's soft flesh as %2$s screams in pain :dagger:",
            stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s passionately plunges their blade into %2$s's soft flesh as %2$s moans with pleasure :eggplant:",
            stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s goes deep into %2$s. ouo3o",
            stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s stabs %2$s, but %2$s seems to enjoy it a little too much... :smirk:",
            stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s stabs %2$s, but doesn't realize that %2$s penetrated them already. :scream:",
            stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> String.format(
            "%1$s flips %2$s, but you have to question how with those wimpy arms... :thinking:",
            stabber, stabbed));
        alternateStabs.add((stabber, stabbed) -> "Takes out camera :smirk:");
        alternateStabs.add((stabber, stabbed) -> "You have to pay Vahr 500g for this feature");



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
        DiscordApiClient api = context.getApiClient();
        Channel channel = context.getChannel();
        Server server = context.getServer();
        String serverId = server.getId();
        args = args.substring("$admin".length()).trim();
        if (args.startsWith("gon")) {
            globalEnabled = true;
            api.sendMessage("[Global] Stabbing enabled", channel);
        } else if (args.startsWith("goff")) {
            globalEnabled = false;
            api.sendMessage("[Global] Stabbing disabled", channel);
        } else if (args.startsWith("on")) {
            disabledServers.remove(serverId);
            api.sendMessage("[Server] Stabbing enabled", channel);
        } else if (args.startsWith("off")) {
            disabledServers.add(serverId);
            api.sendMessage("[Server] Stabbing disabled", channel);
        } else if (args.startsWith("altinvoke ")) {
            String name = args.substring("altinvoke ".length());
            performStab(context, name, true);
        }
    }

    private void performStab(MessageContext context, String args) {
        performStab(context, args, false);
    }

    private void performStab(MessageContext context, String args, boolean forceAlt) {
        if (!globalEnabled) {
            return;
        }
        DiscordApiClient api = context.getApiClient();
        Server server = context.getServer();
        if (disabledServers.contains(server.getId())) {
            return;
        }
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
        String msg = getStabMessage(context, stabbingUser, stabbedUser, forceAlt);
        api.sendMessage("*" + msg + "*", channel);
    }

    private String getStabMessage(MessageContext context, User stabbingUser, User stabbedUser, boolean forceAlt) {
        DiscordApiClient api = context.getApiClient();
        Server server = context.getServer();
        Member stabbingMember = api.getUserMember(stabbingUser, server);
        Member stabbedMember = api.getUserMember(stabbedUser, server);
        String stabbingName = getUserDisplayName(stabbingUser, stabbingMember);
        String stabbedName = getUserDisplayName(stabbedUser, stabbedMember);
        float rand = random.nextFloat();
        String msg = "";
        if (rand < regularStabChance && !forceAlt) {
            msg = getRegularStabMessage(context, stabbingUser, stabbedUser, stabbingName, stabbedName);
        } else {
            msg = getAlternativeStabbingMessage(stabbingName, stabbedName);
        }
        return msg;
    }

    private String getAlternativeStabbingMessage(String stabbingName, String stabbedName) {
        int randi = random.nextInt(alternateStabs.size());
        return alternateStabs.get(randi).apply(stabbingName, stabbedName);
    }

    private String getRegularStabMessage(MessageContext context, User stabbingUser, User stabbedUser,
                                         String stabbingName, String stabbedName) {
        DiscordApiClient api = context.getApiClient();
        Server server = context.getServer();
        String msg;
        if (api.getClientUser().equals(stabbedUser)) {
            //  Bot stabbing itself
            msg = stabbedName + " stabs itself!";
        } else if (stabbingUser.equals(stabbedUser) ||
                context.getBot().getConfig().isAdmin(stabbedUser.getId())) {
            //  User attempting to stab themselves or an admin
            //  They get stabbed by the bot instead
            Member selfMember = api.getUserMember(api.getClientUser(), server);
            String selfName = getUserDisplayName(api.getClientUser(), selfMember);
            msg = selfName + " stabs " + stabbingName + "!";
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
