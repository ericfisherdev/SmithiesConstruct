package slimeknights.sconstruct.port1211.tools.item;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Pinned-behaviour tests for {@link ToolItems}. Covers the SMTCON-79 acceptance criteria:
 * four tool items registered, each maps to a typed {@link ToolCore} subclass, registration
 * paths match the canonical {@code pickaxe / shovel / axe / sword} keys, and the accept-all
 * visitor reaches every tool — proxy for "Each appears in creative inventory" since the
 * {@code BuildCreativeModeTabContentsEvent} listener routes through the same visitor.
 */
class ToolItemsTest {

    @Test
    void registersAllShippedTools() {
        assertEquals(13, ToolItems.registeredCount(), "4 basic + 5 AOE + 4 melee variants");
        assertEquals(13, ToolItems.ALL_TOOLS.size());
    }

    @Test
    void everyDeferredItemIsNonNull() {
        assertAll(() -> assertNotNull(ToolItems.PICKAXE), () -> assertNotNull(ToolItems.SHOVEL), () -> assertNotNull(ToolItems.AXE), () -> assertNotNull(ToolItems.SWORD),
                () -> assertNotNull(ToolItems.HAMMER), () -> assertNotNull(ToolItems.EXCAVATOR), () -> assertNotNull(ToolItems.LUMBER_AXE), () -> assertNotNull(ToolItems.SCYTHE),
                () -> assertNotNull(ToolItems.MATTOCK));
    }

    @Test
    void aoeToolsAppearInAllToolsRoster() {
        // Pin presence of every AOE tool in the iteration surface — a future ALL_TOOLS rewire
        // that drops one would silently de-register it from the creative tab.
        assertAll(() -> assertTrue(ToolItems.ALL_TOOLS.contains(ToolItems.HAMMER)), () -> assertTrue(ToolItems.ALL_TOOLS.contains(ToolItems.EXCAVATOR)),
                () -> assertTrue(ToolItems.ALL_TOOLS.contains(ToolItems.LUMBER_AXE)), () -> assertTrue(ToolItems.ALL_TOOLS.contains(ToolItems.SCYTHE)),
                () -> assertTrue(ToolItems.ALL_TOOLS.contains(ToolItems.MATTOCK)));
    }

    @Test
    void aoeToolRegistrationPathsMatchExpectedKeys() {
        assertAll(() -> assertEquals("hammer", ToolItems.registeredPath(ToolItems.HAMMER)), () -> assertEquals("excavator", ToolItems.registeredPath(ToolItems.EXCAVATOR)),
                () -> assertEquals("lumberaxe", ToolItems.registeredPath(ToolItems.LUMBER_AXE)), () -> assertEquals("scythe", ToolItems.registeredPath(ToolItems.SCYTHE)),
                () -> assertEquals("mattock", ToolItems.registeredPath(ToolItems.MATTOCK)));
    }

    @Test
    void aoeToolsBindToTheirToolDefinitions() {
        assertAll(() -> assertEquals(ToolDefinition.HAMMER, ToolItems.HAMMER.get().definition), () -> assertEquals(ToolDefinition.EXCAVATOR, ToolItems.EXCAVATOR.get().definition),
                () -> assertEquals(ToolDefinition.LUMBER_AXE, ToolItems.LUMBER_AXE.get().definition), () -> assertEquals(ToolDefinition.SCYTHE, ToolItems.SCYTHE.get().definition),
                () -> assertEquals(ToolDefinition.MATTOCK, ToolItems.MATTOCK.get().definition));
    }

    @Test
    void meleeToolRegistrationPathsMatchExpectedKeys() {
        assertAll(() -> assertEquals("cleaver", ToolItems.registeredPath(ToolItems.CLEAVER)), () -> assertEquals("longsword", ToolItems.registeredPath(ToolItems.LONGSWORD)),
                () -> assertEquals("rapier", ToolItems.registeredPath(ToolItems.RAPIER)), () -> assertEquals("froe", ToolItems.registeredPath(ToolItems.FROE)));
    }

    @Test
    void meleeToolsBindToTheirDefinitions() {
        assertAll(() -> assertEquals(ToolDefinition.CLEAVER, ToolItems.CLEAVER.get().definition), () -> assertEquals(ToolDefinition.LONGSWORD, ToolItems.LONGSWORD.get().definition),
                () -> assertEquals(ToolDefinition.RAPIER, ToolItems.RAPIER.get().definition), () -> assertEquals(ToolDefinition.FROE, ToolItems.FROE.get().definition));
    }

    @Test
    void meleeToolRuntimeTypesMatchExpectedSubclasses() {
        assertAll(() -> assertTrue(ToolItems.CLEAVER.get() instanceof CleaverItem), () -> assertTrue(ToolItems.LONGSWORD.get() instanceof LongswordItem),
                () -> assertTrue(ToolItems.RAPIER.get() instanceof RapierItem), () -> assertTrue(ToolItems.FROE.get() instanceof FroeItem));
    }

