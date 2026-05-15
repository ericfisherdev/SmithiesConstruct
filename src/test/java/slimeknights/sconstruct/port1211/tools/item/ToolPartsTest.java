package slimeknights.sconstruct.port1211.tools.item;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.tools.PartType;

/**
 * Pinned-behaviour tests for {@link ToolParts}. Covers the three acceptance criteria
 * SMTCON-67 calls out: 16 part items registered, lookup-by-PartType returns the right entry,
 * and the accept-all visitor reaches every entry (proxy for "visible in creative inventory"
 * since the BuildCreativeModeTabContentsEvent listener routes through the same visitor).
 */
class ToolPartsTest {

    @Test
    void registersOneItemPerPartType() {
        assertEquals(PartType.values().length, ToolParts.registeredCount(), "one DeferredItem per PartType");
        // SMTCON-67 pinned 16; SMTCON-73 appended WIDEGUARD + LARGEPLATE so legacy-accurate
        // ToolDefinitions (BroadSword, Hammer, LumberAxe) had every part slot to bind to.
        assertEquals(18, ToolParts.registeredCount(), "PartType roster: 16 legacy + 2 SMTCON-73 additions");
    }

    @Test
    void everyPartTypeMapsToANonNullDeferredItem() {
        for (PartType part : PartType.values()) {
            assertNotNull(ToolParts.get(part), () -> "missing entry for " + part);
        }
    }

    @Test
    void getRejectsNullPartType() {
        // Defensive contract — a null lookup is a calling-code bug we want to surface at the
        // call site rather than letting the null propagate into a follow-up NullPointerException
        // somewhere downstream.
        assertThrows(NullPointerException.class, () -> ToolParts.get(null));
    }

    @Test
    void registeredPathMatchesPartTypeId() {
        // The registry id path must equal PartType#id() so the codec form round-trips
        // byte-for-byte against the registry-side key — regressions here would silently break
        // any data-driven part lookup keyed by id (recipes, JSON tags).
        for (PartType part : PartType.values()) {
            assertEquals(part.id(), ToolParts.registeredPath(part), () -> "registry path must equal id for " + part);
        }
    }

    @Test
    void registeredItemsAreMaterialItemInstances() {
        // The DeferredItem<MaterialItem> generic param is only enforced at compile time; this
        // confirms the actual registered instance is a MaterialItem at runtime — guards against
        // a future refactor that loosens the generic without updating the registration factory.
        for (PartType part : PartType.values()) {
            assertTrue(ToolParts.isMaterialItem(part), () -> "registered item must be a MaterialItem for " + part);
        }
    }

    @Test
    void getReturnsTheSameDeferredItemAsThePartsMap() {
        // PARTS is the public unmodifiable view; get(...) is the helper. They must agree, or
        // downstream code that mixes the two would race on which entry it sees.
        for (PartType part : PartType.values()) {
            assertSame(ToolParts.PARTS.get(part), ToolParts.get(part));
        }
    }

    @Test
    void partsMapIsUnmodifiableSoCallersCannotMutateRegistrationState() {
        assertThrows(UnsupportedOperationException.class, () -> ToolParts.PARTS.put(PartType.PICKHEAD, null));
        assertThrows(UnsupportedOperationException.class, () -> ToolParts.PARTS.remove(PartType.HANDLE));
    }

    @Test
    void acceptAllVisitsEveryRegisteredPartItemExactlyOnce() {
        // The BuildCreativeModeTabContentsEvent listener delegates to this helper, so verifying
        // it covers every part is the unit-level proxy for "Each visible in creative inventory".
        // Count and distinctness alone would miss a swap that replaced one registered item with
        // a foreign ItemLike of the same identity bucket — so also assert set-equality against
        // the canonical PARTS values.
        List<ItemLike> visited = new ArrayList<>();
        ToolParts.acceptAll(visited::add);
        Set<ItemLike> canonical = ToolParts.PARTS.values().stream().map(DeferredItem::get).collect(Collectors.toSet());
        assertAll(() -> assertEquals(PartType.values().length, visited.size(), "visitor must reach every part exactly once"),
                () -> assertEquals(PartType.values().length, visited.stream().distinct().count(), "no duplicates"),
                () -> assertEquals(canonical, new HashSet<>(visited), "visited set must equal the canonical PARTS values — no foreign items"));
    }
}
