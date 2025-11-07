package me.libreh.trackercompass.util;

import me.libreh.trackercompass.TrackerCompass;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import javax.imageio.ImageIO;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GenericModInfo {
    private static Text[] icon = new Text[0];
    private static Text[] about = new Text[0];
    private static Text[] consoleAbout = new Text[0];

    public static void build(ModContainer container) {
        {
            final String chr = "█";
            var icon = new ArrayList<MutableText>();
            try {
                Optional<Path> filePath = container.findPath("assets/trackercompass/icon.png");
                if (filePath.isEmpty()) {
                    throw new FileNotFoundException("Icon file was not found!");
                }

                var source = ImageIO.read(Files.newInputStream(filePath.get()));

                for (int y = 0; y < source.getHeight(); y++) {
                    var base = Text.literal("");
                    int line = 0;
                    int color = source.getRGB(0, y) & 0xFFFFFF;
                    for (int x = 0; x < source.getWidth(); x++) {
                        int colorPixel = source.getRGB(x, y) & 0xFFFFFF;

                        if (color == colorPixel) {
                            line++;
                        } else {
                            base.append(Text.literal(chr.repeat(line)).setStyle(Style.EMPTY.withColor(color).withShadowColor(color | 0xFF000000)));
                            color = colorPixel;
                            line = 1;
                        }
                    }

                    base.append(Text.literal(chr.repeat(line)).setStyle(Style.EMPTY.withColor(color).withShadowColor(color | 0xFF000000)));
                    icon.add(base);
                }
            } catch (Throwable e) {
                TrackerCompass.LOGGER.info("Error building GenericModInfo icon", e);
                while (icon.size() < 16) {
                    icon.add(Text.literal("/!\\ [ Invalid icon file ] /!\\").setStyle(Style.EMPTY.withColor(0xFF0000).withItalic(true)));
                }
            }

            GenericModInfo.icon = icon.toArray(new Text[0]);
        }

        {
            var about = new ArrayList<Text>();
            var aboutBasic = new ArrayList<Text>();
            var output = new ArrayList<Text>();

            try {
                about.add(Text.literal(container.getMetadata().getName()).setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(container.getMetadata().getContact().get("github").orElse("https://pb4.eu"))))));
                about.add(Text.literal("Version: ").setStyle(Style.EMPTY.withColor(0xf7e1a7))
                        .append(Text.literal(container.getMetadata().getVersion().getFriendlyString()).setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                aboutBasic.addAll(about);
                aboutBasic.add(Text.empty());
                aboutBasic.add(Text.of(container.getMetadata().getDescription()));

                var contributors = new ArrayList<String>();
                contributors.addAll(container.getMetadata().getAuthors().stream().map(Person::getName).toList());
                contributors.addAll(container.getMetadata().getContributors().stream().map(Person::getName).toList());

                about.add(Text.literal("")
                        .append(Text.literal("Contributors")
                                .setStyle(Style.EMPTY.withColor(Formatting.AQUA)
                                        .withHoverEvent(new HoverEvent.ShowText(
                                                Text.literal(String.join(", ", contributors)
                                        )))
                                        .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/Libreh")))))
                        .append("")
                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
                about.add(Text.empty());

                var desc = new ArrayList<>(List.of(container.getMetadata().getDescription().split(" ")));

                if (!desc.isEmpty()) {
                    StringBuilder descPart = new StringBuilder();
                    while (!desc.isEmpty()) {
                        (descPart.isEmpty() ? descPart : descPart.append(" ")).append(desc.removeFirst());

                        if (descPart.length() > 16) {
                            about.add(Text.literal(descPart.toString()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                            descPart = new StringBuilder();
                        }
                    }

                    if (!descPart.isEmpty()) {
                        about.add(Text.literal(descPart.toString()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                    }
                }

                if (icon.length > about.size() + 2) {
                    int a = 0;
                    for (int i = 0; i < icon.length; i++) {
                        if (i == (icon.length - about.size() - 1) / 2 + a && a < about.size()) {
                            output.add(icon[i].copy().append(Text.literal("  ").setStyle(Style.EMPTY.withItalic(false)).append(about.get(a++))));
                        } else {
                            output.add(icon[i]);
                        }
                    }
                } else {
                    Collections.addAll(output, icon);
                    output.addAll(about);
                }
            } catch (Exception e) {
                TrackerCompass.LOGGER.info("Error building GenericModInfo text", e);
                var invalid = Text.literal("/!\\ [ Invalid about mod info ] /!\\").setStyle(Style.EMPTY.withColor(0xFF0000).withItalic(true));

                output.add(invalid);
                about.add(invalid);
            }

            GenericModInfo.about = output.toArray(new Text[0]);
            GenericModInfo.consoleAbout = aboutBasic.toArray(new Text[0]);
        }
    }

    public static Text[] getAboutFull() {
        return about;
    }

    public static Text[] getAboutConsole() {
        return consoleAbout;
    }
}
