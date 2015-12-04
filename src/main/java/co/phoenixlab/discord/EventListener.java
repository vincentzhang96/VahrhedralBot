package co.phoenixlab.discord;

import co.phoenixlab.discord.api.entities.Message;
import co.phoenixlab.discord.api.event.MessageReceivedEvent;
import com.google.common.eventbus.Subscribe;

public class EventListener {

    private final VahrhedralBot bot;

    public EventListener(VahrhedralBot bot) {
        this.bot = bot;
    }

    @Subscribe
    public void onMessageRecieved(MessageReceivedEvent messageReceivedEvent) {
        Message message = messageReceivedEvent.getMessage();
        if (message.getContent().startsWith(bot.getConfig().getCommandPrefix())) {
            bot.getMainCommandDispatcher().handleCommand(message);
            return;
        }
    }

}
