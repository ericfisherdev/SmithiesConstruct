package slimeknights.sconstruct.smeltery.inventory.client.widget;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Vertical scrollbar widget (SMTCON-223) — draws a track + thumb only when the host supplier
 * reports more than one row to scroll, and handles the wheel / click / drag interactions
 * (host screen forwards its mouse events through {@link #mouseScrolled},
 * {@link #mouseClicked}, {@link #mouseDragged}, {@link #mouseReleased}). Extracted from
 * {@code SmelteryControllerScreen} so a future single-block furnace screen with a similar
 * over-sized inventory can reuse the same scrollbar without copy-pasting the geometry.
 *
 * <p>Row management is owned by the host via the three callbacks the constructor takes — the
 * widget itself stores no scroll state beyond the live {@link #dragging} flag. The host's
 * supplier returns the maximum row, the current row, and {@link IntConsumer#accept setRow}
 * applies a change; the widget never persists or syncs.
 */
public final class SmelteryScrollWidget {

    /** Track tint — opaque dark grey, matches the legacy screen colour. */
    private static final int TRACK_COLOR = 0xFF202020;

    /** Thumb tint — opaque light grey, matches the legacy screen colour. */
    private static final int THUMB_COLOR = 0xFFC0C0C0;

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int thumbHeight;
    private final IntSupplier maxRowSupplier;
    private final IntSupplier currentRowSupplier;
    private final IntConsumer rowSetter;

    private boolean dragging;

    /**
     * @param x                  screen-relative left edge of the scrollbar in GUI pixels
     * @param y                  screen-relative top edge of the track
     * @param width              track / thumb width
     * @param height             track height; the thumb travels {@code height - thumbHeight} pixels
     * @param thumbHeight        thumb height in GUI pixels
     * @param maxRowSupplier     returns the highest valid row; non-positive hides the scrollbar
     * @param currentRowSupplier returns the current row, clamped to {@code [0, maxRow]}
     * @param rowSetter          called with the new row when the widget changes the scroll position
     */
    public SmelteryScrollWidget(int x, int y, int width, int height, int thumbHeight, IntSupplier maxRowSupplier, IntSupplier currentRowSupplier, IntConsumer rowSetter) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.thumbHeight = thumbHeight;
        this.maxRowSupplier = Objects.requireNonNull(maxRowSupplier, "maxRowSupplier");
        this.currentRowSupplier = Objects.requireNonNull(currentRowSupplier, "currentRowSupplier");
        this.rowSetter = Objects.requireNonNull(rowSetter, "rowSetter");
    }

    /** Draws track + thumb when there is more than one row to scroll. */
    public void render(GuiGraphics guiGraphics, int screenLeft, int screenTop) {
        int maxRow = maxRowSupplier.getAsInt();
        if (maxRow <= 0) {
            return;
        }
        int left = screenLeft + x;
        int top = screenTop + y;
        guiGraphics.fill(left, top, left + width, top + height, TRACK_COLOR);
        int thumbTop = top + thumbOffset(maxRow);
        guiGraphics.fill(left, thumbTop, left + width, thumbTop + thumbHeight, THUMB_COLOR);
    }

    /** The thumb top, in pixels below the track top, for the current row over {@code maxRow}. */
    private int thumbOffset(int maxRow) {
        int travel = height - thumbHeight;
        if (travel <= 0 || maxRow <= 0) {
            return 0;
        }
        return travel * Math.clamp(currentRowSupplier.getAsInt(), 0, maxRow) / maxRow;
    }

    /**
     * Forwards a mouse-wheel event. Positive {@code scrollY} scrolls toward row {@code 0}
     * (matches every vanilla container screen). Returns whether the widget consumed the event.
     */
    public boolean mouseScrolled(double scrollY) {
        int maxRow = maxRowSupplier.getAsInt();
        if (maxRow <= 0 || scrollY == 0.0) {
            return false;
        }
        moveBy(-(int) Math.signum(scrollY));
        return true;
    }

    /**
     * Forwards a mouse-click event. When the click lands on the track the thumb jumps to the
     * mouse and the widget enters dragging mode until {@link #mouseReleased}. Returns whether
     * the widget consumed the event.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int screenLeft, int screenTop) {
        if (maxRowSupplier.getAsInt() <= 0 || !isMouseOver(mouseX, mouseY, screenLeft, screenTop)) {
            return false;
        }
        dragging = true;
        scrollToMouse(mouseY, screenTop);
        return true;
    }

    /**
     * Forwards a mouse-drag event. While {@link #dragging} the thumb tracks the cursor; otherwise
     * a no-op returning {@code false}. Returns whether the widget consumed the event.
     */
    public boolean mouseDragged(double mouseY, int screenTop) {
        if (!dragging) {
            return false;
        }
        scrollToMouse(mouseY, screenTop);
        return true;
    }

    /** Clears the dragging flag. Always returns {@code false} so other handlers still see the release. */
    public boolean mouseReleased() {
        dragging = false;
        return false;
    }

    /** Whether {@code (mouseX, mouseY)} sits inside the scrollbar track. */
    public boolean isMouseOver(double mouseX, double mouseY, int screenLeft, int screenTop) {
        int left = screenLeft + x;
        int top = screenTop + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    /** Scrolls so the thumb centre tracks {@code mouseY}, clamped to the row range. */
    private void scrollToMouse(double mouseY, int screenTop) {
        int trackTop = screenTop + y;
        int travel = height - thumbHeight;
        int maxRow = maxRowSupplier.getAsInt();
        double fraction = travel <= 0 ? 0 : Math.clamp((mouseY - trackTop - thumbHeight / 2.0) / travel, 0.0, 1.0);
        applyRow((int) Math.round(fraction * maxRow));
    }

    /** Moves the current row by {@code delta} after clamping to the live range. */
    private void moveBy(int delta) {
        applyRow(currentRowSupplier.getAsInt() + delta);
    }

    /** Clamps {@code row} into {@code [0, maxRow]} and forwards it through the host's setter. */
    private void applyRow(int row) {
        int clamped = Math.clamp(row, 0, maxRowSupplier.getAsInt());
        if (clamped != currentRowSupplier.getAsInt()) {
            rowSetter.accept(clamped);
        }
    }
}
