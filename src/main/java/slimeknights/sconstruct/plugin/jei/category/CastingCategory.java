package slimeknights.sconstruct.plugin.jei.category;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import slimeknights.sconstruct.smeltery.recipe.CastingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * Shared JEI category layout for {@link CastingRecipe} — the casting table and casting basin
 * differ only by their icon, title, and recipe type, so both concrete categories
 * ({@link CastingTableCategory}, {@link CastingBasinCategory}) extend this base and supply just
 * those three variant-specific pieces.
 *
 * <p>The layout shows, left to right, the cast item the recipe requires (empty when the recipe
 * needs no cast), the fluid that must be poured, and the produced item — with the cooling time
 * drawn as a text overlay below the slots.
 *
 * <p>Each concrete category carries a distinct {@code RecipeType}; the JEI plugin partitions the
 * casting recipes by {@link CastingRecipe#isBasin()} when it registers the recipe lists in
 * SMTCON-157, so a basin recipe never appears under the table category and vice versa.
 */
abstract class CastingCategory implements IRecipeCategory<RecipeHolder<CastingRecipe>> {

    private static final int WIDTH = 124;
    private static final int HEIGHT = 40;
    /** X of the cooling-time text — left-aligned under the cast slot. */
    private static final int TEXT_X = 4;
    /** Y of the cooling-time text — clear of the 16px slot row at the top. */
    private static final int TEXT_Y = 28;
    /** Ticks per second — cooling time is stored in ticks, displayed in seconds. */
    private static final int TICKS_PER_SECOND = 20;
    /** Dark grey, the conventional JEI on-background text colour. */
    private static final int TEXT_COLOR = 0x404040;

    private final IDrawable icon;

    CastingCategory(IGuiHelper guiHelper, ItemLike iconBlock) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(iconBlock));
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CastingRecipe> recipe, IFocusGroup focuses) {
        CastingRecipe casting = recipe.value();
        // The cast slot is always laid out; addIngredients with Ingredient.EMPTY leaves it blank
        // for a recipe that pours straight into the block with no cast.
        builder.addInputSlot(4, 6).addIngredients(casting.cast());

        IRecipeSlotBuilder fluidSlot = builder.addInputSlot(30, 6).setFluidRenderer(casting.fluid().amount(), false, 16, 16);
        for (var fluid : casting.fluid().fluids()) {
            fluidSlot.addFluidStack(fluid.value(), casting.fluid().amount());
        }

        builder.addOutputSlot(WIDTH - 20, 6).addItemStack(casting.output());
    }

    @Override
    public void draw(RecipeHolder<CastingRecipe> recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, formatSeconds(recipe.value().coolingTime()), TEXT_X, TEXT_Y, TEXT_COLOR, false);
    }

    /**
     * Formats a tick duration as seconds with one decimal place — plain integer division would
     * truncate a sub-second cool to {@code "0 s"} and round every cast down by up to a tick.
     */
    private static String formatSeconds(int ticks) {
        int wholeSeconds = ticks / TICKS_PER_SECOND;
        int tenths = (ticks % TICKS_PER_SECOND) * 10 / TICKS_PER_SECOND;
        return tenths == 0 ? wholeSeconds + " s" : wholeSeconds + "." + tenths + " s";
    }
}
