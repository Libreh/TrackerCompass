package me.libreh.trackercompass.api;

import eu.pb4.placeholders.api.Placeholder;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class TargetPlaceholders {
    private static final ThreadLocal<ServerPlayer> CURRENT_TARGET = new ThreadLocal<>();

    public static void register() {
        for (var entry : Placeholders.getCommonPlaceholders().entrySet()) {
            if (entry.getKey().getNamespace().equals("player")) {
                mirrorCommon(entry.getKey(), entry.getValue());
            }
        }
        for (var entry : Placeholders.getServerPlaceholders().entrySet()) {
            if (entry.getKey().getNamespace().equals("player")
                    && !Placeholders.getCommonPlaceholders().containsKey(entry.getKey())) {
                mirrorServer(entry.getKey(), entry.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void mirrorCommon(Identifier originalId, Placeholder<PlaceholderContext, ?> original) {
        String subPath = originalId.getPath();
        Identifier targetId = Identifier.fromNamespaceAndPath("target", subPath);
        Placeholder<PlaceholderContext, String> placeholder = (Placeholder<PlaceholderContext, String>) original;

        Placeholders.registerCommon(targetId, placeholder.argumentParser(), (ctx, arg) -> {
            ServerPlayer target = CURRENT_TARGET.get();
            if (target == null) return PlaceholderResult.invalid("No target player!");

            ServerPlaceholderContext targetCtx = ServerPlaceholderContext.of(target);
            return placeholder.onPlaceholderRequest(targetCtx, arg);
        });
    }

    @SuppressWarnings("unchecked")
    private static void mirrorServer(Identifier originalId, Placeholder<ServerPlaceholderContext, ?> original) {
        String subPath = originalId.getPath();
        Identifier targetId = Identifier.fromNamespaceAndPath("target", subPath);
        Placeholder<ServerPlaceholderContext, String> placeholder = (Placeholder<ServerPlaceholderContext, String>) original;

        Placeholders.registerServer(targetId, placeholder.argumentParser(), (ctx, arg) -> {
            ServerPlayer target = CURRENT_TARGET.get();
            if (target == null) return PlaceholderResult.invalid("No target player!");

            ServerPlaceholderContext targetCtx = ServerPlaceholderContext.of(target);
            return placeholder.onPlaceholderRequest(targetCtx, arg);
        });
    }

    public static void withTarget(ServerPlayer target, Runnable action) {
        CURRENT_TARGET.set(target);
        try { action.run(); } finally { CURRENT_TARGET.remove(); }
    }

    public static <T> T withTarget(ServerPlayer target, Supplier<T> action) {
        CURRENT_TARGET.set(target);
        try { return action.get(); } finally { CURRENT_TARGET.remove(); }
    }

    public static @Nullable ServerPlayer getCurrentTarget() {
        return CURRENT_TARGET.get();
    }

    private TargetPlaceholders() {}
}
