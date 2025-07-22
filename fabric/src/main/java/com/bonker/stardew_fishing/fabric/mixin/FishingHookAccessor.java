package com.bonker.stardew_fishing.fabric.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.FishingHook;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("DATA_BITING")
    static EntityDataAccessor<Boolean> getDataBiting() {
        throw new AssertionError("Untransformed accessor");
    }

    @Accessor
    int getNibble();

    @Accessor
    int getTimeUntilHooked();

    @Accessor
    int getTimeUntilLured();

    @Accessor
    void setTimeUntilLured(int value);

    @Accessor
    int getLuck();

    @Accessor
    int getLureSpeed();
}