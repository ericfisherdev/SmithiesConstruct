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
 * Client-side rendering metadata for the four {@link WorldFluids} slime fluids. Each fluid binds
 * a per-colour {@code IClientFluidTypeExtensions} pointing at its own still/flowing texture pair
 * — the PNGs ship pre-tinted under {@code sconstruct:block/fluid/slime_<color>_still} and
 * {@code slime_<color>_flow}, so the runtime tint is the no-op sentinel {@link #NO_TINT}.
 *
 * <p>Pre-tinting was chosen over a tint-multiplied shared sprite because (a) it lets each colour
 * carry its own animation timing in {@code .mcmeta} if it ever diverges from the rest, (b) atlas
 * sampling reads the final pixel rather than multiplying through a fragment-shader tint, which
 * keeps the in-world surface a perfect match for the bucket sprite, and (c) it matches the ticket
 * implementation plan for SMTCON-52 verbatim. Fog colour is still derived from
 * {@link WorldFluids#TINTS} because the effect is a constant submerged blend, independent of the
 * texture data.
 *
 * <p>Loaded only on the client side — the dedicated server has no rendering pipeline and would
 * crash on the client-only {@link IClientFluidTypeExtensions} reference. The world pulse guards
 * the {@link #register(IEventBus)} call with a {@code Dist.CLIENT} check; this class never needs
 * to be loaded on the server.
 */
public final class WorldClientFluidTypes {

    /**
     * No-op tint sentinel returned by every per-fluid extension's {@code getTintColor()}.
     * Matches the {@link IClientFluidTypeExtensions} default of {@code -1} (i.e. {@code 0xFFFFFFFF})
     * which the renderer treats as "no multiplier" — appropriate when the source PNG is already
     * coloured for the target fluid.
     */
    static final int NO_TINT = -1;

    private WorldClientFluidTypes() {
    }

    /**
     * Subscribes a {@link RegisterClientExtensionsEvent} listener that binds each slime fluid
     * type to a per-colour {@link IClientFluidTypeExtensions} carrying its own pre-tinted
     * texture pair and a fog colour derived from {@link WorldFluids#TINTS}.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(WorldClientFluidTypes::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (Map.Entry<SlimeFluidSet, Integer> entry : WorldFluids.TINTS.entrySet()) {
            SlimeFluidSet set = entry.getKey();
            IClientFluidTypeExtensions extensions = extensionFor(stillTextureFor(set), flowTextureFor(set), entry.getValue());
            event.registerFluidType(extensions, set.type().get());
        }
    }

    /**
     * Resolve the still-texture path for a slime fluid. The path is
     * {@code sconstruct:block/fluid/<source-path>_still} where {@code <source-path>} is the
     * source fluid's registry path (e.g. {@code slime_blue}). Package-private so tests can pin
     * the convention without duplicating the formula.
     */
    static ResourceLocation stillTextureFor(SlimeFluidSet set) {
        return rl("block/fluid/" + set.source().getId().getPath() + "_still");
    }

    /**
     * Resolve the flowing-texture path for a slime fluid. Symmetric with
     * {@link #stillTextureFor(SlimeFluidSet)} but appends {@code _flow}.
     */
    static ResourceLocation flowTextureFor(SlimeFluidSet set) {
        return rl("block/fluid/" + set.source().getId().getPath() + "_flow");
    }

    /**
     * Build a {@link IClientFluidTypeExtensions} bound to the supplied texture pair and fog
     * tint. Package-private so tests can drive the texture-path and fog-colour contracts without
     * bootstrapping a real {@link Camera}/{@link ClientLevel}; the rendering pipeline only
     * invokes it via the extension manager attached to the {@code FluidType}.
     *
     * @param still   still-texture resource location used by the surface and bucket sprite
     * @param flow    flowing-texture resource location used by the falling-stream geometry
     * @param fogTint {@code 0xAARRGGBB} colour applied as the submerged-fog blend; the alpha
     *                byte is ignored — only the RGB channels contribute to the resulting
     *                {@link Vector3f}
     */
    static IClientFluidTypeExtensions extensionFor(ResourceLocation still, ResourceLocation flow, int fogTint) {
        Vector3f fogColor = fogColorOf(fogTint);
        return new IClientFluidTypeExtensions() {

            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flow;
            }

            @Override
            public int getTintColor() {
                return NO_TINT;
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
