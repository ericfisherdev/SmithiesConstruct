package slimeknights.sconstruct.port1211.plugin.patchouli.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.google.gson.annotations.SerializedName;

import slimeknights.sconstruct.port1211.SConstruct;

import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;

/**
 * Custom Patchouli page type {@code tconstruct:casting} (SMTCON-162) — visualises a casting
 * table or basin recipe: the optional cast item and the poured fluid on the left, the cast
 * result on the right, and the cooling time below.
 *
 * <p>A book entry adds a page like {@code {"type": "tconstruct:casting", "cast":
 * "sconstruct:ingot_cast", "fluid": "sconstruct:molten_iron", "amount": 90, "output":
 * "minecraft:iron_ingot", "cooling_time": 100}}; the {@code cast} field may be omitted for a
 * recipe that pours straight into the block.
 */
public class PageCasting extends RecipePage {

    @SerializedName("cast")
    private String cast = "";
    @SerializedName("fluid")
    private String fluid = "";
    @SerializedName("amount")
    private int amount;
    @SerializedName("output")
    private String output = "";
    @SerializedName("cooling_time")
    private int coolingTime;

    private transient ItemStack castStack = ItemStack.EMPTY;
    private transient ItemStack outputStack = ItemStack.EMPTY;

    @Override
    public void build(Level level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        super.build(level, entry, builder, pageNum);
        this.castStack = RecipePageRender.resolveItem(cast);
        this.outputStack = RecipePageRender.resolveItem(output);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawTitle(graphics);
        drawItem(graphics, castStack, 8, 22);
        drawText(graphics, RecipePageRender.fluidLabel(fluid, amount), 8, 42);
        drawText(graphics, Component.literal("->"), 44, 26);
        drawItem(graphics, outputStack, 88, 22);
        drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".smeltery.cooling", RecipePageRender.formatSeconds(coolingTime)), 8, 60);
    }
}
