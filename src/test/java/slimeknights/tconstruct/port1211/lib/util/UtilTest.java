package slimeknights.tconstruct.port1211.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import slimeknights.tconstruct.port1211.TConstruct;

/**
 * Pinned-behaviour tests for {@link Util}. Verifies the {@link Util#rl} helper produces a
 * ResourceLocation in the {@code tconstruct} namespace and propagates the underlying
 * {@link ResourceLocation#fromNamespaceAndPath} validation contract (invalid paths throw).
 */
class UtilTest {

    @Test
    void rlReturnsTheTconstructNamespacedResourceLocation() {
        ResourceLocation result = Util.rl("tool_station");
        assertEquals(TConstruct.MOD_ID, result.getNamespace(), "rl() must namespace the location under tconstruct");
        assertEquals("tool_station", result.getPath(), "rl() must preserve the supplied path verbatim");
    }

    @Test
    void rlAcceptsThePathCharactersResourceLocationAllows() {
        // a-z 0-9 _ . / - are all valid in a ResourceLocation path; the helper must propagate
        // each one cleanly so call sites can use namespaced sub-paths like
        // "smeltery/tank/seared".
        ResourceLocation nested = Util.rl("smeltery/tank/seared");
        assertEquals("smeltery/tank/seared", nested.getPath());
    }

    @Test
    void rlRejectsInvalidPathCharactersTheSameWayResourceLocationDoes() {
        // Uppercase or spaces are not valid in a ResourceLocation path; the helper must NOT
        // silently lowercase or sanitise — it must surface the underlying NeoForge contract
        // violation as the same exception type, otherwise call sites bypass the validation
        // they think they're getting.
        assertThrows(net.minecraft.ResourceLocationException.class, () -> Util.rl("Invalid Path"));
    }
}
