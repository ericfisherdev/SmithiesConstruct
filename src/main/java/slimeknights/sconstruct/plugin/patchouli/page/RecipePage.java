package slimeknights.sconstruct.plugin.patchouli.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.google.gson.annotations.SerializedName;

import vazkii.patchouli.client.book.BookPage;

/**
 * Shared base for the three custom smeltery Patchouli pages — {@link PageMelting},
 * {@link PageCasting}, and {@link PageAlloy}. Patchouli deserialises each page's JSON object
 * straight into the concrete subclass, so the recipe fields live on the subclasses; this base
 * only carries the common optional title and the layout primitives every page draws with.
 *
 * <p>All coordinates passed to the {@code draw*} helpers are page-relative — the helpers add
 * the {@link BookPage#left}/{@link BookPage#top} page origin so subclasses lay content out as
 * if the page started at {@code (0, 0)}.
 */
abstract class RecipePage extends BookPage {

    /** Dark grey — the conventional Patchouli on-page text colour. */
    protected static final int TEXT_COLOR = 0x404040;
    /** Patchouli page content width, used to centre the title. */
    protected static final int PAGE_WIDTH = 116;

    /** Optional page title; an i18n key when the book sets {@code i18n: true}. */
    @SerializedName("title")
    protected String title = "";

    /** Draws the page title centred at the top, when one is set. */
    protected void drawTitle(GuiGraphics graphics) {
        if (title == null || title.isBlank()) {
            return;
        }
        Component text = i18nText(title);
        int x = (PAGE_WIDTH - fontRenderer.width(text)) / 2;
        graphics.drawString(fontRenderer, text, left + x, top, TEXT_COLOR, false);
    }

    /** Renders an item stack (with its count/damage decorations) at a page-relative position. */
    protected void drawItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, left + x, top + y);
        graphics.renderItemDecorations(fontRenderer, stack, left + x, top + y);
    }

    /** Draws a line of text at a page-relative position in the standard page colour. */
    protected void drawText(GuiGraphics graphics, Component text, int x, int y) {
        graphics.drawString(fontRenderer, text, left + x, top + y, TEXT_COLOR, false);
    }
}
