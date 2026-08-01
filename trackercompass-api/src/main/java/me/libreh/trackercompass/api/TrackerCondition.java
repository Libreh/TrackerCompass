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
    // Reserved id: a condition with this id tracks the observer's pinned target (from the
    // resolver's pinnedTargetLookup) instead of testing its predicate against candidates.
    public static final String SELECTED_TARGET_ID = "selected_target";

    public TrackerCondition {
        if (condition == null) condition = BuiltinPredicates.alwaysFalse();
    }
}
