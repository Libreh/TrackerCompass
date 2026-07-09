package me.libreh.trackercompass.api;

import eu.pb4.predicate.api.BuiltinPredicates;
import eu.pb4.predicate.api.MinecraftPredicate;
import org.jspecify.annotations.Nullable;

public record TrackerCondition(
    String id,
    MinecraftPredicate condition,
    @Nullable String fallback,
    @Nullable String actionbarFormat,
    int priority,
    boolean enabled
) {
    public TrackerCondition {
        if (condition == null) condition = BuiltinPredicates.alwaysFalse();
    }
}
