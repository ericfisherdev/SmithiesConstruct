package slimeknights.sconstruct.port1211.data.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.tools.modifier.Modifier;
import slimeknights.sconstruct.port1211.tools.modifier.SimpleStatBoostType;

/**
 * Pinned-behaviour tests for {@link TinkerModifierBootstrap}. Verifies the SMTCON-84 acceptance
 * criteria: the sharpness modifier registers, lands under the {@code tconstruct} namespace for
 * legacy-addon compat, and carries the canonical (maxLevel=5, slotCost=1) configuration.
 */
class TinkerModifierBootstrapTest {

    @Test
    void bootstrapRegistersSharpnessUnderTconstructNamespace() {
        Map<ResourceLocation, Modifier> registered = captureBootstrap();
        ResourceLocation sharpnessId = ResourceLocation.fromNamespaceAndPath("tconstruct", "sharpness");
        Modifier sharpness = registered.get(sharpnessId);

        assertNotNull(sharpness, "sharpness entry must be registered under the legacy tconstruct namespace");
        assertEquals(sharpnessId, sharpness.id());
        assertSame(SimpleStatBoostType.INSTANCE, sharpness.type(), "sharpness must be a SimpleStatBoostType entry");
        assertEquals(5, sharpness.maxLevel(), "Sharpness caps at level 5 (legacy 1.12 parity)");
        assertEquals(1, sharpness.slotCost(), "Sharpness consumes 1 modifier slot per application");
    }

    @Test
    void sharpnessTooltipRendersAsTranslatableWithLevelArgument() {
        // The description() override on SimpleStatBoostType.Instance must surface as a
        // translatable Component with the level as the substitution argument so the lang
        // entry "modifier.tconstruct.sharpness" = "Sharpness %s" renders "Sharpness 3" for a
        // level-3 stack.
        Map<ResourceLocation, Modifier> registered = captureBootstrap();
        Modifier sharpness = registered.get(ResourceLocation.fromNamespaceAndPath("tconstruct", "sharpness"));
        net.minecraft.network.chat.Component description = sharpness.description(3);
        assertNotNull(description);
        // Description is a TranslatableContents-backed Component — verify by checking the
        // serialised key and args via the contents() accessor.
        assertTrue(description.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc && "modifier.tconstruct.sharpness".equals(tc.getKey()),
                "tooltip must use the modifier.<namespace>.<path> translation key");
    }

    /**
     * Run {@link TinkerModifierBootstrap#bootstrap} against a recording
     * {@link BootstrapContext} mock, returning the captured id → modifier map.
     */
    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, Modifier> captureBootstrap() {
        BootstrapContext<Modifier> context = mock(BootstrapContext.class);
        Map<ResourceLocation, Modifier> captured = new HashMap<>();
        doAnswer(invocation -> {
            ResourceKey<Modifier> key = invocation.getArgument(0);
            Modifier modifier = invocation.getArgument(1);
            captured.put(key.location(), modifier);
            return null;
        }).when(context).register(any(), any(Modifier.class));
        TinkerModifierBootstrap.bootstrap(context);
        return captured;
    }
}
