package slimeknights.sconstruct.port1211.smeltery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.smeltery.block.SearedChuteBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SearedDrainBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SearedTankGaugeBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SearedTankInBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SearedTankIoBlock;
import slimeknights.sconstruct.port1211.smeltery.block.SmelteryControllerBlock;

/**
 * Pinned-roster tests for {@link SmelteryComponents} (SMTCON-112). Asserts the 6-block roster by
 * id so a future change surfaces loudly, that every block is namespaced under the mod id, that
 * each {@code .get()} resolves to the expected concrete block class, and that all six
 * block-entity-type holders resolve.
 */
class SmelteryComponentsTest {

    @Test
    void registersAllSixComponentBlocks() {
        assertEquals(6, SmelteryComponents.ALL.size(), "the smeltery component roster is 6 blocks");
        Set<String> ids = SmelteryComponents.ALL.stream().map(b -> b.getId().getPath()).collect(Collectors.toSet());
        assertEquals(6, ids.size(), "smeltery component block ids must be unique");
        for (String id : new String[] { "smeltery_controller", "seared_tank_io", "seared_tank_in", "seared_tank_gauge", "seared_drain", "seared_chute" }) {
            assertTrue(ids.contains(id), "missing smeltery component block id: " + id);
        }
    }

    @Test
    void everyBlockIsNamespacedUnderTheModId() {
        assertAll(SmelteryComponents.ALL.stream().map(block -> () -> assertEquals(SConstruct.MOD_ID, block.getId().getNamespace(), "namespace of " + block.getId())));
    }

    @Test
    void eachBlockResolvesToItsExpectedConcreteClass() {
        assertInstanceOf(SmelteryControllerBlock.class, SmelteryComponents.SMELTERY_CONTROLLER.get(), "smeltery_controller is a SmelteryControllerBlock");
        assertInstanceOf(SearedTankIoBlock.class, SmelteryComponents.SEARED_TANK_IO.get(), "seared_tank_io is a SearedTankIoBlock");
        assertInstanceOf(SearedTankInBlock.class, SmelteryComponents.SEARED_TANK_IN.get(), "seared_tank_in is a SearedTankInBlock");
        assertInstanceOf(SearedTankGaugeBlock.class, SmelteryComponents.SEARED_TANK_GAUGE.get(), "seared_tank_gauge is a SearedTankGaugeBlock");
        assertInstanceOf(SearedDrainBlock.class, SmelteryComponents.SEARED_DRAIN.get(), "seared_drain is a SearedDrainBlock");
        assertInstanceOf(SearedChuteBlock.class, SmelteryComponents.SEARED_CHUTE.get(), "seared_chute is a SearedChuteBlock");
    }

    @Test
    void allSixBlockEntityTypeHoldersResolve() {
        assertAll(() -> assertNotNull(SmelteryComponents.SMELTERY_CONTROLLER_BE.get(), "smeltery_controller BE type resolves"),
                () -> assertNotNull(SmelteryComponents.TANK_IO_BE.get(), "seared_tank_io BE type resolves"),
                () -> assertNotNull(SmelteryComponents.TANK_IN_BE.get(), "seared_tank_in BE type resolves"),
                () -> assertNotNull(SmelteryComponents.TANK_GAUGE_BE.get(), "seared_tank_gauge BE type resolves"),
                () -> assertNotNull(SmelteryComponents.DRAIN_BE.get(), "seared_drain BE type resolves"), () -> assertNotNull(SmelteryComponents.CHUTE_BE.get(), "seared_chute BE type resolves"));
    }

    @Test
    void initIsIdempotent() {
        int sizeBefore = SmelteryComponents.ALL.size();
        SmelteryComponents.init();
        SmelteryComponents.init();
        assertEquals(sizeBefore, SmelteryComponents.ALL.size());
    }
}
