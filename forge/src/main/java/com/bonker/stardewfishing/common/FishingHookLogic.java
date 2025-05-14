package com.bonker.stardewfishing.common;

import com.bonker.stardewfishing.forge.FishingHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FishingHookLogic {
    /**
     * TODO: remove this, Exists for backwards compatibility with Tide.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static Optional<ArrayList<ItemStack>> getStoredRewards(net.minecraft.world.entity.projectile.FishingHook entity) {
        return FishingHook.getStoredRewards(entity);
    }

    /**
     * TODO: remove this, Exists for backwards compatibility with Tide.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static void startMinigame(ServerPlayer player) {
        FishingHook.startMinigame(player);
    }

    // todo: added to avoid breaking tide support
    @Deprecated(since = "2.0", forRemoval = true)
    public static void modifyRewards(List<ItemStack> rewards, double accuracy) {
        FishingHook.modifyRewards(rewards, accuracy, null);
    }
}
