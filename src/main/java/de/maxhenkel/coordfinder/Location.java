package de.maxhenkel.coordfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public record Location(ResourceLocation dimension, BlockPos position) {
    @Nullable
    public static Location fromString(String locStr) {
        String[] split = locStr.split(",");
        if (split.length != 4) {
            return null;
        }

        ResourceLocation dimension = new ResourceLocation(split[0]);

        int x, y, z;
        try {
            x = Integer.parseInt(split[1]);
            y = Integer.parseInt(split[2]);
            z = Integer.parseInt(split[3]);
        } catch (NumberFormatException e) {
            return null;
        }

        return new Location(dimension, new BlockPos(x, y, z));

    }

    public static Location fromPlayer(ServerPlayer player) {
        return new Location(player.level().dimension().location(), player.blockPosition());
    }

    @Override
    public String toString() {
        return "%s,%s,%s,%s".formatted(dimension.toString(), position.getX(), position.getY(), position.getZ());
    }
}
