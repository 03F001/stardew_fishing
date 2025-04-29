package com.bonker.stardewfishing.common.init;

import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.common.OptionalLootItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SFLootPoolEntryTypes {
    public static final DeferredRegister<LootPoolEntryType> LOOT_POOL_ENTRY_TYPES = DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, StardewFishing.MODID);

    public static final RegistryObject<LootPoolEntryType> MOD_LOADED = LOOT_POOL_ENTRY_TYPES.register("optional",
            () -> new LootPoolEntryType(new OptionalLootItem.Serializer()));
}
