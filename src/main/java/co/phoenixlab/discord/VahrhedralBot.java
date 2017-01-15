package co.phoenixlab.discord;

import co.phoenixlab.common.localization.LocaleStringProvider;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.common.localization.LocalizerImpl;
import co.phoenixlab.discord.api.DiscordApiClient;
import co.phoenixlab.discord.cfg.ClassDiscussionConfig;
import co.phoenixlab.discord.chatlogger.ChatLogger;
import co.phoenixlab.discord.classdiscussion.ClassRoleManager;
import co.phoenixlab.discord.commands.Commands;
import co.phoenixlab.discord.commands.tempstorage.DnTrackStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONException;
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
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

public class VahrhedralBot implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger("VahrhedralBot");

    public static final Path CONFIG_PATH = Paths.get("config/config.json");
    public static final Path DNTRACK_PATH = Paths.get("config/dntrack.json");
    public static final Path CLASSDISCUSS_PATH = Paths.get("config/classdiscussion.json");

    public static final String USER_AGENT = "DiscordBot (https://github.com/vincentzhang96/VahrhedralBot, 12)";


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
    private Localizer localizer;

    private ChatLogger chatLogger;
    private DnTrackStorage dnTrackStorage;
    private ClassRoleManager classRoleManager;

    public VahrhedralBot() {
        taskQueue = new TaskQueue();
        eventListener = new EventListener(this);
        classRoleManager = null;
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
        try {
            dnTrackStorage = loadDnTrackInfo();
        } catch (IOException e) {
            LOGGER.warn("Unable to load dntrack info, defaulting", e);
            dnTrackStorage = new DnTrackStorage();
            if(!saveDnTrackInfo()) {
                LOGGER.warn("Failed to save empty dntrack");
            }
        }

        try {
            ClassDiscussionConfig classDiscussionConfig = loadClassDiscussionConfig();
            classRoleManager = new ClassRoleManager(classDiscussionConfig);
        } catch (IOException e) {
            LOGGER.warn("Unable to load class discussion config, ignoring", e);
        }

        loadLocalization();
        versionInfo = loadVersionInfo();
        commandDispatcher = new CommandDispatcher(this, config.getCommandPrefix());
        //  Required User-Agent
        Unirest.setDefaultHeader("User-Agent", USER_AGENT);

        apiClient = new DiscordApiClient(config.getApiClientConfig());
        apiClient.getEventBus().register(eventListener);
        if (classRoleManager != null) {
            apiClient.getEventBus().register(classRoleManager);
        }

        chatLogger = new ChatLogger(apiClient);

        commands = new Commands(this);
        commands.register(commandDispatcher);
        try {
            String token = config.getToken();
            if (token == null || token.isEmpty()) {
                apiClient.logIn(config.getEmail(), config.getPassword());
            } else {
                apiClient.logIn(token);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to log in", e);
        }
        taskQueue.executeWaiting();
    }

    private void loadLocalization() {
        localizer = new LocalizerImpl(Locale.getDefault());
        localizer.registerPluralityRules(LocalizerImpl.defaultPluralityRules());
        LocaleStringProvider provider = new LocaleStringProvider() {
            ResourceBundle bundle;
            @Override
            public void setActiveLocale(Locale locale) {
                bundle = ResourceBundle.getBundle("co.phoenixlab.discord.resources.locale", locale);
            }

            @Override
            public String get(String key) {
                if (!contains(key)) {
                    return key;
                }
                return bundle.getString(key);
            }

            @Override
            public boolean contains(String key) {
                return bundle.containsKey(key);
            }
        };
        localizer.addLocaleStringProvider(provider);
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
        } catch (IOException | UnirestException | JSONException e) {
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
        Gson configGson = new GsonBuilder().setPrettyPrinting().create();
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, UTF_8, CREATE, WRITE, TRUNCATE_EXISTING)) {
            configGson.toJson(config, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            LOGGER.warn("Unable to save config", e);
            return false;
        }
    }

    private DnTrackStorage loadDnTrackInfo() throws IOException {
        Gson configGson = new Gson();
        try (Reader reader = Files.newBufferedReader(DNTRACK_PATH, UTF_8)) {
            return configGson.fromJson(reader, DnTrackStorage.class);
        }
    }

    public boolean saveDnTrackInfo() {
        Gson configGson = new GsonBuilder().setPrettyPrinting().create();
        try (BufferedWriter writer = Files.newBufferedWriter(DNTRACK_PATH, UTF_8, CREATE, WRITE, TRUNCATE_EXISTING)) {
            configGson.toJson(dnTrackStorage, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            LOGGER.warn("Unable to save dntrack", e);
            return false;
        }
    }

    private ClassDiscussionConfig loadClassDiscussionConfig() throws IOException {
        Gson configGson = new Gson();
        try (Reader reader = Files.newBufferedReader(CLASSDISCUSS_PATH, UTF_8)) {
            return configGson.fromJson(reader, ClassDiscussionConfig.class);
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

    public Localizer getLocalizer() {
        return localizer;
    }

    public Commands getCommands() {
        return commands;
    }

    public ChatLogger getChatLogger() {
        return chatLogger;
    }

    public DnTrackStorage getDnTrackStorage() {
        return dnTrackStorage;
    }

    public void shutdown() {
        shutdown(0);
    }

    public void shutdown(int code) {
        if (apiClient != null) {
            apiClient.stop();
        }
        try {
            Unirest.shutdown();
        } catch (IOException e) {
            LOGGER.warn("Was unable to cleanly shut down Unirest", e);
        }
        System.exit(code);
    }


}