    @Test
    void aoeToolRuntimeTypesMatchExpectedSubclasses() {
        assertAll(() -> assertTrue(ToolItems.HAMMER.get() instanceof HammerItem), () -> assertTrue(ToolItems.EXCAVATOR.get() instanceof ExcavatorItem),
                () -> assertTrue(ToolItems.LUMBER_AXE.get() instanceof LumberAxeItem), () -> assertTrue(ToolItems.SCYTHE.get() instanceof ScytheItem),
                () -> assertTrue(ToolItems.MATTOCK.get() instanceof MattockItem));
    }

    @Test
    void registrationPathsMatchVanillaToolSlotNames() {
        // Players /give-ing the items expect the canonical vanilla slot names — pickaxe / shovel
        // / axe / sword — rather than the legacy {@code hatchet} / {@code broadsword} part-type
        // identifiers underneath. Pins so a future rename doesn't silently break command-line
        // workflows.
        assertAll(() -> assertEquals("pickaxe", ToolItems.registeredPath(ToolItems.PICKAXE)), () -> assertEquals("shovel", ToolItems.registeredPath(ToolItems.SHOVEL)),
                () -> assertEquals("axe", ToolItems.registeredPath(ToolItems.AXE)), () -> assertEquals("sword", ToolItems.registeredPath(ToolItems.SWORD)));
    }

    @Test
    void eachToolBindsToTheExpectedToolDefinition() {
        // Pin the definition mapping per subclass — a future rename of ToolDefinition constants
        // would otherwise silently swap which tool gets which part-slot recipe.
        assertAll(() -> assertEquals(ToolDefinition.PICKAXE, ToolItems.PICKAXE.get().definition), () -> assertEquals(ToolDefinition.SHOVEL, ToolItems.SHOVEL.get().definition),
                () -> assertEquals(ToolDefinition.HATCHET, ToolItems.AXE.get().definition), () -> assertEquals(ToolDefinition.BROADSWORD, ToolItems.SWORD.get().definition));
    }

    @Test
    void aoeToolsBindToTheirAoePatterns() {
        // Pin pattern selection per AOE tool so a future rewire doesn't silently swap which tool
        // gets which AOE shape — the player-facing behaviour difference between TREE / FULL3x3 /
        // COLUMN_1x3 is non-trivial.
        assertAll(() -> assertEquals(AoePattern.FULL3x3, ToolItems.HAMMER.get().aoePattern()), () -> assertEquals(AoePattern.FULL3x3, ToolItems.EXCAVATOR.get().aoePattern()),
                () -> assertEquals(AoePattern.TREE, ToolItems.LUMBER_AXE.get().aoePattern()), () -> assertEquals(AoePattern.FULL3x3, ToolItems.SCYTHE.get().aoePattern()),
                () -> assertEquals(AoePattern.COLUMN_1x3, ToolItems.MATTOCK.get().aoePattern()));
    }

    @Test
    void registeredItemsAreTypedToolCoreSubclasses() {
        // The DeferredItem generic param is only enforced at compile time; assert the runtime
        // instance is the expected subclass so a future rewire that loosens the factory doesn't
        // slip a foreign type into the registry.
        assertAll(() -> assertTrue(ToolItems.PICKAXE.get() instanceof PickaxeItem), () -> assertTrue(ToolItems.SHOVEL.get() instanceof ShovelItem),
                () -> assertTrue(ToolItems.AXE.get() instanceof AxeItem), () -> assertTrue(ToolItems.SWORD.get() instanceof SwordItem));
    }

    @Test
    void acceptAllVisitsEveryRegisteredToolExactlyOnce() {
        // BuildCreativeModeTabContentsEvent listener delegates here, so verifying coverage is
        // the unit-level proxy for "Each appears in creative inventory". Cross-check against the
        // canonical ALL_TOOLS set so a swap that replaced one tool with a foreign ItemLike of
        // the same identity bucket would still fail.
        List<ItemLike> visited = new ArrayList<>();
        ToolItems.acceptAll(visited::add);
        Set<ItemLike> canonical = ToolItems.ALL_TOOLS.stream().map(DeferredItem::get).collect(Collectors.toSet());
        assertAll(() -> assertEquals(13, visited.size(), "visitor must reach every tool exactly once"), () -> assertEquals(13L, visited.stream().distinct().count(), "no duplicates"),
                () -> assertEquals(canonical, new HashSet<>(visited), "visited set must equal the canonical ALL_TOOLS values"));
    }

    @Test
    void allToolsIsImmutable() {
        // The list is part of the public registration surface — downstream pulses must not be
        // able to mutate the iteration order or roster after registration. List.of returns an
        // immutable list, so any mutator must raise UnsupportedOperationException.
        assertEquals(13, ToolItems.ALL_TOOLS.size());
        assertThrows(UnsupportedOperationException.class, () -> ToolItems.ALL_TOOLS.add(null));
    }

    @Test
    void itemReferenceIsAddressableByItsRegistryHandle() {
        // The DeferredItem handle is the addressable entry point — assert it returns a non-null
        // Item once the deferred holder has resolved so downstream pulses can chain through.
        Item resolved = ToolItems.PICKAXE.get();
        assertNotNull(resolved);
    }
}
