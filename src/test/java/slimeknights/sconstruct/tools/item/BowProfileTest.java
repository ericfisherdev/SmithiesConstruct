package slimeknights.sconstruct.tools.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pinned-constant tests for the bow per-class draw / velocity profiles. The {@link BowToolCore}
 * constructor stores draw-tick count and full-draw velocity, then {@link #computePower}
 * derives the vanilla bow power curve from those constants. Instantiating the real Item
 * subclasses trips the {@code MappedRegistry} freeze in the bare-JVM test environment, so the
 * tests pin the {@code public static final} surface of each subclass (draw ticks, velocity
 * ceiling) rather than constructing items directly.
 */
class BowProfileTest {

    @Test
    void shortbowProfileMatchesVanillaBowBaseline() {
        // Shortbow exists for parity with the vanilla bow's fire rate and velocity ceiling so a
        // player coming from vanilla doesn't have to relearn the draw curve.
        assertEquals(BowToolCore.VANILLA_DRAW_TICKS, ShortbowItem.DRAW_TICKS, "shortbow draw must match vanilla bow draw");
        assertEquals(BowToolCore.VANILLA_FULL_DRAW_VELOCITY, ShortbowItem.FULL_DRAW_VELOCITY, "shortbow velocity must match vanilla bow ceiling");
    }

    @Test
    void longbowDrawIsSlowerThanShortbow() {
        // Acceptance criterion: longbow draw is visibly slower than shortbow / vanilla.
        assertTrue(LongbowItem.DRAW_TICKS > ShortbowItem.DRAW_TICKS, "longbow draw " + LongbowItem.DRAW_TICKS + " must exceed shortbow " + ShortbowItem.DRAW_TICKS);
    }

    @Test
    void longbowFullDrawVelocityExceedsShortbow() {
        // Acceptance criterion: longbow shoots farther / harder than shortbow at full draw.
        assertTrue(LongbowItem.FULL_DRAW_VELOCITY > BowToolCore.VANILLA_FULL_DRAW_VELOCITY,
                "longbow velocity " + LongbowItem.FULL_DRAW_VELOCITY + " must exceed shortbow ceiling " + BowToolCore.VANILLA_FULL_DRAW_VELOCITY);
    }
}
