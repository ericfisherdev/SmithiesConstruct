package slimeknights.sconstruct.port1211.smeltery;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryComponentBlockEntity;
import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryControllerBlockEntity;

/**
 * Unit tests for the SMTCON-116 component-proxy lookup,
 * {@link SmelteryComponentBlockEntity#getControllerOpt()} — the resolution the
 * {@code SmelteryCapabilities} fluid/item proxies route every capability query through.
 *
 * <p>The block-entity registry is frozen before tests run, so the controller is a Mockito mock
 * rather than a constructed instance, and the world is a mocked {@link Level} whose
 * {@code getBlockEntity} return value is scripted per scenario.
 */
// Level is AutoCloseable, but the mocks here are pure stubs with no resources to release —
// PMD's CloseResource heuristic doesn't model Mockito's lifecycle.
@SuppressWarnings("PMD.CloseResource")
class SmelteryComponentProxyTest {

    private static final BlockPos COMPONENT_POS = new BlockPos(4, 64, 4);
    private static final BlockPos CONTROLLER_POS = new BlockPos(2, 64, 2);

    @Test
    void getControllerOptIsEmptyForALooseComponent() {
        SmelteryComponentBlockEntity component = component();
        component.setLevel(mock(Level.class));

        assertTrue(component.getControllerOpt().isEmpty(), "an unassembled component proxies to no controller");
    }

    @Test
    void getControllerOptResolvesTheBoundControllerBlockEntity() {
        Level level = mock(Level.class);
        SmelteryControllerBlockEntity controller = mock(SmelteryControllerBlockEntity.class);
        when(level.getBlockEntity(CONTROLLER_POS)).thenReturn(controller);

        SmelteryComponentBlockEntity component = component();
        component.setLevel(level);
        component.setControllerPos(CONTROLLER_POS);

        assertSame(controller, component.getControllerOpt().orElseThrow(), "a bound component resolves the controller block-entity at its recorded position");
    }

    @Test
    void getControllerOptIsEmptyWhenTheRecordedPositionHasNoController() {
        Level level = mock(Level.class);
        // The controller block was broken — a stale binding now points at a non-controller BE.
        when(level.getBlockEntity(CONTROLLER_POS)).thenReturn(mock(BlockEntity.class));

        SmelteryComponentBlockEntity component = component();
        component.setLevel(level);
        component.setControllerPos(CONTROLLER_POS);

        assertTrue(component.getControllerOpt().isEmpty(), "a stale binding to a non-controller block resolves to no controller");
    }

    @Test
    void setControllerPosInvalidatesTheBlockCapabilitiesOnUnbind() {
        Level level = mock(Level.class);
        when(level.isClientSide()).thenReturn(false);

        SmelteryComponentBlockEntity component = component();
        component.setLevel(level);
        component.setControllerPos(CONTROLLER_POS);
        component.setControllerPos(null);

        // Bind and unbind each change the proxied capability, so each must invalidate the block.
        verify(level, times(2)).invalidateCapabilities(COMPONENT_POS);
        assertTrue(component.getControllerOpt().isEmpty(), "an unbound component proxies to no controller");
    }

    /** A fresh component block-entity at {@link #COMPONENT_POS} under a mocked registered type. */
    @SuppressWarnings("unchecked")
    private static SmelteryComponentBlockEntity component() {
        BlockEntityType<SmelteryComponentBlockEntity> type = mock(BlockEntityType.class);
        // BlockEntity's constructor validates the state against its type — accept the mock state.
        when(type.isValid(any(BlockState.class))).thenReturn(true);
        return new SmelteryComponentBlockEntity(type, COMPONENT_POS, mock(BlockState.class));
    }
}
