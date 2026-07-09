package me.libreh.trackercompass.api;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public record TrackerText(String input, TextNode textNode) {
    public static final ParserContext.Key<Function<String, Component>> DYNAMIC_NODES = ParserContext.Key.of("trackercompass:dynamic");

    public static final NodeParser PARSER = NodeParser.builder()
            .simplifiedTextFormat()
            .quickText()
            .placeholders(TagLikeParser.PLACEHOLDER_USER, DYNAMIC_NODES)
            .serverPlaceholders(TagLikeParser.PLACEHOLDER, ServerPlaceholderContext.SERVER_KEY)
            .staticPreParsing()
            .build();

    public static final TrackerText EMPTY = new TrackerText("", TextNode.empty());

    public static TrackerText of(String input) {
        if (input == null || input.isEmpty()) {
            return EMPTY;
        }
        return new TrackerText(input, PARSER.parseNode(input));
    }

    public Component resolve(Map<String, Component> placeholders) {
        return resolve(placeholders::get, null);
    }

    public Component resolve(Function<String, Component> placeholders) {
        return resolve(placeholders, null);
    }

    public Component resolve(Map<String, Component> placeholders, @Nullable ServerPlaceholderContext serverCtx) {
        return resolve(placeholders::get, serverCtx);
    }

    public Component resolve(Function<String, Component> placeholders, @Nullable ServerPlaceholderContext serverCtx) {
        ParserContext ctx = ParserContext.of(DYNAMIC_NODES, placeholders);
        if (serverCtx != null) {
            ctx = ctx.with(ServerPlaceholderContext.SERVER_KEY, serverCtx);
        }
        return textNode.toComponent(ctx);
    }

    public boolean isEmpty() {
        return input.isEmpty();
    }
}
