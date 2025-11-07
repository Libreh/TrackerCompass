package me.libreh.trackercompass.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.libreh.trackercompass.tracking.PlayerDimensionPositions;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TrackerCompassPersistentState extends PersistentState {
    private static final String ID = "trackercompass";

    private final Map<UUID, PlayerDimensionPositions> playerDimensionPositions;
    private final Map<UUID, UUID> targetPlayerMappings;
    private final Map<UUID, Boolean> playerCompassToggles;

    public static final Codec<PlayerDimensionPositions> PLAYER_DIM_POS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("overworldPos").forGetter(data -> Optional.ofNullable(data.getOverworldPos())),
            BlockPos.CODEC.optionalFieldOf("netherPos").forGetter(data -> Optional.ofNullable(data.getNetherPos())),
            BlockPos.CODEC.optionalFieldOf("endPos").forGetter(data -> Optional.ofNullable(data.getEndPos()))
    ).apply(instance, (overworld, nether, end) ->
            new PlayerDimensionPositions(
                    overworld.orElse(null),
                    nether.orElse(null),
                    end.orElse(null)
            )
    ));

    public static final Codec<TrackerCompassPersistentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Uuids.CODEC, PLAYER_DIM_POS_CODEC)
                    .optionalFieldOf("playerDimensionPositions", new HashMap<>())
                    .forGetter(state -> state.playerDimensionPositions),
            Codec.unboundedMap(Uuids.CODEC, Uuids.CODEC)
                    .optionalFieldOf("targetPlayerMappings", new HashMap<>())
                    .forGetter(state -> state.targetPlayerMappings),
            Codec.unboundedMap(Uuids.CODEC, Codec.BOOL)
                    .optionalFieldOf("playerCompassToggles", new HashMap<>())
                    .forGetter(state -> state.playerCompassToggles)
    ).apply(instance, TrackerCompassPersistentState::new));

    private static final PersistentStateType<TrackerCompassPersistentState> TYPE = new PersistentStateType<>(
            ID,
            TrackerCompassPersistentState::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    public TrackerCompassPersistentState() {
        this.playerDimensionPositions = new HashMap<>();
        this.targetPlayerMappings = new HashMap<>();
        this.playerCompassToggles = new HashMap<>();
    }

    private TrackerCompassPersistentState(Map<UUID, PlayerDimensionPositions> playerDimensionPositions, Map<UUID, UUID> targetPlayerMappings, Map<UUID, Boolean> playerCompassToggles) {
        this.playerDimensionPositions = new HashMap<>(playerDimensionPositions);
        this.targetPlayerMappings = new HashMap<>(targetPlayerMappings);
        this.playerCompassToggles = new HashMap<>(playerCompassToggles);
    }

    public static TrackerCompassPersistentState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not available");
        }

        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }

    public Map<UUID, PlayerDimensionPositions> getPlayerDimensionPositions() {
        return playerDimensionPositions;
    }

    public PlayerDimensionPositions getOrCreatePlayerPositions(UUID playerId) {
        return playerDimensionPositions.computeIfAbsent(playerId, k -> new PlayerDimensionPositions());
    }

    public void updatePlayerPosition(UUID playerId, net.minecraft.registry.RegistryKey<World> dimension, BlockPos pos) {
        PlayerDimensionPositions positions = getOrCreatePlayerPositions(playerId);
        positions.setPosition(dimension, pos);
        markDirty();
    }

    public void clearPlayerPositions(UUID playerId) {
        playerDimensionPositions.remove(playerId);
        markDirty();
    }

    public UUID getTargetPlayer(UUID observer) {
        return targetPlayerMappings.get(observer);
    }

    public void setTargetPlayer(UUID observer, UUID target) {
        targetPlayerMappings.put(observer, target);
        markDirty();
    }

    public Boolean getPlayerCompassToggle(UUID playerId) {
        return playerCompassToggles.get(playerId);
    }

    public void setPlayerCompassToggle(UUID playerId, boolean enabled) {
        playerCompassToggles.put(playerId, enabled);
        markDirty();
    }
}
