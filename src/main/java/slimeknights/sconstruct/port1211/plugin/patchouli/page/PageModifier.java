package slimeknights.sconstruct.port1211.plugin.patchouli.page;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import slimeknights.sconstruct.port1211.plugin.jei.category.ModifierCatalog;
import slimeknights.sconstruct.port1211.plugin.jei.category.ModifierEntry;
import slimeknights.sconstruct.port1211.tools.modifier.Modifier;

/**
 * Custom Patchouli page type {@code tconstruct:modifier} (SMTCON-163) — lists every built-in
 * tool modifier with its level cap and slot cost.
 *
 * <p>The roster comes from {@link ModifierCatalog#entries()}, which harvests
 * {@code TinkerModifierBootstrap} code-side, so the page never depends on the server-only
 * modifier datapack cache and renders the same on a remote client. A book entry adds the page
 * with just {@code {"type": "tconstruct:modifier"}}.
 */
public class PageModifier extends RecipePage {

    /** Vertical distance between rows — leaves a little air around the 9px font. */
    private static final int ROW_HEIGHT = 11;
    /** Y of the first modifier row, clear of the title. */
    private static final int FIRST_ROW_Y = 14;
    /** X of the first column; each further column is offset by one column width. */
    private static final int LEFT_COLUMN_X = 4;
    /** The roster is laid out across this many columns. */
    private static final int COLUMNS = 2;
    /** Horizontal distance between column starts, derived from the page width. */
    private static final int COLUMN_WIDTH = PAGE_WIDTH / COLUMNS;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawTitle(graphics);
        List<ModifierEntry> entries = ModifierCatalog.entries();
        // Rows per column is derived from the live roster size rather than hard-coded, so a
        // grown modifier list still fills both columns evenly instead of overflowing one.
        int rowsPerColumn = Math.max(1, (entries.size() + COLUMNS - 1) / COLUMNS);
        for (int i = 0; i < entries.size(); i++) {
            Modifier modifier = entries.get(i).modifier();
            int column = i / rowsPerColumn;
            int row = i % rowsPerColumn;
            int x = LEFT_COLUMN_X + column * COLUMN_WIDTH;
            drawText(graphics, rowLabel(modifier), x, FIRST_ROW_Y + row * ROW_HEIGHT);
        }
    }

    /** {@code "<name> (Lv N, cost N)"} — the modifier's description plus its caps. */
    private static Component rowLabel(Modifier modifier) {
        int maxLevel = modifier.maxLevel();
        return modifier.description(maxLevel).copy().append(Component.literal(" (Lv " + maxLevel + ", cost " + modifier.slotCost() + ")"));
    }
}
