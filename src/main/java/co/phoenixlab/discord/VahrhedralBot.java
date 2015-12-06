package co.phoenixlab.discord;

import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.commands.Commands;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

public class VahrhedralBot implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger("VahrhedralBot");

    public static final Path CONFIG_PATH = Paths.get("config/config.json");

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

    private Commands commands;
    private CommandDispatcher commandDispatcher;
    private EventListener eventListener;
    private TaskQueue taskQueue;
    private String versionInfo;

    public VahrhedralBot() {
        taskQueue = new TaskQueue();
        eventListener = new EventListener(this);
    }

    @Override
    public void run() {
        //  Set thread name
        Thread.currentThread().setName("VahrhedralBotMain");
        //  Load Config
        try {
            config = loadConfiguration();
        } catch (IOException e) {
            LOGGER.error("Unable to load configuration", e);
            return;
        }
        versionInfo = loadVersionInfo();
        commandDispatcher = new CommandDispatcher(this, config.getCommandPrefix());
        commands = new Commands(this);
        commands.register(commandDispatcher);
        apiClient = new DiscordApiClient();
        apiClient.getEventBus().register(eventListener);
        try {
            apiClient.logIn(config.getEmail(), config.getPassword());
        } catch (IOException e) {
            LOGGER.error("Unable to log in", e);
        }
        taskQueue.executeWaiting();
    }

    private String loadVersionInfo() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/git.properties"));
            String gitHash = properties.getProperty("git-sha-1");
            HttpResponse<JsonNode> ret =
                    Unirest.get("https://api.github.com/repos/vincentzhang96/VahrhedralBot/commits/" + gitHash).
                    asJson();
            if (ret.getStatus() != 200) {
                throw new IOException("Server returned " + ret.getStatus());
            }
            JSONObject node = ret.getBody().getObject();
            JSONObject commitObj = node.getJSONObject("commit");
            Instant time = Instant.parse(commitObj.getJSONObject("committer").getString("date"));
            return String.format("Commit %s\nURL: %s\nMessage: %s\nDate: %s",
                    node.getString("sha"),
                    node.getString("html_url"), commitObj.getString("message"),
                    DateTimeFormatter.ofPattern("MM/dd HH:mm z").withZone(ZoneId.systemDefault()).format(time));
        } catch (IOException | UnirestException e) {
            LOGGER.warn("Unable to load git commit version info", e);
        }
        return "N/A";
    }

    public String getVersionInfo() {
        return versionInfo;
    }

    private Configuration loadConfiguration() throws IOException {
        Gson configGson = new Gson();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, UTF_8)) {
            return configGson.fromJson(reader, Configuration.class);
        }
    }

    public boolean saveConfig() {
        Gson configGson = new Gson();
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, UTF_8, CREATE, WRITE, TRUNCATE_EXISTING)) {
            configGson.toJson(config, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            LOGGER.warn("Unable to save config", e);
            return false;
        }
    }

    public CommandDispatcher getMainCommandDispatcher() {
        return commandDispatcher;
    }

    public Configuration getConfig() {
        return config;
    }

    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    public EventListener getEventListener() {
        return eventListener;
    }

    public DiscordApiClient getApiClient() {
        return apiClient;
    }

    public void shutdown() {
        shutdown(0);
    }

    public void shutdown(int code) {
        try {
            Unirest.shutdown();
        } catch (IOException e) {
            LOGGER.warn("Was unable to cleanly shut down Unirest", e);
        }
        System.exit(code);
    }


}
