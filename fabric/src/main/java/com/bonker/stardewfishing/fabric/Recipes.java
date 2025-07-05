package com.bonker.stardewfishing.fabric;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Recipes {
    public static RecipeType<Recipe<?>> CONDITIONAL_RECIPE_TYPE;
    public static RecipeSerializer<Recipe<?>> CONDITIONAL_RECIPE_SERIALIZER;

    public static void register() {
        ConditionalRecipe.register(new ConditionalRecipe.ModLoadedCondition.Serializer());
        ConditionalRecipe.register(new ConditionalRecipe.OrCondition.Serializer());

        CONDITIONAL_RECIPE_TYPE = Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            new ResourceLocation("forge", "conditional"),
            new RecipeType<>() { }
        );

        CONDITIONAL_RECIPE_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            new ResourceLocation("forge", "conditional"),
            new ConditionalRecipe.Serializer()
        );
    }
}

class ConditionalRecipe {
    static class Serializer implements RecipeSerializer<Recipe<?>> {
        public static final Recipe<?> EMPTY_RECIPE = new Recipe<>() {
            @Override public boolean matches(Container inv, Level world) { return false; }
            @Override public @NotNull ItemStack assemble(Container inv, RegistryAccess access) { return ItemStack.EMPTY; }
            @Override public boolean canCraftInDimensions(int w, int h) { return false; }
            @Override public @NotNull ItemStack getResultItem(RegistryAccess access) { return ItemStack.EMPTY; }
            @Override public @NotNull ResourceLocation getId() { return new ResourceLocation("forge", "empty"); }
            @Override public @NotNull RecipeSerializer<?> getSerializer() { return Recipes.CONDITIONAL_RECIPE_SERIALIZER; }
            @Override public @NotNull RecipeType<?> getType() { return Recipes.CONDITIONAL_RECIPE_TYPE; }
        };

        @Override
        public @NotNull Recipe<?> fromJson(ResourceLocation id, JsonObject json) {
            var recipes = GsonHelper.getAsJsonArray(json, "recipes");
            recipe:for (int i = 0; i < recipes.size(); ++i) {
                if (!recipes.get(i).isJsonObject())
                    throw new JsonSyntaxException("Invalid recipes entry at index " + i + " Must be JsonObject");
                var conds = GsonHelper.getAsJsonArray(recipes.get(i).getAsJsonObject(), "conditions");
                for (int j = 0; j < conds.size(); ++j) {
                    if (!conds.get(j).isJsonObject())
                        throw new JsonSyntaxException("Conditions must be an array of JsonObjects");
                    if (!mkCondition(conds.get(j).getAsJsonObject()).test())
                        continue recipe;
                }
                return RecipeManager.fromJson(id, GsonHelper.getAsJsonObject(recipes.get(i).getAsJsonObject(), "recipe"));
            }
            return EMPTY_RECIPE;
        }

        @Override
        public @NotNull Recipe<?> fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, Recipe<?> recipe) {
            throw new UnsupportedOperationException();
        }
    }

    static final Map<ResourceLocation, IConditionSerializer<?>> conditions = new HashMap<>();

    public static void register(IConditionSerializer<?> ser) {
        var key = ser.getID();
        if (conditions.containsKey(key))
            throw new IllegalStateException("Duplicate recipe condition serializer: " + key);
        conditions.put(key, ser);
    }

    public static ICondition mkCondition(JsonObject json) {
        var type = new ResourceLocation(GsonHelper.getAsString(json, "type"));
        var ser = conditions.get(type);
        if (ser == null) throw new JsonSyntaxException("Unknown condition type: " + type.toString());
        return ser.read(json);
    }

    public static <T extends ICondition> JsonObject toJson(T cond) {
        @SuppressWarnings("unchecked")
        var ser = (IConditionSerializer<T>)conditions.get(cond.getID());
        if (ser == null) throw new JsonSyntaxException("Unknown condition type: " + cond.getID().toString());
        return ser.getJson(cond);
    }

    interface ICondition {
        ResourceLocation getID();
        boolean test();
    }

    interface IConditionSerializer<T extends ICondition> {
        void write(JsonObject json, T value);
        T read(JsonObject json);

        ResourceLocation getID();

        default JsonObject getJson(T value) {
            var ret = new JsonObject();
            write(ret, value);
            ret.addProperty("type", value.getID().toString());
            return ret;
        }
    }

    static class ModLoadedCondition implements ICondition {
        static final ResourceLocation NAME = new ResourceLocation("forge", "mod_loaded");
        final String modid;

        ModLoadedCondition(String modid) {
            this.modid = modid;
        }

        @Override public ResourceLocation getID() { return NAME; }

        @Override public boolean test() {
            return FabricLoader.getInstance().isModLoaded(modid);
        }

        static class Serializer implements IConditionSerializer<ModLoadedCondition> {
            @Override public void write(JsonObject json, ModLoadedCondition value) {
                json.addProperty("modid", value.modid);
            }

            @Override public ModLoadedCondition read(JsonObject json) {
                return new ModLoadedCondition(GsonHelper.getAsString(json, "modid"));
            }

            @Override public ResourceLocation getID() { return ModLoadedCondition.NAME; }
        }
    }

    static class OrCondition implements ICondition {
        static final ResourceLocation NAME = new ResourceLocation("forge", "or");
        final ICondition[] conds;

        OrCondition(ICondition... values) {
            this.conds = values;
        }

        @Override public ResourceLocation getID() { return NAME; }

        @Override
        public boolean test() {
            for (var c : conds) {
                if (c.test())
                    return true;
            }

            return false;
        }

        static class Serializer implements IConditionSerializer<OrCondition> {
            @Override
            public void write(JsonObject json, OrCondition value) {
                var values = new JsonArray();
                for (var c : value.conds)
                    values.add(toJson(c));
                json.add("values", values);
            }

            @Override
            public OrCondition read(JsonObject json) {
                var conds = new ArrayList<ICondition>();
                for (var e : GsonHelper.getAsJsonArray(json, "values")) {
                    if (!e.isJsonObject()) throw new JsonSyntaxException("Or condition values must be an array of JsonObjects");
                    conds.add(mkCondition(e.getAsJsonObject()));
                }
                return new OrCondition(conds.toArray(new ICondition[conds.size()]));
            }

            @Override public ResourceLocation getID() { return OrCondition.NAME; }
        }
    }
}