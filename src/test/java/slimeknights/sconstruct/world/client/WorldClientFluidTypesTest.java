package slimeknights.sconstruct.world.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.world.SlimeFluidSet;
import slimeknights.sconstruct.world.WorldFluids;

/**
 * Pinned-behaviour tests for {@link WorldClientFluidTypes}. The full
 * {@code RegisterClientExtensionsEvent} pipeline depends on a live extension manager that only
 * exists in a running client — unit tests cover the per-fluid still/flow texture path
 * derivation, the {@link WorldClientFluidTypes#NO_TINT} contract, the tint → fog-colour
 * conversion, the defensive-copy invariant on {@code modifyFogColor}, and the presence of every
 * per-colour PNG and {@code .mcmeta} the renderer will look up at runtime.
 */
class WorldClientFluidTypesTest {

    @Test
    void everyFluidGetsItsOwnStillAndFlowTexturePath() {
        // Per-colour pre-tinted sprites — each set carries a distinct texture pair derived from
        // its source registry path. A shared sprite would have been a single ResourceLocation
        // for every set; per-set paths break that aliasing and make a future drift in naming
        // surface as a missing-texture sprite for exactly one fluid.
        for (SlimeFluidSet set : WorldFluids.ALL) {
            String basePath = set.source().getId().getPath();
            ResourceLocation expectedStill = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/" + basePath + "_still");
            ResourceLocation expectedFlow = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/" + basePath + "_flow");
            assertEquals(expectedStill, WorldClientFluidTypes.stillTextureFor(set), "still texture path for " + basePath);
            assertEquals(expectedFlow, WorldClientFluidTypes.flowTextureFor(set), "flow texture path for " + basePath);
        }
    }

    @Test
    void extensionExposesTheSuppliedTexturePair() {
        // Build an extension from a known still/flow pair and assert the getters round-trip
        // verbatim — no path mangling, no normalisation. The factory is the single hop between
        // the registrar and the renderer; anything other than identity here would corrupt
        // every fluid's rendering simultaneously.
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/slime_blue_still");
        ResourceLocation flow = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/slime_blue_flow");
        IClientFluidTypeExtensions ext = WorldClientFluidTypes.extensionFor(still, flow, WorldFluids.SLIMEBLUE_TINT);
        assertAll(() -> assertEquals(still, ext.getStillTexture()), () -> assertEquals(flow, ext.getFlowingTexture()));
    }

    @Test
    void getTintColorReturnsTheNoOpSentinelBecauseTexturesArePreTinted() {
        // The source PNGs ship coloured for each fluid — applying a runtime tint multiplier
        // would double-shade. -1 (0xFFFFFFFF) is the IClientFluidTypeExtensions default that
        // the renderer treats as "no multiplier".
        assertEquals(-1, WorldClientFluidTypes.NO_TINT);
        for (SlimeFluidSet set : WorldFluids.ALL) {
            int fogTint = WorldFluids.TINTS.get(set);
            IClientFluidTypeExtensions ext = WorldClientFluidTypes.extensionFor(WorldClientFluidTypes.stillTextureFor(set), WorldClientFluidTypes.flowTextureFor(set), fogTint);
            assertEquals(-1, ext.getTintColor(), "tint for " + set.source().getId());
        }
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
        SlimeFluidSet set = WorldFluids.SLIMEPURPLE;
        IClientFluidTypeExtensions ext = WorldClientFluidTypes.extensionFor(WorldClientFluidTypes.stillTextureFor(set), WorldClientFluidTypes.flowTextureFor(set), WorldFluids.SLIMEPURPLE_TINT);
        Vector3f first = ext.modifyFogColor(null, 0f, null, 0, 0f, new Vector3f());
        Vector3f second = ext.modifyFogColor(null, 0f, null, 0, 0f, new Vector3f());
        assertEquals(first, second);
        assertNotSame(first, second);
    }

    @Test
    void everyPerColourTextureAssetIsPackagedOnTheClasspath() {
        // The renderer falls back to the missing-texture sprite if a PNG isn't packaged; a
        // failing assertion here points at a missing resource immediately rather than catching
        // it during a manual runClient pass. Each fluid has four resources: still PNG + mcmeta,
        // flow PNG + mcmeta.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        assertAll(WorldFluids.ALL.stream().map(set -> () -> {
            String base = "assets/sconstruct/textures/block/fluid/" + set.source().getId().getPath();
            assertTrue(cl.getResource(base + "_still.png") != null, base + "_still.png missing");
            assertTrue(cl.getResource(base + "_still.png.mcmeta") != null, base + "_still.png.mcmeta missing — animation metadata required");
            assertTrue(cl.getResource(base + "_flow.png") != null, base + "_flow.png missing");
            assertTrue(cl.getResource(base + "_flow.png.mcmeta") != null, base + "_flow.png.mcmeta missing — animation metadata required");
        }));
    }

    @Test
    void sharedSlimeBaseSpritesAreNoLongerPackaged() {
        // The SMTCON-51 implementation shipped a single slime_still / slime_flow pair tinted at
        // render time. SMTCON-52 replaces that with per-colour pre-tinted sprites; the shared
        // pair must be gone so a stale reference doesn't fall back to it silently.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        assertAll(() -> assertTrue(cl.getResource("assets/sconstruct/textures/block/fluid/slime_still.png") == null, "shared slime_still.png should not be packaged"),
                () -> assertTrue(cl.getResource("assets/sconstruct/textures/block/fluid/slime_flow.png") == null, "shared slime_flow.png should not be packaged"));
    }
}
