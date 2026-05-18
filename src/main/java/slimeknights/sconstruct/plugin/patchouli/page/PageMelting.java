package slimeknights.sconstruct.plugin.patchouli.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.google.gson.annotations.SerializedName;

import slimeknights.sconstruct.SConstruct;

import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;

/**
 * Custom Patchouli page type {@code tconstruct:melting} (SMTCON-162) — visualises a smeltery
 * melting recipe: an item input on the left, the molten-fluid output on the right, and the
 * required temperature and melt time below.
 *
 * <p>The recipe values are read straight from the page JSON (registered by
 * {@code SmithiesPatchouliPlugin}); a book entry adds a page like
 * {@code {"type": "tconstruct:melting", "input": "minecraft:iron_ingot", "output":
 * "sconstruct:molten_iron", "amount": 90, "temperature": 800, "time": 100}}.
 */
public class PageMelting extends RecipePage {

    @SerializedName("input")
    private String input = "";
    @SerializedName("output")
    private String output = "";
    @SerializedName("amount")
    private int amount;
    @SerializedName("temperature")
    private int temperature;
    @SerializedName("time")
    private int time;

    private transient ItemStack inputStack = ItemStack.EMPTY;

    @Override
    public void build(Level level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        super.build(level, entry, builder, pageNum);
        this.inputStack = RecipePageRender.resolveItem(input);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawTitle(graphics);
        drawItem(graphics, inputStack, 22, 22);
        drawText(graphics, Component.literal("->"), 44, 26);
        drawText(graphics, RecipePageRender.fluidLabel(output, amount), 8, 48);
        drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".smeltery.temperature", temperature), 8, 64);
        drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".smeltery.time", RecipePageRender.formatSeconds(time)), 8, 76);
    }
}
