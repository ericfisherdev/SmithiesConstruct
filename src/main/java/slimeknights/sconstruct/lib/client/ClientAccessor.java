package slimeknights.sconstruct.lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Package-private holder for every {@code net.minecraft.client.*} reference {@link SafeClient}
 * needs. Concentrating the client-class imports in this one file keeps {@link SafeClient}
 * itself free of any client-only types — so on a dedicated server, the JVM can load and verify
 * {@code SafeClient} without ever touching this class, and only resolves it the moment the
 * client-side branch in {@code SafeClient} actually calls one of these methods.
 *
 * <p>That separation is what makes the {@code SafeClient.get*()} accessors safe to call from
 * common (non-client-only) code without risking a {@code NoClassDefFoundError} or premature
 * {@code Minecraft} singleton initialisation on the server.
 */
final class ClientAccessor {

    private ClientAccessor() {
    }

    /** Direct passthrough to {@link Minecraft#getInstance()}; may be {@code null} pre-init. */
    static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    /** The currently-loaded {@link ClientLevel}, or {@code null} if no level is loaded. */
    static ClientLevel clientLevel() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : mc.level;
    }

    /** The local player, or {@code null} if not connected to a level. */
    static Player clientPlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : mc.player;
    }
}
