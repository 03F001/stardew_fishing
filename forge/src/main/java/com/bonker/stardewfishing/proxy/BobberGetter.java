package com.bonker.stardewfishing.proxy;

import com.bonker.stardewfishing.StardewFishing;
import net.minecraft.world.item.ItemStack;

public class BobberGetter {
    public static ItemStack getBobber(ItemStack fishingRod) {
        if (StardewFishing.AQUACULTURE_INSTALLED && AquacultureProxy.isAquaRod(fishingRod)) {
            return AquacultureProxy.getBobber(fishingRod);
        } else if (StardewFishing.TIDE_INSTALLED) {
            return TideProxy.getBobber(fishingRod);
        } else {
            return ItemStack.EMPTY;
        }
    }
}
