package slimeknights.sconstruct.port1211.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import slimeknights.sconstruct.port1211.common.data.ToolStats;
import slimeknights.sconstruct.port1211.tools.ToolDefinition;
import slimeknights.sconstruct.port1211.tools.ToolHelper;
import slimeknights.sconstruct.port1211.tools.item.ToolItems;

/**
 * End-to-end {@link GameTest}s for the pickaxe build / mine / break-and-repair lifecycle —
 * the SMTCON-77 → SMTCON-80 contract chain. These run inside a real NeoForge
 * {@code gameTestServer} so the materials datapack registry, server-thread affinity, and
 * vanilla {@code DataComponents} integration are exercised against the same surfaces a
 * player hits in a live world.
 *
 * <p>Reuses the shared {@code gametest_7x7x7} air template from
 * {@link SlimeIslandTests}: tool-lifecycle tests don't need terrain — every check operates on
 * an {@link ItemStack} held off the world map or against an isolated block the helper places
 * inside the structure bounds.
 *
 * <p>The three cases map directly to SMTCON-81's acceptance criteria:
 * <ol>
 *   <li>{@link #buildPickaxe} — build a pickaxe out of iron, assert the cached
 *       {@link ToolStats} component matches the expected iron-pickaxe values.</li>
 *   <li>{@link #minePickaxe} — verify the built pickaxe answers {@link ToolDefinition#PICKAXE}
 *       contract for stone blocks (correct-for-drops + non-trivial destroy speed) so an
 *       in-world swing would actually mine; the swing event itself is covered by the
 *       attribute-modifier path which vanilla wires from {@link ToolStats#attackDamage}.</li>
 *   <li>{@link #breakAndRepair} — drive damage past max via {@link ToolHelper}'s break hook
 *       and prove the BROKEN flag latches, then call {@link ToolHelper#repair} with an iron
 *       ingot and prove the flag clears with damage reduced.</li>
 * </ol>
 *
 * <p>Determinism: every test rebuilds its own ItemStack from scratch and never reads from a
 * shared mutable; the gameTestServer harness re-runs the test class in isolation across
 * repeats, so a 5× run produces identical traces.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PickaxeTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Iron material id — the canonical mid-tier material from {@code TinkerMaterialBootstrap}.
     *  Registered under the legacy {@code tconstruct} namespace so cross-mod material references
     *  resolved against id keep working when SMTCON sits alongside addons that still ship
     *  materials under {@code tconstruct:*}. */
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    /** Expected iron-pickaxe durability: head {@code 250} × handle modifier {@code 1.0} +
     *  binding extra {@code 25} (legacy 1.12 parity). */
    private static final int EXPECTED_DURABILITY = 275;

    private PickaxeTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void buildPickaxe(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ToolItems.PICKAXE.get());
        ToolHelper.setMaterials(stack, List.of(IRON, IRON, IRON));
        ToolHelper.rebuildStats(stack, helper.getLevel().getServer(), ToolDefinition.PICKAXE);

        ToolStats stats = ToolHelper.getStats(stack);
        helper.assertValueEqual(stats.maxDurability(), EXPECTED_DURABILITY, "iron pickaxe durability (head 250 × handle 1.0 + extra 25)");
        helper.assertValueEqual(stats.harvestLevel(), 2, "iron pickaxe harvest level (iron head tier)");
        // HeadStats positional fields: (durability, harvestLevel, miningSpeed, attackDamage) —
        // iron's TinkerMaterialBootstrap entry feeds (250, 2, 6.0F, 4.0F).
        helper.assertTrue(Math.abs(stats.attackDamage() - 4.0F) < 0.0001F, "iron pickaxe attack damage (head 4.0)");
        helper.assertTrue(Math.abs(stats.miningSpeed() - 6.0F) < 0.0001F, "iron pickaxe mining speed (head 6.0 × handle 1.0)");
        helper.assertValueEqual(stack.getMaxDamage(), EXPECTED_DURABILITY, "vanilla MAX_DAMAGE component mirrors stats");
        helper.assertFalse(ToolHelper.isBroken(stack), "fresh tool is not broken");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void minePickaxe(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ToolItems.PICKAXE.get());
        ToolHelper.setMaterials(stack, List.of(IRON, IRON, IRON));
        ToolHelper.rebuildStats(stack, helper.getLevel().getServer(), ToolDefinition.PICKAXE);

        BlockState stone = Blocks.STONE.defaultBlockState();
        // The pickaxe must be correct for stone (drops) and mine it faster than the vanilla
        // fall-through speed of 1.0F — anything less and the tool wouldn't actually progress
        // a block-break tick at this material tier.
        helper.assertTrue(ToolItems.PICKAXE.get().isCorrectToolForDrops(stack, stone), "iron pickaxe is correct for stone drops");
        helper.assertTrue(ToolItems.PICKAXE.get().getDestroySpeed(stack, stone) > 1.0F, "iron pickaxe mines stone faster than the vanilla 1.0F baseline");

        // Iron pickaxe cleanly clears NEEDS_IRON_TOOL blocks — diamond ore vanilla-tags into
        // NEEDS_IRON_TOOL (an iron pickaxe is the minimum tier that drops the diamond).
        helper.assertTrue(ToolItems.PICKAXE.get().isCorrectToolForDrops(stack, Blocks.DIAMOND_ORE.defaultBlockState()), "iron pickaxe gates past NEEDS_IRON_TOOL on diamond ore");

        // But it must NOT be correct for NEEDS_DIAMOND_TOOL blocks — obsidian is the canonical
        // diamond-needs block, so an iron pickaxe (harvest level 2) is below the tier-3 floor.
        // Pins the upper bound of the harvest-level ladder against an in-world block.
        helper.assertFalse(ToolItems.PICKAXE.get().isCorrectToolForDrops(stack, Blocks.OBSIDIAN.defaultBlockState()), "iron pickaxe is blocked from NEEDS_DIAMOND_TOOL blocks (obsidian)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void breakAndRepair(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ToolItems.PICKAXE.get());
        ToolHelper.setMaterials(stack, List.of(IRON, IRON, IRON));
        ToolHelper.rebuildStats(stack, helper.getLevel().getServer(), ToolDefinition.PICKAXE);

        // Drive the durability tick to the broken threshold via the ToolCore damageItem hook:
        // setDamageValue to max-1 then call the hook with amount=1 to flip the broken flag.
        stack.setDamageValue(EXPECTED_DURABILITY - 1);
        int leftover = ToolItems.PICKAXE.get().damageItem(stack, 1, null, item -> {
            /* on-break callback unused */ });
        helper.assertValueEqual(leftover, 0, "damageItem must absorb the tick that would otherwise shrink the stack");
        helper.assertTrue(ToolHelper.isBroken(stack), "tool flips to broken once damage reaches max");
        helper.assertValueEqual(stack.getDamageValue(), EXPECTED_DURABILITY - 1, "damageValue pinned one below max so vanilla hurtAndBreak never shrinks");

        // Repair with one iron ingot: 25% of maxDurability = 69 (Math.round(275 × 0.25) = 69).
        // ingest enough items to fully restore from a 274-damage broken tool.
        ItemStack repairItem = new ItemStack(Items.IRON_INGOT, 8);
        int consumed = ToolHelper.repair(stack, repairItem, helper.getLevel().getServer(), ToolDefinition.PICKAXE);
        helper.assertTrue(consumed > 0, "repair consumes at least one item");
        helper.assertFalse(ToolHelper.isBroken(stack), "any repair against a broken tool clears the broken flag");
        helper.assertTrue(stack.getDamageValue() < EXPECTED_DURABILITY - 1, "repair lowers the damage value below the pre-repair pin");
        helper.succeed();
    }
}
