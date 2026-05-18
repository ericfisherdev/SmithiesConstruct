package slimeknights.sconstruct.plugin.patchouli.page;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.google.gson.annotations.SerializedName;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.tools.ToolDefinition;
import slimeknights.sconstruct.tools.item.ToolCore;

import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;

/**
 * Custom Patchouli page type {@code tconstruct:tool_stats} (SMTCON-163) — shows a built tool
 * example: the tool item, the materials it was assembled from, and the part / modifier-slot
 * grid read off its {@link ToolDefinition}.
 *
 * <p>A book entry adds a page like {@code {"type": "tconstruct:tool_stats", "tool":
 * "sconstruct:pickaxe", "materials": ["tconstruct:iron", "tconstruct:wood"]}}.
 *
 * <p>A freshly-constructed {@link ToolCore} stack carries a zero durability budget — the
 * unbuilt "broken" sentinel — so the page never reads durability off the stack; it renders the
 * static {@link ToolDefinition} grid instead, which is always valid. An unknown tool id, or a
 * tool item that is not a {@link ToolCore}, degrades to a short note rather than crashing.
 */
public class PageToolStats extends RecipePage {

    @SerializedName("tool")
    private String tool = "";
    @SerializedName("materials")
    private List<String> materials = List.of();

    private transient ItemStack toolStack = ItemStack.EMPTY;

    @Override
    public void build(Level level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        super.build(level, entry, builder, pageNum);
        this.toolStack = RecipePageRender.resolveItem(tool);
        if (materials == null) {
            materials = List.of();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawTitle(graphics);
        if (toolStack.isEmpty()) {
            drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".book.unknown_tool", tool), 8, 16);
            return;
        }
        drawItem(graphics, toolStack, 8, 16);
        drawText(graphics, toolStack.getHoverName(), 30, 20);

        int y = 40;
        drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".book.materials"), 8, y);
        y += 11;
        for (String materialId : materials) {
            drawText(graphics, Component.literal("- ").append(materialName(materialId)), 12, y);
            y += 10;
        }

        y += 4;
        if (toolStack.getItem() instanceof ToolCore toolCore) {
            ToolDefinition definition = toolCore.definition;
            drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".book.parts", definition.getPartCount()), 8, y);
            y += 11;
            drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".book.modifier_slots", definition.baseModifierSlots()), 8, y);
        }
        else {
            drawText(graphics, Component.translatable("gui." + SConstruct.MOD_ID + ".book.not_a_tool"), 8, y);
        }
    }

    /** The translatable display name for a material id, falling back to the raw id. */
    private static Component materialName(String id) {
        ResourceLocation key = id == null ? null : ResourceLocation.tryParse(id);
        if (key == null) {
            return Component.literal(String.valueOf(id));
        }
        return Component.translatable("material." + key.getNamespace() + "." + key.getPath());
    }
}
