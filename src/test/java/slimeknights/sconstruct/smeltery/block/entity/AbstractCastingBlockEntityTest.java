package slimeknights.sconstruct.smeltery.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.smeltery.CastingBlocks;

/**
 * Pinned-behaviour tests for {@link AbstractCastingBlockEntity} via its two concrete subclasses.
 * Covers the per-block tank capacities (288 mB table / 2592 mB basin), the single-slot cast
 * inventory, and in-memory cast insert / extract. The NBT persistence path delegates straight
 * to the well-tested NeoForge {@code ItemStackHandler} / {@code FluidTank} classes; it is left
 * to in-game verification rather than reconstructed here with a stub registry provider.
 */
class AbstractCastingBlockEntityTest {

    private static CastingTableBlockEntity table() {
        return new CastingTableBlockEntity(BlockPos.ZERO, CastingBlocks.CASTING_TABLE.get().defaultBlockState());
    }

    private static CastingBasinBlockEntity basin() {
        return new CastingBasinBlockEntity(BlockPos.ZERO, CastingBlocks.CASTING_BASIN.get().defaultBlockState());
    }

    @Test
    void tankCapacitiesMatchTheCastingUnits() {
        assertEquals(288, table().getFluidHandler().getTankCapacity(0), "table tank holds one ingot (288 mB)");
        assertEquals(2592, basin().getFluidHandler().getTankCapacity(0), "basin tank holds one block (2592 mB)");
    }

    @Test
    void castInventoryHasExactlyOneSlot() {
        assertEquals(1, table().getCastHandler().getSlots(), "table has one cast slot");
        assertEquals(1, basin().getCastHandler().getSlots(), "basin has one cast slot");
    }

    @Test
    void castSlotAcceptsAndReturnsAnItemOnBothBlocks() {
        for (AbstractCastingBlockEntity be : new AbstractCastingBlockEntity[] { table(), basin() }) {
            assertTrue(be.getCastHandler().getStackInSlot(0).isEmpty(), "cast slot starts empty");

            be.getCastHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            assertSame(Items.IRON_INGOT, be.getCastHandler().getStackInSlot(0).getItem(), "cast slot holds the deposited item");

            ItemStack extracted = be.getCastHandler().extractItem(0, 1, false);
            assertSame(Items.IRON_INGOT, extracted.getItem(), "extract returns the cast item");
            assertTrue(be.getCastHandler().getStackInSlot(0).isEmpty(), "cast slot empty after extraction");
        }
    }
}
