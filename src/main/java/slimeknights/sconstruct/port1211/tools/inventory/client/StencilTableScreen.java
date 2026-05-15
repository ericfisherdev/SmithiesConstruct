package slimeknights.sconstruct.port1211.tools.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.systems.RenderSystem;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.network.StencilTablePartPayload;
import slimeknights.sconstruct.port1211.tools.block.entity.StencilTableBlockEntity;
import slimeknights.sconstruct.port1211.tools.inventory.StencilTableMenu;

/**
 * Client-side {@link AbstractContainerScreen} for the Stencil Table (SMTCON-91). Renders the
 * standard 176×166 container background plus two arrow buttons above the input slot — left
 * cycles to the previous {@link slimeknights.sconstruct.port1211.tools.PartType}, right cycles
 * to the next. Each click dispatches a {@link StencilTablePartPayload} packet to the server,
 * which mutates the BE's cursor and re-stamps the output slot.
 *
 * <p>TODO(SMTCON-91 follow-up): the background PNG at {@link #BACKGROUND} does not yet exist
 * on disk. Vanilla falls back to the missing-texture sprite — the screen still opens and
 * interacts correctly; only the visual quality suffers until the artist drops in the PNG.
 */
public class StencilTableScreen extends AbstractContainerScreen<StencilTableMenu> {

    /** Background sprite path. PNG asset is a follow-up per the ticket plan. */
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "textures/gui/container/stencil_table.png");

    /** Cycle button width — vanilla arrow-button width. */
    private static final int BUTTON_W = 14;
    /** Cycle button height. */
    private static final int BUTTON_H = 20;
    /** y offset of the cycle button row above the input slot (input is at y=35). */
    private static final int BUTTON_Y_OFFSET = 14;

    public StencilTableScreen(StencilTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Place the arrow buttons flanking the input slot horizontally, one row above. The
        // input slot lives at GUI-local (48, 35) per StencilTableMenu — convert to screen-
        // space by adding leftPos/topPos.
        int leftButtonX = leftPos + 48 - BUTTON_W - 2;
        int rightButtonX = leftPos + 48 + 18 + 2;
        int buttonY = topPos + 35 - BUTTON_Y_OFFSET;
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> sendCycle(-1)).bounds(leftButtonX, buttonY, BUTTON_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> sendCycle(1)).bounds(rightButtonX, buttonY, BUTTON_W, BUTTON_H).build());
    }

    /**
     * Dispatch a {@link StencilTablePartPayload} packet carrying the BE's pos + cycle
     * direction. No-op if the client BE handle is null (server's CloseMenu in flight) —
     * suppresses the packet so the server doesn't log a "pos not a stencil table" warning.
     */
    private void sendCycle(int direction) {
        StencilTableBlockEntity be = getMenu().getBlockEntity();
        if (be == null) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        PacketDistributor.sendToServer(new StencilTablePartPayload(pos, direction));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
