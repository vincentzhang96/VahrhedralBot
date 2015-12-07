package co.phoenixlab.discord.api.event;

import co.phoenixlab.discord.api.entities.Channel;

public class ChannelChangeEvent {

    private final Channel channel;
    private final ChannelChange channelChange;

    public ChannelChangeEvent(Channel channel, ChannelChange channelChange) {
        this.channel = channel;
        this.channelChange = channelChange;
    }

    public Channel getChannel() {
        return channel;
    }

    public ChannelChange getChannelChange() {
        return channelChange;
    }

    public enum ChannelChange {
        ADDED,
        DELETED,
        UPDATED
    }
}
