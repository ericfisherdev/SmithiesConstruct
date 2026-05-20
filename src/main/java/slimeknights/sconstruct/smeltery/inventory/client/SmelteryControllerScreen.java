package slimeknights.sconstruct.smeltery.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.systems.RenderSystem;

import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;
import slimeknights.sconstruct.smeltery.inventory.SmelteryControllerMenu;
import slimeknights.sconstruct.smeltery.inventory.client.widget.FuelGaugeWidget;
import slimeknights.sconstruct.smeltery.inventory.client.widget.MeltProgressOverlayWidget;
import slimeknights.sconstruct.smeltery.inventory.client.widget.SmelteryScrollWidget;
import slimeknights.sconstruct.smeltery.inventory.client.widget.TankGaugeWidget;
import slimeknights.sconstruct.smeltery.network.SmelteryScrollPayload;

/**
 * Client-side {@link AbstractContainerScreen} for the smeltery controller (SMTCON-125 /
 * SMTCON-216). Over the standard container background it draws the smeltery's live state — a
 * vertical tank gauge filled to the molten metal's level, the current internal temperature, a
 * progress bar across every melting slot with a melt in flight, and (when the inventory exceeds
 * the visible grid) a scrollbar.
 *
 * <p>All four of those overlays live in standalone widget classes under
 * {@link slimeknights.sconstruct.smeltery.inventory.client.widget} (SMTCON-223) so the future
 * single-block melter and alloy-furnace screens can compose them without copy-paste. This screen
 * is now a thin assembly of those widgets plus the container background and tooltip routing.
 *
 * <p>TODO(SMTCON-125 follow-up): the bespoke {@code sconstruct:textures/gui/smeltery.png}
 * background has not been authored yet. Until the artist drops it in, {@link #BACKGROUND} points
 * at the vanilla generic 54-slot container sprite so the screen renders a real background rather
 * than the missing-texture sprite — the same temporary-art rationale as {@code ToolStationScreen}.
 */
public class SmelteryControllerScreen extends AbstractContainerScreen<SmelteryControllerMenu> {

    /** Temporary background sprite — replaced when bespoke art lands. */
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    /** Top border + slot-area height blitted from the source sprite's top. */
    private static final int TOP_SECTION_H = 70;
    /** Player-inventory band height. */
    private static final int BOTTOM_SECTION_H = 96;
    /** Source y of the player-inventory band in the vanilla generic_54 sprite. */
    private static final int SOURCE_PLAYER_INV_Y = 126;

    /** Tank gauge geometry, relative to the screen's top-left. */
    private static final int TANK_X = 8;
    private static final int TANK_Y = 17;
    private static final int TANK_W = 16;
    private static final int TANK_H = 54;

    /** Melting-slot grid geometry — must match {@link SmelteryControllerMenu}. */
    private static final int SLOT_PITCH = 18;
    private static final int MELTING_X = SmelteryControllerMenu.MELTING_X;
    private static final int MELTING_Y = SmelteryControllerMenu.MELTING_Y;
    private static final int SLOT_INNER = 16;
    private static final int PROGRESS_BAR_H = 3;

    /** Scrollbar geometry, relative to the screen's top-left. */
    private static final int SCROLLBAR_X = MELTING_X + SmelteryControllerMenu.MELTING_COLS * SLOT_PITCH + 2;
    private static final int SCROLLBAR_Y = MELTING_Y;
    private static final int SCROLLBAR_W = 12;
    private static final int SCROLLBAR_H = SmelteryControllerMenu.VISIBLE_ROWS * SLOT_PITCH;
    private static final int THUMB_H = 15;

    /** Temperature label position, container-relative — {@link FuelGaugeWidget}'s anchor. */
    private static final int TEMPERATURE_LABEL_X = MELTING_X;
    private static final int TEMPERATURE_LABEL_Y = 6;

    /** Vanilla's left-mouse-button code — matches {@code InputConstants.MOUSE_BUTTON_LEFT}. */
    private static final int LEFT_MOUSE_BUTTON = 0;

    private final TankGaugeWidget tankGauge;
    private final MeltProgressOverlayWidget meltProgress;
    private final SmelteryScrollWidget scrollbar;
    private final FuelGaugeWidget temperatureLabel;

    public SmelteryControllerScreen(SmelteryControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.tankGauge = new TankGaugeWidget(TANK_X, TANK_Y, TANK_W, TANK_H, this::tankFluid, this::tankCapacity);
        this.meltProgress = new MeltProgressOverlayWidget(MELTING_X, MELTING_Y, SLOT_PITCH, SLOT_INNER, PROGRESS_BAR_H, SmelteryControllerMenu.MELTING_COLS, SmelteryControllerMenu.VISIBLE_SLOTS,
                menu::getMeltProgress);
        this.scrollbar = new SmelteryScrollWidget(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_W, SCROLLBAR_H, THUMB_H, menu::maxScrollRow, menu::getScrollRow, this::applyScrollRow);
        this.temperatureLabel = new FuelGaugeWidget(TEMPERATURE_LABEL_X, TEMPERATURE_LABEL_Y, menu::getCurrentTemperature, "gui.sconstruct.smeltery.temperature");
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Two-pass blit of the taller generic_54 sprite — see ToolStationScreen for the rationale.
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, TOP_SECTION_H);
        guiGraphics.blit(BACKGROUND, x, y + TOP_SECTION_H, 0, SOURCE_PLAYER_INV_Y, this.imageWidth, BOTTOM_SECTION_H);
        tankGauge.render(guiGraphics, x, y);
        meltProgress.render(guiGraphics, x, y);
        scrollbar.render(guiGraphics, x, y);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Only consume the wheel when the cursor is actually over the scrollbar — otherwise we
        // trample legitimate scroll targets (JEI overlay, chat) that the screen does not own.
        if (scrollbar.isMouseOver(mouseX, mouseY, this.leftPos, this.topPos) && scrollbar.mouseScrolled(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Only left-click should seize the scrollbar drag — right and middle have other meanings
        // in container screens (pick-half / pick-stack, mouse-bound features) we must not eat.
        if (button == LEFT_MOUSE_BUTTON && scrollbar.mouseClicked(mouseX, mouseY, this.leftPos, this.topPos)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.mouseDragged(mouseY, this.topPos)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollbar.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Applies a scroll-row change locally and reports it to the server. */
    private void applyScrollRow(int row) {
        menu.setScrollRow(row);
        PacketDistributor.sendToServer(new SmelteryScrollPayload(menu.containerId, row));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        temperatureLabel.render(guiGraphics, this.font);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (tankGauge.isMouseOver(mouseX, mouseY, this.leftPos, this.topPos)) {
            tankGauge.renderTooltip(guiGraphics, this.font, mouseX, mouseY, "gui.sconstruct.smeltery.tank_empty", "gui.sconstruct.smeltery.tank_fluid");
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** The fluid currently in the controller's tank, or {@link FluidStack#EMPTY} on a client stub. */
    private FluidStack tankFluid() {
        SmelteryControllerBlockEntity controller = menu.getController();
        return controller == null ? FluidStack.EMPTY : controller.getFluidHandler().getFluidInTank(0);
    }

    /** The tank capacity in mB, or the initial fallback on a client stub. */
    private int tankCapacity() {
        SmelteryControllerBlockEntity controller = menu.getController();
        return controller == null ? SmelteryControllerBlockEntity.INITIAL_TANK_CAPACITY : controller.getFluidHandler().getTankCapacity(0);
    }
}
