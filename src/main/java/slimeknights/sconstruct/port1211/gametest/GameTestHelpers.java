package slimeknights.sconstruct.port1211.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Shared utility surface for the {@code sconstruct} {@link net.minecraft.gametest.framework.GameTest}
 * suite (SMTCON-174). The {@code @GameTest} classes under this package each assemble a small
 * scene inside the shared {@code gametest_7x7x7} air template, drive a block entity or item
 * directly, and assert on the result. Four operations recur across that work — fetching a
 * typed block entity, fabricating an actor, feeding a machine, and reading a tank — and this
 * class is their single home so a behaviour change lands in one place rather than across ten
 * test files.
 *
 * <p>The fluid / item operations resolve their target through the NeoForge block-capability
 * system ({@link Capabilities#FluidHandler} / {@link Capabilities#ItemHandler}) rather than a
 * concrete block-entity type, so a test can pour into any registered machine without this
 * helper knowing the machine's class — the same indirection a hopper or a pipe mod would use.
 * The smeltery controller, the seared tanks, and the casting blocks all register those
 * capabilities, so each is reachable through this helper.
 *
 * <p>Every method takes the {@link GameTestHelper} so failures surface as test failures with a
 * structure-relative {@link BlockPos} in the message; positions passed in are structure-relative
 * and converted to world coordinates via {@link GameTestHelper#absolutePos} before any
 * capability lookup.
 */
public final class GameTestHelpers {

    private GameTestHelpers() {
    }

    /**
     * Spawns a survival-mode mock player for interaction tests. Thin wrapper over
     * {@link GameTestHelper#makeMockPlayer} that pins the {@link GameType} so every test's
     * fake player shares the same creative-flags / damage profile — a creative-mode player
     * would, for example, never consume durability or take fall damage.
     */
    public static Player setupFakePlayer(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    /**
     * Fetches the block entity at the structure-relative {@code pos}, asserting it is an
     * instance of {@code type} before the downcast. A wiring regression then fails with a
     * clear assertion message naming {@code description} rather than as an opaque
     * {@link ClassCastException} deep in the caller.
     */
    public static <T extends BlockEntity> T blockEntityAt(GameTestHelper helper, BlockPos pos, Class<T> type, String description) {
        BlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(be), description + " block entity attaches at " + pos);
        return type.cast(be);
    }

    /**
     * Inserts {@code stack} into the item handler the block at {@code pos} exposes, asserting
     * the block advertises an {@link Capabilities#ItemHandler} capability and that the handler
     * accepted the whole stack. Distribution across slots follows
     * {@link ItemHandlerHelper#insertItem} — the same first-fit walk a hopper performs.
     */
    public static void insertItem(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "block at " + pos + " exposes an item handler");
        ItemStack remainder = ItemHandlerHelper.insertItem(handler, stack, false);
        helper.assertTrue(remainder.isEmpty(), "item handler at " + pos + " accepted the whole stack of " + stack.getItem());
    }

    /**
     * Pours {@code fluid} into the fluid handler the block at {@code pos} exposes, asserting
     * the block advertises a {@link Capabilities#FluidHandler} capability and that the handler
     * accepted the full amount — a partial fill means the tank was smaller than the test
     * assumed and is reported as a failure rather than silently dropped.
     */
    public static void pourFluid(GameTestHelper helper, BlockPos pos, FluidStack fluid) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "block at " + pos + " exposes a fluid handler");
        int filled = handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(filled, fluid.getAmount(), "fluid handler at " + pos + " accepted the full pour of " + fluid.getFluid());
    }

    /**
     * Asserts the fluid handler at {@code pos} holds exactly {@code expected}: the same fluid
     * type and a total amount, summed across every tank the handler exposes, equal to
     * {@link FluidStack#getAmount()}. Summing rather than reading tank 0 keeps the assertion
     * correct for multi-tank handlers (a smeltery controller exposes one tank per molten
     * metal).
     *
     * <p>Any non-empty tank holding a fluid other than {@code expected} fails the test
     * immediately — an "exactly expected" assertion that silently tolerated a stray fluid
     * would mask the very contamination regression these tests exist to catch.
     */
    public static void assertTank(GameTestHelper helper, BlockPos pos, FluidStack expected) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "block at " + pos + " exposes a fluid handler");
        int total = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack contents = handler.getFluidInTank(tank);
            if (contents.isEmpty()) {
                continue;
            }
            if (FluidStack.isSameFluid(contents, expected)) {
                total += contents.getAmount();
            }
            else {
                helper.fail("tank at " + pos + " holds unexpected fluid " + contents.getFluid() + " in tank " + tank);
            }
        }
        helper.assertValueEqual(total, expected.getAmount(), "tank at " + pos + " holds the expected amount of " + expected.getFluid());
    }
}
