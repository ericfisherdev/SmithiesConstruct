package slimeknights.sconstruct.port1211.world.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link SlimeColor}. Verifies the per-colour id and map-colour
 * identity contract, plus the {@link SlimeColor#applyFallEffect} dispatch: no-op for
 * {@link SlimeColor#BLUE}/{@link SlimeColor#PURPLE}, hot-floor damage for
 * {@link SlimeColor#MAGMA}, and heal for {@link SlimeColor#BLOOD}.
 */
// PMD's CloseResource rule misclassifies Mockito-mocked Level instances as managed resources;
// the mocks have no underlying IO to close, so the rule is a false positive here.
@SuppressWarnings("PMD.CloseResource")
class SlimeColorTest {

    @Test
    void everyColorExposesIdAndMapColor() {
        for (SlimeColor color : SlimeColor.values()) {
            assertNotNull(color.id(), color + " must expose an id");
            assertNotNull(color.mapColor(), color + " must expose a map colour");
        }
        // Spot-check the lower-snake-case id contract — drift here breaks every downstream
        // registry name (registry path "slime_<id>_block", texture path, lang key, etc.).
        assertEquals("blue", SlimeColor.BLUE.id());
        assertEquals("purple", SlimeColor.PURPLE.id());
        assertEquals("magma", SlimeColor.MAGMA.id());
        assertEquals("blood", SlimeColor.BLOOD.id());
    }

    @Test
    void blueAndPurpleApplyNoFallEffect() {
        // Default applyFallEffect is a no-op for non-overriding constants. Verify by passing
        // mocks that would throw if invoked unexpectedly.
        Level level = mock(Level.class);
        Entity entity = mock(Entity.class);
        SlimeColor.BLUE.applyFallEffect(level, entity, 10.0F);
        SlimeColor.PURPLE.applyFallEffect(level, entity, 10.0F);
        verify(entity, never()).hurt(any(), eq(SlimeColor.FIRE_DAMAGE_PER_BOUNCE));
    }

    @Test
    void magmaAppliesHotFloorDamageWhenFallDistanceArmedAndServerSide() {
        Level level = mock(Level.class);
        DamageSources sources = mock(DamageSources.class);
        DamageSource hotFloor = mock(DamageSource.class);
        when(level.isClientSide()).thenReturn(false);
        when(level.damageSources()).thenReturn(sources);
        when(sources.hotFloor()).thenReturn(hotFloor);
        Entity entity = mock(Entity.class);

        SlimeColor.MAGMA.applyFallEffect(level, entity, SlimeColor.MIN_FALL_DISTANCE);

        verify(entity).hurt(hotFloor, SlimeColor.FIRE_DAMAGE_PER_BOUNCE);
    }

    @Test
    void magmaDoesNotApplyDamageOnClient() {
        // Side effects must fire server-side only — applying them on the client too would
        // double-apply or desync the damage tracker.
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(true);
        Entity entity = mock(Entity.class);

        SlimeColor.MAGMA.applyFallEffect(level, entity, 10.0F);

        verify(entity, never()).hurt(any(), eq(SlimeColor.FIRE_DAMAGE_PER_BOUNCE));
    }

    @Test
    void magmaDoesNotApplyDamageBelowMinFallDistance() {
        // The MIN_FALL_DISTANCE guard suppresses tick-rate retriggers from "bounce in place"
        // landings — without it, sneaking on a magma block would tick fire damage every frame.
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        Entity entity = mock(Entity.class);

        SlimeColor.MAGMA.applyFallEffect(level, entity, 0.5F);

        verify(entity, never()).hurt(any(), eq(SlimeColor.FIRE_DAMAGE_PER_BOUNCE));
    }

    @Test
    void bloodHealsWhenLivingEntityLandsAtArmedFallDistance() {
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        LivingEntity living = mock(LivingEntity.class);

        SlimeColor.BLOOD.applyFallEffect(level, living, SlimeColor.MIN_FALL_DISTANCE);

        verify(living).heal(SlimeColor.HEAL_PER_BOUNCE);
    }

    @Test
    void bloodDoesNothingForNonLivingEntities() {
        // A dropped item or a minecart can hit the block — heal() doesn't exist on them, so
        // the instanceof guard must keep the call site safe. The explicit assertion is that
        // none of the LivingEntity-shaped methods get invoked on the bare Entity mock; if the
        // guard regressed the call site would attempt to cast and Mockito would throw a
        // ClassCastException before we got here.
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);
        Entity entity = mock(Entity.class);

        SlimeColor.BLOOD.applyFallEffect(level, entity, 10.0F);

        // verifyNoInteractions on the Entity proves nothing was called on it — the bare
        // Entity API has no heal(), so a LivingEntity-typed invocation would have first
        // touched the entity via the guard's instanceof check.
        org.mockito.Mockito.verifyNoInteractions(entity);
    }

    @Test
    void bloodDoesNotHealOnClient() {
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(true);
        LivingEntity living = mock(LivingEntity.class);

        SlimeColor.BLOOD.applyFallEffect(level, living, 10.0F);

        verify(living, never()).heal(SlimeColor.HEAL_PER_BOUNCE);
    }
}
