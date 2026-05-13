package slimeknights.tconstruct.port1211.lib.client;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Dist-aware accessor for the singletons every client-side helper used to reach for directly:
 * {@link Minecraft#getInstance()}, the current {@link ClientLevel}, the local {@link Player}.
 * Replaces upstream Mantle's {@code SafeClientAccess} pattern.
 *
 * <p>Every accessor here is safe to call from common (non-client-only) code: a call on a
 * dedicated server returns {@link Optional#empty()} without touching {@link Minecraft} or any
 * other {@code net.minecraft.client.*} type. The dist guard short-circuits before
 * {@link ClientAccessor} — which holds the actual client-class references — is ever resolved.
 * That separation is what makes the API server-safe regardless of NeoForge's merged-jar
 * packaging.
 *
 * <p>The return types are still the concrete client types (because the merged jar makes them
 * available on the server classpath anyway, and concrete types serve callers better than
 * {@code Optional<Object>}). The crucial invariant is that no {@code Minecraft.getInstance()}
 * call ever fires on the server side — that's where the actual class-load + singleton-init
 * cost lives.
 */
public final class SafeClient {

    private SafeClient() {
    }

    /** Returns {@link Minecraft#getInstance()} on the client; empty on the dedicated server. */
    public static Optional<Minecraft> getMinecraft() {
        return getMinecraft(FMLEnvironment.dist);
    }

    /** Returns the client's currently-loaded {@link ClientLevel}; empty off-client or unloaded. */
    public static Optional<ClientLevel> getClientLevel() {
        return getClientLevel(FMLEnvironment.dist);
    }

    /** Returns the local {@link Player}; empty off-client or before joining a level. */
    public static Optional<Player> getClientPlayer() {
        return getClientPlayer(FMLEnvironment.dist);
    }

    /**
     * Dist-injectable overload of {@link #getMinecraft()}. Package-private so unit tests can
     * exercise the {@link Dist#DEDICATED_SERVER} branch without spawning a server JVM; production
     * always reaches the no-arg form which feeds {@link FMLEnvironment#dist}.
     */
    static Optional<Minecraft> getMinecraft(Dist dist) {
        if (dist != Dist.CLIENT) {
            return Optional.empty();
        }
        return Optional.ofNullable(ClientAccessor.minecraft());
    }

    /** Test-injectable overload of {@link #getClientLevel()}. */
    static Optional<ClientLevel> getClientLevel(Dist dist) {
        if (dist != Dist.CLIENT) {
            return Optional.empty();
        }
        return Optional.ofNullable(ClientAccessor.clientLevel());
    }

    /** Test-injectable overload of {@link #getClientPlayer()}. */
    static Optional<Player> getClientPlayer(Dist dist) {
        if (dist != Dist.CLIENT) {
            return Optional.empty();
        }
        return Optional.ofNullable(ClientAccessor.clientPlayer());
    }
}
