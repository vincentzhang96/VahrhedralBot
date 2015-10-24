package co.phoenixlab.discord;

import co.phoenixlab.discord.api.DiscordApiClient;
import com.google.gson.Gson;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VahrhedralBot implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger("VahrhedralBot");

    private DiscordApiClient apiClient;

    public static void main(String[] args) {
        LOGGER.info("Starting Vahrhedral bot");
        VahrhedralBot bot = new VahrhedralBot();
        try {
            bot.run();
        } catch (Exception e) {
            LOGGER.error("Fatal error while running bot", e);
        }
        bot.shutdown();
    }

    private Configuration config;
    private CommandDispatcher commandDispatcher;
    private TaskQueue taskQueue;

    public VahrhedralBot() {
        taskQueue = new TaskQueue();
    }

    @Override
    public void run() {
        //  Load Config
        try {
            config = loadConfiguration(Paths.get("config/config.json"));
        } catch (IOException e) {
            LOGGER.error("Unable to load configuration", e);
            return;
        }
        apiClient = new DiscordApiClient();
        try {
            apiClient.logIn(config.getEmail(), config.getPassword());
        } catch (IOException e) {
            LOGGER.error("Unable to log in", e);
        }
        //  TODO

    }

    private Configuration loadConfiguration(Path path) throws IOException {
        Gson configGson = new Gson();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return configGson.fromJson(reader, Configuration.class);
        }
    }

    public CommandDispatcher getCommandDispatcher() {
        return commandDispatcher;
    }

    public Configuration getConfig() {
        return config;
    }

    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    public void shutdown() {
        try {
            Unirest.shutdown();
        } catch (IOException e) {
            LOGGER.warn("Was unable to cleanly shut down Unirest", e);
        }
        System.exit(0);
    }


}
