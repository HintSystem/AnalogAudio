package com.palm1.analogaudio.registry;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.recipe.DirectColoredCassetteRecipe;
import com.palm1.analogaudio.recipe.DyeCassetteRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModRecipeSerializers {
        public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
                        .create(BuiltInRegistries.RECIPE_SERIALIZER, AnalogAudio.MODID);

        public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DyeCassetteRecipe>> DYE_CASSETTE = SERIALIZERS
                        .register("dye_cassette",
                                        () -> new SimpleCraftingRecipeSerializer<>(DyeCassetteRecipe::new));

        public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DirectColoredCassetteRecipe>> DIRECT_COLORED_CASSETTE = SERIALIZERS
                        .register("direct_colored_cassette",
                                        () -> new SimpleCraftingRecipeSerializer<>(DirectColoredCassetteRecipe::new));
}
