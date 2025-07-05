package com.bonker.stardewfishing.fabric;

import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.compat.TideProxy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Items {
    public static Item TRAP_BOBBER;
    public static Item CORK_BOBBER;
    public static Item SONAR_BOBBER;
    public static Item TREASURE_BOBBER;
    public static Item QUALITY_BOBBER;

    public static CreativeModeTab TAB;

    public static void register() {
        TRAP_BOBBER = Registry.register(BuiltInRegistries.ITEM, StardewFishing.mkResLoc("trap_bobber"), new BobberItem(new Item.Properties().durability(64)));
        CORK_BOBBER = Registry.register(BuiltInRegistries.ITEM, StardewFishing.mkResLoc("cork_bobber"), new BobberItem(new Item.Properties().durability(64)));
        SONAR_BOBBER = Registry.register(BuiltInRegistries.ITEM, StardewFishing.mkResLoc("sonar_bobber"), new BobberItem(new Item.Properties().durability(64)));
        TREASURE_BOBBER = Registry.register(BuiltInRegistries.ITEM, StardewFishing.mkResLoc("treasure_bobber"), new BobberItem(new Item.Properties().durability(64)));
        QUALITY_BOBBER = Registry.register(BuiltInRegistries.ITEM, StardewFishing.mkResLoc("quality_bobber"), new BobberItem(new Item.Properties().durability(64)) {
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

        TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            StardewFishing.mkResLoc("items"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.stardewFishing"))
                .icon(() -> new ItemStack(TRAP_BOBBER))
                .displayItems((pParameters, pOutput) -> {
                    pOutput.accept(TRAP_BOBBER);
                    pOutput.accept(CORK_BOBBER);
                    pOutput.accept(SONAR_BOBBER);
                    pOutput.accept(TREASURE_BOBBER);
                    pOutput.accept(QUALITY_BOBBER);
                })
                .build());
    }

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
        if (StardewFishing.TIDE_INSTALLED) {
            return TideProxy.getBobber(fishingRod);
        } else {
            return ItemStack.EMPTY;
        }
    }
}
