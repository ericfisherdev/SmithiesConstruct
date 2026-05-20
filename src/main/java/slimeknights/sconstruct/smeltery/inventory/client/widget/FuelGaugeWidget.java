package slimeknights.sconstruct.smeltery.inventory.client.widget;

import java.util.Objects;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Burner-temperature widget (SMTCON-223) — renders the current smeltery temperature as a label,
 * positioned by the host screen. The widget exists in its own class so the future single-block
 * melter and alloy-furnace screens can reuse the temperature display verbatim.
 *
 * <p>This is intentionally a label rather than a graphical bar today. A heat-bar variant would
 * need the {@link slimeknights.sconstruct.smeltery.block.entity.module.SmelteryFuelModule}'s
 * remaining-ticks state, which the controller does not yet expose; once it does, this widget is
 * the seam to swap the label for a sprite-driven bar without changing the screen layout.
 */
public final class FuelGaugeWidget {

    /** Foreground colour for the temperature label — matches vanilla container labels. */
    private static final int LABEL_COLOR = 0x404040;

    private final int x;
    private final int y;
    private final IntSupplier temperatureSupplier;
    private final String labelKey;

    /**
     * @param x                   container-relative left edge passed straight to the GUI
     * @param y                   container-relative top edge
     * @param temperatureSupplier returns the temperature in kelvin to render
     * @param labelKey            translation key whose format argument is the supplied temperature
     */
    public FuelGaugeWidget(int x, int y, IntSupplier temperatureSupplier, String labelKey) {
        this.x = x;
        this.y = y;
        this.temperatureSupplier = Objects.requireNonNull(temperatureSupplier, "temperatureSupplier");
        this.labelKey = Objects.requireNonNull(labelKey, "labelKey");
    }

    /**
     * Renders the temperature label. Call from {@code Screen.renderLabels} — the pose stack the
     * container screen pushes there already includes the {@code (leftPos, topPos)} translation,
     * so the widget's coordinates are container-relative and no screen origin is needed.
     */
    public void render(GuiGraphics guiGraphics, Font font) {
        guiGraphics.drawString(font, Component.translatable(labelKey, temperatureSupplier.getAsInt()), x, y, LABEL_COLOR, false);
    }
}
