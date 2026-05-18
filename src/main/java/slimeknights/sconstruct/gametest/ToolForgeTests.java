package slimeknights.sconstruct.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import slimeknights.sconstruct.common.data.TinkerDataComponents;
import slimeknights.sconstruct.tools.PartType;
import slimeknights.sconstruct.tools.ToolDefinition;
import slimeknights.sconstruct.tools.ToolHelper;
import slimeknights.sconstruct.tools.ToolStationRegistry;
import slimeknights.sconstruct.tools.block.entity.ToolForgeBlockEntity;
import slimeknights.sconstruct.tools.block.entity.ToolStationBlockEntity;
import slimeknights.sconstruct.tools.item.ToolCore;
import slimeknights.sconstruct.tools.item.ToolItems;
import slimeknights.sconstruct.tools.item.ToolParts;

/**
 * End-to-end {@link GameTest} for the Tool Forge (SMTCON-93) — proves the forge's BE attaches
 * with the broadened advanced-tool candidate roster ({@link ToolDefinition#ALL_ADVANCED}) and
 * builds a tool from positional iron parts.
 *
 * <p><strong>Roster note:</strong> the SMTCON-96 spec calls for a 5-part Hammer build to
 * exercise the advanced-only side of the forge's roster. The Hammer {@link ToolCore} item is
 * not yet registered in {@link ToolItems} (the basic four-tool roster — pickaxe, shovel, axe,
 * sword — is what ships today); {@link ToolStationBlockEntity#candidateMap} filters
 * {@link ToolDefinition#ALL_ADVANCED} against the registered {@link ToolCore} items, so a
 * forge-builds-hammer test cannot resolve a buildable Hammer item until a follow-up ticket
 * adds the registration. To still cover the AC's "forge accepts the advanced roster" intent,
 * this test (a) asserts {@link ToolForgeBlockEntity#acceptsAdvancedTools} returns {@code true}
 * so the broader roster is in play, and (b) builds an iron pickaxe at the forge — the basic
 * roster is a subset of the advanced roster, so the build path exercises the same
 * {@link ToolStationBlockEntity#refreshOutput} machinery as the station test.
 *
 * <p>Drives the BE handler directly rather than through the menu — see the class-level note
 * on {@link PatternChestTests}.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ToolForgeTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Placement position inside the 7x7x7 template — chosen well inside the bounds. */
    private static final BlockPos FORGE_POS = new BlockPos(2, 2, 2);

    /** Iron material id — registered under the legacy {@code tconstruct} namespace. */
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    private ToolForgeTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void forgeAcceptsAdvancedRosterAndBuilds(GameTestHelper helper) {
        helper.setBlock(FORGE_POS, ToolStationRegistry.TOOL_FORGE.get());
        ToolStationBlockEntity be = (ToolStationBlockEntity) helper.getBlockEntity(FORGE_POS);
        helper.assertTrue(be != null, "Tool Forge BlockEntity must attach after setBlock");
        helper.assertTrue(be instanceof ToolForgeBlockEntity, "BE attached is the ToolForgeBlockEntity subclass (not the bare station)");
        helper.assertTrue(be.acceptsAdvancedTools(), "Tool Forge BE advertises the advanced tool roster");

        // Build an iron pickaxe at the forge. ALL_BASIC ⊆ ALL_ADVANCED, so the basic 3-part
        // pickaxe is one of the candidates the forge's candidateMap considers — exercises the
        // same refreshOutput / tryBuild / rebuildStats chain as the station test, with the
        // forge's roster predicate in play instead of the station's.
        for (int i = 0; i < ToolDefinition.PICKAXE.getPartCount(); i++) {
            PartType slotPart = ToolDefinition.PICKAXE.getPartSlot(i);
            be.getHandler().setStackInSlot(i, materialPartStack(slotPart, IRON));
        }

        ItemStack output = be.getHandler().getStackInSlot(ToolStationBlockEntity.OUTPUT_SLOT);
        helper.assertFalse(output.isEmpty(), "output assembled at the forge from iron parts");
        helper.assertTrue(output.getItem() instanceof ToolCore, "output is a ToolCore instance");
        helper.assertTrue(output.is(ToolItems.PICKAXE.get()), "output is the pickaxe item");

        List<ResourceLocation> materials = ToolHelper.getMaterials(output);
        helper.assertValueEqual(materials.size(), 3, "output carries three positional material entries");
        for (int i = 0; i < materials.size(); i++) {
            helper.assertTrue(IRON.equals(materials.get(i)), "slot " + i + " material is iron, got " + materials.get(i));
        }
        helper.assertTrue(output.getMaxDamage() > 0, "stat rebuild populated MAX_DAMAGE on the forge output");
        helper.assertFalse(ToolHelper.isBroken(output), "freshly-built tool is not broken");

        helper.succeed();
    }

    /** Build an iron-stamped {@link slimeknights.sconstruct.tools.item.MaterialItem}
     *  stack for the supplied part slot — mirrors the shape the Part Builder writes. */
    private static ItemStack materialPartStack(PartType part, ResourceLocation materialId) {
        ItemStack stack = new ItemStack(ToolParts.get(part).get());
        stack.set(TinkerDataComponents.PART_MATERIAL.get(), materialId);
        return stack;
    }
}
