package me.libreh.trackercompass.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

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

    public void setPosition(ResourceKey<Level> dimension, BlockPos pos) {
        if (dimension.equals(Level.OVERWORLD)) {
            overworldPos = pos;
        } else if (dimension.equals(Level.NETHER)) {
            netherPos = pos;
        } else if (dimension.equals(Level.END)) {
            endPos = pos;
        }
    }

    public BlockPos getPosition(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) {
            return overworldPos;
        } else if (dimension.equals(Level.NETHER)) {
            return netherPos;
        } else if (dimension.equals(Level.END)) {
            return endPos;
        }
        return null;
    }
}
