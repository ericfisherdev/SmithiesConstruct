package slimeknights.sconstruct.plugin.jei.category;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.tools.PartBuilderRegistry;
import slimeknights.sconstruct.tools.StencilTableRegistry;
import slimeknights.sconstruct.tools.item.PatternItem;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * JEI category for {@link PartBuilderEntry} — shows how each tool part is shaped at the Part
 * Builder: the typed pattern for the part on the left, the built part on the right.
 *
 * <p>The output slot cycles through the registered materials (the part item stamped with every
 * {@code sconstruct:material} id) rather than rendering a material × part-type cross-product —
 * see the Phase-7 risk note referenced on {@link PartBuilderEntry}. The raw material the Part
 * Builder also consumes is implicit in this cycling: each material the output cycles to is the
 * material that produced it.
 *
 * <p>The category is registered by {@code SmithiesJeiPlugin#registerCategories}; the entry list
 * ({@link PartBuilderCatalog}) and the catalyst block (the Part Builder) that opens this
 * category are wired in SMTCON-157.
 */
public class PartBuilderCategory implements IRecipeCategory<PartBuilderEntry> {

    /** JEI recipe type for the part-building category — keyed {@code sconstruct:part_building}. */
    public static final RecipeType<PartBuilderEntry> TYPE = RecipeType.create(SConstruct.MOD_ID, "part_building", PartBuilderEntry.class);

    private static final int WIDTH = 124;
    private static final int HEIGHT = 28;
    private static final int SLOT_Y = 6;
    /** X of the typed-pattern input slot. */
    private static final int PATTERN_X = 4;
    /** X of the built-part output slot, clear of the right edge. */
    private static final int OUTPUT_X = WIDTH - 20;
    /** X of the {@code ->} glyph — centred in the gap between the pattern and the output. */
    private static final int ARROW_X = (PATTERN_X + 16 + OUTPUT_X) / 2 - 4;
    /** Dark grey, the conventional JEI on-background text colour. */
    private static final int TEXT_COLOR = 0x404040;

    private final IDrawable icon;

    public PartBuilderCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(PartBuilderRegistry.PART_BUILDER.get()));
    }

    @Override
    public RecipeType<PartBuilderEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jei.part_building");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PartBuilderEntry recipe, IFocusGroup focuses) {
        ItemStack typedPattern = PatternItem.makeTyped(StencilTableRegistry.PATTERN.get(), recipe.part());
        builder.addInputSlot(PATTERN_X, SLOT_Y).addItemStack(typedPattern);
        builder.addOutputSlot(OUTPUT_X, SLOT_Y).addItemStacks(PartMaterialPreview.stampedVariants(recipe.part()));
    }

    @Override
    public void draw(PartBuilderEntry recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.drawString(Minecraft.getInstance().font, "->", ARROW_X, SLOT_Y + 4, TEXT_COLOR, false);
    }
}
