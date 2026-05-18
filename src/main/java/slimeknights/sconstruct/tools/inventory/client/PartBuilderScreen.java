package slimeknights.sconstruct.tools.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.mojang.blaze3d.systems.RenderSystem;

import slimeknights.sconstruct.tools.inventory.PartBuilderMenu;

/**
 * Client-side {@link AbstractContainerScreen} for the Part Builder (SMTCON-92). Renders the
 * standard 176×166 container background; the three slots (pattern, material, output) and the
 * player inventory are drawn by the base class from the {@link PartBuilderMenu} layout.
 *
 * <p>TODO(SMTCON-92 follow-up): the bespoke {@code sconstruct:textures/gui/container/
 * part_builder.png} PNG has not been authored yet. Until the artist drops it in, point
 * {@link #BACKGROUND} at the vanilla generic 54-slot container sprite so the screen renders
 * a real background rather than the magenta missing-texture sprite — both layouts are
 * 176×166 with a left-aligned panel, so the inventory grid still falls under the slots.
 * Same temporary-art rationale as {@link StencilTableScreen}.
 */
public class PartBuilderScreen extends AbstractContainerScreen<PartBuilderMenu> {

    /** Background sprite path. Points at the vanilla generic 54-slot container until the
     *  bespoke PNG lands per the ticket plan. Reference held so the SMTCON-92 follow-up
     *  swap is a one-line edit. */
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public PartBuilderScreen(PartBuilderMenu menu, Inventory playerInventory, Component title) {
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
