package slimeknights.sconstruct.plugin.jei.category;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.gadgets.GadgetBlocks;
import slimeknights.sconstruct.gadgets.recipe.DryingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * JEI category for {@link DryingRecipe} — shows a drying rack recipe: the item ingredient on the
 * left, the dried-item output on the right, and the drying time as a text overlay between them.
 *
 * <p>The category is registered by {@code SmithiesJeiPlugin#registerCategories}. The recipe
 * instances and the catalyst block (the drying rack) that opens this category are wired in
 * SMTCON-157.
 */
public class DryingRackCategory implements IRecipeCategory<RecipeHolder<DryingRecipe>> {

    /** JEI recipe type for the drying category — keyed {@code sconstruct:drying}. */
    public static final RecipeType<RecipeHolder<DryingRecipe>> TYPE = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "drying"));

    private static final int WIDTH = 124;
    private static final int HEIGHT = 28;
    /** X of the text column — clear of the 16px input slot at the left edge. */
    private static final int TEXT_X = 26;
    /** Ticks per second — dry time is stored in ticks, displayed in seconds. */
    private static final int TICKS_PER_SECOND = 20;
    /** Dark grey, the conventional JEI on-background text colour. */
    private static final int TEXT_COLOR = 0x404040;

    private final IDrawable icon;

    public DryingRackCategory(IGuiHelper guiHelper) {
        // The drying rack is the block that performs drying — a natural category icon.
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(GadgetBlocks.DRYING_RACK.get()));
    }

    @Override
    public RecipeType<RecipeHolder<DryingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jei.drying");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<DryingRecipe> recipe, IFocusGroup focuses) {
        DryingRecipe drying = recipe.value();
        builder.addInputSlot(4, 6).addIngredients(drying.input());
        builder.addOutputSlot(WIDTH - 20, 6).addItemStack(drying.output());
    }

    @Override
    public void draw(RecipeHolder<DryingRecipe> recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.drawString(Minecraft.getInstance().font, formatSeconds(recipe.value().dryTime()), TEXT_X, 10, TEXT_COLOR, false);
    }

    /**
     * Formats a tick duration as seconds with one decimal place — plain integer division would
     * truncate a sub-second dry to {@code "0 s"} and round every recipe down by up to a tick.
     */
    private static String formatSeconds(int ticks) {
        int wholeSeconds = ticks / TICKS_PER_SECOND;
        int tenths = (ticks % TICKS_PER_SECOND) * 10 / TICKS_PER_SECOND;
        return tenths == 0 ? wholeSeconds + " s" : wholeSeconds + "." + tenths + " s";
    }
}
