package slimeknights.sconstruct.port1211.gametest;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.StencilTableRegistry;
import slimeknights.sconstruct.port1211.tools.block.entity.StencilTableBlockEntity;
import slimeknights.sconstruct.port1211.tools.item.PatternItem;

/**
 * End-to-end {@link GameTest} for the Stencil Table (SMTCON-91) — proves the BE's
 * cursor-driven refresh pipeline: setting a {@link PartType} on the cursor, dropping a blank
 * pattern into the input slot, and reading the typed pattern back out of the output slot.
 *
 * <p>Drives the BE handler directly rather than through the menu — {@code gameTestServer}
 * doesn't have an ergonomic fake-player + screen harness, and the handler / cursor pair is
 * the same surface the menu's slot bindings exercise. {@link StencilTableBlockEntity#refreshOutput}
 * runs synchronously from the handler's {@code onContentsChanged} callback, so the assertion
 * sees the typed output on the next read with no tick wait.
 */
@GameTestHolder(SmokeTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class StencilTableTests {

    private static final String TEMPLATE = "gametest_7x7x7";

    /** Placement position inside the 7x7x7 template — chosen well inside the bounds. */
    private static final BlockPos TABLE_POS = new BlockPos(2, 2, 2);

    private StencilTableTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void stampPickheadPattern(GameTestHelper helper) {
        helper.setBlock(TABLE_POS, StencilTableRegistry.STENCIL_TABLE.get());
        StencilTableBlockEntity be = (StencilTableBlockEntity) helper.getBlockEntity(TABLE_POS);
        helper.assertTrue(be != null, "Stencil Table BlockEntity must attach after setBlock");

        // Pin the cursor to PICKHEAD on the server side. setSelectedPart fires a refreshOutput
        // synchronously; with no input in the slot it clears the output.
        be.setSelectedPart(PartType.PICKHEAD, (ServerLevel) helper.getLevel());
        helper.assertTrue(be.getSelectedPart() == PartType.PICKHEAD, "cursor persists the requested part type");

        // Feed one blank pattern into the input slot. The handler's onContentsChanged callback
        // runs refreshOutput synchronously, which stamps the typed pattern into the output.
        be.getHandler().setStackInSlot(StencilTableBlockEntity.INPUT_SLOT, new ItemStack(StencilTableRegistry.BLANK_PATTERN.get()));

        ItemStack output = be.getHandler().getStackInSlot(StencilTableBlockEntity.OUTPUT_SLOT);
        helper.assertFalse(output.isEmpty(), "output slot stamped after blank pattern fed into input");
        helper.assertTrue(output.getItem() instanceof PatternItem, "output is the registered PatternItem class");
        helper.assertTrue(output.is(StencilTableRegistry.PATTERN.get()), "output is the typed PATTERN item (not the BLANK_PATTERN variant)");

        Optional<PartType> typed = PatternItem.getPart(output);
        helper.assertTrue(typed.isPresent(), "output carries the TINKER_PATTERN_PART component");
        helper.assertTrue(typed.get() == PartType.PICKHEAD, "output is typed for the cursor's PICKHEAD part");

        // Clear the input — refreshOutput should clear the output too. Catches a regression
        // where the BE leaves a stale typed pattern in the output after the input drains.
        be.getHandler().setStackInSlot(StencilTableBlockEntity.INPUT_SLOT, ItemStack.EMPTY);
        helper.assertTrue(be.getHandler().getStackInSlot(StencilTableBlockEntity.OUTPUT_SLOT).isEmpty(), "output cleared once input drains");

        helper.succeed();
    }
}
