package slimeknights.sconstruct.tools.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.systems.RenderSystem;

import slimeknights.sconstruct.common.network.ToolStationActionPayload;
import slimeknights.sconstruct.tools.block.entity.ToolStationBlockEntity;
import slimeknights.sconstruct.tools.inventory.ToolStationMenu;

/**
 * Client-side {@link AbstractContainerScreen} for the Tool Station and Tool Forge (SMTCON-93).
 * Renders the standard 176×166 container background — the six input slots, the output slot,
 * and the player inventory are drawn by the base class from the {@link ToolStationMenu}
 * layout.
 *
 * <p>The Tool Forge reuses this same screen class (registered against the forge's menu type by
 * {@code ToolStationClient}) — the menu's slot layout and title key already differentiate the
 * two stations.
 *
 * <p>TODO(SMTCON-93 follow-up): bespoke {@code sconstruct:textures/gui/container/tool_station.png}
 * and {@code tool_forge.png} PNGs have not been authored yet. Until the artist drops them in,
 * point {@link #BACKGROUND} at the vanilla generic 54-slot container sprite so the screen
 * renders a real background rather than the magenta missing-texture sprite — same temporary-art
 * rationale as {@link PartBuilderScreen}.
 */
public class ToolStationScreen extends AbstractContainerScreen<ToolStationMenu> {

    /** Temporary background sprite — replaced when bespoke art lands. */
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public ToolStationScreen(ToolStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // SMTCON-94 action buttons. Build commits the input-derived tool; Modify clears the
        // slot-0 input tool. Reach + open-menu validation runs server-side in
        // {@link ToolStationActionPayload#handleServer}; the buttons only fire if the menu's
        // cached BE reference is non-null (the open-screen sync populated it).
        ToolStationBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return;
        }
        int baseX = this.leftPos + 6;
        int baseY = this.topPos + 50;
        addRenderableWidget(
                Button.builder(Component.translatable("button.sconstruct.tool_station.build"), btn -> sendAction(be, ToolStationActionPayload.Action.BUILD)).bounds(baseX, baseY, 56, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("button.sconstruct.tool_station.modify"), btn -> sendAction(be, ToolStationActionPayload.Action.MODIFY))
                .bounds(baseX, baseY + 22, 56, 20).build());
    }

    private static void sendAction(ToolStationBlockEntity be, ToolStationActionPayload.Action action) {
        PacketDistributor.sendToServer(new ToolStationActionPayload(be.getBlockPos(), action));
    }

    /** Top section height — vanilla 17px border + 1 slot row + a small buffer. The bottom
     *  blit picks up where this ends so the player-inventory + hotbar section of the source
     *  sprite lands at the right place. */
    private static final int TOP_SECTION_H = 70;

    /** Player inventory + hotbar section height — vanilla's stock 96px so the slots line up
     *  with the {@link net.minecraft.world.inventory.AbstractContainerMenu} player-inv layout. */
    private static final int BOTTOM_SECTION_H = 96;

    /** Source y-offset for the player-inventory section in the vanilla generic_54 sprite. */
    private static final int SOURCE_PLAYER_INV_Y = 126;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // The vanilla generic_54 sprite is 222px tall (6 chest rows + player inv); blitting it
        // with a single pass over our 166px screen would clip the player-inventory band off
        // the bottom. Two passes: top chest-border + slot area from src y=0, then the
        // player-inventory band from src y=126 stitched directly below.
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, TOP_SECTION_H);
        guiGraphics.blit(BACKGROUND, x, y + TOP_SECTION_H, 0, SOURCE_PLAYER_INV_Y, this.imageWidth, BOTTOM_SECTION_H);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
