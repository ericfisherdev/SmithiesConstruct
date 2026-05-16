package slimeknights.sconstruct.port1211.smeltery.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Pinned-behaviour tests for {@link SmelteryComponentBlockEntity} (SMTCON-112) — the
 * controller-position proxy carried by every smeltery component block.
 *
 * <p>This test sits in the same package as the BE so it can call the {@code protected}
 * {@link SmelteryComponentBlockEntity#saveAdditional} / {@link SmelteryComponentBlockEntity#loadAdditional}
 * overrides directly, exercising the full NBT round-trip rather than only the in-memory setter.
 * A {@code null} {@code HolderLookup.Provider} is passed to those calls because the controller-
 * position serialisation goes through {@code NbtUtils.writeBlockPos} / {@code readBlockPos},
 * neither of which touches the provider.
 */
class SmelteryComponentBlockEntityTest {

    @Test
    void controllerPosDefaultsToEmptyForALooseComponent() {
        SmelteryComponentBlockEntity be = newComponent();
        assertTrue(be.getControllerPos().isEmpty(), "an unassembled component has no controller");
    }

    @Test
    void setControllerPosIsReflectedByGetControllerPos() {
        SmelteryComponentBlockEntity be = newComponent();
        be.setControllerPos(new BlockPos(3, 4, 5));
        assertEquals(Optional.of(new BlockPos(3, 4, 5)), be.getControllerPos(), "the set controller position is returned");
    }

    @Test
    void setControllerPosNullDetachesTheComponent() {
        SmelteryComponentBlockEntity be = newComponent();
        be.setControllerPos(new BlockPos(3, 4, 5));
        be.setControllerPos(null);
        assertTrue(be.getControllerPos().isEmpty(), "passing null detaches the component from its controller");
    }

    @Test
    void controllerPosSurvivesAnNbtRoundTrip() {
        SmelteryComponentBlockEntity saved = newComponent();
        saved.setControllerPos(new BlockPos(3, 4, 5));

        CompoundTag tag = new CompoundTag();
        saved.saveAdditional(tag, null);

        SmelteryComponentBlockEntity loaded = newComponent();
        loaded.loadAdditional(tag, null);
        assertEquals(Optional.of(new BlockPos(3, 4, 5)), loaded.getControllerPos(), "controller position round-trips through NBT");
    }

    @Test
    void aLooseComponentRoundTripsBackAsLoose() {
        SmelteryComponentBlockEntity saved = newComponent();

        CompoundTag tag = new CompoundTag();
        saved.saveAdditional(tag, null);

        SmelteryComponentBlockEntity loaded = newComponent();
        loaded.setControllerPos(new BlockPos(9, 9, 9));
        loaded.loadAdditional(tag, null);
        assertTrue(loaded.getControllerPos().isEmpty(), "a component saved while loose loads back unassembled");
    }

    /**
     * Constructs a fresh component BE bound to the {@code seared_tank_io} type at the origin.
     * The block state is the tank IO block's default state — any registered smeltery component
     * state works since the BE keeps no state-derived data.
     */
    private static SmelteryComponentBlockEntity newComponent() {
        BlockState state = SmelteryComponents.SEARED_TANK_IO.get().defaultBlockState();
        return new SmelteryComponentBlockEntity(SmelteryComponents.TANK_IO_BE.get(), BlockPos.ZERO, state);
    }
}
