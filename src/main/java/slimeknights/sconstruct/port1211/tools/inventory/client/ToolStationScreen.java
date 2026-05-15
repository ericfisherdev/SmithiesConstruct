package slimeknights.sconstruct.port1211.tools.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.mojang.blaze3d.systems.RenderSystem;

import slimeknights.sconstruct.port1211.tools.inventory.ToolStationMenu;

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
