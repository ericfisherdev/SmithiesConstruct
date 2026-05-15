package slimeknights.sconstruct.port1211.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.tools.PartBuilderRegistry;
import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.StencilTableRegistry;
import slimeknights.sconstruct.port1211.tools.block.entity.PartBuilderBlockEntity;
import slimeknights.sconstruct.port1211.tools.item.MaterialItem;
import slimeknights.sconstruct.port1211.tools.item.PatternItem;
import slimeknights.sconstruct.port1211.tools.item.ToolParts;

/**
 * End-to-end {@link GameTest} for the Part Builder (SMTCON-92) — proves the typed-pattern +
 * material → stamped {@link MaterialItem} pipeline. Feeds a PICKHEAD-typed pattern and an
 * iron ingot into the BE's input slots, then asserts the output slot resolves to a
 * pick-head MaterialItem stamped with the iron material id.
 *
 * <p>The match relies on the live {@code sconstruct:material} datapack registry — the iron
 * material entry ships under {@code data/tconstruct/sconstruct/material/iron.json} with a
 * {@code repair_tag} of {@code c:ingots/iron}, which {@link Items#IRON_INGOT} sits in via
 * the NeoForge common tag. {@code gameTestServer} loads datapacks before any test runs.
 *
 * <p>Drives the BE handler directly rather than through the menu — see the class-level note
 * on {@link PatternChestTests}. {@link PartBuilderBlockEntity#refreshOutput} runs synchronously
 * from the handler's {@code onContentsChanged} callback once both inputs are populated.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PartBuilderTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Placement position inside the 7x7x7 template — chosen well inside the bounds. */
    private static final BlockPos BUILDER_POS = new BlockPos(2, 2, 2);

    /** Iron material id — registered under the legacy {@code tconstruct} namespace. */
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("tconstruct", "iron");

    private PartBuilderTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void buildIronPickhead(GameTestHelper helper) {
        helper.setBlock(BUILDER_POS, PartBuilderRegistry.PART_BUILDER.get());
        PartBuilderBlockEntity be = (PartBuilderBlockEntity) helper.getBlockEntity(BUILDER_POS);
        helper.assertTrue(be != null, "Part Builder BlockEntity must attach after setBlock");

        // Build a PICKHEAD-typed pattern via the same helper the Stencil Table uses, so the
        // component payload is byte-identical to a real player workflow.
        ItemStack typedPattern = PatternItem.makeTyped(StencilTableRegistry.PATTERN.get(), PartType.PICKHEAD);
        be.getHandler().setStackInSlot(PartBuilderBlockEntity.PATTERN_SLOT, typedPattern);

        // Material slot — vanilla iron ingot. Matches iron's repair_tag (c:ingots/iron) via the
        // NeoForge common-tag chain, which is the canonical "any iron ingot" surface.
        be.getHandler().setStackInSlot(PartBuilderBlockEntity.MATERIAL_SLOT, new ItemStack(Items.IRON_INGOT));

        // Output rebuilt synchronously through the handler callback; assert directly.
        ItemStack output = be.getHandler().getStackInSlot(PartBuilderBlockEntity.OUTPUT_SLOT);
        helper.assertFalse(output.isEmpty(), "output stamped after pattern + material populate the input slots");
        helper.assertTrue(output.getItem() instanceof MaterialItem, "output is the registered MaterialItem class");
        helper.assertTrue(output.is(ToolParts.get(PartType.PICKHEAD).get()), "output is the PICKHEAD part item (not some other slot)");

        ResourceLocation stampedMaterial = output.get(TinkerDataComponents.PART_MATERIAL.get());
        helper.assertTrue(stampedMaterial != null, "output carries the PART_MATERIAL component");
        helper.assertTrue(IRON.equals(stampedMaterial), "output is stamped with the iron material id (" + IRON + "), got " + stampedMaterial);

        // Drain the material slot — refreshOutput must clear the output now that the inputs
        // are incomplete. Pins the "either input missing ⇒ no output" rule against a regression.
        be.getHandler().setStackInSlot(PartBuilderBlockEntity.MATERIAL_SLOT, ItemStack.EMPTY);
        helper.assertTrue(be.getHandler().getStackInSlot(PartBuilderBlockEntity.OUTPUT_SLOT).isEmpty(), "output cleared once material drains");

        helper.succeed();
    }
}
