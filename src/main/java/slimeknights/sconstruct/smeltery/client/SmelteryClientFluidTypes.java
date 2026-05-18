package slimeknights.sconstruct.smeltery.client;

import static slimeknights.sconstruct.lib.util.Util.rl;

import java.util.Map;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import org.joml.Vector3f;

import slimeknights.sconstruct.smeltery.MoltenFluidSet;
import slimeknights.sconstruct.smeltery.SmelteryFluids;

/**
 * Client-side rendering metadata for the 20 {@link SmelteryFluids} molten-metal fluids. Unlike
 * the Phase-3 slime fluids — which each ship a pre-tinted texture pair — every molten metal
 * shares a single greyscale {@code still}/{@code flow} texture pair and is differentiated at
 * render time by a per-metal {@code getTintColor()} multiplier. This keeps the asset footprint
 * at two PNGs rather than 40, and lets the molten-metal palette stay data-driven through the
 * {@link SmelteryFluids#TINTS} table the {@link slimeknights.sconstruct.smeltery.MoltenMetal}
 * driver feeds.
 *
 * <p>Submerged-fog colour is a single warm orange shared by every melt — standing in molten iron
 * and molten cobalt should both read as "inside something lava-hot", so the fog does not track
 * the per-metal surface tint.
 *
 * <p>Loaded only on the client side — the dedicated server has no rendering pipeline and would
 * crash on the client-only {@link IClientFluidTypeExtensions} reference. The caller guards the
 * {@link #register(IEventBus)} call with a {@code Dist.CLIENT} check.
 */
public final class SmelteryClientFluidTypes {

    /** Shared still texture for every molten metal; the per-metal look comes from the tint. */
    static final ResourceLocation STILL_TEXTURE = rl("block/fluid/molten_metal_still");

    /** Shared flowing texture for every molten metal. */
    static final ResourceLocation FLOW_TEXTURE = rl("block/fluid/molten_metal_flow");

    /**
     * Warm orange ({@code 0xRRGGBB}) blended into the submerged fog for every molten metal — a
     * lava-hot cast independent of the fluid's surface tint.
     */
    static final int WARM_FOG_TINT = 0xC8641E;

    private SmelteryClientFluidTypes() {
    }

    /**
     * Subscribes a {@link RegisterClientExtensionsEvent} listener that binds each molten-metal
     * fluid type to a per-metal {@link IClientFluidTypeExtensions} carrying the shared texture
     * pair, the metal's tint from {@link SmelteryFluids#TINTS}, and the warm-orange fog.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(SmelteryClientFluidTypes::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (Map.Entry<MoltenFluidSet, Integer> entry : SmelteryFluids.TINTS.entrySet()) {
            event.registerFluidType(extensionFor(entry.getValue()), entry.getKey().type().get());
        }
    }

    /**
     * Build a {@link IClientFluidTypeExtensions} for one molten metal. The still/flow textures
     * are shared across every melt; {@code tint} is the per-metal {@code 0xAARRGGBB} multiplier
     * the renderer applies to the greyscale base sprite. Package-private so tests can pin the
     * texture, tint, and fog contracts without a live extension manager.
     */
    static IClientFluidTypeExtensions extensionFor(int tint) {
        Vector3f fogColor = fogColorOf(WARM_FOG_TINT);
        return new IClientFluidTypeExtensions() {

            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOW_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return tint;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                // Fresh copy — the vanilla fog pipeline mutates the returned Vector3f in-place
                // when blending, so aliasing the captured constant would corrupt later calls.
                return new Vector3f(fogColor);
            }
        };
    }

    /**
     * Derive a normalised fog colour from a {@code 0xRRGGBB} tint. Only the RGB channels
     * contribute — the submerged fog is always opaque from the camera's perspective.
     */
    static Vector3f fogColorOf(int tint) {
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        return new Vector3f(r, g, b);
    }
}
