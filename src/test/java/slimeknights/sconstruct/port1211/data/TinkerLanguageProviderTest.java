package slimeknights.sconstruct.port1211.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedMetals;

/**
 * Pinned-behaviour tests for the generated {@code en_us.json}. {@link TinkerLanguageProvider}
 * runs during {@code ./gradlew runData} and writes one file at
 * {@code src/generated/resources/assets/sconstruct/lang/en_us.json}. The tests treat the
 * file as a snapshot read from the test classpath and check key presence + display values
 * for the canonical Phase-2 surfaces.
 */
class TinkerLanguageProviderTest {

    private static final Gson GSON = new Gson();
    private static final String LANG_FILE = "assets/sconstruct/lang/en_us.json";

    @Test
    void creativeTabTitleIsSmithiesConstruct() {
        // The lang key here must match the Component.translatable("itemGroup.sconstruct")
        // wired by SharedTabs.GENERAL — a drifted key would render the tab as a raw key
        // string in-game.
        assertEquals("Smithies' Construct", lang().get("itemGroup.sconstruct").getAsString());
    }

    @Test
    void everyMetalHasIngotAndNuggetEntries() {
        JsonObject lang = lang();
        assertAll(SharedMetals.ALL.stream().map(metal -> () -> {
            String ingotKey = "item.sconstruct.ingot_" + metal.id();
            String nuggetKey = "item.sconstruct.nugget_" + metal.id();
            assertTrue(lang.has(ingotKey), ingotKey + " missing");
            assertTrue(lang.has(nuggetKey), nuggetKey + " missing");
            assertTrue(lang.get(ingotKey).getAsString().endsWith(" Ingot"), ingotKey + " should end with ' Ingot'");
            assertTrue(lang.get(nuggetKey).getAsString().endsWith(" Nugget"), nuggetKey + " should end with ' Nugget'");
        }));
    }

    @Test
    void metalDisplayNameOverridesApplied() {
        // pigiron and alubrass have irregular display names that override the default
        // title-case derivation. A future refactor that drops the override map would render
        // them as "Pigiron Ingot" / "Alubrass Ingot" — pin the friendly forms.
        JsonObject lang = lang();
        assertAll(() -> assertEquals("Pig Iron Ingot", lang.get("item.sconstruct.ingot_pigiron").getAsString()),
                () -> assertEquals("Pig Iron Nugget", lang.get("item.sconstruct.nugget_pigiron").getAsString()),
                () -> assertEquals("Block of Pig Iron", lang.get("block.sconstruct.block_pigiron").getAsString()),
                () -> assertEquals("Aluminum Brass Ingot", lang.get("item.sconstruct.ingot_alubrass").getAsString()),
                () -> assertEquals("Aluminum Brass Nugget", lang.get("item.sconstruct.nugget_alubrass").getAsString()),
                () -> assertEquals("Block of Aluminum Brass", lang.get("block.sconstruct.block_alubrass").getAsString()));
    }

    @Test
    void metalBlockEntriesPresentOnlyForMetalsWithABlock() {
        // Lead and nickel have no storage block — they must NOT have a block.sconstruct.block_X
        // entry. Every other metal does.
        JsonObject lang = lang();
        for (String id : SharedBlocks.METAL_BLOCKS.keySet()) {
            String key = "block.sconstruct.block_" + id;
            assertTrue(lang.has(key), key + " missing");
            assertTrue(lang.get(key).getAsString().startsWith("Block of "), key + " should start with 'Block of '");
        }
        assertAll(() -> assertTrue(!lang.has("block.sconstruct.block_lead"), "lead has no block — no lang entry"),
                () -> assertTrue(!lang.has("block.sconstruct.block_nickel"), "nickel has no block — no lang entry"));
    }

    @Test
    void decorativeBlocksAndMiscItemsHaveExpectedDisplayNames() {
        JsonObject lang = lang();
        assertAll(() -> assertEquals("Glow", lang.get("block.sconstruct.glow").getAsString()), () -> assertEquals("Firewood", lang.get("block.sconstruct.firewood").getAsString()),
                () -> assertEquals("Lavawood", lang.get("block.sconstruct.lavawood").getAsString()), () -> assertEquals("Bacon", lang.get("item.sconstruct.bacon").getAsString()),
                () -> assertEquals("Mud Brick", lang.get("item.sconstruct.mudbrick").getAsString()), () -> assertEquals("Bucket of Blood", lang.get("item.sconstruct.blood_bucket").getAsString()),
                () -> assertEquals("Blood", lang.get("fluid.sconstruct.blood").getAsString()));
    }

