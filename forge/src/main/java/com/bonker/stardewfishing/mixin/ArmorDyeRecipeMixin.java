package com.bonker.stardewfishing.mixin;

import com.bonker.stardewfishing.forge.Items;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmorDyeRecipe.class)
public abstract class ArmorDyeRecipeMixin {
    /**
     * Prevents BobberItems from being dyed in a crafting table.
     */
    @Redirect(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean stardewFishing_isEmpty(ItemStack instance) {
        if (instance.getItem() instanceof Items.BobberItem) {
            return true;
        }
        return instance.isEmpty();
    }
}
