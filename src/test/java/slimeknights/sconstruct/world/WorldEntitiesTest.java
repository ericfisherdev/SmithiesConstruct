package slimeknights.sconstruct.world;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.world.entity.EntityBlueslime;
import slimeknights.sconstruct.world.entity.EntityHugeSlime;

/**
 * Pinned-behaviour tests for {@link WorldEntities}. The
 * {@link net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent} and
 * {@link net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent} listeners can't be
 * fired here without a live mod bus — those are wired by {@link TinkerWorldPulse#register} and
 * exercised at runtime — but the per-entity {@code createAttributes()} factory output, the
 * registry-id contract, and the {@link MobCategory} placement bucket are fully testable
 * statically.
 */
class WorldEntitiesTest {

    @Test
    void bothEntityTypesAreRegisteredUnderTheSconstructNamespaceWithStableIds() {
        assertEquals(SConstruct.MOD_ID, WorldEntities.BLUESLIME.getId().getNamespace());
        assertEquals("blueslime", WorldEntities.BLUESLIME.getId().getPath());
        assertEquals(SConstruct.MOD_ID, WorldEntities.HUGESLIME.getId().getNamespace());
        assertEquals("hugeslime", WorldEntities.HUGESLIME.getId().getPath());
    }

    @Test
    void allListContainsBothEntityTypesInDeclarationOrder() {
        assertEquals(2, WorldEntities.ALL.size());
        assertEquals(WorldEntities.BLUESLIME, WorldEntities.ALL.get(0));
        assertEquals(WorldEntities.HUGESLIME, WorldEntities.ALL.get(1));
    }

    @Test
    void everyEntityTypeIsBucketedAsAMonsterMobCategory() {
        // Both slime mobs go through the monster spawn cap — drift to CREATURE would let them
        // spawn at peaceful difficulty and overflow the friendly cap.
        for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> holder : WorldEntities.ALL) {
            assertEquals(MobCategory.MONSTER, holder.get().getCategory(), holder.getId() + " must be MobCategory.MONSTER");
        }
    }

    @Test
    void blueslimeAttributesMatchTheSpecValues() {
        // MAX_HEALTH 8.0, MOVEMENT_SPEED 0.3 per SMTCON-56 AC. Build the supplier directly
        // (the factory is a static helper) and read the values; a drift in the AC numbers
        // would silently scale the entity's spawn health and tracker pacing.
        AttributeSupplier supplier = EntityBlueslime.createAttributes().build();
        assertAll(() -> assertEquals(8.0D, supplier.getValue(Attributes.MAX_HEALTH), 1e-6D), () -> assertEquals(0.3D, supplier.getValue(Attributes.MOVEMENT_SPEED), 1e-6D));
    }

    @Test
    void hugeSlimeAttributesPinMaxHealthAndDefaultSizeConstant() {
        AttributeSupplier supplier = EntityHugeSlime.createAttributes().build();
        assertEquals(64.0D, supplier.getValue(Attributes.MAX_HEALTH), 1e-6D);
        // Default size constant carries the AC's "size=4 default" verbatim — drift would
        // silently change the spawn hitbox the next time finalizeSpawn fires.
        assertEquals(4, EntityHugeSlime.DEFAULT_SIZE);
    }

    @Test
    void everyEntityTypeHasNonNullDescriptionIdMatchingTheRegisteredPath() {
        // The descriptionId is what LanguageProvider keys against (entity.<modid>.<path>).
        // A null or drifted descriptionId would break the lang entries.
        assertNotNull(WorldEntities.BLUESLIME.get().getDescriptionId());
        assertNotNull(WorldEntities.HUGESLIME.get().getDescriptionId());
        Set<String> expected = Set.of("entity.sconstruct.blueslime", "entity.sconstruct.hugeslime");
        assertTrue(expected.contains(WorldEntities.BLUESLIME.get().getDescriptionId()), "blueslime descriptionId");
        assertTrue(expected.contains(WorldEntities.HUGESLIME.get().getDescriptionId()), "hugeslime descriptionId");
    }
}
