package me.libreh.trackercompass.api;

import com.mojang.serialization.MapCodec;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateRegistry;
import net.minecraft.resources.Identifier;

public final class TrackerConditionRegistry {
    private TrackerConditionRegistry() {}

    public static void register(Identifier id, MapCodec<MinecraftPredicate> codec) {
        PredicateRegistry.register(id, codec);
    }
}