    @Test
    void slimeballEntriesUseSlimeBallTwoWordCapitalisation() {
        // Vanilla writes "Slime Ball" (two words). The lang strings here should match so
        // sconstruct slimeballs read naturally next to vanilla ones in the recipe book.
        JsonObject lang = lang();
        assertAll(() -> assertEquals("Blue Slime Ball", lang.get("item.sconstruct.slimeball_blue").getAsString()),
                () -> assertEquals("Purple Slime Ball", lang.get("item.sconstruct.slimeball_purple").getAsString()),
                () -> assertEquals("Blood Slime Ball", lang.get("item.sconstruct.slimeball_blood").getAsString()),
                () -> assertEquals("Magma Slime Ball", lang.get("item.sconstruct.slimeball_magma").getAsString()));
    }

    @Test
    void smtcon87SpecialtyModifierLabelsRegistered() {
        // Pinned key/value assertions for SMTCON-87 — the size check below would still pass if
        // a key were mistyped (e.g. "auto-repair" vs "auto_repair") as long as the count stayed
        // at 113, so verify each key + display value directly.
        JsonObject lang = lang();
        assertAll(() -> assertTrue(lang.has("modifier.tconstruct.fiery"), "fiery key missing"), () -> assertEquals("Fiery %s", lang.get("modifier.tconstruct.fiery").getAsString()),
                () -> assertTrue(lang.has("modifier.tconstruct.necrotic"), "necrotic key missing"), () -> assertEquals("Necrotic %s", lang.get("modifier.tconstruct.necrotic").getAsString()),
                () -> assertTrue(lang.has("modifier.tconstruct.moss"), "moss key missing"), () -> assertEquals("Mossy", lang.get("modifier.tconstruct.moss").getAsString()),
                () -> assertTrue(lang.has("modifier.tconstruct.mending"), "mending key missing"), () -> assertEquals("Mending Moss", lang.get("modifier.tconstruct.mending").getAsString()),
                () -> assertTrue(lang.has("modifier.tconstruct.auto_repair"), "auto_repair key missing"),
                () -> assertEquals("Auto-Repair %s", lang.get("modifier.tconstruct.auto_repair").getAsString()));
    }

    @Test
    void smtcon88CapTierModifierLabelsRegistered() {
        // Pinned key/value assertions for SMTCON-88 — the size check below would still pass if
        // a key were mistyped, so verify each key + display value directly.
        JsonObject lang = lang();
        assertAll(() -> assertTrue(lang.has("modifier.tconstruct.gilded"), "gilded key missing"), () -> assertEquals("Gilded", lang.get("modifier.tconstruct.gilded").getAsString()),
                () -> assertTrue(lang.has("modifier.tconstruct.reinforced"), "reinforced key missing"), () -> assertEquals("Reinforced %s", lang.get("modifier.tconstruct.reinforced").getAsString()),
                () -> assertTrue(lang.has("modifier.tconstruct.haste"), "haste key missing"), () -> assertEquals("Hasty %s", lang.get("modifier.tconstruct.haste").getAsString()),
                () -> assertTrue(lang.has("modifier.tconstruct.luck"), "luck key missing"), () -> assertEquals("Lucky %s", lang.get("modifier.tconstruct.luck").getAsString()));
    }

    @Test
    void smtcon89ToolTooltipKeyRegistered() {
        // SMTCON-89 adds one tooltip lang key for the free-modifier-slot count rendered by
        // ToolCore.appendHoverText. The key must carry a %s substitution for the level.
        JsonObject lang = lang();
        assertTrue(lang.has("tooltip.sconstruct.free_modifiers"), "free_modifiers tooltip key missing");
        assertEquals("Free Modifier Slots: %s", lang.get("tooltip.sconstruct.free_modifiers").getAsString());
    }

    @Test
    void patternChestLangKeysRegistered() {
        // SMTCON-90 adds two lang keys: the block-name key (used by the placed block + inventory
        // BlockItem) and the container-title key (used by PatternChestBlockEntity#getDisplayName).
        // A mistyped key here would render the GUI title as raw "container.sconstruct.pattern_chest"
        // in-game — pin both keys + their friendly display values directly.
        JsonObject lang = lang();
        assertAll(() -> assertTrue(lang.has("block.sconstruct.pattern_chest"), "block pattern_chest key missing"),
                () -> assertEquals("Pattern Chest", lang.get("block.sconstruct.pattern_chest").getAsString()),
                () -> assertTrue(lang.has("container.sconstruct.pattern_chest"), "container pattern_chest key missing"),
                () -> assertEquals("Pattern Chest", lang.get("container.sconstruct.pattern_chest").getAsString()));
    }

