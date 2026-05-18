package slimeknights.sconstruct.tools.item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import slimeknights.sconstruct.tools.ToolHelper;

/**
 * Pure-function helpers behind {@link AoeToolCore#aoeMine}. Lives in its own class so the AOE
 * iteration, harvest-level gate, and drop-redirection logic can be inspected without standing up
 * a real {@link AoeToolCore} item — constructing one trips the {@code MappedRegistry} freeze in
 * the bare-JVM test environment.
 *
 * <p>The helper is intentionally server-only: the {@link Level#isClientSide()} guard short-
 * circuits client-side calls so the AOE pass never double-fires across the client/server split.
 * Block destruction is non-trivial state mutation — the client side relies on the server's
 * authoritative break notifications driving its world view rather than running the pattern
 * itself.
 */
public final class AoeHelper {

    /**
     * Hard cap on the {@link AoePattern#TREE} log walk so a recursive crawl through a custom
     * mega-tree doesn't lag the server. Matches the legacy 1.12 lumber-axe cap.
     */
    private static final int TREE_WALK_LIMIT = 64;

    /**
     * Sentinel value vanilla {@link BlockState#getDestroySpeed} returns for unbreakable blocks
     * (bedrock, command block, etc.). Compared against with {@code <=} so any block whose hardness
     * is at or below this sentinel is treated as unbreakable.
     */
    private static final float UNBREAKABLE_HARDNESS_SENTINEL = -1.0F;

    private AoeHelper() {
    }

    /**
     * Run the AOE break pass for a swing.
     *
     * <p>Iterates the pattern positions, skips blocks the tool cannot harvest (broken tool,
     * harvest-level gate via {@link ItemStack#isCorrectToolForDrops}, air, unbreakable), and for
     * each block that passes drops its loot at the player's feet rather than at the broken
     * position so the player doesn't have to chase the AOE drops across the dig face.
     *
     * @return the number of blocks broken (excluding the centre, which vanilla's {@code mineBlock}
     *         hook has already handled by the time this helper fires). Durability is deducted
     *         internally via {@link ItemStack#hurtAndBreak} — one point per additional block
     *         broken — so callers don't have to thread the haul count back through their own
     *         damage path.
     */
    @SuppressWarnings("PMD.CloseResource") // Level/ServerLevel are not closable resources; PMD false-positive on pattern matching.
    public static int aoeMine(ItemStack stack, Player player, BlockPos centre, BlockState preBreakState, Direction hitFace, AoePattern pattern) {
        if (ToolHelper.isBroken(stack)) {
            return 0;
        }
        Level rawLevel = player.level();
        if (rawLevel.isClientSide() || !(rawLevel instanceof ServerLevel level)) {
            return 0;
        }
        // TREE walk only fires when the centre itself was a log — otherwise the BFS would
        // wastefully explore the 26-neighbourhood of a non-log block before bailing out, and a
        // canopy mined sideways shouldn't sympathy-fell a stray neighbouring trunk.
        List<BlockPos> targets;
        if (pattern == AoePattern.TREE) {
            targets = preBreakState.is(BlockTags.LOGS) ? walkConnectedLogs(level, centre) : List.of();
        }
        else {
            targets = pattern.positions(centre, hitFace);
        }
        int broken = 0;
        for (BlockPos pos : targets) {
            if (pos.equals(centre)) {
                continue;
            }
            if (tryBreakOne(stack, player, level, pos)) {
                broken++;
            }
        }
        if (broken > 0) {
            stack.hurtAndBreak(broken, player, EquipmentSlot.MAINHAND);
        }
        return broken;
    }

    /**
     * Single-block break step: harvest-level gate, drop redirection, and the
     * {@code playerWillDestroy → removeBlock → spawnAfterBreak} sequence vanilla's
     * {@code Player#destroyBlock} runs in-line. Returns {@code true} if the block was actually
     * broken — the caller increments the durability deduction off this count.
     */
    private static boolean tryBreakOne(ItemStack stack, Player player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) <= UNBREAKABLE_HARDNESS_SENTINEL) {
            return false;
        }
        if (!stack.isCorrectToolForDrops(state)) {
            return false;
        }
        // Fire the Forge BlockEvent.BreakEvent so protection plugins / claim mods can veto an
        // AOE break the same way they veto a single-block break. The event runs against each
        // AOE candidate (not just the centre) so a 3x3 hammer swing that clips a protected
        // claim only breaks the unprotected blocks.
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        if (NeoForge.EVENT_BUS.post(breakEvent).isCanceled()) {
            return false;
        }
        Block block = state.getBlock();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        block.playerWillDestroy(level, pos, state, player);
        if (!level.removeBlock(pos, false)) {
            return false;
        }
        block.destroy(level, pos, state);
        // Drop redirection: vanilla's playerDestroy pops resources at the broken position.
        // Re-route every drop to the player's feet so the AoE swing gathers in one spot — the
        // legacy 1.12 hammer / excavator / lumber-axe behaviour callers rely on.
        BlockPos feet = player.blockPosition();
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, stack);
        for (ItemStack drop : drops) {
            Block.popResource(level, feet, drop);
        }
        state.spawnAfterBreak(level, pos, stack, true);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        // Match vanilla Player#destroyBlock — each AOE block break drains hunger at the same
        // rate as a single-block break so a 3x3 hammer swing exhausts the player nine times.
        player.causeFoodExhaustion(0.005F);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(block));
        }
        return true;
    }

    /**
     * Breadth-first walk over connected log blocks starting from {@code start}, capped at
     * {@link #TREE_WALK_LIMIT}. Logs are identified by {@link BlockTags#LOGS} membership so
     * datapack-added log types (cherry, custom mod logs) participate without code changes.
     */
    static List<BlockPos> walkConnectedLogs(ServerLevel level, BlockPos start) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && result.size() < TREE_WALK_LIMIT) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            boolean isStart = pos.equals(start);
            boolean isLog = state.is(BlockTags.LOGS);
            // The start block has already been broken by vanilla's mineBlock hook by the time the
            // AoE pass fires — its state reads as air. Always explore the start's neighbours so
            // the BFS can reach the canopy; for every other position, only follow connectivity
            // through actual logs so the walk doesn't bleed into the surrounding world.
            if (!isStart && !isLog) {
                continue;
            }
            if (!isStart) {
                result.add(pos);
            }
            // 26-neighbourhood walk so diagonally-stacked branches (vanilla oak/jungle canopies)
            // stay connected — pure-cardinal would orphan branch tips on legacy 1.12-shaped trees.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }
}
