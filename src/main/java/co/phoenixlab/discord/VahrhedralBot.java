package co.phoenixlab.discord;

import com.google.gson.Gson;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.DiscordClient;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private Configuration config;

    public VahrhedralBot() {
        discord = DiscordClient.get();
    }

    @Override
    public void run() {
        //  TODO
        //  Load Config
        try {
            config = loadConfiguration(Paths.get("config.json"));
        } catch (IOException e) {
            LOGGER.error("Unable to load configuration", e);
            return;
        }
        try {
            discord.login(config.getEmail(), config.getPassword());
        } catch (IOException | ParseException | URISyntaxException e) {
            LOGGER.error("Unable to log in", e);
            return;
        }
        if (!discord.isReady()) {
            LOGGER.error("Discord client is not ready");
            return;
        }
    }

    private Configuration loadConfiguration(Path path) throws IOException {
        Gson configGson = new Gson();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return configGson.fromJson(reader, Configuration.class);
        }
    }



}
