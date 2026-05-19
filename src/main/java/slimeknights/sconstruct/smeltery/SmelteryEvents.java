package slimeknights.sconstruct.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import slimeknights.sconstruct.smeltery.block.entity.SmelteryControllerBlockEntity;
import slimeknights.sconstruct.smeltery.multiblock.SmelteryStructureValidator;

/**
 * Game-bus event handling for the smeltery (SMTCON-117) — the disassembly trigger. When a
 * player breaks any seared or component block, every smeltery controller close enough to have
 * owned that block is flagged to re-validate its multiblock on its next tick.
 *
 * <p>The controller is not re-validated inline: {@link BlockEvent.BreakEvent} fires <em>before</em>
 * the block is removed, so a same-tick re-scan would still see the doomed block and wrongly
 * keep the smeltery assembled. Flagging instead defers the re-scan one tick, by which point the
 * block is gone — see {@link SmelteryControllerBlockEntity#invalidate()}.
 *
 * <p>Invalidating a controller that did not actually own the broken block is harmless: it simply
 * re-validates, finds itself still well-formed, and stays assembled. That tolerance is why a
 * generous box scan around the break is acceptable rather than tracking exact ownership.
 *
 * <p>Registered against {@code NeoForge.EVENT_BUS} from {@code SConstruct} for now; the call
 * relocates into the smeltery pulse's {@code register()} at SMTCON-131.
 */
public final class SmelteryEvents {

    /**
     * Half-extent of the cube scanned around a broken block for controllers to invalidate.
     * A controller can sit anywhere in the wall ring of its smeltery, so the farthest a
     * controller can be from one of its own shell blocks is the interior span plus the wall
     * thickness — {@link SmelteryStructureValidator#MAX_INTERIOR_SIZE} plus a two-block margin
     * covers every reachable controller with room to spare.
     */
    private static final int CONTROLLER_SEARCH_RADIUS = SmelteryStructureValidator.MAX_INTERIOR_SIZE + 2;

    private SmelteryEvents() {
    }

    /** Subscribes the smeltery game-bus listeners. Invoked with {@code NeoForge.EVENT_BUS}. */
    public static void register(IEventBus neoForgeBus) {
        neoForgeBus.addListener(SmelteryEvents::onBlockBreak);
    }

    /**
     * On a seared or component block break, flags every nearby controller to re-validate. Other
     * block breaks are ignored cheaply — the shell-block test runs before any world scan.
     */
    // The Level is the running world, not a resource this listener owns or may close — PMD's
    // CloseResource heuristic does not model that.
    @SuppressWarnings("PMD.CloseResource")
    @SubscribeEvent
    static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!SmelteryStructureValidator.isSmelteryShellBlock(event.getState())) {
            return;
        }
        notifyControllersNear(level, event.getPos());
    }

    /**
     * Notifies every {@link SmelteryControllerBlockEntity} within the search cube of a shell-block
     * break — the controller's own structure decides whether the change warrants re-validation
     * (SMTCON-227) via {@link SmelteryStructureValidator#shouldUpdate}. The block has not been
     * removed yet (the event fires pre-removal), so the post-change role is the air-equivalent
     * {@link SmelteryStructureValidator.BlockRole#INTERIOR}.
     */
    private static void notifyControllersNear(Level level, BlockPos broken) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos brokenImmutable = broken.immutable();
        for (int dx = -CONTROLLER_SEARCH_RADIUS; dx <= CONTROLLER_SEARCH_RADIUS; dx++) {
            for (int dy = -CONTROLLER_SEARCH_RADIUS; dy <= CONTROLLER_SEARCH_RADIUS; dy++) {
                for (int dz = -CONTROLLER_SEARCH_RADIUS; dz <= CONTROLLER_SEARCH_RADIUS; dz++) {
                    cursor.set(broken.getX() + dx, broken.getY() + dy, broken.getZ() + dz);
                    if (level.getBlockEntity(cursor) instanceof SmelteryControllerBlockEntity controller) {
                        controller.notifyChange(brokenImmutable, SmelteryStructureValidator.BlockRole.INTERIOR);
                    }
                }
            }
        }
    }
}
