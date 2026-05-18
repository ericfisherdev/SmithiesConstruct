package slimeknights.sconstruct.plugin.patchouli.page;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import com.google.gson.annotations.SerializedName;

import slimeknights.sconstruct.SConstruct;

import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;

/**
 * Custom Patchouli page type {@code tconstruct:alloy} (SMTCON-162) — visualises a smeltery
 * alloy recipe: the molten input fluids stacked on the left, the alloyed-fluid output below
 * them, and the required temperature.
 *
 * <p>A book entry adds a page like {@code {"type": "tconstruct:alloy", "inputs": [{"fluid":
 * "sconstruct:molten_copper", "amount": 270}, {"fluid": "sconstruct:molten_zinc", "amount":
 * 90}], "output": "sconstruct:molten_brass", "amount": 360, "temperature": 800}}.
 */
public class PageAlloy extends RecipePage {

    @SerializedName("inputs")
    private List<FluidEntry> inputs = List.of();
    @SerializedName("output")
    private String output = "";
    @SerializedName("amount")
    private int amount;
    @SerializedName("temperature")
    private int temperature;

    @Override
    public void build(Level level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        super.build(level, entry, builder, pageNum);
        if (inputs == null) {
            inputs = List.of();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawTitle(graphics);
        int y = 22;
        for (FluidEntry in : inputs) {
            drawText(graphics, RecipePageRender.fluidLabel(in.fluid, in.amount), 8, y);
            y += 12;
        }
        drawText(graphics, Component.literal("->"), 8, y + 2);
        drawText(graphics, RecipePageRender.fluidLabel(output, amount), 20, y + 2);
        drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".smeltery.temperature", temperature), 8, y + 18);
    }

    /** One molten input of the alloy — a fluid id plus the millibucket amount consumed. */
    private static final class FluidEntry {

        @SerializedName("fluid")
        private String fluid = "";
        @SerializedName("amount")
        private int amount;
    }
}
