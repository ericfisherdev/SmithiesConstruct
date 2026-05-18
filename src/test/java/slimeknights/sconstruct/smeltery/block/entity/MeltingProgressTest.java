package slimeknights.sconstruct.smeltery.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link MeltingProgress} — the per-tick advance / completion
 * contract the smeltery controller relies on, and the constructor's argument validation.
 */
class MeltingProgressTest {

    private static MeltingProgress melt(int requiredTicks) {
        return new MeltingProgress(0, requiredTicks, new FluidStack(Fluids.WATER, 100));
    }

    @Test
    void advanceIncrementsElapsedTicks() {
        MeltingProgress progress = melt(5);
        assertEquals(0, progress.elapsedTicks(), "a fresh melt has run zero ticks");
        progress.advance();
        progress.advance();
        assertEquals(2, progress.elapsedTicks(), "advance increments elapsed once per call");
    }

    @Test
    void isCompleteOnlyAfterRequiredTicksElapse() {
        MeltingProgress progress = melt(3);
        assertFalse(progress.isComplete(), "not complete before any ticks");
        progress.advance();
        progress.advance();
        assertFalse(progress.isComplete(), "not complete one tick short");
        progress.advance();
        assertTrue(progress.isComplete(), "complete once elapsed reaches the required duration");
        progress.advance();
        assertTrue(progress.isComplete(), "stays complete past the required duration");
    }

    @Test
    void resultIsCopiedDefensivelyOnStoreAndOnRead() {
        // Store-side: mutating the FluidStack passed to the constructor must not reach the melt.
        FluidStack source = new FluidStack(Fluids.WATER, 100);
        MeltingProgress progress = new MeltingProgress(0, 5, source);
        source.setAmount(999);
        assertEquals(100, progress.result().getAmount(), "the constructor must copy the result fluid");

        // Read-side: mutating the FluidStack handed back by result() must not reach the melt.
        FluidStack handedBack = progress.result();
        handedBack.setAmount(7);
        assertEquals(100, progress.result().getAmount(), "result() must hand back a fresh copy each call");
    }

    @Test
    void constructorRejectsInvalidArguments() {
        FluidStack water = new FluidStack(Fluids.WATER, 100);
        assertThrows(IllegalArgumentException.class, () -> new MeltingProgress(-1, 5, water), "negative slot");
        assertThrows(IllegalArgumentException.class, () -> new MeltingProgress(0, 0, water), "non-positive required ticks");
        assertThrows(IllegalArgumentException.class, () -> new MeltingProgress(0, 5, FluidStack.EMPTY), "empty result fluid");
    }
}
