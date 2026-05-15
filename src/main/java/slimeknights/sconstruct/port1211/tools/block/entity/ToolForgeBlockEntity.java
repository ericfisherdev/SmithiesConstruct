package slimeknights.sconstruct.port1211.tools.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import slimeknights.sconstruct.port1211.tools.ToolStationRegistry;

/**
 * Block entity backing the Tool Forge (SMTCON-93). Inherits the 6-input + 1-output handler /
 * menu / build logic from {@link ToolStationBlockEntity}; overrides
 * {@link #acceptsAdvancedTools} to broaden the candidate roster from
 * {@link slimeknights.sconstruct.port1211.tools.ToolDefinition#ALL_BASIC} to
 * {@link slimeknights.sconstruct.port1211.tools.ToolDefinition#ALL_ADVANCED} so the 4 / 5-part
 * hammer, lumberaxe, shortbow, crossbow, and arrow definitions become buildable here.
 *
 * <p>{@link #getDisplayName} is overridden so the menu header reads "Tool Forge" rather than
 * inheriting the station's title.
 */
public class ToolForgeBlockEntity extends ToolStationBlockEntity {

    public ToolForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ToolStationRegistry.TOOL_FORGE_BE.get(), pos, state);
    }

    @Override
    public boolean acceptsAdvancedTools() {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.sconstruct.tool_forge");
    }
}
