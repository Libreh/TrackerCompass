package me.libreh.trackercompass.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.libreh.trackercompass.TrackerCompass;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final String FILE_NAME = "trackercompass.json";
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve(FILE_NAME);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Config CONFIG;

    public static boolean load() {
        Config oldConfig = CONFIG;
        boolean success;
        try {
            Config config;
            File configFile = CONFIG_PATH.toFile();

            if (configFile.exists()) {
                try (var reader = Files.newBufferedReader(CONFIG_PATH)) {
                    config = GSON.fromJson(reader, Config.class);
                }
            } else {
                config = new Config();
            }
            CONFIG = config;
            save();
            success = true;
        } catch(Exception e) {
            success = false;
            CONFIG = oldConfig;
            TrackerCompass.LOGGER.error("Failed to read config " + FILE_NAME, e);
        }
        return success;
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(CONFIG));
        } catch (Exception e) {
            TrackerCompass.LOGGER.error("Failed to save config " + FILE_NAME, e);
        }
    }

    public static Config config() {
        if (CONFIG == null) {
            CONFIG = new Config();
        }
        return CONFIG;
    }
}