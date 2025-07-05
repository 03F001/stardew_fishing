package com.bonker.stardewfishing.forge;

import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.forge.compat.AquacultureProxy;
import com.bonker.stardewfishing.compat.TideProxy;

import com.google.common.collect.ImmutableList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Items {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StardewFishing.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StardewFishing.MODID);

    public static final RegistryObject<Item> TRAP_BOBBER = ITEMS.register("trap_bobber", () -> new BobberItem(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> CORK_BOBBER = ITEMS.register("cork_bobber", () -> new BobberItem(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> SONAR_BOBBER = ITEMS.register("sonar_bobber", () -> new BobberItem(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> TREASURE_BOBBER = ITEMS.register("treasure_bobber", () -> new BobberItem(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> QUALITY_BOBBER = ITEMS.register("quality_bobber", () -> new BobberItem(new Item.Properties().durability(64)) {
        @Override
        protected List<Component> makeTooltip() {
            ImmutableList.Builder<Component> builder = new ImmutableList.Builder<>();
            builder.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
            if (StardewFishing.QUALITY_FOOD_INSTALLED) {
                builder.add(Component.translatable(getDescriptionId() + ".quality_food_tooltip").withStyle(ChatFormatting.GRAY));
            }
            return builder.build();
        }
    });

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("items", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.stardewFishing"))
        .icon(() -> new ItemStack(TRAP_BOBBER.get()))
        .displayItems((pParameters, pOutput) -> {
            pOutput.accept(TRAP_BOBBER.get());
            pOutput.accept(CORK_BOBBER.get());
            pOutput.accept(SONAR_BOBBER.get());
            pOutput.accept(TREASURE_BOBBER.get());
            pOutput.accept(QUALITY_BOBBER.get());
        })
        .build());

    public static class BobberItem extends Item implements DyeableLeatherItem {
        private List<Component> tooltip;

        public BobberItem(Properties pProperties) {
            super(pProperties);
        }

        protected List<Component> makeTooltip() {
            return List.of(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        }

        // called by tide
        public ResourceLocation getTextureLocation() {
            return TideProxy.DEFAULT_BOBBER_TEXTURE;
        }

        // called by tide
        public Component getTranslation() {
            return getDescription();
        }

        @Override
        public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
            if (tooltip == null) {
                tooltip = makeTooltip();
            }
            pTooltipComponents.addAll(tooltip);
        }
    }

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
