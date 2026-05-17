package slimeknights.sconstruct.port1211.plugin.jei.category;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.CastingBlocks;
import slimeknights.sconstruct.port1211.smeltery.recipe.CastingRecipe;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;

/**
 * JEI category for casting-table {@link CastingRecipe}s — the small-volume casting variant that
 * pours over a cast on a {@link CastingBlocks#CASTING_TABLE} to produce ingots, plates, and
 * other parts. The shared layout lives in {@link CastingCategory}.
 */
public class CastingTableCategory extends CastingCategory {

    /** JEI recipe type for the casting-table category — keyed {@code sconstruct:casting_table}. */
    public static final RecipeType<RecipeHolder<CastingRecipe>> TYPE = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "casting_table"));

    public CastingTableCategory(IGuiHelper guiHelper) {
        super(guiHelper, CastingBlocks.CASTING_TABLE.get());
    }

    @Override
    public RecipeType<RecipeHolder<CastingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jei.casting_table");
    }
}
