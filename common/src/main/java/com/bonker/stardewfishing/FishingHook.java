package com.bonker.stardewfishing;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public class FishingHook {
    public static InteractionHand getRodHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof FishingRodItem)
            return InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem().getItem() instanceof FishingRodItem)
            return InteractionHand.OFF_HAND;

        return null;
    }

    public static boolean hasBobber(@Nullable ItemStack fishingRod, com.bonker.stardewfishing.Item item) {
        if (!StardewFishing.BOBBER_ITEMS_REGISTERED || fishingRod == null) {
            return false;
        }
        return StardewFishing.platform.getBobber(fishingRod).is(StardewFishing.platform.getItem(item));
    }
}