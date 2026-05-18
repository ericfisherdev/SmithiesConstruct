package slimeknights.sconstruct.world.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.world.WorldBlocks;

/**
 * Pinned-behaviour tests for {@link TinkerSlimeBlock}. The Minecraft {@link
 * net.minecraft.world.level.block.Block} constructor registers an intrusive holder against the
 * frozen BLOCK registry, so the test JVM cannot {@code new TinkerSlimeBlock(...)} directly —
 * tests here exercise the already-registered instances from {@link WorldBlocks}, which were
 * constructed during the registry-event window before the registry froze. The {@code fallOn}
 * dispatch contract is covered separately in {@link SlimeColorTest} where the per-colour side
 * effect is the only thing that varies; vanilla bounce mechanics are inherited from
 * {@link net.minecraft.world.level.block.SlimeBlock} and exercised in a runClient pass.
 */
class TinkerSlimeBlockTest {

    @Test
    void registeredBlocksAreTinkerSlimeBlockInstancesCarryingTheirKeyColor() {
        // Every entry in WorldBlocks.SLIME_BLOCKS must resolve to a TinkerSlimeBlock whose
        // color() echoes the map key — a drifted ctor argument here would silently wire the
        // wrong fallOn side effect at registration time.
        WorldBlocks.SLIME_BLOCKS.forEach((color, holder) -> {
            TinkerSlimeBlock block = holder.get();
            assertNotNull(block, "registered block for " + color + " must not be null");
            assertSame(color, block.color(), "block at " + holder.getId() + " carries the wrong colour");
        });
    }

    @Test
    void colorAccessorMatchesNamedConstantsExactly() {
        // Spot-check the named-field bindings so a future rename of WorldBlocks.SLIMEBLUE etc.
        // (e.g. a swap of the BLUE/PURPLE order) trips the test instead of silently swapping
        // colours at runtime.
        assertEquals(SlimeColor.BLUE, WorldBlocks.SLIMEBLUE.get().color());
        assertEquals(SlimeColor.PURPLE, WorldBlocks.SLIMEPURPLE.get().color());
        assertEquals(SlimeColor.MAGMA, WorldBlocks.SLIMEMAGMA.get().color());
        assertEquals(SlimeColor.BLOOD, WorldBlocks.SLIMEBLOOD.get().color());
    }
}
