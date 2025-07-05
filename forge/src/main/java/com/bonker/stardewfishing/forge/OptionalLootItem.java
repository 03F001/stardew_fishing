package com.bonker.stardewfishing.forge;

import com.bonker.stardewfishing.StardewFishing;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class OptionalLootItem extends LootPoolSingletonContainer {
    private final ResourceLocation itemId;
    @Nullable
    private final Item item;
    private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;
    private final LootPoolEntry entry = new OptionalEntryBase() {
        public void createItemStack(Consumer<ItemStack> stackConsumer, LootContext context) {
        OptionalLootItem.this.createItemStack(LootItemFunction.decorate(OptionalLootItem.this.compositeFunction, stackConsumer, context), context);
        }
    };

    protected OptionalLootItem(ResourceLocation itemId, int pWeight, int pQuality, LootItemCondition[] pConditions, LootItemFunction[] pFunctions) {
        super(pWeight, pQuality, pConditions, pFunctions);
        this.itemId = itemId;
        this.item = ForgeRegistries.ITEMS.getValue(itemId);
        this.compositeFunction = LootItemFunctions.compose(pFunctions);
    }

    @Override
    public boolean expand(LootContext pLootContext, Consumer<LootPoolEntry> pEntryConsumer) {
        if (this.canRun(pLootContext)) {
            pEntryConsumer.accept(entry);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void createItemStack(Consumer<ItemStack> pStackConsumer, LootContext pLootContext) {
        pStackConsumer.accept(item == null ? ItemStack.EMPTY : new ItemStack(item));
    }

    @Override
    public LootPoolEntryType getType() {
        return StardewFishing.platform.getLootPoolEntryType();
    }

    protected abstract class OptionalEntryBase extends LootPoolSingletonContainer.EntryBase {
        @Override
        public int getWeight(float luck) {
            return item == null ? 0 : super.getWeight(luck);
        }
    }

    public static class Serializer extends LootPoolSingletonContainer.Serializer<OptionalLootItem> {
        public void serializeCustom(JsonObject json, OptionalLootItem entry, JsonSerializationContext conditions) {
            super.serializeCustom(json, entry, conditions);
            json.addProperty("name", entry.itemId.toString());
        }

        protected OptionalLootItem deserialize(JsonObject json, JsonDeserializationContext context, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(json, "name"));
            return new OptionalLootItem(itemId, weight, quality, conditions, functions);
        }
    }
}