    @Test
    void stencilTableLangKeysRegistered() {
        // SMTCON-91 adds four lang keys: block name, container title, blank pattern item, and
        // the typed-pattern name format (carrying a %s substitution for the PartType display
        // name). Pin each key + display value directly so a mistyped key wouldn't render the
        // GUI title or item name as raw lang keys in-game.
        JsonObject lang = lang();
        assertAll(() -> assertTrue(lang.has("block.sconstruct.stencil_table"), "block stencil_table key missing"),
                () -> assertEquals("Stencil Table", lang.get("block.sconstruct.stencil_table").getAsString()),
                () -> assertTrue(lang.has("container.sconstruct.stencil_table"), "container stencil_table key missing"),
                () -> assertEquals("Stencil Table", lang.get("container.sconstruct.stencil_table").getAsString()),
                () -> assertTrue(lang.has("item.sconstruct.blank_pattern"), "blank_pattern key missing"), () -> assertEquals("Blank Pattern", lang.get("item.sconstruct.blank_pattern").getAsString()),
                () -> assertTrue(lang.has("item.sconstruct.pattern"), "pattern key missing"), () -> assertEquals("Pattern: %s", lang.get("item.sconstruct.pattern").getAsString()));
    }

    @Test
    void partBuilderLangKeysRegistered() {
        // SMTCON-92 adds two lang keys: the block-name key (used by the placed block + inventory
        // BlockItem) and the container-title key (used by PartBuilderBlockEntity#getDisplayName).
        // A mistyped key here would render the GUI title as raw "container.sconstruct.part_builder"
        // in-game — pin both keys + their friendly display values directly.
        JsonObject lang = lang();
        assertAll(() -> assertTrue(lang.has("block.sconstruct.part_builder"), "block part_builder key missing"),
                () -> assertEquals("Part Builder", lang.get("block.sconstruct.part_builder").getAsString()),
                () -> assertTrue(lang.has("container.sconstruct.part_builder"), "container part_builder key missing"),
                () -> assertEquals("Part Builder", lang.get("container.sconstruct.part_builder").getAsString()));
    }

    @Test
    void toolStationAndForgeLangKeysRegistered() {
        // SMTCON-93 adds four lang keys — block-name + container-title for each of the two
        // stations. Pinning both pairs here so a typo in either key (which would render the
        // GUI title or placed-block tooltip as the raw key in-game) fails fast.
        JsonObject lang = lang();
        assertAll(() -> assertTrue(lang.has("block.sconstruct.tool_station"), "block tool_station key missing"),
                () -> assertEquals("Tool Station", lang.get("block.sconstruct.tool_station").getAsString()),
                () -> assertTrue(lang.has("container.sconstruct.tool_station"), "container tool_station key missing"),
                () -> assertEquals("Tool Station", lang.get("container.sconstruct.tool_station").getAsString()),
                () -> assertTrue(lang.has("block.sconstruct.tool_forge"), "block tool_forge key missing"), () -> assertEquals("Tool Forge", lang.get("block.sconstruct.tool_forge").getAsString()),
                () -> assertTrue(lang.has("container.sconstruct.tool_forge"), "container tool_forge key missing"),
                () -> assertEquals("Tool Forge", lang.get("container.sconstruct.tool_forge").getAsString()));
    }

    @Test
    void totalEntryCountMatchesExpectedCoverage() {
        // 1 tab + 13 metal blocks + 3 decoratives + 15 ingots + 15 nuggets + 4 slimeballs
        // + bacon + mudbrick + blood bucket + blood fluid + 4 slime fluids × 3 entries each
        // (block, bucket, fluid descriptionId) + 4 slime blocks + 4 plant sets × 4 entries
        // each (dirt, grass, leaves, sapling) + 4 slime logs + 4 stripped slime logs + 2 slime
        // mobs (blueslime + hugeslime) + 1 sharpness + 5 SMTCON-85 vanilla modifier labels
        // + 5 SMTCON-86 utility modifier labels (silktouch, beheading, smite,
        // bane_of_arthropods, knockback) + 5 SMTCON-87 specialty modifier labels (fiery,
        // necrotic, moss, mending, auto_repair) + 4 SMTCON-88 cap-tier modifier labels
        // (gilded, reinforced, haste, luck) + 1 SMTCON-89 tool tooltip key + 2 SMTCON-90
        // pattern chest keys (block + container title) + 4 SMTCON-91 stencil table keys
        // (block + container title + blank_pattern item + typed pattern name format)
        // + 2 SMTCON-92 part builder keys (block + container title) + 4 SMTCON-93 tool
        // station / tool forge keys (2 blocks × {block name + container title}) + 2
        // SMTCON-94 action-button keys (Build, Modify) = 132.
        assertEquals(132, lang().entrySet().size());
    }

    @SuppressWarnings("PMD.UseProperClassLoader") // proper context loader checked first; fallback fires only when null
    private static JsonObject lang() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = TinkerLanguageProviderTest.class.getClassLoader();
        }
        try (InputStream stream = cl.getResourceAsStream(LANG_FILE)) {
            assertNotNull(stream, LANG_FILE + " missing from test classpath — did you re-run ./gradlew runData?");
            JsonObject parsed = GSON.fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
            assertNotNull(parsed, LANG_FILE + " is not valid JSON");
            return parsed;
        }
        catch (IOException e) {
            throw new AssertionError("failed reading " + LANG_FILE, e);
        }
    }
}
