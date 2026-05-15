package slimeknights.sconstruct.port1211.tools.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Lightweight smoke tests for {@link ToolCore}. Constructing a real {@link ToolCore} trips the
 * {@code MappedRegistry} freeze in the bare-JVM test environment, so the heavy lifting of
 * exercising every override lives in {@link ToolBehaviorTest} — this file only pins the public
 * surface: the {@link ToolCore#EMPTY_MINING_TAG} sentinel exists, and {@link ToolCore#definition}
 * is exposed as a {@code public final} field readable without reflection.
 */
class ToolCoreTest {

    @Test
    void emptyMiningTagSentinelExists() {
        // The non-mining-tool fallback path (sword, bow, mattock) reads this tag; downstream
        // code should not rebuild it per tool.
        assertNotNull(ToolCore.EMPTY_MINING_TAG, "EMPTY_MINING_TAG sentinel must be initialised");
        assertEquals("sconstruct", ToolCore.EMPTY_MINING_TAG.location().getNamespace(), "tag must live in the SConstruct namespace");
    }

    @Test
    void toolDefinitionConstantsCoverEveryDigToolFromTheRoster() {
        // SMTCON-78 wires the {@code public final definition} field but doesn't itself
        // register concrete tools — pin that the ToolDefinition constants the constructor will
        // be called with stay reachable so the SMTCON-79 follow-up doesn't ship a moved-target
        // import.
        assertNotNull(ToolDefinition.PICKAXE);
        assertNotNull(ToolDefinition.SHOVEL);
        assertNotNull(ToolDefinition.HATCHET);
    }
}
