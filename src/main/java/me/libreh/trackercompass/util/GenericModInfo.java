package me.libreh.trackercompass.util;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building and displaying mod information including icon and about text.
 * <p>
 * Originally from <a href="https://github.com/Patbox">Patbox's</a> mods, adapted for more customizability.
 * <p>
 * Requirements in fabric.mod.json:
 * <ul>
 *   <li>contact.homepage - Homepage URL for the mod</li>
 *   <li>contact.sources - Source code repository URL</li>
 *   <li>icon - Path to icon.png file (e.g., "assets/modid/icon.png")</li>
 * </ul>
 */
public class GenericModInfo {
    private static final int ERROR_COLOR = 0xFF0000;
    private static final int VERSION_LABEL_COLOR = 0xF7E1A7;
    private static final int GITHUB_LINK_COLOR = 0x58A6FF;

    private static Text[] icon = new Text[0];
    private static Text[] about = new Text[0];
    private static Text[] consoleAbout = new Text[0];

    /**
     * Builds the mod icon and about information with custom title color.
     *
     * @param container the mod container
     * @param id the mod id
     * @param logger the logger for error reporting
     * @param showModrinth whether to show Modrinth link
     * @param showGitHub whether to show GitHub link
     * @param titleColor RGB color value (e.g., 0xFFFFFF for white, or Formatting.AQUA.getColorValue())
     */
    public static void build(ModContainer container, String id, Logger logger, boolean showModrinth, boolean showGitHub, int titleColor) {
        buildIcon(container, id, logger);
        buildAbout(container, id, logger, showModrinth, showGitHub, titleColor);
    }

    private static void buildIcon(ModContainer container, String id, Logger logger) {
        var iconLines = new ArrayList<MutableText>();
        try {
            var filePath = container.findPath("assets/" + id + "/icon.png");
            if (filePath.isEmpty()) throw new FileNotFoundException("Icon not found");

            var source = ImageIO.read(Files.newInputStream(filePath.get()));
            for (int y = 0; y < source.getHeight(); y++) {
                var line = Text.literal("");
                int runLength = 0, currentColor = source.getRGB(0, y) & 0xFFFFFF;

                for (int x = 0; x < source.getWidth(); x++) {
                    int pixelColor = source.getRGB(x, y) & 0xFFFFFF;
                    if (currentColor == pixelColor) {
                        runLength++;
                    } else {
                        line.append(Text.literal("█".repeat(runLength))
                                .setStyle(Style.EMPTY.withColor(currentColor).withShadowColor(currentColor | 0xFF000000)));
                        currentColor = pixelColor;
                        runLength = 1;
                    }
                }

                line.append(Text.literal("█".repeat(runLength))
                        .setStyle(Style.EMPTY.withColor(currentColor).withShadowColor(currentColor | 0xFF000000)));
                iconLines.add(line);
            }
        } catch (Throwable e) {
            logger.warn("Error building icon", e);
            while (iconLines.size() < 16) {
                iconLines.add(Text.literal("/!\\ [ Invalid icon file ] /!\\")
                        .setStyle(Style.EMPTY.withColor(ERROR_COLOR).withItalic(true)));
            }
        }

        icon = iconLines.toArray(new Text[0]);
    }

    private static void buildAbout(ModContainer container, String id, Logger logger, boolean showModrinth, boolean showGitHub, int titleColor) {
        var fullAbout = new ArrayList<Text>();
        var basicAbout = new ArrayList<Text>();

        try {
            var metadata = container.getMetadata();
            var sources = metadata.getContact().get("sources").orElse(metadata.getContact().get("homepage").orElse(""));
            var hasSourcesUrl = metadata.getContact().get("sources").isPresent();
            var versionString = metadata.getVersion().getFriendlyString();

            var title = Text.literal(metadata.getName())
                    .setStyle(Style.EMPTY.withColor(titleColor).withBold(true)
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(sources))));

            var versionUrl = hasSourcesUrl ? sources + "/releases/tag/" + versionString : sources;
            var version = Text.literal("Version: ").setStyle(Style.EMPTY.withColor(VERSION_LABEL_COLOR))
                    .append(Text.literal(versionString)
                            .setStyle(Style.EMPTY.withColor(Formatting.WHITE)
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(versionUrl)))));

            var links = Text.literal("");
            if (showModrinth) {
                var modrinthUrl = "https://modrinth.com/mod/" + id + "/version/" + versionString;
                var modrinth = Text.literal("Modrinth")
                        .setStyle(Style.EMPTY.withColor(Formatting.GREEN)
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(modrinthUrl))));
                links.append(modrinth);
            }

            if (showGitHub) {
                var github = Text.literal("GitHub")
                        .setStyle(Style.EMPTY.withColor(GITHUB_LINK_COLOR)
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(sources))));
                if (showModrinth) {
                    links.append(Text.literal(" • ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                }
                links.append(github);
            }

            fullAbout.add(title);
            fullAbout.add(version);
            if (showModrinth || showGitHub) {
                fullAbout.add(links);
            }

            basicAbout.add(title);
            basicAbout.add(version);
            basicAbout.add(Text.empty());
            basicAbout.add(Text.of(metadata.getDescription()));

            var contributors = new ArrayList<String>();
            metadata.getAuthors().stream().map(Person::getName).forEach(contributors::add);
            metadata.getContributors().stream().map(Person::getName).forEach(contributors::add);

            var contributorsUrl = hasSourcesUrl ? sources + "/contributors" : sources;
            fullAbout.add(Text.literal("Contributors")
                    .setStyle(Style.EMPTY.withColor(Formatting.AQUA)
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal(String.join(", ", contributors))))
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(contributorsUrl)))));
            fullAbout.add(Text.empty());

            var words = new ArrayList<>(List.of(metadata.getDescription().split(" ")));
            var line = new StringBuilder();
            while (!words.isEmpty()) {
                (line.isEmpty() ? line : line.append(" ")).append(words.removeFirst());
                if (line.length() > 16) {
                    fullAbout.add(Text.literal(line.toString()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                    line = new StringBuilder();
                }
            }
            if (!line.isEmpty()) {
                fullAbout.add(Text.literal(line.toString()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
            }

            var output = new ArrayList<Text>();
            if (icon.length > fullAbout.size() + 2) {
                int textIndex = 0, startLine = (icon.length - fullAbout.size() - 1) / 2;
                for (int i = 0; i < icon.length; i++) {
                    if (i >= startLine && textIndex < fullAbout.size()) {
                        output.add(icon[i].copy()
                                .append(Text.literal("  ").setStyle(Style.EMPTY.withItalic(false)))
                                .append(fullAbout.get(textIndex++)));
                    } else {
                        output.add(icon[i]);
                    }
                }
            } else {
                output.addAll(List.of(icon));
                output.addAll(fullAbout);
            }

            about = output.toArray(new Text[0]);
            consoleAbout = basicAbout.toArray(new Text[0]);

        } catch (Exception e) {
            logger.warn("Error building about text", e);
            var invalid = Text.literal("/!\\ [ Invalid about mod info ] /!\\")
                    .setStyle(Style.EMPTY.withColor(ERROR_COLOR).withItalic(true));
            about = new Text[]{invalid};
            consoleAbout = new Text[]{invalid};
        }
    }

    public static Text[] getAboutFull() {
        return about;
    }

    public static Text[] getAboutConsole() {
        return consoleAbout;
    }
}