package me.libreh.trackercompass;

import com.mojang.serialization.MapCodec;
import eu.pb4.predicate.api.BuiltinPredicates;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.predicate.api.PredicateResult;
import me.libreh.trackercompass.api.TrackerConditionRegistry;
import net.minecraft.resources.Identifier;

public final class TrackerPredicates {
    private static final Identifier NEAREST_ID = Identifier.tryParse("trackercompass:nearest");

    public static void register() {
        TrackerConditionRegistry.register(NEAREST_ID,
            MinecraftPredicate.simpleCodec(NEAREST_ID, ctx ->
                PredicateResult.ofBoolean(true)));
    }

    private TrackerPredicates() {}
}
