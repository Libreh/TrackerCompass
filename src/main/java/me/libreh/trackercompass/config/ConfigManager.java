package me.libreh.trackercompass.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import me.libreh.trackercompass.TrackerCompass;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    public static final String FILE_NAME = "trackercompass.json";
    public static final Path FILE_PATH = CONFIG_DIR.resolve(FILE_NAME);

    private static TrackerCompassConfig CONFIG;

    public static boolean load() {
        TrackerCompassConfig oldConfig = CONFIG;
        boolean success;

        try (FileReader reader = new FileReader(FILE_PATH.toFile())) {
            TrackerCompassConfig config = GSON.fromJson(reader, TrackerCompassConfig.class);
            if (config == null) {
                TrackerCompass.LOGGER.error("Failed to load " + FILE_NAME + ": Config parsed as null");
                CONFIG = oldConfig;
                return false;
            }
            CONFIG = config;
            save();
            success = true;
        } catch (FileNotFoundException e) {
            TrackerCompass.LOGGER.info("Creating default config " + FILE_NAME);
            CONFIG = new TrackerCompassConfig();
            save();
            success = true;
        } catch (IOException | JsonSyntaxException e) {
            TrackerCompass.LOGGER.error("Failed to read config " + FILE_NAME, e);
            CONFIG = oldConfig;
            success = false;
        }

        return success;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            String json = GSON.toJson(CONFIG);
            Files.writeString(FILE_PATH, json);
        } catch (IOException e) {
            TrackerCompass.LOGGER.error("Failed to save config " + FILE_NAME, e);
        }
    }

    public static TrackerCompassConfig getConfig() {
        return CONFIG;
    }
}
