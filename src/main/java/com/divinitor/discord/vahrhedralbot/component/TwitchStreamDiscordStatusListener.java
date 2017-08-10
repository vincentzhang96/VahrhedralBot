package com.divinitor.discord.vahrhedralbot.component;

import co.phoenixlab.common.lang.SafeNav;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.discord.api.entities.*;
import co.phoenixlab.discord.api.event.PresenceUpdateEvent;
import co.phoenixlab.discord.util.ResourceBundleLocaleStringProvider;
import com.divinitor.discord.vahrhedralbot.AbstractBotComponent;
import com.divinitor.discord.vahrhedralbot.EntryPoint;
import com.divinitor.discord.vahrhedralbot.secrets.SecretHandle;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TwitchStreamDiscordStatusListener extends AbstractBotComponent {

    public static final Logger LOGGER = LoggerFactory.getLogger("VahrhedralBot.TwitchStreamStatus");
    public static final String TWITCH_API_CLIENTID_SECRET_KEY = "api.twitch.clientid";

    /**
     * UUID (serverId-userId) to Game
     */
    private Map<String, Game> gameStatus;

    /**
     * Cached user stream info (key is twitch id)
     */
    private LoadingCache<String, Stream> userStreamInfoCache;

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
                "com.divinitor.discord.vahrhedralbot.component.twitchstreamdiscordstatuslocale"));

        twitchClientId = entryPoint.getSecretsStore().getSecretHandler(TWITCH_API_CLIENTID_SECRET_KEY);

        gameStatus = new HashMap<>();

        userStreamInfoCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(64)
            .build(createStreamCacheLoader());

        userNameToIdCache = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(1024)
            .build(createClientIdCacheLoader());
    }

    private CacheLoader<String, Stream> createStreamCacheLoader() {
        return new CacheLoader<String, Stream>() {
            @Override
            public Stream load(String key) throws Exception {
                return getStream(key);
            }
        };
    }

    private Stream getStream(String twitchId) throws UnirestException {
        HttpResponse<String> resp = Unirest.get("https://api.twitch.tv/kraken/streams/" + twitchId)
            .headers(headers())
            .asString();

        LOGGER.debug(resp.getBody());

        Stream ret = (new Gson()).fromJson(resp.getBody(), Stream.class);

        return ret;
    }

    private CacheLoader<String, String> createClientIdCacheLoader() {
        return new CacheLoader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                return getId(key);
            }
        };
    }

    private String getId(String twitchUsername) throws UnirestException, IOException {
        HttpResponse<JsonNode> resp = Unirest.get("https://api.twitch.tv/kraken/users?login=" +
            URLEncoder.encode(twitchUsername, "UTF-8"))
        .headers(headers())
        .asJson();

        LOGGER.debug(resp.getBody().toString());

        try {
            return resp.getBody()
                .getObject()
                .getJSONArray("users")
                .getJSONObject(0)
                .getString("_id");
        } catch (Exception e) {
            throw new IOException("Failed to get id for " + twitchUsername + ": " + resp.getStatus() + ": " +
                resp.getStatusText() + ": " + resp.getBody().toString());
        }
    }

    private Map<String, String> headers() {
        Map<String, String> ret = new HashMap<>();
        ret.put("Accept", "application/vnd.twitchtv.v5+json");
        ret.put("Client-ID", twitchClientId.get());
        return ret;
    }

    @Override
    public void init() throws Exception {
        super.init();

        if (Strings.isNullOrEmpty(twitchClientId.get())) {
            LOGGER.warn("Twitch Client ID is not set, please ensure that the secret \"{}\" has been set.",
                TWITCH_API_CLIENTID_SECRET_KEY);
        }
    }

    @Subscribe
    public void onPresenceChanged(PresenceUpdateEvent event) {
        //  This event fires off once for each server that the user shares with us
        String userId = event.getPresenceUpdate().getUser().getId();
        String serverId = event.getServer().getId();

        if (!getBot().getToggleConfig().getToggle("component.twitch.discord.streamstatus.listen").use(serverId)) {
            return;
        }

        String uuid = uuid(userId, serverId);

        //  Check that we care about this user
        if (!shouldWatchUser(userId, serverId)) {
            return;
        }

        if (!gameStatus.containsKey(uuid)) {
            gameStatus.put(uuid, event.getPresenceUpdate().getGame());
            return;
        }

        Game oldGame = gameStatus.get(uuid);
        Game newGame = SafeNav.of(event).next(PresenceUpdateEvent::getPresenceUpdate).get(PresenceUpdate::getGame);
        //  Update
        gameStatus.put(uuid, newGame);

        //  Null
        if (oldGame == null && newGame == null) {
            return;
        }

        //  Ending stream
        if (newGame == null) {
            return;
        }

        //  No previous status, now streaming
        if (oldGame == null || !oldGame.isStreaming()) {
            if (newGame.isStreaming()) {
                announceStreamUp(event);
            }
            return;
        }

        //  Had previous status, ending stream
        if (oldGame.isStreaming() && !newGame.isStreaming()) {
            return;
        }

        //  Change in stream?
        if (oldGame.isStreaming() && newGame.isStreaming()) {
            //  TODO
//            announceStreamUp(event);
        }
    }

    private void announceStreamUp(PresenceUpdateEvent event) {
        String serverId = event.getServer().getId();
        if (!getBot().getToggleConfig().getToggle("component.twitch.discord.streamstatus.announce")
            .use(serverId)) {
            return;
        }

        String twitchUrl = event.getPresenceUpdate().getGame().getUrl();
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
        Stream stream = userStreamInfoCache.getUnchecked(twitchId);

        if (stream == null) {
            LOGGER.warn("Stream {} isn't live?", twitchUrl);
            return;
        }

        StreamInfo sinfo = stream.getStream();
        if (sinfo == null) {
            LOGGER.warn("Stream {} has no channel info", twitchUrl);
            return;
        }

        ChannelInfo channel = sinfo.getChannel();

        PresenceUpdate update = event.getPresenceUpdate();
        User user = update.getUser();
        Member member = getBot().getApiClient().getUserMember(user.getId(), update.getServerId());
        Localizer loc = getBot().getLocalizer();

        Embed embed = new Embed();
        embed.setType(Embed.TYPE_RICH);
        embed.setColor(5846677);

        embed.setDescription(loc.localize("component.twitch.sdsl.status.startstream.message",
            channel.getDisplayName(),
            sinfo.getGame(),
            channel.getUrl()));

        EmbedAuthor author = new EmbedAuthor();
        author.setIconUrl(user.getAvatarUrl().toExternalForm());
        String authorNameFormatKey;
        if (member.getNick() != null) {
            authorNameFormatKey = "component.twitch.sdsl.status.startstream.author.nickname";
        } else {
            authorNameFormatKey = "component.twitch.sdsl.status.startstream.author";
        }
        author.setName(loc.localize(authorNameFormatKey,
            user.getUsername(), user.getDiscriminator(), member.getNick()));
        embed.setAuthor(author);

        EmbedImage image = new EmbedImage();
        image.setUrl(sinfo.getPreview().getMedium());
        embed.setImage(image);

        EmbedThumbnail thumbnail = new EmbedThumbnail();
        thumbnail.setUrl(channel.getLogo());
        embed.setThumbnail(thumbnail);

        EmbedFooter footer = new EmbedFooter();
        footer.setText(loc.localize("component.twitch.sdsl.status.startstream.footer"));
        embed.setFooter(footer);

        embed.setTimestamp(sinfo.getCreatedAt());

        List<EmbedField> fields = new ArrayList<>();
        fields.add(new EmbedField(
            loc.localize("component.twitch.sdsl.status.startstream.field.title"),
            loc.localize("component.twitch.sdsl.status.startstream.field.title.value",
                channel.getStatus()),
            false));
        fields.add(new EmbedField(
            loc.localize("component.twitch.sdsl.status.startstream.field.followers"),
            loc.localize("component.twitch.sdsl.status.startstream.field.followers.value",
                channel.getFollowers()),
            true));
        fields.add(new EmbedField(
            loc.localize("component.twitch.sdsl.status.startstream.field.views"),
            loc.localize("component.twitch.sdsl.status.startstream.field.views.value",
                channel.getViews()),
            true));

        embed.setFields(fields.toArray(new EmbedField[fields.size()]));

        getBot().getApiClient().sendMessage("", getBroadcastChannelForServer(serverId), embed);

        LOGGER.debug("Dispatched Twitch notice for {}#{} in {}",
            user.getUsername(), user.getDiscriminator(), serverId);
    }

    private boolean shouldWatchUser(String userId, String serverId) {
        return true;
    }

    private boolean shouldWatchUser(String uuid) {
        String[] split = uuid.split("-");
        return shouldWatchUser(split[0], split[1]);
    }

    private String getBroadcastChannelForServer(String serverId) {
        //  TODO
        return serverId;
    }

    private static String uuid(String userId, String serverId) {
        return userId + "-" + serverId;
    }

    public static class Stream {
        private StreamInfo stream;

        public Stream() {
        }

        public StreamInfo getStream() {
            return stream;
        }
    }

    public static class StreamInfo {

        @SerializedName("_id")
        private long id;
        private String game;
        private int viewers;
        @SerializedName("video_height")
        private int videoHeight;
        @SerializedName("average_fps")
        private int averageFps;
        private int delay;
        @SerializedName("created_at")
        private String createdAt;
        @SerializedName("is_playlist")
        private boolean isPlaylist;
        private Preview preview;
        private ChannelInfo channel;

        public StreamInfo() {
        }

        public long getId() {
            return id;
        }

        public String getGame() {
            return game;
        }

        public int getViewers() {
            return viewers;
        }

        public int getVideoHeight() {
            return videoHeight;
        }

        public int getAverageFps() {
            return averageFps;
        }

        public int getDelay() {
            return delay;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public boolean isPlaylist() {
            return isPlaylist;
        }

        public Preview getPreview() {
            return preview;
        }

        public ChannelInfo getChannel() {
            return channel;
        }
    }

    public static class Preview {
        private String small;
        private String medium;
        private String large;
        private String template;

        public Preview() {
        }

        public String getSmall() {
            return small;
        }

        public String getMedium() {
            return medium;
        }

        public String getLarge() {
            return large;
        }

        public String getTemplate() {
            return template;
        }
    }

    public static class ChannelInfo {

        private boolean mature;
        private String status;
        @SerializedName("broadcaster_language")
        private String broadcasterLanguage;
        @SerializedName("display_name")
        private String displayName;
        private String game;
        private String language;
        @SerializedName("_id")
        private long id;
        private String name;
        @SerializedName("created_at")
        private String createdAt;
        @SerializedName("updated_at")
        private String updatedAt;
        private boolean partner;
        private String logo;
        @SerializedName("video_banner")
        private String videoBanner;
        @SerializedName("profile_banner")
        private String profileBanner;
        @SerializedName("profile_banner_background")
        private String profileBannerBackground;
        private String url;
        private int views;
        private int followers;

        public ChannelInfo() {
        }

        public boolean isMature() {
            return mature;
        }

        public String getStatus() {
            return status;
        }

        public String getBroadcasterLanguage() {
            return broadcasterLanguage;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getGame() {
            return game;
        }

        public String getLanguage() {
            return language;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public boolean isPartner() {
            return partner;
        }

        public String getLogo() {
            return logo;
        }

        public String getVideoBanner() {
            return videoBanner;
        }

        public String getProfileBanner() {
            return profileBanner;
        }

        public String getProfileBannerBackground() {
            return profileBannerBackground;
        }

        public String getUrl() {
            return url;
        }

        public int getViews() {
            return views;
        }

        public int getFollowers() {
            return followers;
        }
    }
}
