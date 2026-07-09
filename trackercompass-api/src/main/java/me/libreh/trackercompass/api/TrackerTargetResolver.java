package me.libreh.trackercompass.api;

import eu.pb4.predicate.api.PredicateContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TrackerTargetResolver {
    private TrackerTargetResolver() {}

    public record ResolveResult(
        ServerPlayer target,
        @Nullable TrackerCondition activeCondition
    ) {}

    public static Optional<ResolveResult> resolve(
        ServerPlayer observer,
        List<TrackerCondition> conditions,
        Collection<ServerPlayer> candidatePool,
        Map<UUID, java.util.function.Supplier<BlockPos>> positionLookup,
        java.util.function.Function<UUID, UUID> targetLookup
    ) {
        List<TrackerCondition> sorted = conditions.stream()
            .filter(TrackerCondition::enabled)
            .sorted(Comparator.comparingInt(TrackerCondition::priority).reversed())
            .toList();

        return resolveRecursive(observer, sorted, candidatePool, positionLookup, targetLookup, 0, null);
    }

    private static Optional<ResolveResult> resolveRecursive(
        ServerPlayer observer,
        List<TrackerCondition> conditions,
        Collection<ServerPlayer> candidatePool,
        Map<UUID, java.util.function.Supplier<BlockPos>> positionLookup,
        java.util.function.Function<UUID, UUID> targetLookup,
        int index,
        @Nullable String fallbackId
    ) {
        if (fallbackId != null) {
            for (int i = 0; i < conditions.size(); i++) {
                if (conditions.get(i).id().equals(fallbackId)) {
                    index = i;
                    break;
                }
            }
        }

        for (int i = index; i < conditions.size(); i++) {
            TrackerCondition condition = conditions.get(i);
            List<ServerPlayer> matching = new ArrayList<>();

            if (condition.id().equals("selected_target")) {
                UUID targetUuid = targetLookup.apply(observer.getUUID());
                if (targetUuid != null) {
                    for (ServerPlayer candidate : candidatePool) {
                        if (candidate.getUUID().equals(targetUuid)) {
                            matching.add(candidate);
                            break;
                        }
                    }
                }
            } else {
                for (ServerPlayer candidate : candidatePool) {
                    if (candidate.getUUID().equals(observer.getUUID())) continue;
                    if (condition.condition().test(PredicateContext.of(candidate)).success()) {
                        matching.add(candidate);
                    }
                }
            }

            if (!matching.isEmpty()) {
                ServerPlayer target = findNearest(observer, matching);
                if (target != null) {
                    return Optional.of(new ResolveResult(target, condition));
                }
            }

            if (condition.fallback() != null) {
                return resolveRecursive(observer, conditions, candidatePool, positionLookup, targetLookup, 0, condition.fallback());
            }
        }

        return Optional.empty();
    }

    @Nullable
    private static ServerPlayer findNearest(ServerPlayer searcher, List<ServerPlayer> candidates) {
        ServerPlayer nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (ServerPlayer candidate : candidates) {
            if (candidate.level() == searcher.level()) {
                double d = searcher.distanceToSqr(candidate);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = candidate;
                }
            }
        }

        if (nearest == null && !candidates.isEmpty()) {
            nearest = candidates.getFirst();
        }

        return nearest;
    }
}
