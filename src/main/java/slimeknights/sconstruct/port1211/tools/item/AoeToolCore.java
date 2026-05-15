package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Base class for tools that break more than one block per swing — hammer (3x3), excavator (3x3),
 * lumber-axe (tree felling), scythe (3x3), mattock (1x3 column). Subclasses pass their own
 * {@link ToolDefinition} and {@link AoePattern} through the constructor; {@link #mineBlock}
 * extends the inherited single-block break with an {@link AoeHelper#aoeMine} pass so the per-tool
 * code stays a constructor and nothing else.
 *
 * <p>{@link AoePattern#TREE} subclasses (lumber-axe) tolerate that the connected-log walk reads
 * the world after vanilla has already destroyed the centre block — the start position drops out
 * of the walk via {@link AoeHelper#walkConnectedLogs}'s {@code !pos.equals(start)} guard.
 *
 * <p>The AOE pass runs server-side only and after vanilla's single-block break has fired so the
 * centre block's drops, XP, and stat ticks follow vanilla rules; only the extra AoE blocks get
 * the legacy "drops at player feet" redirection. Durability deducts one tick per additional
 * block broken — see {@link AoeHelper#aoeMine}.
 */
public class AoeToolCore extends ToolCore {

    private final AoePattern aoePattern;

    public AoeToolCore(Item.Properties properties, ToolDefinition definition, AoePattern aoePattern) {
        super(properties, definition);
        this.aoePattern = aoePattern;
    }

    /** Pattern selector this subclass was registered with. Used by tests and JEI integration. */
    public AoePattern aoePattern() {
        return aoePattern;
    }

    /**
     * Extend the vanilla single-block break with an AOE pass. The single-block break (durability
     * tick, dispatcher, XP) runs through {@link ToolCore#mineBlock} first; only after that
     * succeeds does the AOE pass extend outwards, so a broken or wrong-tool stack falls through
     * to the inherited single-block behaviour without touching neighbouring blocks.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean handled = super.mineBlock(stack, level, state, pos, miningEntity);
        if (handled && miningEntity instanceof Player player) {
            AoeHelper.aoeMine(stack, player, pos, aoePattern);
        }
        return handled;
    }
}
