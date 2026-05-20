package slimeknights.sconstruct.smeltery.inventory.client.widget;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Per-slot melt-progress overlay (SMTCON-223) — draws a thin progress bar across the bottom of
 * every visible melting slot whose backing melt is in flight. Extracted from
 * {@code SmelteryControllerScreen} so the same overlay can paint over any
 * slot-grid screen that maps visible-slot indices to active melts via the
 * {@link IntUnaryOperator} {@code progressSupplier}.
 *
 * <p>Slot indices are in row-major order ({@code col + row * columns}); the host screen is
 * responsible for matching that to its own visible-slot layout.
 */
public final class MeltProgressOverlayWidget {

    /** Filled-bar tint — opaque green, matches the legacy screen colour. */
    private static final int PROGRESS_BAR_COLOR = 0xFF4CAF50;

    /** Whole-percent divisor — progress in {@code [0, 100]} maps onto slot width. */
    private static final int PERCENT = 100;

    private final int gridX;
    private final int gridY;
    private final int slotPitch;
    private final int slotInner;
    private final int barHeight;
    private final int columns;
    private final int visibleSlots;
    private final IntUnaryOperator progressSupplier;

    /**
     * @param gridX            screen-relative left edge of the slot grid, matching the menu's layout
     * @param gridY            screen-relative top edge of the slot grid
     * @param slotPitch        column / row stride in GUI pixels — vanilla slots are 18
     * @param slotInner        the slot's inner edge length, vanilla 16
     * @param barHeight        progress-bar thickness in GUI pixels
     * @param columns          number of slot columns in the visible grid
     * @param visibleSlots     total visible-slot count — bar painted across {@code [0, visibleSlots)}
     * @param progressSupplier maps a visible-slot index to its melt's progress in {@code [0, 100]};
     *                         {@code 0} skips the slot
     */
    public MeltProgressOverlayWidget(int gridX, int gridY, int slotPitch, int slotInner, int barHeight, int columns, int visibleSlots, IntUnaryOperator progressSupplier) {
        // Fail-fast on bad geometry — columns is the divisor in the slot-index math (col = visible
        // % columns) and a non-positive slotInner/barHeight produces zero-area or inverted bars.
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be > 0, was " + columns);
        }
        if (slotPitch <= 0 || slotInner <= 0 || barHeight <= 0) {
            throw new IllegalArgumentException("slotPitch / slotInner / barHeight must all be > 0");
        }
        if (visibleSlots < 0) {
            throw new IllegalArgumentException("visibleSlots must be >= 0, was " + visibleSlots);
        }
        this.gridX = gridX;
        this.gridY = gridY;
        this.slotPitch = slotPitch;
        this.slotInner = slotInner;
        this.barHeight = barHeight;
        this.columns = columns;
        this.visibleSlots = visibleSlots;
        this.progressSupplier = Objects.requireNonNull(progressSupplier, "progressSupplier");
    }

    /** Paints the progress bars over each visible slot whose progress is positive. */
    public void render(GuiGraphics guiGraphics, int screenLeft, int screenTop) {
        for (int visible = 0; visible < visibleSlots; visible++) {
            int progress = progressSupplier.applyAsInt(visible);
            if (progress <= 0) {
                continue;
            }
            int col = visible % columns;
            int row = visible / columns;
            int slotLeft = screenLeft + gridX + col * slotPitch;
            int slotBottom = screenTop + gridY + row * slotPitch + slotInner;
            int filled = Math.min(slotInner, slotInner * progress / PERCENT);
            guiGraphics.fill(slotLeft, slotBottom - barHeight, slotLeft + filled, slotBottom, PROGRESS_BAR_COLOR);
        }
    }
}
