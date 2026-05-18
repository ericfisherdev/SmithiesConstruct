package slimeknights.sconstruct.gametest;

import java.util.Collections;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.common.data.ToolStats;
import slimeknights.sconstruct.tools.ToolDefinition;
import slimeknights.sconstruct.tools.ToolHelper;
import slimeknights.sconstruct.tools.item.ToolCore;
import slimeknights.sconstruct.tools.item.ToolItems;
import slimeknights.sconstruct.tools.modifier.Modifier;

/**
 * Cross-product {@link GameTest}s for the tool system (SMTCON-175) — coverage for the
 * combinatorial behaviours a per-phase test cannot reach because they only regress when the
 * full roster of tools / modifiers / materials interacts.
 *
 * <p>Where {@link PickaxeTests} pins one tool's build / mine / repair lifecycle in depth,
 * this class sweeps breadth: every {@link ToolDefinition} builds, every registered
 * {@link Modifier} applies, durability latches the broken flag at zero, and a repair from a
 * broken state fully restores. Each test runs inside a real {@code gameTestServer} so the
 * materials and modifier datapack registries are the live server-authoritative ones.
 *
 * <p>The four cases map directly to SMTCON-175's acceptance criteria; each loop asserts a
 * count at the end so the coverage matrix (which tool / modifier was exercised) is recorded
 * as a test assertion rather than left implicit.
 */
