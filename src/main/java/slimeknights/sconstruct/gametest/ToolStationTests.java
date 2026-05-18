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
import slimeknights.sconstruct.tools.block.entity.ToolStationBlockEntity;
import slimeknights.sconstruct.tools.item.ToolCore;
import slimeknights.sconstruct.tools.item.ToolItems;
import slimeknights.sconstruct.tools.item.ToolParts;

/**
 * End-to-end {@link GameTest} for the Tool Station (SMTCON-93) — proves the 3-part build path:
 * three iron-stamped {@link slimeknights.sconstruct.tools.item.MaterialItem} stacks in
 * the legacy {@code (handle, pickhead, binding)} positional order resolve to a freshly-built
 * iron pickaxe in the output slot, with materials written and stats rebuilt against the live
 * server-side materials registry.
 *
 * <p>Part-slot order pinned by {@link ToolDefinition#PICKAXE} — input slot {@code i} feeds part
 * {@code def.getPartSlot(i)}, so a handle goes in slot 0, a pickhead in slot 1, and a binding
 * in slot 2.
 *
 * <p>Drives the BE handler directly rather than through the menu — see the class-level note on
 * {@link PatternChestTests}. {@link ToolStationBlockEntity#refreshOutput} runs synchronously
 * from the handler's {@code onContentsChanged} callback after each input populates.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ToolStationTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Placement position inside the 7x7x7 template — chosen well inside the bounds. */
    private static final BlockPos STATION_POS = new BlockPos(2, 2, 2);

    /** Iron material id — registered under the legacy {@code tconstruct} namespace. */
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    private ToolStationTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void buildIronPickaxe(GameTestHelper helper) {
        helper.setBlock(STATION_POS, ToolStationRegistry.TOOL_STATION.get());
        ToolStationBlockEntity be = (ToolStationBlockEntity) helper.getBlockEntity(STATION_POS);
        helper.assertTrue(be != null, "Tool Station BlockEntity must attach after setBlock");
        helper.assertFalse(be.acceptsAdvancedTools(), "base Tool Station rejects the advanced tool roster");

        // Build three iron-stamped part stacks in pickaxe positional order: handle, pickhead, binding.
        // The order is dictated by ToolDefinition.PICKAXE.parts() — slot N feeds part N.
        for (int i = 0; i < ToolDefinition.PICKAXE.getPartCount(); i++) {
            PartType slotPart = ToolDefinition.PICKAXE.getPartSlot(i);
            be.getHandler().setStackInSlot(i, materialPartStack(slotPart, IRON));
        }

        ItemStack output = be.getHandler().getStackInSlot(ToolStationBlockEntity.OUTPUT_SLOT);
        helper.assertFalse(output.isEmpty(), "output assembled after three iron part stacks populate the inputs");
        helper.assertTrue(output.getItem() instanceof ToolCore, "output is a ToolCore instance");
        helper.assertTrue(output.is(ToolItems.PICKAXE.get()), "output is the pickaxe item (not some other 3-part tool)");

        // The BE's refreshOutput drives ToolHelper.rebuildStats for the matched definition, so
        // the output stack must carry three iron entries in ToolMaterials.
        List<ResourceLocation> materials = ToolHelper.getMaterials(output);
        helper.assertValueEqual(materials.size(), 3, "output carries three positional material entries");
        for (int i = 0; i < materials.size(); i++) {
            helper.assertTrue(IRON.equals(materials.get(i)), "slot " + i + " material is iron, got " + materials.get(i));
        }

        // Stat rebuild ran — vanilla MAX_DAMAGE component populated, broken flag clear.
        helper.assertTrue(output.getMaxDamage() > 0, "stat rebuild populated MAX_DAMAGE on the output");
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
