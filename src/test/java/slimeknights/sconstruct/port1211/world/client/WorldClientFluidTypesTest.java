package slimeknights.sconstruct.port1211.world.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.world.WorldFluids;

/**
 * Pinned-behaviour tests for {@link WorldClientFluidTypes}. The full
 * {@code RegisterClientExtensionsEvent} pipeline depends on a live extension manager that only
 * exists in a running client — unit tests cover the shared still/flow texture paths, the
 * tint-driven extension factory, the tint → fog-colour derivation, and the presence of the
 * shared slime-base texture assets the renderer will look up at runtime.
 */
class WorldClientFluidTypesTest {

    @Test
    void everyExtensionPointsAtTheSharedSlimeTexturePair() {
        // All four slime fluids share one still/flow texture pair — the colour is driven by
        // tint, not by per-fluid sprites. Drifting the path would silently fall back to the
        // missing-texture sprite for every slime fluid simultaneously.
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/slime_still");
        ResourceLocation flow = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/slime_flow");
        assertAll(WorldFluids.TINTS.values().stream().map(tint -> () -> {
            IClientFluidTypeExtensions ext = WorldClientFluidTypes.extensionFor(tint);
            assertEquals(still, ext.getStillTexture());
            assertEquals(flow, ext.getFlowingTexture());
        }));
    }

    @Test
    void extensionTintRoundTripsThroughTheFactory() {
        // The factory's only job around tint is to expose it via getTintColor — assert each
        // mapped tint comes back verbatim from the matching extension.
        assertAll(WorldFluids.TINTS.entrySet().stream().map(entry -> () -> {
            IClientFluidTypeExtensions ext = WorldClientFluidTypes.extensionFor(entry.getValue());
            assertEquals(entry.getValue().intValue(), ext.getTintColor(), "tint roundtrip for " + entry.getKey().source().getId());
        }));
    }

    @Test
    void fogColorMatchesTintRgbChannelsNormalised() {
        // SLIMEBLUE_TINT = 0xFF2AEC81 → fog rgb (0x2A/255, 0xEC/255, 0x81/255). The alpha byte
        // is dropped because fog is treated as opaque from the camera's perspective.
        Vector3f fog = WorldClientFluidTypes.fogColorOf(WorldFluids.SLIMEBLUE_TINT);
        assertEquals(0x2A / 255.0F, fog.x, 1e-6F);
        assertEquals(0xEC / 255.0F, fog.y, 1e-6F);
        assertEquals(0x81 / 255.0F, fog.z, 1e-6F);
    }

    @Test
    void modifyFogColorReturnsAFreshVectorPerCall() {
        // Vanilla's fog blender mutates the returned Vector3f in place; aliasing a captured
        // constant would let one frame's blend corrupt the next call against the same fluid.
        IClientFluidTypeExtensions ext = WorldClientFluidTypes.extensionFor(WorldFluids.SLIMEPURPLE_TINT);
        Vector3f first = ext.modifyFogColor(null, 0f, null, 0, 0f, new Vector3f());
        Vector3f second = ext.modifyFogColor(null, 0f, null, 0, 0f, new Vector3f());
        assertEquals(first, second);
        assertNotSame(first, second);
    }

    @Test
    void sharedTextureAssetsArePackagedOnTheClasspath() {
        // The renderer falls back to the missing-texture sprite if the PNG isn't packaged; a
        // failing assertion here points at a missing resource immediately rather than catching
        // it during a manual runClient pass.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String still = "assets/sconstruct/textures/block/fluid/slime_still.png";
        String flow = "assets/sconstruct/textures/block/fluid/slime_flow.png";
        String flowMcmeta = "assets/sconstruct/textures/block/fluid/slime_flow.png.mcmeta";
        assertAll(() -> assertTrue(cl.getResource(still) != null, still + " missing from classpath"), () -> assertTrue(cl.getResource(flow) != null, flow + " missing from classpath"),
                () -> assertTrue(cl.getResource(flowMcmeta) != null, flowMcmeta + " missing — animation metadata required for the multi-frame flow tile"));
    }
}
