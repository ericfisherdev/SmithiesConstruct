package slimeknights.sconstruct.port1211.smeltery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Pinned-roster tests for {@link MoltenMetals}. The 20 molten-metal fluids are asserted by id
 * so a future roster change surfaces loudly, and the {@link MoltenMetal} field invariants
 * (kelvin temperature, density, 0-15 luminosity) are spot-checked against the SMTCON-108
 * temperature anchors (iron 1500 K, cobalt 2400 K, ender 3000 K).
 */
class MoltenMetalsTest {

    @Test
    void registersAllTwentyMoltenMetals() {
        assertEquals(20, MoltenMetals.ALL.size(), "ALL must hold exactly the 20 molten metals");
        Set<String> ids = Set.copyOf(MoltenMetals.ALL.stream().map(MoltenMetal::id).toList());
        assertEquals(20, ids.size(), "molten-metal ids must be unique");
        for (String id : new String[] { "iron", "gold", "copper", "tin", "zinc", "brass", "alubrass", "bronze", "silver", "lead", "steel", "cobalt", "ardite", "manyullyn", "pigiron", "knightslime",
                "obsidian", "glass", "emerald", "ender" }) {
            assertTrue(ids.contains(id), "missing molten metal id: " + id);
        }
    }

    @Test
    void temperatureAnchorsMatchTheSmtcon108Plan() {
        assertEquals(1500, MoltenMetals.IRON.temperature(), "iron is the 1500 K anchor");
        assertEquals(2400, MoltenMetals.COBALT.temperature(), "cobalt is the 2400 K anchor");
        assertEquals(3000, MoltenMetals.ENDER.temperature(), "ender is the 3000 K anchor — the hottest melt");
    }

    @Test
    void everyMeltHasFiniteSinkingFluidProperties() {
        for (MoltenMetal metal : MoltenMetals.ALL) {
            assertTrue(metal.temperature() > 0, metal.id() + " temperature must be a positive kelvin value");
            assertTrue(metal.density() > 0, metal.id() + " density must be positive — molten metal sinks");
            assertTrue(metal.luminosity() >= 0 && metal.luminosity() <= 15, metal.id() + " luminosity must be a 0-15 block-light level");
            assertEquals(0, metal.tint() & ~0xFFFFFF, metal.id() + " tint must fit in 24 bits");
        }
    }
}
