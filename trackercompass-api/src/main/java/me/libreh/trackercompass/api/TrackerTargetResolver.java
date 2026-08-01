package me.libreh.trackercompass.api;

import eu.pb4.predicate.api.PredicateContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class TrackerTargetResolver {
    private TrackerTargetResolver() {}

    public record ResolveRequest(
        ServerPlayer observer,
        List<TrackerCondition> conditions,
        Collection<ServerPlayer> candidatePool,
        Function<UUID, UUID> pinnedTargetLookup
    ) {}

    public record ResolveResult(
        ServerPlayer target,
        @Nullable TrackerCondition activeCondition
    ) {}

    public static Optional<ResolveResult> resolve(ResolveRequest request) {
        List<TrackerCondition> sorted = request.conditions().stream()
            .filter(TrackerCondition::enabled)
            .sorted(Comparator.comparingInt(TrackerCondition::priority).reversed())
            .toList();

        return resolveFrom(request, sorted, 0, new HashSet<>());
    }

    // Where to point at a resolved target: its live position if it's online in the observer's
    // dimension, else its last known position stored for that dimension. Dimension resolution goes
    // through DimensionUtil so spoofed dimensions stay consistent between storage and lookup.
    @Nullable
    public static BlockPos resolvePosition(ServerPlayer observer, UUID targetUuid, MinecraftServer server,
                                           Map<UUID, PlayerDimensionPositions> positions) {
        ResourceKey<Level> dimension = DimensionUtil.resolve(observer);

        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
        if (target != null && DimensionUtil.resolve(target).equals(dimension)) {
            return target.blockPosition();
        }

        PlayerDimensionPositions stored = positions.get(targetUuid);
        return stored != null ? stored.getPosition(dimension) : null;
    }

    private static Optional<ResolveResult> resolveFrom(
        ResolveRequest request,
        List<TrackerCondition> conditions,
        int startIndex,
        Set<String> visitedFallbacks
    ) {
        for (int i = startIndex; i < conditions.size(); i++) {
            TrackerCondition condition = conditions.get(i);
            List<ServerPlayer> matching = matchCandidates(request, condition);

            if (!matching.isEmpty()) {
                ServerPlayer target = findNearest(request.observer(), matching);
                if (target != null) {
                    return Optional.of(new ResolveResult(target, condition));
                }
            }

            if (condition.fallback() != null) {
                // Follow the fallback chain, guarding against cycles (a -> b -> a) and unknown ids.
                if (!visitedFallbacks.add(condition.fallback())) {
                    return Optional.empty();
                }
                int fallbackIndex = indexOfCondition(conditions, condition.fallback());
                if (fallbackIndex < 0) {
                    return Optional.empty();
                }
                return resolveFrom(request, conditions, fallbackIndex, visitedFallbacks);
            }
        }

        return Optional.empty();
    }

    private static List<ServerPlayer> matchCandidates(ResolveRequest request, TrackerCondition condition) {
        List<ServerPlayer> matching = new ArrayList<>();

        if (condition.id().equals(TrackerCondition.SELECTED_TARGET_ID)) {
            UUID targetUuid = request.pinnedTargetLookup().apply(request.observer().getUUID());
            if (targetUuid != null) {
                for (ServerPlayer candidate : request.candidatePool()) {
                    if (candidate.getUUID().equals(targetUuid)) {
                        matching.add(candidate);
                        break;
                    }
                }
            }
        } else {
            for (ServerPlayer candidate : request.candidatePool()) {
                if (candidate.getUUID().equals(request.observer().getUUID())) continue;
                if (condition.condition().test(PredicateContext.of(candidate)).success()) {
                    matching.add(candidate);
                }
            }
        }

        return matching;
    }

    private static int indexOfCondition(List<TrackerCondition> conditions, String id) {
        for (int i = 0; i < conditions.size(); i++) {
            if (conditions.get(i).id().equals(id)) return i;
        }
        return -1;
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
