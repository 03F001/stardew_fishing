package com.bonker.stardewfishing;

import com.bonker.stardewfishing.common.init.SFItems;
import com.bonker.stardewfishing.common.init.SFLootPoolEntryTypes;
import com.bonker.stardewfishing.common.init.SFSoundEvents;
import com.bonker.stardewfishing.common.networking.SFNetworking;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(StardewFishing.MODID)
public class StardewFishing {
    @Deprecated(since = "2.0", forRemoval = true)
    public static RegistryObject<SoundEvent> PULL_ITEM; //TODO remove this, temporary tide fix
    public static final String MODID = Version.id;

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final boolean QUALITY_FOOD_INSTALLED = ModList.get().isLoaded("quality_food");
    public static final boolean AQUACULTURE_INSTALLED = ModList.get().isLoaded("aquaculture");
    public static final boolean TIDE_INSTALLED = ModList.get().isLoaded("tide");
    public static final boolean BOBBER_ITEMS_REGISTERED = AQUACULTURE_INSTALLED || TIDE_INSTALLED;

    public static final TagKey<Item> STARTS_MINIGAME = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), new ResourceLocation(MODID, "starts_minigame"));

    public static final ResourceLocation TREASURE_CHEST_LOOT = new ResourceLocation(MODID, "treasure_chest");
    public static final ResourceLocation TREASURE_CHEST_NETHER_LOOT = new ResourceLocation(MODID, "treasure_chest_nether");

    public static String MOD_NAME;

    public StardewFishing() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        IModInfo info = ModLoadingContext.get().getActiveContainer().getModInfo();
        MOD_NAME = info.getDisplayName() + " " + info.getVersion();

        SFSoundEvents.SOUND_EVENTS.register(bus);
        PULL_ITEM = SFSoundEvents.PULL_ITEM; //TODO remove this, temporary tide fix
        SFLootPoolEntryTypes.LOOT_POOL_ENTRY_TYPES.register(bus);

        if (BOBBER_ITEMS_REGISTERED) {
            SFItems.ITEMS.register(bus);
            SFItems.CREATIVE_MODE_TABS.register(bus);
        }

        SFNetworking.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SFConfig.SERVER_SPEC);
    }
}
