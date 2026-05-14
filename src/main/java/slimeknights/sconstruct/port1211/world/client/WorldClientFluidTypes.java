package slimeknights.sconstruct.port1211.world.client;

import static slimeknights.sconstruct.port1211.lib.util.Util.rl;

import java.util.Map;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import org.joml.Vector3f;

import slimeknights.sconstruct.port1211.world.SlimeFluidSet;
import slimeknights.sconstruct.port1211.world.WorldFluids;

/**
 * Client-side rendering metadata for the four {@link WorldFluids} slime fluids. Provides a
 * shared still/flowing texture pair tinted per-colour and a matching fog colour applied while
 * the camera is submerged.
 *
 * <p>All four fluids reuse the same {@code sconstruct:block/fluid/slime_still} and
 * {@code sconstruct:block/fluid/slime_flow} sprites — the tint colour from
 * {@link WorldFluids#TINTS} is what differentiates them at render time, matching the legacy 1.12
 * {@code FluidColored} pattern. Sharing the texture pair keeps the atlas footprint small and
 * means adding a fifth slime colour later is purely a registration + tint change with no new
 * asset.
 *
 * <p>Loaded only on the client side — the dedicated server has no rendering pipeline and would
 * crash on the client-only {@link IClientFluidTypeExtensions} reference. The world pulse guards
 * the {@link #register(IEventBus)} call with a {@code Dist.CLIENT} check; this class never needs
 * to be loaded on the server.
 */
public final class WorldClientFluidTypes {

    private static final ResourceLocation SLIME_STILL = rl("block/fluid/slime_still");
    private static final ResourceLocation SLIME_FLOW = rl("block/fluid/slime_flow");

    private WorldClientFluidTypes() {
    }

    /**
     * Subscribes a {@link RegisterClientExtensionsEvent} listener that binds each slime fluid
     * type to a {@link IClientFluidTypeExtensions} built from {@link WorldFluids#TINTS}.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(WorldClientFluidTypes::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (Map.Entry<SlimeFluidSet, Integer> entry : WorldFluids.TINTS.entrySet()) {
            event.registerFluidType(extensionFor(entry.getValue()), entry.getKey().type().get());
        }
    }

    /**
     * Build a {@link IClientFluidTypeExtensions} bound to the supplied tint. Package-private so
     * tests can pin the shared texture paths and the tint → fog-colour conversion without
     * bootstrapping a real {@link Camera}/{@link ClientLevel}; the rendering pipeline only
     * invokes it via the extension manager attached to the {@code FluidType}.
     */
    static IClientFluidTypeExtensions extensionFor(int tint) {
        Vector3f fogColor = fogColorOf(tint);
        return new IClientFluidTypeExtensions() {

            @Override
            public ResourceLocation getStillTexture() {
                return SLIME_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return SLIME_FLOW;
            }

            @Override
            public int getTintColor() {
                return tint;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                // Return a fresh copy because the vanilla fog pipeline mutates the returned
                // Vector3f in-place when blending; aliasing the captured constant would corrupt
                // subsequent calls for the same fluid type.
                return new Vector3f(fogColor);
            }
        };
    }

    /**
     * Derive a normalised fog colour from a {@code 0xAARRGGBB} tint. Alpha is ignored — the fog
     * is always opaque from the camera's perspective; only the RGB channels contribute.
     */
    static Vector3f fogColorOf(int tint) {
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        return new Vector3f(r, g, b);
    }
}
