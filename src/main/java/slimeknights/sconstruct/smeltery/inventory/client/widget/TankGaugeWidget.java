package slimeknights.sconstruct.smeltery.inventory.client.widget;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Vertical tank gauge widget (SMTCON-223 / SMTCON-234) — draws an empty frame filled from the
 * bottom with the tank's fluids, each fluid stacked as its own coloured layer scaled to its
 * share of the tank capacity. Extracted from {@code SmelteryControllerScreen} so a future
 * single-block melter or alloy-furnace screen can reuse the same gauge without copy-pasting the
 * fill math, the long-overflow guard, or the tooltip.
 *
 * <p>The gauge mirrors the in-world interior renderer's multi-fluid layering (SMTCON-221): the
 * smeltery tank can hold several molten metals at once, and the gauge shows every one of them
 * bottom-up in tank order rather than only the first.
 *
 * <p>Positions are screen-relative — the host screen passes its {@code leftPos}/{@code topPos}
 * to {@link #render} and {@link #renderTooltip} every frame so the widget can compose against
 * the {@code AbstractContainerScreen} background without owning a position state.
 */
public final class TankGaugeWidget {

    /** Tint applied to the empty-frame fill — opaque dark grey, matches the legacy screen colour. */
    private static final int FRAME_COLOR = 0xFF3A3A3A;

    /** Alpha bits OR-ed into the fluid's tint so a colour with no alpha channel still draws. */
    private static final int OPAQUE_ALPHA = 0xFF000000;

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final Supplier<List<FluidStack>> fluidsSupplier;
    private final IntSupplier capacitySupplier;

    /**
     * @param x                screen-relative left edge of the gauge in GUI pixels
     * @param y                screen-relative top edge of the gauge in GUI pixels
     * @param width            gauge width in GUI pixels
     * @param height           gauge height in GUI pixels — fluids fill from {@code y+height} upward
     * @param fluidsSupplier   returns the fluids to display this frame, bottom-up in tank order;
     *                         an empty list draws frame only
     * @param capacitySupplier returns the tank capacity in mB; non-positive draws frame only
     */
    public TankGaugeWidget(int x, int y, int width, int height, Supplier<List<FluidStack>> fluidsSupplier, IntSupplier capacitySupplier) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fluidsSupplier = Objects.requireNonNull(fluidsSupplier, "fluidsSupplier");
        this.capacitySupplier = Objects.requireNonNull(capacitySupplier, "capacitySupplier");
    }

    /** Draws the gauge frame and a stacked layer per fluid, translated by the host screen's origin. */
    public void render(GuiGraphics guiGraphics, int screenLeft, int screenTop) {
        int left = screenLeft + x;
        int top = screenTop + y;
        guiGraphics.fill(left, top, left + width, top + height, FRAME_COLOR);
        int capacity = capacitySupplier.getAsInt();
        if (capacity <= 0) {
            return;
        }
        // Walk the fluids bottom-up. Each layer's top is the cumulative fill height — the pixel
        // height of every fluid so far — rather than this layer's own height floored in
        // isolation. Flooring per layer would let several layers' rounding losses accumulate and
        // leave an empty seam at the top of a tank that is actually full; carving each layer from
        // the running cumulative target keeps the topmost edge exact.
        int filledFromBottom = 0;
        long cumulativeAmount = 0L;
        for (FluidStack fluid : fluidsSupplier.get()) {
            if (fluid.isEmpty()) {
                continue;
            }
            cumulativeAmount += fluid.getAmount();
            // Long math — a large cumulative amount times the gauge height could overflow a plain int.
            int targetFilled = (int) Math.min(height, cumulativeAmount * (long) height / capacity);
            int layerHeight = targetFilled - filledFromBottom;
            if (layerHeight <= 0) {
                continue;
            }
            int tint = OPAQUE_ALPHA | IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
            int layerTop = top + height - targetFilled;
            guiGraphics.fill(left, layerTop, left + width, top + height - filledFromBottom, tint);
            filledFromBottom = targetFilled;
            if (filledFromBottom >= height) {
                break;
            }
        }
    }

    /** Whether {@code (mouseX, mouseY)} sits inside the gauge's screen-relative bounds. */
    public boolean isMouseOver(double mouseX, double mouseY, int screenLeft, int screenTop) {
        int left = screenLeft + x;
        int top = screenTop + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    /**
     * Renders the tank-contents tooltip — one line per fluid in the tank, or the empty label
     * when the tank holds nothing. The host screen is responsible for guarding the call on
     * {@link #isMouseOver}.
     *
     * @param emptyLabel the label to show when the tank is empty
     * @param fluidLabel the format key for each fluid line; receives {@code (name, amountMb)}
     */
    public void renderTooltip(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY, String emptyLabel, String fluidLabel) {
        List<Component> lines = fluidsSupplier.get().stream().filter(fluid -> !fluid.isEmpty()).<Component> map(fluid -> Component.translatable(fluidLabel, fluid.getHoverName(), fluid.getAmount()))
                .toList();
        if (lines.isEmpty()) {
            guiGraphics.renderTooltip(font, Component.translatable(emptyLabel), mouseX, mouseY);
        }
        else {
            guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