// Every test borrows the MinecraftServer from the gameTestServer harness via
// GameTestHelper#getLevel — the harness owns its lifecycle and the test must never close it.
// PMD's CloseResource heuristic cannot model that borrowed-handle ownership.
@SuppressWarnings("PMD.CloseResource")
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ToolMatrixTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Iron material id — mid-tier material with a head, handle, extra, bow, and arrow stat
     *  block, so it is a valid part for every {@link PartType} slot any tool declares. */
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    /** Wood material id — the lowest head durability in the roster ({@code 35}), so a wood
     *  pickaxe is the fastest tool to drive to its broken threshold. */
    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");

    /** The 20 built-in modifiers SMTCON-175's plan calls out — the floor the registry sweep
     *  asserts against so a dropped modifier JSON fails the test loudly. */
    private static final int EXPECTED_MODIFIER_COUNT = 20;

    private ToolMatrixTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void buildEveryToolWithIron(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        int built = 0;
        for (DeferredItem<? extends ToolCore> handle : ToolItems.ALL_TOOLS) {
            ToolCore item = handle.get();
            ToolDefinition definition = item.definition;

            ItemStack stack = new ItemStack(item);
            ToolHelper.setMaterials(stack, Collections.nCopies(definition.getPartCount(), IRON));
            ToolHelper.rebuildStats(stack, server, definition);

            ToolStats stats = ToolHelper.getStats(stack);
            // Every built tool clears the StatsBuilder's max(1, ...) durability floor and is
            // therefore never the zero snapshot — a build that produced ToolStats.zero() means
            // the rebuild silently no-op'd rather than aggregating the iron part stats.
            helper.assertTrue(stats.maxDurability() > 0, definition.id() + " builds with a positive max durability");
            helper.assertFalse(stats.equals(ToolStats.zero()), definition.id() + " produces a non-trivial stat snapshot");
            helper.assertFalse(ToolHelper.isBroken(stack), definition.id() + " is intact immediately after building");
            built++;
        }
        helper.assertValueEqual(built, ToolItems.ALL_TOOLS.size(), "every registered tool definition was built and asserted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void applyAllModifiers(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        HolderLookup.RegistryLookup<Modifier> modifiers = server.registryAccess().lookupOrThrow(Modifier.REGISTRY_KEY);
        List<ResourceKey<Modifier>> modifierKeys = modifiers.listElementIds().toList();
        helper.assertTrue(modifierKeys.size() >= EXPECTED_MODIFIER_COUNT, "at least " + EXPECTED_MODIFIER_COUNT + " built-in modifiers are registered (found " + modifierKeys.size() + ")");

        int applied = 0;
        for (ResourceKey<Modifier> key : modifierKeys) {
            ResourceLocation modId = key.location();
            // A fresh tool per modifier isolates each application — a slot-cost or hook
            // interaction between modifiers cannot mask a single modifier that fails to apply.
            ItemStack stack = new ItemStack(ToolItems.PICKAXE.get());
            ToolHelper.setMaterials(stack, Collections.nCopies(ToolDefinition.PICKAXE.getPartCount(), IRON));
            ToolHelper.rebuildStats(stack, server, ToolDefinition.PICKAXE);

            ToolHelper.addModifier(stack, modId, 1);
            // Rebuild after applying so the modifier's onBuild hook runs through the dispatcher
            // — a modifier whose hook throws fails here rather than silently at first use.
            ToolHelper.rebuildStats(stack, server, ToolDefinition.PICKAXE);

            helper.assertValueEqual(ToolHelper.getModifierLevel(stack, modId), 1, "modifier " + modId + " applies at level 1");
            applied++;
        }
        helper.assertValueEqual(applied, modifierKeys.size(), "every registered modifier was applied and asserted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void maxDurabilityBreaksAtZero(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        // A wood pickaxe is the lowest-durability build in the roster — its broken threshold is
        // reached in the fewest durability ticks.
        ItemStack pickaxe = new ItemStack(ToolItems.PICKAXE.get());
        ToolHelper.setMaterials(pickaxe, Collections.nCopies(ToolDefinition.PICKAXE.getPartCount(), WOOD));
        ToolHelper.rebuildStats(pickaxe, server, ToolDefinition.PICKAXE);

        int maxDurability = ToolHelper.getStats(pickaxe).maxDurability();
        helper.assertTrue(maxDurability > 0, "the wood pickaxe has a positive durability budget");
        helper.assertFalse(ToolHelper.isBroken(pickaxe), "the fresh wood pickaxe is intact");

        // Spend the whole durability budget one tick at a time, mirroring per-block mining
        // wear. ToolCore#damageItem returns the wear to actually apply (the broken-tick path
        // returns 0 and pins the damage itself); the loop applies that wear the way vanilla's
        // hurtAndBreak would. The broken flag must latch exactly when the budget runs out.
        int ticks = 0;
        while (!ToolHelper.isBroken(pickaxe) && ticks <= maxDurability) {
            int wear = ToolItems.PICKAXE.get().damageItem(pickaxe, 1, null, item -> {
                // on-break callback unused — the broken flag is read back via ToolHelper.
            });
            if (wear > 0) {
                pickaxe.setDamageValue(pickaxe.getDamageValue() + wear);
            }
            ticks++;
        }
        helper.assertTrue(ToolHelper.isBroken(pickaxe), "the pickaxe latches the broken flag once its durability is spent");
        helper.assertValueEqual(ticks, maxDurability, "the broken flag latches exactly when the durability budget reaches zero");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void repairFullyRestores(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ItemStack pickaxe = new ItemStack(ToolItems.PICKAXE.get());
        ToolHelper.setMaterials(pickaxe, Collections.nCopies(ToolDefinition.PICKAXE.getPartCount(), IRON));
        ToolHelper.rebuildStats(pickaxe, server, ToolDefinition.PICKAXE);
        int maxDurability = ToolHelper.getStats(pickaxe).maxDurability();

        // Drive the tool to its broken threshold: damage pinned one below max, then the
        // damageItem hook flips the broken flag on the final tick.
        pickaxe.setDamageValue(maxDurability - 1);
        ToolItems.PICKAXE.get().damageItem(pickaxe, 1, null, item -> {
            // on-break callback unused.
        });
        helper.assertTrue(ToolHelper.isBroken(pickaxe), "the pickaxe is broken before the repair");

        // A stack of iron ingots large enough that the 25%-per-item repair fully restores the
        // tool — repair caps at fully repaired, so any surplus is simply not consumed.
        ItemStack repairMaterial = new ItemStack(Items.IRON_INGOT, 16);
        int consumed = ToolHelper.repair(pickaxe, repairMaterial, server, ToolDefinition.PICKAXE);

        helper.assertTrue(consumed > 0, "the repair consumed at least one iron ingot");
        helper.assertFalse(ToolHelper.isBroken(pickaxe), "the repair cleared the broken flag");
        helper.assertValueEqual(pickaxe.getDamageValue(), 0, "a surplus repair material fully restores the tool's durability");
        helper.succeed();
    }
}
