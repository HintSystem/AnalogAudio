package com.palm1.analogaudio.recipe;

import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.registry.ModRecipeSerializers;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.ArrayList;

public class DyeCassetteRecipe extends CustomRecipe {

    public DyeCassetteRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int items = 0;
        int dyes = 0;

        for (int i = 0; i < input.size(); ++i) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.CASSETTE_TAPE.get()) || stack.is(ModItems.CASSETTE_BAG.get())) {
                    items++;
                } else if (stack.getItem() instanceof DyeItem) {
                    dyes++;
                } else {
                    return false;
                }
            }
        }

        return items == 1 && dyes >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        List<DyeItem> dyes = new ArrayList<>();
        ItemStack targetItem = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); ++i) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.CASSETTE_TAPE.get()) || stack.is(ModItems.CASSETTE_BAG.get())) {
                    targetItem = stack;
                } else if (stack.getItem() instanceof DyeItem dyeItem) {
                    dyes.add(dyeItem);
                }
            }
        }

        if (targetItem.isEmpty() || dyes.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int currentColor = -1;
        if (targetItem.is(ModItems.CASSETTE_TAPE.get())) {
            CassetteData data = targetItem.get(ModDataComponents.CASSETTE_DATA.get());
            if (data != null && data.color() != 0xFFFFFF && data.color() != 0) {
                currentColor = data.color();
            }
        } else {
            DyedItemColor dyedColor = targetItem.get(DataComponents.DYED_COLOR);
            if (dyedColor != null) {
                currentColor = dyedColor.rgb();
            }
        }

        int finalColor = calculateColor(currentColor, dyes);
        ItemStack result = targetItem.copy();

        if (result.is(ModItems.CASSETTE_TAPE.get())) {
            CassetteData oldData = result.get(ModDataComponents.CASSETTE_DATA.get());
            if (oldData != null) {
                result.set(ModDataComponents.CASSETTE_DATA.get(),
                        new CassetteData(oldData.uuid(), oldData.url(), oldData.name(), finalColor, oldData.volume()));
            } else {
                result.set(ModDataComponents.CASSETTE_DATA.get(),
                        new CassetteData("", "", "", finalColor, 0.75f));
            }
        } else {
            result.set(DataComponents.DYED_COLOR, new DyedItemColor(finalColor, true));
        }

        return result;
    }

    private int calculateColor(int currentColor, List<DyeItem> dyes) {
        int[] rgbSum = new int[3];
        int maxIntensity = 0;
        int count = 0;

        if (currentColor != -1) {
            int r = (currentColor >> 16) & 255;
            int g = (currentColor >> 8) & 255;
            int b = currentColor & 255;
            maxIntensity += Math.max(r, Math.max(g, b));
            rgbSum[0] += r;
            rgbSum[1] += g;
            rgbSum[2] += b;
            count++;
        }

        for (DyeItem dye : dyes) {
            int colorInt = dye.getDyeColor().getFireworkColor();
            int r = (colorInt >> 16) & 255;
            int g = (colorInt >> 8) & 255;
            int b = colorInt & 255;
            maxIntensity += Math.max(r, Math.max(g, b));
            rgbSum[0] += r;
            rgbSum[1] += g;
            rgbSum[2] += b;
            count++;
        }

        int avgR = rgbSum[0] / count;
        int avgG = rgbSum[1] / count;
        int avgB = rgbSum[2] / count;
        float avgIntensity = (float) maxIntensity / (float) count;
        float maxAvg = (float) Math.max(avgR, Math.max(avgG, avgB));
        
        if (maxAvg > 0) {
            avgR = (int) ((float) avgR * avgIntensity / maxAvg);
            avgG = (int) ((float) avgG * avgIntensity / maxAvg);
            avgB = (int) ((float) avgB * avgIntensity / maxAvg);
        }
        
        return (avgR << 16) | (avgG << 8) | avgB;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DYE_CASSETTE.get();
    }
}
