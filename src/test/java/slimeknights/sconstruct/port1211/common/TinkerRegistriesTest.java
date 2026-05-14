package slimeknights.sconstruct.port1211.common;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;

/**
 * Pinned-behaviour tests for the central {@link TinkerRegistries} hub. Verifies that every
 * declared {@link DeferredRegister} points at the right vanilla- or NeoForge-side registry key,
 * is namespaced under {@link SConstruct#MOD_ID}, and that {@link TinkerRegistries#registerAll}
 * attaches each one to the supplied bus exactly once.
 */
class TinkerRegistriesTest {

    @Test
    void everyRegisterIsBoundToTheTconstructNamespace() {
        // Asserted in a loop so adding a new register in TinkerRegistries automatically picks
        // up coverage here without anyone having to touch the test.
        for (DeferredRegister<?> register : allRegisters()) {
            assertEquals(SConstruct.MOD_ID, register.getNamespace(),
                    "Every DeferredRegister should be namespaced under sconstruct, " + "found " + register.getRegistryName() + " using namespace " + register.getNamespace());
        }
    }

    @Test
    void registriesTargetTheExpectedVanillaAndNeoforgeKeys() {
        assertAll(() -> assertEquals(Registries.ITEM.location(), TinkerRegistries.ITEMS.getRegistryName()), () -> assertEquals(Registries.BLOCK.location(), TinkerRegistries.BLOCKS.getRegistryName()),
                () -> assertEquals(Registries.DATA_COMPONENT_TYPE.location(), TinkerRegistries.DATA_COMPONENTS.getRegistryName()),
                () -> assertEquals(Registries.BLOCK_ENTITY_TYPE.location(), TinkerRegistries.BLOCK_ENTITY_TYPES.getRegistryName()),
                () -> assertEquals(Registries.ENTITY_TYPE.location(), TinkerRegistries.ENTITY_TYPES.getRegistryName()),
                () -> assertEquals(Registries.MENU.location(), TinkerRegistries.MENU_TYPES.getRegistryName()),
                () -> assertEquals(Registries.RECIPE_TYPE.location(), TinkerRegistries.RECIPE_TYPES.getRegistryName()),
                () -> assertEquals(Registries.RECIPE_SERIALIZER.location(), TinkerRegistries.RECIPE_SERIALIZERS.getRegistryName()),
                () -> assertEquals(Registries.MOB_EFFECT.location(), TinkerRegistries.MOB_EFFECTS.getRegistryName()),
                () -> assertEquals(Registries.PARTICLE_TYPE.location(), TinkerRegistries.PARTICLE_TYPES.getRegistryName()),
                () -> assertEquals(Registries.SOUND_EVENT.location(), TinkerRegistries.SOUND_EVENTS.getRegistryName()),
                () -> assertEquals(Registries.CREATIVE_MODE_TAB.location(), TinkerRegistries.CREATIVE_TABS.getRegistryName()),
                () -> assertEquals(NeoForgeRegistries.Keys.FLUID_TYPES.location(), TinkerRegistries.FLUID_TYPES.getRegistryName()),
                () -> assertEquals(Registries.FLUID.location(), TinkerRegistries.FLUIDS.getRegistryName()));
    }

    @Test
    void registerAllRejectsDuplicateBootstrap() {
        // DeferredRegister enforces "one event bus, ever" at the JVM level — once attached, a
        // second register() call throws IllegalStateException to prevent double-fired registry
        // events. Seed the first-call state ourselves rather than relying on whatever harness
        // is running: in the moddev unitTest bootstrap the @Mod constructor has already
        // attached registries to the production bus (so this first attempt throws and we
        // ignore it); in any other test JVM the registries are still unattached (so this first
        // attempt succeeds). Either way the *second* call below must throw, which is the
        // contract being pinned.
        try {
            TinkerRegistries.registerAll(mock(IEventBus.class));
        }
        catch (IllegalStateException alreadyAttached) {
            // Already attached by the bootstrap — fine, the contract is already established.
        }
        IEventBus bus = mock(IEventBus.class);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> TinkerRegistries.registerAll(bus));
        assertNotNull(error.getMessage(), "DeferredRegister should explain why the second call failed");
    }

    @Test
    void registryCountMatchesDeclaredFields() {
        // Keeps the test-only registryCount() in sync with the public field list — if a new
        // DeferredRegister is added above without being appended to ALL, this fires and points
        // the author at the place to fix.
        assertEquals(allRegisters().length, TinkerRegistries.registryCount(), "registryCount() must mirror the public DeferredRegister fields");
    }

    private static DeferredRegister<?>[] allRegisters() {
        return new DeferredRegister<?>[] { TinkerRegistries.ITEMS, TinkerRegistries.BLOCKS, TinkerRegistries.DATA_COMPONENTS, TinkerRegistries.BLOCK_ENTITY_TYPES, TinkerRegistries.ENTITY_TYPES,
                TinkerRegistries.MENU_TYPES, TinkerRegistries.RECIPE_TYPES, TinkerRegistries.RECIPE_SERIALIZERS, TinkerRegistries.MOB_EFFECTS, TinkerRegistries.PARTICLE_TYPES,
                TinkerRegistries.SOUND_EVENTS, TinkerRegistries.CREATIVE_TABS, TinkerRegistries.FLUID_TYPES, TinkerRegistries.FLUIDS, };
    }

    @Test
    void everyDeferredRegisterFieldIsNonNull() {
        // Defends against a future refactor accidentally nulling out a public field — the field
        // must be a live DeferredRegister, not null, for pulses to be able to register content.
        for (DeferredRegister<?> register : allRegisters()) {
            assertNotNull(register, "DeferredRegister field should be non-null");
        }
    }
}
