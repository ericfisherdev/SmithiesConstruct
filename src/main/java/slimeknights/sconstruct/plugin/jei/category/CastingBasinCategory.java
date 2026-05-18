package slimeknights.sconstruct.plugin.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.smeltery.CastingBlocks;
import slimeknights.sconstruct.smeltery.recipe.CastingRecipe;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

/**
 * JEI category for casting-basin {@link CastingRecipe}s — the large-volume casting variant that
 * pours into a {@link CastingBlocks#CASTING_BASIN} to produce blocks. The shared layout lives in
 * {@link CastingCategory}.
 */
public class CastingBasinCategory extends CastingCategory {

    /** JEI recipe type for the casting-basin category — keyed {@code sconstruct:casting_basin}. */
    public static final RecipeType<RecipeHolder<CastingRecipe>> TYPE = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "casting_basin"));

    public CastingBasinCategory(IGuiHelper guiHelper) {
        super(guiHelper, CastingBlocks.CASTING_BASIN.get());
    }

    @Override
    public RecipeType<RecipeHolder<CastingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jei.casting_basin");
    }
}
