package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
        // Cache the struck face BEFORE super.mineBlock so the ray-trace still hits the centre
        // block. Vanilla clears the broken block during super.mineBlock, so a post-call retrace
        // would land in air on most swings and the AOE plane would silently drift to the gaze
        // fallback for floor / wall breaks.
        Direction hitFace = miningEntity instanceof Player aimingPlayer ? resolveHitFace(aimingPlayer, pos) : Direction.UP;
        boolean handled = super.mineBlock(stack, level, state, pos, miningEntity);
        if (handled && miningEntity instanceof Player player) {
            // {@code state} is the pre-break BlockState — vanilla copies it before clearing the
            // block, so threading it through gates the TREE pattern on whether the centre was
            // actually a log and feeds an accurate isCorrectToolForDrops baseline elsewhere.
            AoeHelper.aoeMine(stack, player, pos, state, hitFace, aoePattern);
        }
        return handled;
    }

    /**
     * Resolve the {@link Direction} the player struck via a server-side ray from the player's
     * eye to the broken block. Falls back to the player's view direction if the trace doesn't
     * resolve to a block face (a player who has already moved off the dig face by the time
     * mineBlock fires) — keeps the AOE plane oriented sensibly rather than defaulting to UP.
     */
    private static Direction resolveHitFace(Player player, BlockPos broken) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getLookAngle().scale(player.blockInteractionRange() + 1.0D));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(broken)) {
            return hit.getDirection();
        }
        return Direction.getNearest(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z).getOpposite();
    }
}
