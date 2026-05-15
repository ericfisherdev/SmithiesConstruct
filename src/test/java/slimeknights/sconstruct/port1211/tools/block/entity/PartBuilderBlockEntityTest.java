package slimeknights.sconstruct.port1211.tools.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.tools.material.Material;

/**
 * Pinned-behaviour tests for {@link PartBuilderBlockEntity#firstMaterialMatching}. The full BE
 * construction path needs a live {@link net.minecraft.world.level.block.entity.BlockEntityType}
 * (registry-bound) and a live {@link net.minecraft.world.level.Level}, neither of which the
 * unit-test harness exposes — but the matching rule is the load-bearing piece of the resolve
 * step and is exposed as a pure static helper for exactly this reason.
 *
 * <p>The tests build a small {@link Material} list (some with a repair tag, some without) and
 * stub {@link ItemStack#is(TagKey)} for each variant so the first-match-wins ordering and the
 * "skip materials with no repair tag" rule are both exercised directly.
 */
class PartBuilderBlockEntityTest {

    private static final ResourceLocation WOOD_ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");
    private static final ResourceLocation IRON_ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    private static final TagKey<Item> PLANKS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("planks"));
    private static final TagKey<Item> IRON_INGOTS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("iron_ingots"));

    @Test
    void firstMaterialMatchingReturnsTheHolderWhoseRepairTagContainsTheStack() {
        // ItemStack mocked: "is iron_ingots" true, others false. Expect the iron material to
        // be returned even though wood is earlier in the stream — wood's tag doesn't match.
        ItemStack ironStack = mock(ItemStack.class);
        when(ironStack.is(PLANKS_TAG)).thenReturn(false);
        when(ironStack.is(IRON_INGOTS_TAG)).thenReturn(true);

        Holder<Material> wood = holderOf(WOOD_ID, Optional.of(PLANKS_TAG));
        Holder<Material> iron = holderOf(IRON_ID, Optional.of(IRON_INGOTS_TAG));

        Optional<Holder<Material>> result = PartBuilderBlockEntity.firstMaterialMatching(Stream.of(wood, iron), ironStack);
        assertTrue(result.isPresent(), "iron material should match the iron ingot stack");
        assertSame(iron, result.get());
    }

    @Test
    void firstMaterialMatchingReturnsEmptyWhenNoMaterialMatches() {
        ItemStack stoneStack = mock(ItemStack.class);
        when(stoneStack.is(PLANKS_TAG)).thenReturn(false);
        when(stoneStack.is(IRON_INGOTS_TAG)).thenReturn(false);

        Holder<Material> wood = holderOf(WOOD_ID, Optional.of(PLANKS_TAG));
        Holder<Material> iron = holderOf(IRON_ID, Optional.of(IRON_INGOTS_TAG));

        Optional<Holder<Material>> result = PartBuilderBlockEntity.firstMaterialMatching(Stream.of(wood, iron), stoneStack);
        assertTrue(result.isEmpty(), "stone stack matches no repair tag — must return empty");
    }

    @Test
    void firstMaterialMatchingSkipsMaterialsWithoutARepairTag() {
        // A material with Optional.empty() repairTag (slime, paper in the real roster) must
        // not be considered — the matching rule requires the repair tag to be present.
        ItemStack anyStack = mock(ItemStack.class);
        // Defensive — the stack should never be queried with a missing tag, but if the code
        // ever regresses and calls is(...) on a null TagKey this would NPE; instead leave the
        // mock returning the default (false).
        when(anyStack.is(IRON_INGOTS_TAG)).thenReturn(true);

        Holder<Material> tagless = holderOf(WOOD_ID, Optional.empty());
        Holder<Material> iron = holderOf(IRON_ID, Optional.of(IRON_INGOTS_TAG));

        Optional<Holder<Material>> result = PartBuilderBlockEntity.firstMaterialMatching(Stream.of(tagless, iron), anyStack);
        assertTrue(result.isPresent());
        assertEquals(IRON_ID, result.get().value().id(), "tagless material must be skipped, iron is the first matching tagged material");
    }

    @Test
    void firstMaterialMatchingHonoursStreamOrderingForFirstMatchWins() {
        // Both materials' tags match — the first one in the stream is the winner.
        ItemStack stack = mock(ItemStack.class);
        when(stack.is(PLANKS_TAG)).thenReturn(true);
        when(stack.is(IRON_INGOTS_TAG)).thenReturn(true);

        Holder<Material> wood = holderOf(WOOD_ID, Optional.of(PLANKS_TAG));
        Holder<Material> iron = holderOf(IRON_ID, Optional.of(IRON_INGOTS_TAG));

        Optional<Holder<Material>> result = PartBuilderBlockEntity.firstMaterialMatching(Stream.of(wood, iron), stack);
        assertTrue(result.isPresent());
        assertSame(wood, result.get(), "wood is first in the stream and its tag matches — first-match-wins selects it");
    }

    @SuppressWarnings("unchecked")
    private static Holder<Material> holderOf(ResourceLocation id, Optional<TagKey<Item>> repairTag) {
        Material material = new Material(id, 1, repairTag, Map.of(), List.of(), 0);
        Holder<Material> holder = mock(Holder.class);
        when(holder.value()).thenReturn(material);
        return holder;
    }
}
