package slimeknights.sconstruct.port1211.plugin.jei.category;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.ToolDefinition;
import slimeknights.sconstruct.port1211.tools.ToolStationRegistry;
import slimeknights.sconstruct.port1211.tools.item.ToolParts;
import slimeknights.sconstruct.port1211.tools.material.MaterialRegistry;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * JEI category for {@link ToolBuildingEntry} — shows how each Smithies' Construct tool is
 * assembled: one input slot per {@link PartType} the {@link ToolDefinition} requires, laid out
 * left to right, with the built tool as the single output on the right.
 *
 * <p>Each part slot cycles through the registered materials (a part item stamped with every
 * {@code sconstruct:material} id) rather than rendering a material × tool cross-product — see
 * the Phase-7 risk note referenced on {@link ToolBuildingEntry}. The slots cycle independently,
 * so the panel shows "any of these materials in this slot" without ever multiplying the recipe
 * count.
 *
 * <p>The category is registered by {@code SmithiesJeiPlugin#registerCategories}; the entry list
 * ({@link ToolBuildingCatalog}) and the catalyst blocks (Tool Station, Tool Forge) that open
 * this category are wired in SMTCON-157.
 */
public class ToolBuildingCategory implements IRecipeCategory<ToolBuildingEntry> {

    /** JEI recipe type for the tool-building category — keyed {@code sconstruct:tool_building}. */
    public static final RecipeType<ToolBuildingEntry> TYPE = RecipeType.create(SConstruct.MOD_ID, "tool_building", ToolBuildingEntry.class);

    /** Widest tool roster — hammer / cleaver / lumber-axe / bows all take four part slots. */
    private static final int MAX_PART_SLOTS = 4;
    /** Horizontal distance between successive part slots — a 16px slot plus a 2px gap. */
    private static final int SLOT_PITCH = 18;
    private static final int FIRST_PART_X = 4;
    private static final int SLOT_Y = 6;
    private static final int WIDTH = 124;
    private static final int HEIGHT = 28;
    /** X of the output slot, clear of the right edge. */
    private static final int OUTPUT_X = WIDTH - 20;
    /** X of the {@code ->} glyph — centred in the gap between the last part slot and the output. */
    private static final int ARROW_X = FIRST_PART_X + MAX_PART_SLOTS * SLOT_PITCH;
    /** Dark grey, the conventional JEI on-background text colour. */
    private static final int TEXT_COLOR = 0x404040;

    private final IDrawable icon;

    public ToolBuildingCategory(IGuiHelper guiHelper) {
        // The Tool Station is the block players build basic tools at — a natural category icon.
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ToolStationRegistry.TOOL_STATION.get()));
    }

    @Override
    public RecipeType<ToolBuildingEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + SConstruct.MOD_ID + ".jei.tool_building");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ToolBuildingEntry recipe, IFocusGroup focuses) {
        ToolDefinition definition = recipe.definition();
        for (int i = 0; i < definition.getPartCount(); i++) {
            IRecipeSlotBuilder slot = builder.addInputSlot(FIRST_PART_X + i * SLOT_PITCH, SLOT_Y);
            slot.addItemStacks(materialVariants(definition.getPartSlot(i)));
        }
        builder.addOutputSlot(OUTPUT_X, SLOT_Y).addItemStack(new ItemStack(recipe.tool()));
    }

    /**
     * The part item for {@code part}, stamped once per registered material so JEI cycles the
     * slot through every material. Falls back to a single unstamped part stack when no
     * materials are loaded — JEI never shows a blank slot, and the part item's own default
     * material ({@code tconstruct:wood}) keeps the preview meaningful.
     */
    private static List<ItemStack> materialVariants(PartType part) {
        var partItem = ToolParts.get(part).get();
        List<ResourceLocation> materials = MaterialRegistry.materialIds();
        if (materials.isEmpty()) {
            return List.of(new ItemStack(partItem));
        }
        List<ItemStack> variants = new ArrayList<>(materials.size());
        for (ResourceLocation material : materials) {
            ItemStack stack = new ItemStack(partItem);
            stack.set(TinkerDataComponents.PART_MATERIAL.get(), material);
            variants.add(stack);
        }
        return variants;
    }

    @Override
    public void draw(ToolBuildingEntry recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.drawString(Minecraft.getInstance().font, "->", ARROW_X, SLOT_Y + 4, TEXT_COLOR, false);
    }
}
