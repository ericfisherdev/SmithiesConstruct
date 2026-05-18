package slimeknights.sconstruct.tools.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.mojang.blaze3d.systems.RenderSystem;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.tools.inventory.PatternChestMenu;

/**
 * Client-side {@link AbstractContainerScreen} for the 32-slot Pattern Chest (SMTCON-90).
 * Renders the background sprite + slot frames; vanilla {@code Slot} rendering draws the
 * item stacks themselves.
 *
 * <p>TODO(SMTCON-90 follow-up): the background PNG at
 * {@link #BACKGROUND} does not yet exist on disk. Vanilla falls back to the missing-texture
 * sprite when the path is unresolved, so the screen still opens and interacts correctly —
 * the visual quality is the only thing that suffers until the artist drops in the PNG.
 *
 * <p>GUI dimensions: 176px wide × 204px tall — wider than vanilla's 166 to leave room for
 * the 8-column × 4-row chest grid above the standard player inventory. The label positions
 * shift to match the taller layout so the "Pattern Chest" title sits above the grid and the
 * "Inventory" sub-label sits above the player inv area.
 */
public class PatternChestScreen extends AbstractContainerScreen<PatternChestMenu> {

    /**
     * Background sprite path. The PNG asset is a follow-up per the ticket plan; the missing
     * file does not break the menu — vanilla renders the no-texture sprite in its place.
     */
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "textures/gui/container/pattern_chest.png");

    public PatternChestScreen(PatternChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        // 8 cols × 4 rows = 72px of chest grid + standard 18px slot height border + the
        // 90px-tall player inventory section beneath. 204 total fits inside a 240px-tall window
        // (smallest supported GUI scale on Minecraft's UI grid).
        this.imageHeight = 204;
        // Label positions inherit the AbstractContainerScreen defaults: titleLabelY = 6
        // (top-left of the GUI) and inventoryLabelY = imageHeight - 94 (anchored above the
        // 27-slot player inventory). Both defaults are correct for the taller 204px window
        // because inventoryLabelY scales with imageHeight automatically.
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // RenderSystem set-up matches vanilla ChestScreen — the colour reset guards against
        // a previous draw call leaving a non-white tint multiplier on the matrix.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Standard container screen render: dimmed background, then the menu's slots/labels,
        // then any tooltip for the slot under the cursor.
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
