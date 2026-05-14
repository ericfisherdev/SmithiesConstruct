package slimeknights.sconstruct.port1211.lib.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link SafeClient}. Exercises the dist-injectable overloads so
 * the server-side branch can be driven without spawning a real dedicated-server JVM, and the
 * client-side branch is exercised against the unit-test JVM (which has the client classes on
 * its classpath but no live {@link Minecraft} singleton — so the {@code Optional.ofNullable}
 * shape returns empty without throwing).
 */
class SafeClientTest {

    // ────────────────────── Dedicated-server branch ──────────────────────

    @Test
    void getMinecraftReturnsEmptyOnDedicatedServer() {
        assertEquals(Optional.empty(), SafeClient.getMinecraft(Dist.DEDICATED_SERVER));
    }

    @Test
    void getClientLevelReturnsEmptyOnDedicatedServer() {
        assertEquals(Optional.empty(), SafeClient.getClientLevel(Dist.DEDICATED_SERVER));
    }

    @Test
    void getClientPlayerReturnsEmptyOnDedicatedServer() {
        assertEquals(Optional.empty(), SafeClient.getClientPlayer(Dist.DEDICATED_SERVER));
    }

    @Test
    void everyAccessorIsExceptionSafeOnDedicatedServer() {
        // Belt-and-suspenders: the contract is "return empty without throwing"; assertDoesNotThrow
        // pins that explicitly so a future regression that started raising on the server branch
        // would surface here rather than as a stack trace at boot.
        assertDoesNotThrow(() -> SafeClient.getMinecraft(Dist.DEDICATED_SERVER));
        assertDoesNotThrow(() -> SafeClient.getClientLevel(Dist.DEDICATED_SERVER));
        assertDoesNotThrow(() -> SafeClient.getClientPlayer(Dist.DEDICATED_SERVER));
    }

    // ────────────────────── Client branch ──────────────────────

    @Test
    void getMinecraftOnClientReturnsEmptyWhenSingletonIsNotInitialised() {
        // The unit-test JVM has the client classes on its classpath but no live Minecraft
        // singleton (no LWJGL, no client thread). The Optional.ofNullable wrapper turns that
        // null into Optional.empty rather than throwing — the contract for "client side but
        // game not running".
        Optional<Minecraft> result = SafeClient.getMinecraft(Dist.CLIENT);
        assertTrue(result.isEmpty(), "Expected empty because Minecraft.getInstance() is null in the unit-test JVM");
    }

    @Test
    void getClientLevelOnClientReturnsEmptyWhenSingletonIsNotInitialised() {
        Optional<ClientLevel> result = SafeClient.getClientLevel(Dist.CLIENT);
        assertTrue(result.isEmpty());
    }

    @Test
    void getClientPlayerOnClientReturnsEmptyWhenSingletonIsNotInitialised() {
        Optional<Player> result = SafeClient.getClientPlayer(Dist.CLIENT);
        assertTrue(result.isEmpty());
    }

    @Test
    void clientBranchIsExceptionSafe() {
        // Even on the client branch, the accessor must not propagate the NPE that
        // Minecraft.getInstance().level would throw if it dereferenced a null mc directly.
        // The implementation guards each chain inside ClientAccessor; this pins that contract.
        assertDoesNotThrow(() -> SafeClient.getMinecraft(Dist.CLIENT));
        assertDoesNotThrow(() -> SafeClient.getClientLevel(Dist.CLIENT));
        assertDoesNotThrow(() -> SafeClient.getClientPlayer(Dist.CLIENT));
    }

    // ────────────────────── Production no-arg shape ──────────────────────

    @Test
    void productionNoArgShapeReturnsAnOptionalWithoutThrowing() {
        // The no-arg overload feeds FMLEnvironment.dist — under moddev's unitTest harness that
        // resolves to Dist.CLIENT. We can't easily mock the static; just verify each accessor
        // returns *some* Optional (empty or present, both legal) without raising.
        // Lambdas (not method-refs) because assertDoesNotThrow has both Executable and
        // ThrowingSupplier<T> overloads — a bare method-ref to a value-returning method is
        // ambiguous to the compiler.
        assertDoesNotThrow(() -> SafeClient.getMinecraft());
        assertDoesNotThrow(() -> SafeClient.getClientLevel());
        assertDoesNotThrow(() -> SafeClient.getClientPlayer());
    }
}
