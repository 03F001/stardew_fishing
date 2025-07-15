package com.bonker.stardewfishing.common;

import com.bonker.stardewfishing.FishingHookExt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FishingHookLogic {
    /**
     * TODO: remove this, Exists for backwards compatibility with Tide.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static Optional<ArrayList<ItemStack>> getStoredRewards(net.minecraft.world.entity.projectile.FishingHook entity) {
        return Optional.of(FishingHookExt.getStoredRewards(entity));
    }

    /**
     * TODO: remove this, Exists for backwards compatibility with Tide.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static void startMinigame(ServerPlayer player) {
        FishingHookExt.startMinigame(player);
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public static boolean startStardewMinigame(ServerPlayer player) {
        return FishingHookExt.startMinigame(player);
    }

    // todo: added to avoid breaking tide support
    @Deprecated(since = "2.0", forRemoval = true)
    public static void modifyRewards(List<ItemStack> rewards, double accuracy) {
        FishingHookExt.modifyRewards(rewards, accuracy, null);
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public static void modifyRewards(List<ItemStack> rewards, double accuracy, @Nullable ItemStack fishingRod) {
        FishingHookExt.modifyRewards(rewards, accuracy, fishingRod);
    }
}
