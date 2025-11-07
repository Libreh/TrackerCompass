package me.libreh.trackercompass.tracking;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PlayerDimensionPositions {
    private BlockPos overworldPos = null;
    private BlockPos netherPos = null;
    private BlockPos endPos = null;

    public PlayerDimensionPositions() {
    }

    public PlayerDimensionPositions(BlockPos overworldPos, BlockPos netherPos, BlockPos endPos) {
        this.overworldPos = overworldPos;
        this.netherPos = netherPos;
        this.endPos = endPos;
    }

    public BlockPos getOverworldPos() {
        return overworldPos;
    }

    public BlockPos getNetherPos() {
        return netherPos;
    }

    public BlockPos getEndPos() {
        return endPos;
    }

    public void setPosition(RegistryKey<World> dimension, BlockPos pos) {
        if (dimension.equals(World.OVERWORLD)) {
            overworldPos = pos;
        } else if (dimension.equals(World.NETHER)) {
            netherPos = pos;
        } else if (dimension.equals(World.END)) {
            endPos = pos;
        }
    }

    public BlockPos getPosition(RegistryKey<World> dimension) {
        if (dimension.equals(World.OVERWORLD)) {
            return overworldPos;
        } else if (dimension.equals(World.NETHER)) {
            return netherPos;
        } else if (dimension.equals(World.END)) {
            return endPos;
        }
        return null;
    }
}
