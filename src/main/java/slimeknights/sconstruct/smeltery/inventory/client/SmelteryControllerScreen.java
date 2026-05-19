package slimeknights.sconstruct.smeltery.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.systems.RenderSystem;

import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;
import slimeknights.sconstruct.smeltery.inventory.SmelteryControllerMenu;
import slimeknights.sconstruct.smeltery.network.SmelteryScrollPayload;

/**
 * Client-side {@link AbstractContainerScreen} for the smeltery controller (SMTCON-125 /
 * SMTCON-216). Over the standard container background it draws the smeltery's live state: a
 * vertical tank gauge filled to the molten metal's level and tinted with its colour, the current
 * internal temperature, and a progress bar across every melting slot with a melt in flight.
 *
 * <p>The controller's melting inventory is sized to the smeltery's interior volume, so it can
 * exceed the 3&times;3 grid the screen shows. When it does, a scrollbar appears beside the grid;
 * the mouse wheel and the dragged thumb both move the {@link SmelteryControllerMenu}'s visible
 * window, and {@link SmelteryScrollPayload} mirrors the offset to the server so slot clicks and
 * shift-clicks land on the right inventory slot.
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
    private static final int PROGRESS_BAR_COLOR = 0xFF4CAF50;
    private static final int PERCENT = 100;

    /** Scrollbar geometry, relative to the screen's top-left. */
    private static final int SCROLLBAR_X = MELTING_X + SmelteryControllerMenu.MELTING_COLS * SLOT_PITCH + 2;
    private static final int SCROLLBAR_Y = MELTING_Y;
    private static final int SCROLLBAR_W = 12;
    private static final int SCROLLBAR_H = SmelteryControllerMenu.VISIBLE_ROWS * SLOT_PITCH;
    private static final int THUMB_H = 15;
    private static final int TRACK_COLOR = 0xFF202020;
    private static final int THUMB_COLOR = 0xFFC0C0C0;

    /** Whether the scrollbar thumb is currently being dragged. */
    private boolean scrolling;

    public SmelteryControllerScreen(SmelteryControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Two-pass blit of the taller generic_54 sprite — see ToolStationScreen for the rationale.
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, TOP_SECTION_H);
        guiGraphics.blit(BACKGROUND, x, y + TOP_SECTION_H, 0, SOURCE_PLAYER_INV_Y, this.imageWidth, BOTTOM_SECTION_H);
        renderTank(guiGraphics, x, y);
        renderMeltProgress(guiGraphics, x, y);
        renderScrollbar(guiGraphics, x, y);
    }

    /** Draws the tank gauge — an empty frame filled from the bottom with the molten metal's tint. */
    private void renderTank(GuiGraphics guiGraphics, int left, int top) {
        int tankLeft = left + TANK_X;
        int tankTop = top + TANK_Y;
        guiGraphics.fill(tankLeft, tankTop, tankLeft + TANK_W, tankTop + TANK_H, 0xFF3A3A3A);
        FluidStack fluid = tankFluid();
        if (fluid.isEmpty()) {
            return;
        }
        int capacity = menu.getController() == null ? SmelteryControllerBlockEntity.INITIAL_TANK_CAPACITY : menu.getController().getFluidHandler().getTankCapacity(0);
        // Long math — a large tank capacity times TANK_H could overflow a plain int multiply.
        int fillHeight = capacity <= 0 ? 0 : (int) Math.min(TANK_H, (long) fluid.getAmount() * TANK_H / capacity);
        int tint = 0xFF000000 | IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        guiGraphics.fill(tankLeft, tankTop + TANK_H - fillHeight, tankLeft + TANK_W, tankTop + TANK_H, tint);
    }

    /** Draws a progress bar along the bottom of every visible melting slot with a melt in flight. */
    private void renderMeltProgress(GuiGraphics guiGraphics, int left, int top) {
        for (int visible = 0; visible < SmelteryControllerMenu.VISIBLE_SLOTS; visible++) {
            int progress = menu.getMeltProgress(visible);
            if (progress <= 0) {
                continue;
            }
            int col = visible % SmelteryControllerMenu.MELTING_COLS;
            int row = visible / SmelteryControllerMenu.MELTING_COLS;
            int slotLeft = left + MELTING_X + col * SLOT_PITCH;
            int slotBottom = top + MELTING_Y + row * SLOT_PITCH + SLOT_INNER;
            int filled = Math.min(SLOT_INNER, SLOT_INNER * progress / PERCENT);
            guiGraphics.fill(slotLeft, slotBottom - PROGRESS_BAR_H, slotLeft + filled, slotBottom, PROGRESS_BAR_COLOR);
        }
    }

    /** Draws the scrollbar track and thumb — only when the melting inventory exceeds the window. */
    private void renderScrollbar(GuiGraphics guiGraphics, int left, int top) {
        if (menu.maxScrollRow() <= 0) {
            return;
        }
        int barLeft = left + SCROLLBAR_X;
        int barTop = top + SCROLLBAR_Y;
        guiGraphics.fill(barLeft, barTop, barLeft + SCROLLBAR_W, barTop + SCROLLBAR_H, TRACK_COLOR);
        int thumbTop = barTop + thumbOffset();
        guiGraphics.fill(barLeft, thumbTop, barLeft + SCROLLBAR_W, thumbTop + THUMB_H, THUMB_COLOR);
    }

    /** The thumb's top, in pixels below the track top, for the menu's current scroll row. */
    private int thumbOffset() {
        int max = menu.maxScrollRow();
        return max <= 0 ? 0 : (SCROLLBAR_H - THUMB_H) * menu.getScrollRow() / max;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.maxScrollRow() > 0 && scrollY != 0) {
            // Wheel up (positive) scrolls toward the top — a lower row index.
            scrollTo(menu.getScrollRow() - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.maxScrollRow() > 0 && overScrollbar(mouseX, mouseY)) {
            scrolling = true;
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling) {
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Whether {@code (mouseX, mouseY)} is inside the scrollbar track. */
    private boolean overScrollbar(double mouseX, double mouseY) {
        int barLeft = this.leftPos + SCROLLBAR_X;
        int barTop = this.topPos + SCROLLBAR_Y;
        return mouseX >= barLeft && mouseX < barLeft + SCROLLBAR_W && mouseY >= barTop && mouseY < barTop + SCROLLBAR_H;
    }

    /** Scrolls so the thumb centre tracks {@code mouseY}. */
    private void scrollToMouse(double mouseY) {
        int trackTop = this.topPos + SCROLLBAR_Y;
        int travel = SCROLLBAR_H - THUMB_H;
        double fraction = travel <= 0 ? 0 : Math.clamp((mouseY - trackTop - THUMB_H / 2.0) / travel, 0.0, 1.0);
        scrollTo((int) Math.round(fraction * menu.maxScrollRow()));
    }

    /** Applies a scroll-row change locally and reports it to the server. */
    private void scrollTo(int row) {
        int clamped = Math.clamp(row, 0, menu.maxScrollRow());
        if (clamped != menu.getScrollRow()) {
            menu.setScrollRow(clamped);
            PacketDistributor.sendToServer(new SmelteryScrollPayload(menu.containerId, clamped));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(this.font, Component.translatable("gui.sconstruct.smeltery.temperature", menu.getCurrentTemperature()), MELTING_X, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTankTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** Shows the fluid name and amount when the cursor hovers the tank gauge. */
    private void renderTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int tankLeft = this.leftPos + TANK_X;
        int tankTop = this.topPos + TANK_Y;
        if (mouseX < tankLeft || mouseX >= tankLeft + TANK_W || mouseY < tankTop || mouseY >= tankTop + TANK_H) {
            return;
        }
        FluidStack fluid = tankFluid();
        Component line = fluid.isEmpty() ? Component.translatable("gui.sconstruct.smeltery.tank_empty")
                : Component.translatable("gui.sconstruct.smeltery.tank_fluid", fluid.getHoverName(), fluid.getAmount());
        guiGraphics.renderTooltip(this.font, line, mouseX, mouseY);
    }

    /** The fluid currently in the controller's tank, or {@link FluidStack#EMPTY} on a client stub. */
    private FluidStack tankFluid() {
        SmelteryControllerBlockEntity controller = menu.getController();
        return controller == null ? FluidStack.EMPTY : controller.getFluidHandler().getFluidInTank(0);
    }
}
