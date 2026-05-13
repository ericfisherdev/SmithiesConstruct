package slimeknights.tconstruct.port1211.shared.client;

import static slimeknights.tconstruct.port1211.lib.util.Util.rl;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import org.joml.Vector3f;

import slimeknights.tconstruct.port1211.shared.SharedFluids;

/**
 * Client-side rendering metadata for {@link SharedFluids#BLOOD_TYPE}. Provides the still and
 * flowing texture paths, the tint colour applied to the bucket sprite and the in-world fluid
 * surface, and the dark-red fog colour the player sees when submerged in blood.
 *
 * <p>Loaded only on the client side — the dedicated server has no rendering pipeline and would
 * crash on the client-only {@code IClientFluidTypeExtensions} reference. {@link TConstruct}
 * guards the {@link #register(IEventBus)} call with a {@code Dist.CLIENT} check; this class
 * never needs to be loaded on the server.
 *
 * <p>Texture paths are {@code tconstruct:block/fluid/bloodstill} (16×16 still tile) and
 * {@code tconstruct:block/fluid/bloodflow} (16×32 two-frame flowing tile with a {@code .mcmeta}
 * animation descriptor). Tint colour {@code 0xFF6F0000} matches the legacy 1.12
 * {@code FluidColored("blood", 0x540000)} value, adjusted to opaque alpha.
 */
public final class SharedClientFluidTypes {

    /** {@code 0xAARRGGBB} tint applied to both the bucket sprite and the in-world fluid surface. */
    public static final int BLOOD_TINT = 0xFF6F0000;

    private static final ResourceLocation BLOOD_STILL = rl("block/fluid/bloodstill");
    private static final ResourceLocation BLOOD_FLOW = rl("block/fluid/bloodflow");

    private static final Vector3f BLOOD_FOG_COLOR = new Vector3f(0.4F, 0.0F, 0.0F);

    /**
     * Client-only {@link IClientFluidTypeExtensions} bound to {@link SharedFluids#BLOOD_TYPE}.
     * Visible for tests so the texture paths and tint can be pinned without bootstrapping a
     * client {@link Camera}/{@link ClientLevel}; the rendering pipeline only invokes it via the
     * extension manager attached to the {@code FluidType}.
     */
    static final IClientFluidTypeExtensions BLOOD_EXTENSIONS = new IClientFluidTypeExtensions() {

        @Override
        public ResourceLocation getStillTexture() {
            return BLOOD_STILL;
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return BLOOD_FLOW;
        }

        @Override
        public int getTintColor() {
            return BLOOD_TINT;
        }

        @Override
        public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
            // Constant dark-red fog while submerged — the vanilla water/lava behaviour passes
            // through camera light levels, but blood is a flat-shaded thematic effect. Return
            // a fresh copy because the vanilla fog pipeline mutates the returned Vector3f
            // in-place when blending; aliasing the constant would corrupt subsequent calls.
            return new Vector3f(BLOOD_FOG_COLOR);
        }
    };

    private SharedClientFluidTypes() {
    }

    /**
     * Subscribes the blood {@link IClientFluidTypeExtensions} against the mod event bus.
     * Called from {@link TConstruct} during mod construction, guarded by a {@code Dist.CLIENT}
     * check so this class is never touched on a dedicated server.
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(SharedClientFluidTypes::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(BLOOD_EXTENSIONS, SharedFluids.BLOOD_TYPE.get());
    }
}
