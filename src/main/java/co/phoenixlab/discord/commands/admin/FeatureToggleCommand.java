package co.phoenixlab.discord.commands.admin;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.discord.Command;
import co.phoenixlab.discord.MessageContext;
import co.phoenixlab.discord.VahrhedralBot;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.cfg.FeatureToggle;
import co.phoenixlab.discord.cfg.FeatureToggle.Override;
import co.phoenixlab.discord.cfg.FeatureToggleConfig;

public class FeatureToggleCommand implements Command {

    private final VahrhedralBot bot;
    private final FeatureToggleConfig config;

    public FeatureToggleCommand(VahrhedralBot bot) {
        this.bot = bot;
        this.config = bot.getToggleConfig();
    }

    @java.lang.Override
    public void handleCommand(MessageContext context, String args) {
        String[] params = args.split(" ", 3);
        String cmd = params[0].toLowerCase();
        switch (cmd) {
            case "enable":
            case "enable-toggle":
            case "toggle-enable":
            case "toggle-on":
                if (params.length == 2) {
                    setToggle(params[1], true, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "disable":
            case "disable-toggle":
            case "toggle-disable":
            case "toggle-off":
                if (params.length == 2) {
                    setToggle(params[1], false, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "global-enable":
            case "enable-global":
                if (params.length == 2) {
                    setGlobalToggle(params[1], false, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "global-disable":
            case "disable-global":
                if (params.length == 2) {
                    setGlobalToggle(params[1], true, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "status":
            case "get":
            case "get-status":
            case "show-status":
            case "show":
            case "show-toggle":
                if (params.length == 2) {
                    getToggleStatus(params[1], context.getServer().getId(), context.getChannel().getId(), context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "enable-server":
            case "server-enable":
                if (params.length == 2) {
                    setServerToggle(params[1], context.getServer().getId(), Override.ENABLED, context);
                } else if (params.length == 3) {
                    setServerToggle(params[1], params[2], Override.ENABLED, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "disable-server":
            case "server-disable":
                if (params.length == 2) {
                    setServerToggle(params[1], context.getServer().getId(), Override.DISABLED, context);
                } else if (params.length == 3) {
                    setServerToggle(params[1], params[2], Override.DISABLED, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "remove-server":
            case "delete-server":
            case "server-remove":
            case "server-delete":
                if (params.length == 2) {
                    setServerToggle(params[1], context.getServer().getId(), Override.NOT_SET, context);
                } else if (params.length == 3) {
                    setServerToggle(params[1], params[2], Override.NOT_SET, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "enable-channel":
            case "channel-enable":
                if (params.length == 2) {
                    setChannelToggle(params[1], context.getChannel().getId(), Override.ENABLED, context);
                } else if (params.length == 3) {
                    setChannelToggle(params[1], params[2], Override.ENABLED, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "disable-channel":
            case "channel-disable":
                if (params.length == 2) {
                    setChannelToggle(params[1], context.getChannel().getId(), Override.DISABLED, context);
                } else if (params.length == 3) {
                    setChannelToggle(params[1], params[2], Override.DISABLED, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "remove-channel":
            case "delete-channel":
            case "channel-remove":
            case "channel-delete":
                if (params.length == 2) {
                    setChannelToggle(params[1], context.getChannel().getId(), Override.NOT_SET, context);
                } else if (params.length == 3) {
                    setChannelToggle(params[1], params[2], Override.NOT_SET, context);
                } else {
                    //  TODO error missing toggle name
                }
                break;
            case "flush-config":
            case "save-config":
            case "config-flush":
            case "config-save":
            case "save":
                bot.saveFeatureToggleConfig();
                context.getApiClient().sendMessage("Config flushed", context.getChannel());
                break;
            case "delete-toggle":
            case "toggle-delete":
            case "remove-toggle":
            case "toggle-remove":
                if (params.length == 2) {
                    boolean removed = config.getToggles().remove(params[1]) != null;
                    bot.saveFeatureToggleConfig();
                    if (removed) {
                        context.getApiClient().sendMessage(String.format("Toggle `%s` removed", params[1]),
                            context.getChannel());
                    } else {
                        context.getApiClient().sendMessage(String.format("Toggle `%s` does not exist", params[1]),
                            context.getChannel());
                    }
                } else {
                    //  TODO error missing toggle name
                }
        }
    }

    private void setToggle(String toggle, boolean enabled, MessageContext ctx) {
        config.getToggle(toggle).setEnabled(enabled);
        DiscordApiClient api = ctx.getApiClient();
        api.sendMessage(String.format("Toggle `%s` has been %s for non overriden channels and servers.",
            toggle, enabled ? ":white_check_mark: ENABLED" : ":x: DISABLED"),
            ctx.getChannel());
        getToggleStatus(toggle, null, null, ctx);
        bot.saveFeatureToggleConfig();
    }

    private void setGlobalToggle(String toggle, boolean disabled, MessageContext ctx) {
        config.getToggle(toggle).setGloballyDisabled(disabled);
        DiscordApiClient api = ctx.getApiClient();
        api.sendMessage(String.format("Toggle `%s` has been %s globally (overrides all settings).",
            toggle, disabled ? ":x: DISABLED" : ":white_check_mark: ENABLED"),
            ctx.getChannel());
        getToggleStatus(toggle, null, null, ctx);
        bot.saveFeatureToggleConfig();
    }

    private void getToggleStatus(String toggle, String serverId, String channelId, MessageContext ctx) {
        serverId = SafeNav.of(serverId).orElse(ctx.getServer().getId());
        channelId = SafeNav.of(channelId).orElse(ctx.getChannel().getId());
        FeatureToggle featureToggle = config.getToggle(toggle);
        StringBuilder builder = new StringBuilder(String.format("**__%s Toggle Status__**\n", toggle));
        builder.append(String.format("Status: %s\n",
        featureToggle.isGloballyDisabled() ? ":no_entry: GLOBAL DISABLED" :
            (featureToggle.isEnabled() ? ":white_check_mark: ENABLED" : ":x: DISABLED")));
        Override override = featureToggle.getServerOverride(serverId);
        if (override != Override.NOT_SET) {
            builder.append(String.format("Server override: %s\n", override.name()));
        }
        override = featureToggle.getChannelOverride(channelId);
        if (override != Override.NOT_SET) {
            builder.append(String.format("Channel override: %s\n", override.name()));
        }
        builder.append("\n**EVALUATED STATUS: ");
        builder.append(featureToggle.use(serverId, channelId) ? ":white_check_mark: ENABLED" : ":x: DISABLED");
        builder.append("**");
        DiscordApiClient api = ctx.getApiClient();
        api.sendMessage(builder.toString(), ctx.getChannel());
    }

    private void setServerToggle(String toggle, String serverId, Override override, MessageContext ctx) {
        config.getToggle(toggle).setServerOverride(serverId, override);
        DiscordApiClient api = ctx.getApiClient();
        if (override == Override.NOT_SET) {
            api.sendMessage(String.format("Toggle `%s` server override removed.",
                toggle),
                ctx.getChannel());
        } else {
            api.sendMessage(String.format("Toggle `%s` has been %s for this server (override).",
                toggle, override.name()),
                ctx.getChannel());
        }
        getToggleStatus(toggle, serverId, null, ctx);
        bot.saveFeatureToggleConfig();
    }

    private void setChannelToggle(String toggle, String channelId, Override override, MessageContext ctx) {
        config.getToggle(toggle).setChannelOverride(channelId, override);
        DiscordApiClient api = ctx.getApiClient();
        if (override == Override.NOT_SET) {
            api.sendMessage(String.format("Toggle `%s` channel override removed.",
                toggle),
                ctx.getChannel());
        } else {
            api.sendMessage(String.format("Toggle `%s` has been %s for this channel (override).",
                toggle, override.name()),
                ctx.getChannel());
        }
        getToggleStatus(toggle, null, channelId, ctx);
        bot.saveFeatureToggleConfig();
    }
}
