package slimeknights.sconstruct.plugin.jei.category;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.smeltery.SmelteryComponents;
import slimeknights.sconstruct.smeltery.recipe.MeltingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * JEI category for {@link MeltingRecipe} — shows a smeltery melt: the item ingredient on the
 * left, the molten-fluid output on the right, and the temperature / time requirement as a text
 * overlay between them.
 *
 * <p>The category is registered by {@code SmithiesJeiPlugin#registerCategories}. The recipe
 * instances and the catalyst block (the smeltery controller) that opens this category are
 * wired in SMTCON-157.
 */
public class MeltingCategory implements IRecipeCategory<RecipeHolder<MeltingRecipe>> {

    /** JEI recipe type for the melting category — keyed {@code sconstruct:melting}. */
    public static final RecipeType<RecipeHolder<MeltingRecipe>> TYPE = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "melting"));

    private static final int WIDTH = 124;
    private static final int HEIGHT = 28;
    /** X of the text column — clear of the 16px input slot at the left edge. */
    private static final int TEXT_X = 26;
    /** Ticks per second — melt time is stored in ticks, displayed in seconds. */
    private static final int TICKS_PER_SECOND = 20;
    /** Dark grey, the conventional JEI on-background text colour. */
    private static final int TEXT_COLOR = 0x404040;

    private final IDrawable icon;

    public MeltingCategory(IGuiHelper guiHelper) {
        // The smeltery controller is the block that performs melting — a natural category icon.
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(SmelteryComponents.SMELTERY_CONTROLLER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<MeltingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jei.melting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MeltingRecipe> recipe, IFocusGroup focuses) {
        MeltingRecipe melting = recipe.value();
        FluidStack output = melting.output();
        builder.addInputSlot(4, 6).addIngredients(melting.input());
        builder.addOutputSlot(WIDTH - 20, 6).setFluidRenderer(output.getAmount(), false, 16, 16).addFluidStack(output.getFluid(), output.getAmount());
    }

    @Override
    public void draw(RecipeHolder<MeltingRecipe> recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        MeltingRecipe melting = recipe.value();
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, melting.temperature() + " K", TEXT_X, 4, TEXT_COLOR, false);
        graphics.drawString(font, formatSeconds(melting.time()), TEXT_X, 16, TEXT_COLOR, false);
    }

    /**
     * Formats a tick duration as seconds with one decimal place — plain integer division would
     * truncate a sub-second melt to {@code "0 s"} and round every melt down by up to a tick.
     */
    private static String formatSeconds(int ticks) {
        int wholeSeconds = ticks / TICKS_PER_SECOND;
        int tenths = (ticks % TICKS_PER_SECOND) * 10 / TICKS_PER_SECOND;
        return tenths == 0 ? wholeSeconds + " s" : wholeSeconds + "." + tenths + " s";
    }
}
