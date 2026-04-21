package me.libreh.trackercompass.util;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GenericModInfo {
    private static final int ERROR_COLOR = 0xFF0000;
    private static final int VERSION_LABEL_COLOR = 0xF7E1A7;
    private static final int GITHUB_LINK_COLOR = 0x58A6FF;

    private static ModContainer container;
    private static String id;
    private static Logger logger;
    private static boolean showModrinth;
    private static boolean showGitHub;
    private static int titleColor;

    private static Component[] icon;
    private static Component[] about;
    private static Component[] consoleAbout;

    public static void build(ModContainer container, String id, Logger logger, boolean showModrinth, boolean showGitHub, int titleColor) {
        GenericModInfo.container = container;
        GenericModInfo.id = id;
        GenericModInfo.logger = logger;
        GenericModInfo.showModrinth = showModrinth;
        GenericModInfo.showGitHub = showGitHub;
        GenericModInfo.titleColor = titleColor;
    }

    private static void ensureBuilt() {
        if (about != null) return;
        buildIcon();
        buildAbout();
    }

    private static void buildIcon() {
        var iconLines = new ArrayList<MutableComponent>();
        var filePath = container.findPath("assets/" + id + "/icon.png");

        try {
            if (filePath.isEmpty()) throw new FileNotFoundException("Icon not found");

            try (InputStream stream = Files.newInputStream(filePath.get())) {
                var source = ImageIO.read(stream);
                for (int y = 0; y < source.getHeight(); y++) {
                    var line = Component.literal("");
                    int runLength = 0, currentColor = source.getRGB(0, y) & 0xFFFFFF;

                    for (int x = 0; x < source.getWidth(); x++) {
                        int pixelColor = source.getRGB(x, y) & 0xFFFFFF;
                        if (currentColor == pixelColor) {
                            runLength++;
                        } else {
                            line.append(Component.literal("█".repeat(runLength))
                                    .setStyle(Style.EMPTY.withColor(currentColor).withShadowColor(currentColor | 0xFF000000)));
                            currentColor = pixelColor;
                            runLength = 1;
                        }
                    }

                    line.append(Component.literal("█".repeat(runLength))
                            .setStyle(Style.EMPTY.withColor(currentColor).withShadowColor(currentColor | 0xFF000000)));
                    iconLines.add(line);
                }
            }
        } catch (Throwable e) {
            logger.warn("Error building icon", e);
            while (iconLines.size() < 16) {
                iconLines.add(Component.literal("/!\\ [ Invalid icon file ] /!\\")
                        .setStyle(Style.EMPTY.withColor(ERROR_COLOR).withItalic(true)));
            }
        }

        icon = iconLines.toArray(new Component[0]);
    }

    private static void buildAbout() {
        var fullAbout = new ArrayList<Component>();
        var basicAbout = new ArrayList<Component>();

        try {
            var metadata = container.getMetadata();
            var sources = metadata.getContact().get("sources").orElse(metadata.getContact().get("homepage").orElse(""));
            var hasSourcesUrl = metadata.getContact().get("sources").isPresent();
            var versionString = metadata.getVersion().getFriendlyString();

            var title = Component.literal(metadata.getName())
                    .setStyle(Style.EMPTY.withColor(titleColor).withBold(true)
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(sources))));

            var versionUrl = hasSourcesUrl ? sources + "/releases/tag/" + versionString : sources;
            var version = Component.literal("Version: ").setStyle(Style.EMPTY.withColor(VERSION_LABEL_COLOR))
                    .append(Component.literal(versionString)
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(versionUrl)))));

            var links = Component.literal("");
            if (showModrinth) {
                var modrinthUrl = "https://modrinth.com/mod/" + id + "/version/" + versionString;
                var modrinth = Component.literal("Modrinth")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(modrinthUrl))));
                links.append(modrinth);
            }

            if (showGitHub) {
                var github = Component.literal("GitHub")
                        .setStyle(Style.EMPTY.withColor(GITHUB_LINK_COLOR)
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(sources))));
                if (showModrinth) {
                    links.append(Component.literal(" • ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
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
            basicAbout.add(Component.empty());
            basicAbout.add(Component.literal(metadata.getDescription()));

            var contributors = new ArrayList<String>();
            metadata.getAuthors().stream().map(Person::getName).forEach(contributors::add);
            metadata.getContributors().stream().map(Person::getName).forEach(contributors::add);

            var contributorsUrl = hasSourcesUrl ? sources + "/contributors" : sources;
            fullAbout.add(Component.literal("Contributors")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(String.join(", ", contributors))))
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(contributorsUrl)))));
            fullAbout.add(Component.empty());

            var words = new ArrayList<>(List.of(metadata.getDescription().split(" ")));
            var line = new StringBuilder();
            while (!words.isEmpty()) {
                (line.isEmpty() ? line : line.append(" ")).append(words.removeFirst());
                if (line.length() > 16) {
                    fullAbout.add(Component.literal(line.toString()).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
                    line = new StringBuilder();
                }
            }
            if (!line.isEmpty()) {
                fullAbout.add(Component.literal(line.toString()).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
            }

            var output = new ArrayList<Component>();
            if (icon.length > fullAbout.size() + 2) {
                int componentIndex = 0, startLine = (icon.length - fullAbout.size() - 1) / 2;
                for (int i = 0; i < icon.length; i++) {
                    if (i >= startLine && componentIndex < fullAbout.size()) {
                        output.add(icon[i].copy()
                                .append(Component.literal("  ").setStyle(Style.EMPTY.withItalic(false)))
                                .append(fullAbout.get(componentIndex++)));
                    } else {
                        output.add(icon[i]);
                    }
                }
            } else {
                output.addAll(List.of(icon));
                output.addAll(fullAbout);
            }

            about = output.toArray(new Component[0]);
            consoleAbout = basicAbout.toArray(new Component[0]);

        } catch (Exception e) {
            logger.warn("Error building about Component", e);
            var invalid = Component.literal("/!\\ [ Invalid about mod info ] /!\\")
                    .setStyle(Style.EMPTY.withColor(ERROR_COLOR).withItalic(true));
            about = new Component[]{invalid};
            consoleAbout = new Component[]{invalid};
        }
    }

    public static Component[] getAboutFull() {
        ensureBuilt();
        return about;
    }

    public static Component[] getAboutConsole() {
        ensureBuilt();
        return consoleAbout;
    }
}
