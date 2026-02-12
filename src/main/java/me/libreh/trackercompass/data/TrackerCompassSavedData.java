package me.libreh.trackercompass.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.libreh.trackercompass.tracking.PlayerDimensionPositions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TrackerCompassSavedData extends SavedData {
    private static final String ID = "trackercompass";

    private final Map<UUID, PlayerDimensionPositions> playerDimensionPositions;
    private final Map<UUID, UUID> targetPlayerMappings;
    private final Map<UUID, Boolean> playerCompassToggles;

    private boolean needsSave = false;

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

    public static final Codec<TrackerCompassSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PLAYER_DIM_POS_CODEC)
                    .optionalFieldOf("playerDimensionPositions", new HashMap<>())
                    .forGetter(state -> state.playerDimensionPositions),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC)
                    .optionalFieldOf("targetPlayerMappings", new HashMap<>())
                    .forGetter(state -> state.targetPlayerMappings),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.BOOL)
                    .optionalFieldOf("playerCompassToggles", new HashMap<>())
                    .forGetter(state -> state.playerCompassToggles)
    ).apply(instance, TrackerCompassSavedData::new));

    private static final SavedDataType<TrackerCompassSavedData> TYPE = new SavedDataType<>(
            ID,
            TrackerCompassSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    public TrackerCompassSavedData() {
        this.playerDimensionPositions = new HashMap<>();
        this.targetPlayerMappings = new HashMap<>();
        this.playerCompassToggles = new HashMap<>();
    }

    private TrackerCompassSavedData(Map<UUID, PlayerDimensionPositions> playerDimensionPositions, Map<UUID, UUID> targetPlayerMappings, Map<UUID, Boolean> playerCompassToggles) {
        this.playerDimensionPositions = new HashMap<>(playerDimensionPositions);
        this.targetPlayerMappings = new HashMap<>(targetPlayerMappings);
        this.playerCompassToggles = new HashMap<>(playerCompassToggles);
    }

    public static TrackerCompassSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not available");
        }

        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<UUID, PlayerDimensionPositions> getPlayerDimensionPositions() {
        return playerDimensionPositions;
    }

    public PlayerDimensionPositions getOrCreatePlayerPositions(UUID playerId) {
        return playerDimensionPositions.computeIfAbsent(playerId, k -> new PlayerDimensionPositions());
    }

    public void updatePlayerPosition(UUID playerId, ResourceKey<Level> dimension, BlockPos pos) {
        PlayerDimensionPositions positions = getOrCreatePlayerPositions(playerId);
        positions.setPosition(dimension, pos);
        needsSave = true;
    }

    public void clearPlayerPositions(UUID playerId) {
        playerDimensionPositions.remove(playerId);
        needsSave = true;
    }

    public UUID getTargetPlayer(UUID observer) {
        return targetPlayerMappings.get(observer);
    }

    public void setTargetPlayer(UUID observer, UUID target) {
        targetPlayerMappings.put(observer, target);
        setDirty();
    }

    public Boolean getPlayerCompassToggle(UUID playerId) {
        return playerCompassToggles.get(playerId);
    }

    public void setPlayerCompassToggle(UUID playerId, boolean enabled) {
        playerCompassToggles.put(playerId, enabled);
        setDirty();
    }

    public void markDirtyIfNeeded() {
        if (needsSave) {
            setDirty();
            needsSave = false;
        }
    }
}
