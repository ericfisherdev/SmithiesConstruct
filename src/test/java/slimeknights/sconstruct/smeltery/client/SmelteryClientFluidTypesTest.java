package slimeknights.sconstruct.smeltery.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;

/**
 * Pinned-behaviour tests for {@link SmelteryClientFluidTypes}. The full
 * {@code RegisterClientExtensionsEvent} pipeline depends on a live extension manager that only
 * exists in a running client — these tests cover the shared texture-pair contract, the
 * per-metal tint round-trip, the warm-orange fog colour, and the defensive-copy invariant on
 * {@code modifyFogColor}.
 */
class SmelteryClientFluidTypesTest {

    @Test
    void everyMoltenMetalSharesOneStillAndFlowTexture() {
        // The molten metals deliberately share a single greyscale texture pair — the per-metal
        // look is the tint multiplier, not a distinct sprite. Pin the namespace and path so a
        // future drift surfaces as a test failure rather than a missing-texture sprite.
        assertEquals(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/molten_metal_still"), SmelteryClientFluidTypes.STILL_TEXTURE);
        assertEquals(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "block/fluid/molten_metal_flow"), SmelteryClientFluidTypes.FLOW_TEXTURE);
    }

    @Test
    void extensionExposesTheSharedTexturesAndTheSuppliedTint() {
        int tint = 0xFF2882D4;
        IClientFluidTypeExtensions extension = SmelteryClientFluidTypes.extensionFor(tint);
        assertEquals(SmelteryClientFluidTypes.STILL_TEXTURE, extension.getStillTexture());
        assertEquals(SmelteryClientFluidTypes.FLOW_TEXTURE, extension.getFlowingTexture());
        assertEquals(tint, extension.getTintColor(), "getTintColor must round-trip the per-metal tint verbatim");
    }

    @Test
    void fogColorOfNormalisesEachChannelToUnitRange() {
        Vector3f fog = SmelteryClientFluidTypes.fogColorOf(0xC8641E);
        assertEquals(0xC8 / 255.0F, fog.x(), 1.0e-6F);
        assertEquals(0x64 / 255.0F, fog.y(), 1.0e-6F);
        assertEquals(0x1E / 255.0F, fog.z(), 1.0e-6F);
    }

    @Test
    void modifyFogColorReturnsTheWarmFogAsAFreshCopyEachCall() {
        IClientFluidTypeExtensions extension = SmelteryClientFluidTypes.extensionFor(0xFFFFFFFF);
        Vector3f expected = SmelteryClientFluidTypes.fogColorOf(SmelteryClientFluidTypes.WARM_FOG_TINT);
        Vector3f first = extension.modifyFogColor(null, 0.0F, null, 0, 0.0F, new Vector3f());
        Vector3f second = extension.modifyFogColor(null, 0.0F, null, 0, 0.0F, new Vector3f());
        assertEquals(expected, first, "fog colour must be the warm-orange constant");
        assertEquals(expected, second);
        // The vanilla fog pipeline mutates the returned vector in place — each call must hand
        // back a distinct instance so a blend on one call cannot corrupt the next.
        assertNotSame(first, second, "modifyFogColor must return a fresh copy each call");
    }

    @Test
    void warmFogTintIsAWarmOrange() {
        // Sanity-check the constant reads as orange: red dominates, green is mid, blue is lowest.
        int r = (SmelteryClientFluidTypes.WARM_FOG_TINT >> 16) & 0xFF;
        int g = (SmelteryClientFluidTypes.WARM_FOG_TINT >> 8) & 0xFF;
        int b = SmelteryClientFluidTypes.WARM_FOG_TINT & 0xFF;
        assertTrue(r > g && g > b, "warm orange should grade red > green > blue");
    }
}
