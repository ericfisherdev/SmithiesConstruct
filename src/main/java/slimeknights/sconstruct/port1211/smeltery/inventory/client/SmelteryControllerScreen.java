package slimeknights.sconstruct.port1211.smeltery.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.blaze3d.systems.RenderSystem;

import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.inventory.SmelteryControllerMenu;

/**
 * Client-side {@link AbstractContainerScreen} for the smeltery controller (SMTCON-125). Over the
 * standard container background it draws the smeltery's live state: a vertical tank gauge filled
 * to the molten metal's level and tinted with its colour, the current internal temperature, and
 * a progress bar across every melting slot that has a melt in flight.
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

    /** Melting-slot grid geometry — must match {@code SmelteryControllerMenu}. */
    private static final int MELTING_GRID = 3;
    private static final int SLOT_PITCH = 18;
    private static final int MELTING_X = 62;
    private static final int MELTING_Y = 17;
    private static final int SLOT_INNER = 16;
    private static final int PROGRESS_BAR_H = 3;
    private static final int PROGRESS_BAR_COLOR = 0xFF4CAF50;
    private static final int PERCENT = 100;

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

    /** Draws a progress bar along the bottom of every melting slot with a melt in flight. */
    private void renderMeltProgress(GuiGraphics guiGraphics, int left, int top) {
        for (int slot = 0; slot < MELTING_GRID * MELTING_GRID; slot++) {
            int progress = menu.getMeltProgress(slot);
            if (progress <= 0) {
                continue;
            }
            int col = slot % MELTING_GRID;
            int row = slot / MELTING_GRID;
            int slotLeft = left + MELTING_X + col * SLOT_PITCH;
            int slotBottom = top + MELTING_Y + row * SLOT_PITCH + SLOT_INNER;
            int filled = Math.min(SLOT_INNER, SLOT_INNER * progress / PERCENT);
            guiGraphics.fill(slotLeft, slotBottom - PROGRESS_BAR_H, slotLeft + filled, slotBottom, PROGRESS_BAR_COLOR);
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
