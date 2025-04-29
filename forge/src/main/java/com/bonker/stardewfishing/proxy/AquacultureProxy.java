package com.bonker.stardewfishing.proxy;

import com.teammetallurgy.aquaculture.item.AquaFishingRodItem;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;

public class AquacultureProxy {
    public static void damageEquippedBobber(ItemStack fishingRod, ServerPlayer player) {
        ItemStackHandler handler = AquaFishingRodItem.getHandler(fishingRod);
        ItemStack bobber = handler.getStackInSlot(3);
        if (bobber.isDamageableItem()) {
            bobber.hurtAndBreak(1, player, p -> {
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS);
                Vec3 particlePos = player.getEyePosition().add(player.getLookAngle());
                player.serverLevel().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, bobber), particlePos.x(), particlePos.y(), particlePos.z(), 15, 0.1, 0.1, 0.1, 0.1);
                player.displayClientMessage(Component.translatable("stardew_fishing.bobber_broke", bobber.getDisplayName()), true);
            });
            handler.setStackInSlot(3, bobber);
        }
    }

    public static ItemStack getBobber(ItemStack fishingRod) {
        return AquaFishingRodItem.getBobber(fishingRod);
    }

    public static boolean isAquaRod(ItemStack fishingRod) {
        return fishingRod.getItem() instanceof AquaFishingRodItem;
    }
}
