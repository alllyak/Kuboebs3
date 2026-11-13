package ru.historyprogression.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public record RegionKey(String dimension, int x, int z) {
    public static RegionKey from(ServerLevel level, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return new RegionKey(
                level.dimension().location().toString(),
                Math.floorDiv(chunkX, 8),
                Math.floorDiv(chunkZ, 8)
        );
    }

    public String storageKey() {
        return dimension + "|" + x + "|" + z;
    }
}
