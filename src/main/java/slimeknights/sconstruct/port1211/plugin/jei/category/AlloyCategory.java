package slimeknights.sconstruct.port1211.plugin.jei.category;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;
import slimeknights.sconstruct.port1211.smeltery.recipe.AlloyRecipe;
import slimeknights.sconstruct.port1211.smeltery.recipe.FluidIngredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * JEI category for {@link AlloyRecipe} — shows a smeltery alloy: the input fluids stacked in a
 * column on the left with a {@code +} between each pair, the alloyed-fluid output on the right,
 * and the minimum smeltery temperature drawn as a text overlay below the output.
 *
 * <p>The category is registered by {@code SmithiesJeiPlugin#registerCategories}. The recipe
 * instances and the catalyst block (the smeltery controller) that opens this category are wired
 * in SMTCON-157.
 *
 * <p>JEI's {@link IRecipeCategory} reports one fixed width/height for the whole category, so the
 * height is sized for {@link #MAX_INPUT_ROWS} input rows. Every smeltery alloy combines just two
 * or three fluids, so four rows is a deliberate upper bound with headroom; {@link #setRecipe}
 * still lays out exactly {@code inputs().size()} slots, and a hypothetical alloy with more inputs
 * than that bound would render its surplus rows below the panel rather than be silently dropped.
 */
public class AlloyCategory implements IRecipeCategory<RecipeHolder<AlloyRecipe>> {

    /** JEI recipe type for the alloy category — keyed {@code sconstruct:alloying}. */
    public static final RecipeType<RecipeHolder<AlloyRecipe>> TYPE = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "alloying"));

    /** Vertical distance between successive input rows — a 16px slot plus a 6px gap for the {@code +}. */
    private static final int ROW_PITCH = 22;
    /**
     * Number of input rows the fixed category height is sized for. Smeltery alloys combine two
     * or three fluids, so four rows covers every recipe with headroom — see the class javadoc.
     */
    private static final int MAX_INPUT_ROWS = 4;
    private static final int WIDTH = 124;
    private static final int HEIGHT = 6 + MAX_INPUT_ROWS * ROW_PITCH;
    /** X of the input column. */
    private static final int INPUT_X = 4;
    /** Y of the first input row. */
    private static final int FIRST_ROW_Y = 6;
    /** X of the single output slot, clear of the right edge. */
    private static final int OUTPUT_X = WIDTH - 20;
    /** Y of the output slot — vertically centred in the fixed category height. */
    private static final int OUTPUT_Y = (HEIGHT - 16) / 2;
    /** X of the {@code +} glyph — centred under the 16px input slot. */
    private static final int PLUS_X = INPUT_X + 8 - 3;
    /** Dark grey, the conventional JEI on-background text colour. */
    private static final int TEXT_COLOR = 0x404040;

    private final IDrawable icon;

    public AlloyCategory(IGuiHelper guiHelper) {
        // The smeltery controller is the block that performs alloying — a natural category icon.
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(SmelteryComponents.SMELTERY_CONTROLLER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<AlloyRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jei.alloying");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlloyRecipe> recipe, IFocusGroup focuses) {
        AlloyRecipe alloy = recipe.value();
        var inputs = alloy.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            FluidIngredient ingredient = inputs.get(i);
            IRecipeSlotBuilder slot = builder.addInputSlot(INPUT_X, FIRST_ROW_Y + i * ROW_PITCH).setFluidRenderer(ingredient.amount(), false, 16, 16);
            for (Holder<Fluid> fluid : ingredient.fluids()) {
                slot.addFluidStack(fluid.value(), ingredient.amount());
            }
        }

        FluidStack output = alloy.output();
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y).setFluidRenderer(output.getAmount(), false, 16, 16).addFluidStack(output.getFluid(), output.getAmount());
    }

    @Override
    public void draw(RecipeHolder<AlloyRecipe> recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int inputCount = recipe.value().inputs().size();
        // A "+" sits in the gap above every input row after the first.
        for (int i = 1; i < inputCount; i++) {
            graphics.drawString(font, "+", PLUS_X, FIRST_ROW_Y + i * ROW_PITCH - 9, TEXT_COLOR, false);
        }
        graphics.drawString(font, recipe.value().temperature() + " K", OUTPUT_X, OUTPUT_Y + 18, TEXT_COLOR, false);
    }
}
