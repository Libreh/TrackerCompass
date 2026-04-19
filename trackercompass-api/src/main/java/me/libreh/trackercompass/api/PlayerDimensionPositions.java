package me.libreh.trackercompass.api;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerDimensionPositions {
    public static final Codec<PlayerDimensionPositions> CODEC =
            Codec.unboundedMap(Identifier.CODEC, BlockPos.CODEC)
                    .xmap(PlayerDimensionPositions::fromLocationMap, PlayerDimensionPositions::toLocationMap);

    private final Map<ResourceKey<Level>, BlockPos> positions;

    public PlayerDimensionPositions() {
        this.positions = new HashMap<>();
    }

    public PlayerDimensionPositions(Map<ResourceKey<Level>, BlockPos> positions) {
        this.positions = new HashMap<>(positions);
    }

    private static PlayerDimensionPositions fromLocationMap(Map<Identifier, BlockPos> map) {
        var pdp = new PlayerDimensionPositions();
        map.forEach((loc, pos) -> pdp.positions.put(ResourceKey.create(Registries.DIMENSION, loc), pos));
        return pdp;
    }

    private Map<Identifier, BlockPos> toLocationMap() {
        var map = new LinkedHashMap<Identifier, BlockPos>();
        positions.forEach((key, pos) -> map.put(key.identifier(), pos));
        return map;
    }

    public void setPosition(ResourceKey<Level> dimension, BlockPos pos) {
        positions.put(dimension, pos);
    }

    public BlockPos getPosition(ResourceKey<Level> dimension) {
        return positions.get(dimension);
    }

    public Map<ResourceKey<Level>, BlockPos> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    public void clear() {
        positions.clear();
    }
}
