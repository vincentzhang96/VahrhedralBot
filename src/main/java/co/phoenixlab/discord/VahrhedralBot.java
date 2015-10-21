package co.phoenixlab.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.DiscordClient;

public class VahrhedralBot implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger("VahrhedralBot");

    public static void main(String[] args) {
        LOGGER.info("Starting Vahrhedral bot");
        try {
            VahrhedralBot bot = new VahrhedralBot();
            bot.run();
        } catch (Exception e) {
            LOGGER.error("Fatal error while running bot", e);
        }
        LOGGER.info("Vahrhedral bot stopped");
    }

    private DiscordClient discord;

    public VahrhedralBot() {
        discord = DiscordClient.get();
    }

    @Override
    public void run() {
        //  TODO

    }



}
