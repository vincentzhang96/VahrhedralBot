package com.divinitor.discord.vahrhedralbot.component;

import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.util.ResourceBundleLocaleStringProvider;
import com.divinitor.discord.vahrhedralbot.AbstractBotComponent;
import com.divinitor.discord.vahrhedralbot.EntryPoint;
import com.divinitor.discord.vahrhedralbot.secrets.SecretHandle;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TwitchStreamListener extends AbstractBotComponent {

    public static final Logger LOGGER = LoggerFactory.getLogger("VahrhedralBot.TwitchStreamStatus");
    public static final String TWITCH_API_CLIENTID_SECRET_KEY = "api.twitch.clientid";

    /**
     * Cached user stream info (key is twitch id)
     */
    private LoadingCache<String, TwitchStreamDiscordStatusListener.Stream> userStreamInfoCache;

    /**
     * Cached username -> ID
     */
    private LoadingCache<String, String> userNameToIdCache;

    private SecretHandle twitchClientId;

    @Override
    public void register(EntryPoint entryPoint) throws Exception {
        super.register(entryPoint);

        entryPoint.getBot().getLocalizer().addLocaleStringProvider(
            new ResourceBundleLocaleStringProvider(
                "com.divinitor.discord.vahrhedralbot.component.twitchstreamlocale"));

        twitchClientId = entryPoint.getSecretsStore().getSecretHandler(TWITCH_API_CLIENTID_SECRET_KEY);
    }

    @Override
    public void init() throws Exception {
        super.init();

        //  Start ticking task
//        VahrhedralBot.EXECUTOR_SERVICE.scheduleWithFixedDelay()
    }

    private void poll() {

    }

    private void postUpdate(String serverId, String twitchUrl) {
        if (twitchUrl.endsWith("/")) {
            twitchUrl = twitchUrl.substring(0, twitchUrl.length() - 1);
        }

        int lastIdxOf = twitchUrl.lastIndexOf('/');
        if (lastIdxOf == -1) {
            LOGGER.warn("Unable to get username from twitch URL {}", twitchUrl);
            return;
        }

        String twitchName = twitchUrl.substring(lastIdxOf + 1);
        String twitchId = userNameToIdCache.getUnchecked(twitchName);
        TwitchStreamDiscordStatusListener.Stream stream = userStreamInfoCache.getUnchecked(twitchId);

        if (stream == null) {
            LOGGER.warn("Stream {} isn't live?", twitchUrl);
            return;
        }

        TwitchStreamDiscordStatusListener.StreamInfo sinfo = stream.getStream();
        if (sinfo == null) {
            LOGGER.warn("Stream {} has no channel info", twitchUrl);
            return;
        }

        TwitchStreamDiscordStatusListener.ChannelInfo channel = sinfo.getChannel();

        Localizer loc = getBot().getLocalizer();

        Embed embed = new Embed();
        embed.setType(Embed.TYPE_RICH);
        embed.setColor(5846677);

        embed.setDescription(loc.localize("component.twitch.sl.status.startstream.message",
            channel.getDisplayName(),
            sinfo.getGame(),
            channel.getUrl()));

        EmbedImage image = new EmbedImage();
        image.setUrl(sinfo.getPreview().getMedium());
        embed.setImage(image);

        EmbedThumbnail thumbnail = new EmbedThumbnail();
        thumbnail.setUrl(channel.getLogo());
        embed.setThumbnail(thumbnail);

        EmbedFooter footer = new EmbedFooter();
        footer.setText(loc.localize("component.twitch.sl.status.startstream.footer"));
        embed.setFooter(footer);

        embed.setTimestamp(sinfo.getCreatedAt());

        List<EmbedField> fields = new ArrayList<>();
        fields.add(new EmbedField(
            loc.localize("component.twitch.sl.status.startstream.field.title"),
            loc.localize("component.twitch.sl.status.startstream.field.title.value",
                channel.getStatus()),
            false));
        fields.add(new EmbedField(
            loc.localize("component.twitch.sl.status.startstream.field.followers"),
            loc.localize("component.twitch.sl.status.startstream.field.followers.value",
                channel.getFollowers()),
            true));
        fields.add(new EmbedField(
            loc.localize("component.twitch.sl.status.startstream.field.views"),
            loc.localize("component.twitch.sl.status.startstream.field.views.value",
                channel.getViews()),
            true));

        embed.setFields(fields.toArray(new EmbedField[fields.size()]));

        getBot().getApiClient().sendMessage("", getBroadcastChannelForServer(serverId), embed);

        LOGGER.debug("Dispatched Twitch notice for {} in {}",
            twitchName, serverId);
    }

    private String getBroadcastChannelForServer(String serverId) {
        //  TODO
        return serverId;
    }
}
