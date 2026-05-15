package slimeknights.sconstruct.port1211.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.port1211.tools.PatternChestRegistry;
import slimeknights.sconstruct.port1211.tools.StencilTableRegistry;
import slimeknights.sconstruct.port1211.tools.block.entity.PatternChestBlockEntity;

/**
 * End-to-end {@link GameTest} for the Pattern Chest (SMTCON-90) — the SMTCON-96 acceptance
 * criterion that the 32-slot inventory accepts blank patterns into every slot, surfaces them
 * back, and releases them when pulled. Runs inside a real NeoForge {@code gameTestServer} so
 * the block placement, BE attach, {@link ItemStackHandler} round-trip, and component map all
 * exercise the live block-entity system rather than a unit-test double.
 *
 * <p>The workflow is simulated against the BE's {@link ItemStackHandler} directly rather than
 * through the menu — {@code gameTestServer} does not have an ergonomic fake-player + screen
 * harness, and the handler is the same surface a hopper neighbour or capability-API client
 * sees. The menu binds {@code SlotItemHandler}s straight to this handler, so exercising it
 * here covers the same code paths a real player interaction would.
 *
 * <p>Reuses the shared {@code gametest_7x7x7} air template; the chest only needs a single
 * block position inside the structure bounds.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PatternChestTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Placement position inside the 7x7x7 template — chosen well inside the bounds. */
    private static final BlockPos CHEST_POS = new BlockPos(2, 2, 2);

    private PatternChestTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void fillEverySlot(GameTestHelper helper) {
        helper.setBlock(CHEST_POS, PatternChestRegistry.PATTERN_CHEST.get());
        PatternChestBlockEntity be = (PatternChestBlockEntity) helper.getBlockEntity(CHEST_POS);
        helper.assertTrue(be != null, "Pattern Chest BlockEntity must attach after setBlock");

        ItemStackHandler handler = be.getHandler();
        helper.assertValueEqual(handler.getSlots(), PatternChestBlockEntity.SLOTS, "pattern chest exposes 32 slots");

        // Fill every slot with a blank pattern. insertItem returns the remainder; an empty
        // remainder proves the slot accepted the full stack.
        for (int i = 0; i < PatternChestBlockEntity.SLOTS; i++) {
            ItemStack remainder = handler.insertItem(i, new ItemStack(StencilTableRegistry.BLANK_PATTERN.get()), false);
            helper.assertTrue(remainder.isEmpty(), "slot " + i + " accepted the blank pattern insertion");
        }

        // Read each slot back; each must hold exactly one blank pattern. Catches a regression
        // where the handler silently dropped an insertion or coalesced into the wrong slot.
        for (int i = 0; i < PatternChestBlockEntity.SLOTS; i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            helper.assertTrue(slotStack.is(StencilTableRegistry.BLANK_PATTERN.get()), "slot " + i + " holds a blank pattern");
            helper.assertValueEqual(slotStack.getCount(), 1, "slot " + i + " count is 1");
        }

        // Pull one stack out — the slot must come back empty. Round-trips the extraction side
        // of the handler so a half-broken impl that only supported insertion would still fail.
        ItemStack extracted = handler.extractItem(0, 64, false);
        helper.assertTrue(extracted.is(StencilTableRegistry.BLANK_PATTERN.get()), "extracted stack is the blank pattern previously inserted");
        helper.assertValueEqual(extracted.getCount(), 1, "extracted exactly one item from slot 0");
        helper.assertTrue(handler.getStackInSlot(0).isEmpty(), "slot 0 is empty after extraction");

        helper.succeed();
    }
}
