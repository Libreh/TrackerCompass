package me.libreh.trackercompass.api;

import com.mojang.serialization.MapCodec;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateRegistry;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TrackerConditionRegistry {
    private static final Map<Identifier, MapCodec<MinecraftPredicate>> CODECS = new LinkedHashMap<>();

    private TrackerConditionRegistry() {}

    public static void register(Identifier id, MapCodec<MinecraftPredicate> codec) {
        CODECS.put(id, codec);
        PredicateRegistry.register(id, codec);
    }

    @Nullable
    public static MapCodec<MinecraftPredicate> getCodec(Identifier id) {
        return CODECS.get(id);
    }

    public static Collection<Map.Entry<Identifier, MapCodec<MinecraftPredicate>>> entries() {
        return Collections.unmodifiableCollection(CODECS.entrySet());
    }
}
