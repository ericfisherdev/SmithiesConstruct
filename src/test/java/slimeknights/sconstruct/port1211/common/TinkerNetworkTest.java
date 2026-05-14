package slimeknights.sconstruct.port1211.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link TinkerNetwork}. Drives the package-private
 * {@link TinkerNetwork#configure} seam with a mocked {@link RegisterPayloadHandlersEvent} so
 * the version contract and the null-registrar guard are unit-testable without bringing up FML.
 */
class TinkerNetworkTest {

    @Test
    void versionConstantIsTheOnePassedToRegistrar() {
        // Sanity: the public VERSION constant is what gets handed to event.registrar(...) — a
        // copy-paste mismatch would let production drift away from the documented contract.
        assertEquals("1", TinkerNetwork.VERSION, "Phase 1 protocol version is the string \"1\"");
    }

    @Test
    void configureRequestsTheVersionedRegistrar() {
        RegisterPayloadHandlersEvent event = mock(RegisterPayloadHandlersEvent.class);
        PayloadRegistrar registrar = mock(PayloadRegistrar.class);
        when(event.registrar(TinkerNetwork.VERSION)).thenReturn(registrar);

        TinkerNetwork.configure(event);

        verify(event).registrar(TinkerNetwork.VERSION);
    }

    @Test
    void configureThrowsWhenRegistrarIsNull() {
        // NeoForge's contract is that event.registrar(version) returns non-null. If a future
        // upstream change ever started returning null (refactor, version mismatch, …), we want
        // a loud failure at boot rather than a silent NPE when Phase 2 pulses try to use it.
        RegisterPayloadHandlersEvent event = mock(RegisterPayloadHandlersEvent.class);
        when(event.registrar(TinkerNetwork.VERSION)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> TinkerNetwork.configure(event));
    }
}
