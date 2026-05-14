package slimeknights.sconstruct.port1211.shared.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;

/**
 * Pinned-behaviour tests for {@link SharedClientFluidTypes}. The full {@code modifyFogColor}
 * pipeline depends on a live {@code Camera} and {@code ClientLevel} which only exist in a
 * running client — unit tests cover the texture paths, tint, fog-colour constant, and the
 * presence of the placeholder texture assets that the renderer will look up.
 */
class SharedClientFluidTypesTest {

    @Test
    void stillAndFlowingTexturesPointAtTconstructFluidPaths() {
        assertAll(() -> assertEquals(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/bloodstill"), SharedClientFluidTypes.BLOOD_EXTENSIONS.getStillTexture()),
                () -> assertEquals(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/bloodflow"), SharedClientFluidTypes.BLOOD_EXTENSIONS.getFlowingTexture()));
    }

    @Test
    void tintColorMatchesLegacyValue() {
        // 0xFF6F0000 = legacy FluidColored("blood", 0x540000) bumped to opaque alpha. The
        // exact value is referenced by the bucket sprite renderer and the fluid-surface
        // tintindex; pinning here catches a silent recolour.
        assertEquals(0xFF6F0000, SharedClientFluidTypes.BLOOD_EXTENSIONS.getTintColor());
    }

    @Test
    void modifyFogColorReturnsDarkRedWhenSubmerged() {
        // The signature requires Camera/ClientLevel parameters we can't construct in a unit
        // test, but the implementation returns a constant Vector3f independent of them. Pass
        // nulls and verify the constant — plus verify the defensive-copy contract by calling
        // twice and asserting the returned instances are distinct objects. Vanilla's fog
        // blender mutates returned vectors in place; aliasing the shared constant would let
        // one frame's blend corrupt the next.
        Vector3f first = SharedClientFluidTypes.BLOOD_EXTENSIONS.modifyFogColor(null, 0f, null, 0, 0f, new Vector3f());
        Vector3f second = SharedClientFluidTypes.BLOOD_EXTENSIONS.modifyFogColor(null, 0f, null, 0, 0f, new Vector3f());
        Vector3f expected = new Vector3f(0.4F, 0.0F, 0.0F);
        assertAll(() -> assertEquals(expected, first), () -> assertEquals(expected, second),
                () -> org.junit.jupiter.api.Assertions.assertNotSame(first, second, "modifyFogColor must return a fresh Vector3f per call"));
    }

    @Test
    void placeholderTextureAssetsArePackagedOnTheClasspath() {
        // The renderer will silently fall back to the missing-texture sprite if the PNG isn't
        // packaged; a failing assertion here points at a missing resource immediately rather
        // than catching it during a manual runClient pass. Resources are looked up via the
        // classloader so this works whether the test runs from `build/resources/main/` or
        // from a packed jar.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String still = "assets/sconstruct/textures/block/fluid/bloodstill.png";
        String flow = "assets/sconstruct/textures/block/fluid/bloodflow.png";
        String mcmeta = "assets/sconstruct/textures/block/fluid/bloodflow.png.mcmeta";
        assertAll(() -> assertTrue(cl.getResource(still) != null, still + " missing from classpath"), () -> assertTrue(cl.getResource(flow) != null, flow + " missing from classpath"),
                () -> assertTrue(cl.getResource(mcmeta) != null, mcmeta + " missing — animation metadata required for the multi-frame flow tile"));
    }

    @Test
    void tintConstantMatchesTheExtensionsLookup() {
        // BLOOD_TINT is exposed publicly for downstream code (e.g. a future tint provider on
        // the bucket item); pin that it agrees with the value the extensions return.
        assertEquals(SharedClientFluidTypes.BLOOD_TINT, SharedClientFluidTypes.BLOOD_EXTENSIONS.getTintColor());
    }
}
