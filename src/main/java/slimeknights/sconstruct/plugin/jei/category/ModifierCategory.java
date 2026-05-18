package slimeknights.sconstruct.plugin.jei.category;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.tools.ToolStationRegistry;
import slimeknights.sconstruct.tools.modifier.Modifier;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * JEI category for {@link ModifierEntry} — one panel per tool modifier, showing the modifier's
 * localised name, its level cap, and the number of free-modifier slots each application costs.
 *
 * <p>A modifier has no item ingredient in the data model (it is applied at the Tool Station's
 * Modify action), so this category renders text only: {@link #setRecipe} adds no slots and the
 * three lines are drawn in {@link #draw}.
 *
 * <p>The category is registered by {@code SmithiesJeiPlugin#registerCategories}; the entry list
 * ({@link ModifierCatalog}) and the catalyst block (the Tool Station) that opens this category
 * are wired in SMTCON-157.
 */
public class ModifierCategory implements IRecipeCategory<ModifierEntry> {

    /** JEI recipe type for the modifier category — keyed {@code sconstruct:modifier}. */
    public static final RecipeType<ModifierEntry> TYPE = RecipeType.create(SConstruct.MOD_ID, "modifier", ModifierEntry.class);

    private static final int WIDTH = 132;
    private static final int HEIGHT = 40;
    private static final int TEXT_X = 4;
    /** Y of the modifier-name line. */
    private static final int NAME_Y = 2;
    /** Y of the level-cap line. */
    private static final int MAX_LEVEL_Y = 16;
    /** Y of the slot-cost line. */
    private static final int SLOT_COST_Y = 28;
    /** Dark grey, the conventional JEI on-background text colour. */
    private static final int TEXT_COLOR = 0x404040;

    private final IDrawable icon;

    public ModifierCategory(IGuiHelper guiHelper) {
        // Modifiers are applied via the Tool Station's Modify action — its block is the icon.
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ToolStationRegistry.TOOL_STATION.get()));
    }

    @Override
    public RecipeType<ModifierEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jei.modifiers");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ModifierEntry recipe, IFocusGroup focuses) {
        // A modifier has no item ingredient — the panel is text only, drawn in draw().
    }

    @Override
    public void draw(ModifierEntry recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        Modifier modifier = recipe.modifier();
        graphics.drawString(font, modifier.description(modifier.maxLevel()), TEXT_X, NAME_Y, TEXT_COLOR, false);
        graphics.drawString(font, Component.translatable("gui." + SConstruct.MOD_ID + ".jei.modifier.max_level", modifier.maxLevel()), TEXT_X, MAX_LEVEL_Y, TEXT_COLOR, false);
        graphics.drawString(font, Component.translatable("gui." + SConstruct.MOD_ID + ".jei.modifier.slot_cost", modifier.slotCost()), TEXT_X, SLOT_COST_Y, TEXT_COLOR, false);
    }
}
