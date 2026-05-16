package slimeknights.sconstruct.port1211.data;

import java.util.Map;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.shared.Metal;
import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedItems;
import slimeknights.sconstruct.port1211.shared.SharedMetals;
import slimeknights.sconstruct.port1211.world.SlimeFluidSet;
import slimeknights.sconstruct.port1211.world.WorldBlocks;
import slimeknights.sconstruct.port1211.world.WorldFluids;
import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * {@code en_us} translation data provider. Baseline for the other twelve locales — those land
 * as static JSONs in a later migration task; this provider only owns English.
 *
 * <p>Entries are produced per registered object using the {@link LanguageProvider} helpers
 * ({@code add(Block, String)}, {@code add(Item, String)},
 * {@code add(CreativeModeTab, String)}), so the lang key is derived from the registered name
 * automatically and a future rename only requires editing here.
 *
 * <p>Metal display names use vanilla's "Block of X" / "X Ingot" / "X Nugget" templates so the
 * UI feels native. A few metals carry irregular names ({@code pigiron} → "Pig Iron",
 * {@code alubrass} → "Aluminum Brass") — those overrides live in {@link #METAL_DISPLAY_NAMES}
 * so the otherwise-derived title-casing stays the default.
 */
public final class TinkerLanguageProvider extends LanguageProvider {

    /** Display-name overrides for metal ids that don't title-case cleanly. */
    private static final Map<String, String> METAL_DISPLAY_NAMES = Map.of("pigiron", "Pig Iron", "alubrass", "Aluminum Brass");

    /**
     * Tool item display names keyed by registration path. Driven by an explicit table rather
     * than title-casing the id because several tools intentionally diverge from their registry
     * key — {@code axe} reads as "Hatchet" (the legacy single-handed axe), {@code sword} as
     * "Broadsword", and {@code tinker_arrow} drops its namespace-ish prefix to plain "Arrow".
     * {@code shuriken} lives here too even though it is not in {@link slimeknights.sconstruct.port1211.tools.item.ToolItems#ALL_TOOLS}
     * (it extends plain {@code Item}, not {@code ToolCore}) and is added separately below.
     */
    private static final Map<String, String> TOOL_DISPLAY_NAMES;
    static {
        Map<String, String> tools = new java.util.HashMap<>();
        tools.put("pickaxe", "Pickaxe");
        tools.put("shovel", "Shovel");
        tools.put("axe", "Hatchet");
        tools.put("sword", "Broadsword");
        tools.put("hammer", "Hammer");
        tools.put("excavator", "Excavator");
        tools.put("lumberaxe", "Lumber Axe");
        tools.put("scythe", "Scythe");
        tools.put("mattock", "Mattock");
        tools.put("cleaver", "Cleaver");
        tools.put("longsword", "Longsword");
        tools.put("rapier", "Rapier");
        tools.put("froe", "Froe");
        tools.put("shortbow", "Shortbow");
        tools.put("longbow", "Longbow");
        tools.put("crossbow", "Crossbow");
        tools.put("tinker_arrow", "Arrow");
        tools.put("shuriken", "Shuriken");
        TOOL_DISPLAY_NAMES = java.util.Collections.unmodifiableMap(tools);
    }

    /**
     * Tool-part display names keyed by {@link slimeknights.sconstruct.port1211.tools.PartType#id()}.
     * This is the complete expected id set — the {@code addTranslations} loop iterates
     * {@code PartType.values()} (count deliberately not hardcoded, since a future PartType may
     * push it past 18) and throws if it meets an id absent from this table, so a new part type
     * fails datagen loudly instead of silently shipping an untranslated item.
     */
    private static final Map<String, String> TOOL_PART_DISPLAY_NAMES;
    static {
        Map<String, String> parts = new java.util.HashMap<>();
        parts.put("pickhead", "Pickaxe Head");
        parts.put("axehead", "Axe Head");
        parts.put("shovelhead", "Shovel Head");
        parts.put("swordblade", "Sword Blade");
        parts.put("broadaxehead", "Broad Axe Head");
        parts.put("broadblade", "Broad Blade");
        parts.put("hammerhead", "Hammer Head");
        parts.put("handle", "Tool Rod");
        parts.put("binding", "Binding");
        parts.put("toughhandle", "Tough Tool Rod");
        parts.put("toughbinding", "Tough Binding");
        parts.put("bowlimb", "Bow Limb");
        parts.put("bowstring", "Bowstring");
        parts.put("arrowshaft", "Arrow Shaft");
        parts.put("arrow_head", "Arrow Head");
        parts.put("fletching", "Fletching");
        parts.put("wideguard", "Wide Guard");
        parts.put("largeplate", "Large Plate");
        TOOL_PART_DISPLAY_NAMES = java.util.Collections.unmodifiableMap(parts);
    }

    /**
     * Material display names keyed by raw material id. Materials are not registered objects, so
     * the lang key is the raw {@code material.tconstruct.<id>} form the legacy stat / tooltip
     * code resolves; iterated as a table so the set is auditable in one place.
     */
    private static final Map<String, String> MATERIAL_DISPLAY_NAMES;
    static {
        Map<String, String> materials = new java.util.LinkedHashMap<>();
        materials.put("wood", "Wood");
        materials.put("stone", "Stone");
        materials.put("iron", "Iron");
        materials.put("gold", "Gold");
        materials.put("flint", "Flint");
        materials.put("bone", "Bone");
        materials.put("paper", "Paper");
        materials.put("slime", "Green Slime");
        materials.put("blueslime", "Blue Slime");
        materials.put("cobalt", "Cobalt");
        materials.put("ardite", "Ardite");
        materials.put("manyullyn", "Manyullyn");
        materials.put("copper", "Copper");
        materials.put("silver", "Silver");
        materials.put("steel", "Steel");
        MATERIAL_DISPLAY_NAMES = java.util.Collections.unmodifiableMap(materials);
    }

    /**
     * Trait display names keyed by raw trait id. Like materials, traits are not registered
     * objects — the lang key is the raw {@code trait.tconstruct.<id>} form the trait tooltip
     * renderer resolves.
     */
    private static final Map<String, String> TRAIT_DISPLAY_NAMES;
    static {
        Map<String, String> traits = new java.util.LinkedHashMap<>();
        traits.put("autosmelt", "Auto-Smelt");
        traits.put("ecological", "Ecological");
        traits.put("stonebound", "Stonebound");
        traits.put("jagged", "Jagged");
        traits.put("crude", "Crude");
        traits.put("cheap", "Cheap");
        traits.put("dense", "Dense");
        traits.put("duritos", "Duritos");
        traits.put("aquadynamic", "Aquadynamic");
        traits.put("featherweight", "Featherweight");
        traits.put("holy", "Holy");
        traits.put("insatiable", "Insatiable");
        traits.put("magnetic", "Magnetic");
        traits.put("prickly", "Prickly");
        traits.put("slimey", "Slimey");
        traits.put("squeaky", "Squeaky");
        traits.put("fractured", "Fractured");
        traits.put("splintering", "Splintering");
        TRAIT_DISPLAY_NAMES = java.util.Collections.unmodifiableMap(traits);
    }

    public TinkerLanguageProvider(PackOutput output) {
        super(output, SConstruct.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Creative tab. LanguageProvider has no CreativeModeTab overload, so use the raw key.
        // Must match the lang key SharedTabs#GENERAL builder set via Component.translatable.
        add("itemGroup.sconstruct", "Smithies' Construct");

        // Modifier tooltip labels (SMTCON-84+). The key shape is
        // {@code modifier.<namespace>.<path>} — the substitution argument is the modifier's
        // current level, formatted by the description() callback that returns
        // Component.translatable(key, level). Datapacks that ship their own modifier roster
        // override these by shipping a competing translation.
        add("modifier.tconstruct.sharpness", "Sharpness %s");
        // SMTCON-85 vanilla-equivalent modifier labels. Redstone reads as "Haste", quartz as
        // "Sharper" (smaller-per-level sibling), lapis as "Luck" (fortune-equivalent). Diamond
        // and emerald are one-shot modifiers with no per-level number to surface, so their
        // labels omit the level substitution.
        add("modifier.tconstruct.redstone", "Haste %s");
        add("modifier.tconstruct.quartz", "Sharper %s");
        add("modifier.tconstruct.lapis", "Luck %s");
        add("modifier.tconstruct.diamond", "Diamond");
        add("modifier.tconstruct.emerald", "Emerald");
        // SMTCON-86 utility modifier labels. Silktouch is a one-shot (no level suffix);
        // the rest carry per-level numbering.
        add("modifier.tconstruct.silktouch", "Silky");
        add("modifier.tconstruct.beheading", "Beheading %s");
        add("modifier.tconstruct.smite", "Smite %s");
        add("modifier.tconstruct.bane_of_arthropods", "Bane of Arthropods %s");
        add("modifier.tconstruct.knockback", "Knockback %s");
        // SMTCON-87 specialty modifier labels. Moss / mending are one-shots (no level suffix);
        // fiery / necrotic / auto_repair carry per-level numbering.
        add("modifier.tconstruct.fiery", "Fiery %s");
        add("modifier.tconstruct.necrotic", "Necrotic %s");
        add("modifier.tconstruct.moss", "Mossy");
        add("modifier.tconstruct.mending", "Mending Moss");
        add("modifier.tconstruct.auto_repair", "Auto-Repair %s");
        // SMTCON-88 cap-tier / rarity modifier labels. Gilded is a one-shot harvest-tier bump
        // (no level suffix); reinforced / haste / luck carry per-level numbering.
        add("modifier.tconstruct.gilded", "Gilded");
        add("modifier.tconstruct.reinforced", "Reinforced %s");
        add("modifier.tconstruct.haste", "Hasty %s");
        add("modifier.tconstruct.luck", "Lucky %s");

        // SMTCON-89 tool tooltip. ToolCore.appendHoverText emits this line beneath the
        // modifier roster so the player can see how many free modifier slots remain before
        // walking up to the tool station. Substitution argument is the current count.
        add("tooltip.sconstruct.free_modifiers", "Free Modifier Slots: %s");

        // SMTCON-90 pattern chest. Two keys: the block name (shown on the placed-block tooltip
        // and in the inventory) and the container title (shown on the GUI header bar). Both
        // are written as raw keys because PatternChestRegistry registers under the BLOCKS
        // DeferredRegister and there's no Block overload that auto-derives the matching
        // container title key — the BE's getDisplayName resolves the second one.
        add("block.sconstruct.pattern_chest", "Pattern Chest");
        add("container.sconstruct.pattern_chest", "Pattern Chest");

        // SMTCON-91 stencil table. Four keys: the block name (placed-block tooltip + inventory
        // BlockItem), the container title (GUI header — resolved by
        // StencilTableBlockEntity#getDisplayName), the blank pattern item name (fallback when
        // the typed component is absent), and the typed-pattern name format (substituted by
        // PatternItem#getName with the PartType display name).
        add("block.sconstruct.stencil_table", "Stencil Table");
        add("container.sconstruct.stencil_table", "Stencil Table");
        add("item.sconstruct.blank_pattern", "Blank Pattern");
        add("item.sconstruct.pattern", "Pattern: %s");

        // SMTCON-92 part builder. Two keys: the block name (shown on the placed block + the
        // inventory BlockItem) and the container title (shown on the GUI header bar — resolved
        // by PartBuilderBlockEntity#getDisplayName).
        add("block.sconstruct.part_builder", "Part Builder");
        add("container.sconstruct.part_builder", "Part Builder");

        // SMTCON-93 tool station + tool forge. Four keys total: each block name (placed-block
        // tooltip + inventory BlockItem) and each container title (GUI header — resolved by
        // ToolStationBlockEntity#getDisplayName / ToolForgeBlockEntity#getDisplayName).
        add("block.sconstruct.tool_station", "Tool Station");
        add("container.sconstruct.tool_station", "Tool Station");
        add("block.sconstruct.tool_forge", "Tool Forge");
        add("container.sconstruct.tool_forge", "Tool Forge");
        // SMTCON-94 action buttons rendered in ToolStationScreen.
        add("button.sconstruct.tool_station.build", "Build");
        add("button.sconstruct.tool_station.modify", "Modify");

        // SMTCON-107 tool items. Iterate ToolItems.ALL_TOOLS so a tool added to that list
        // lights up here automatically; the display name comes from TOOL_DISPLAY_NAMES keyed by
        // the registration path. A tool present in ALL_TOOLS but absent from the table is a
        // datagen failure (IllegalStateException) rather than a silent untranslated item.
        for (net.neoforged.neoforge.registries.DeferredItem<? extends slimeknights.sconstruct.port1211.tools.item.ToolCore> holder : slimeknights.sconstruct.port1211.tools.item.ToolItems.ALL_TOOLS) {
            String id = holder.getId().getPath();
            String name = TOOL_DISPLAY_NAMES.get(id);
            if (name == null) {
                throw new IllegalStateException("ToolItems.ALL_TOOLS contains tool id '" + id + "' with no entry in TOOL_DISPLAY_NAMES");
            }
            add(holder.get(), name);
        }
        // Shuriken is registered outside ALL_TOOLS (plain Item, not ToolCore) — translate it
        // explicitly from the same table, with the same fail-loud guard as the loop above.
        String shurikenName = TOOL_DISPLAY_NAMES.get("shuriken");
        if (shurikenName == null) {
            throw new IllegalStateException("TOOL_DISPLAY_NAMES is missing the 'shuriken' entry");
        }
        add(slimeknights.sconstruct.port1211.tools.item.ToolItems.SHURIKEN.get(), shurikenName);

        // SMTCON-107 tool parts. Iterate PartType.values() rather than hardcoding a count so a
        // future PartType addition surfaces here — TOOL_PART_DISPLAY_NAMES is the complete
        // expected id set, and an id missing from it throws (datagen fails loudly).
        for (slimeknights.sconstruct.port1211.tools.PartType part : slimeknights.sconstruct.port1211.tools.PartType.values()) {
            String name = TOOL_PART_DISPLAY_NAMES.get(part.id());
            if (name == null) {
                throw new IllegalStateException("PartType '" + part.id() + "' has no entry in TOOL_PART_DISPLAY_NAMES — extend the table");
            }
            add(slimeknights.sconstruct.port1211.tools.item.ToolParts.get(part).get(), name);
        }

        // SMTCON-107 material names. Materials are not registered objects, so the raw
        // material.tconstruct.<id> key is written directly — this is the key the legacy stat /
        // tooltip code resolves when formatting a built tool's "<material> <part>" name.
        for (Map.Entry<String, String> entry : MATERIAL_DISPLAY_NAMES.entrySet()) {
            add("material.tconstruct." + entry.getKey(), entry.getValue());
        }

        // SMTCON-107 trait names. Like materials, traits are not registered objects — the raw
        // trait.tconstruct.<id> key is the one the trait tooltip renderer looks up.
        for (Map.Entry<String, String> entry : TRAIT_DISPLAY_NAMES.entrySet()) {
            add("trait.tconstruct." + entry.getKey(), entry.getValue());
        }

        // Metal storage blocks: "Block of Cobalt" (vanilla iron_block convention).
        // Ingots: "<Metal> Ingot"; nuggets: "<Metal> Nugget".
        int metals = SharedMetals.ALL.size();
        if (SharedItems.INGOTS.size() != metals || SharedItems.NUGGETS.size() != metals) {
            throw new IllegalStateException("SharedMetals.ALL / SharedItems.INGOTS / SharedItems.NUGGETS must be parallel in size");
        }
        for (int i = 0; i < metals; i++) {
            Metal metal = SharedMetals.ALL.get(i);
            String name = metalDisplayName(metal.id());
            if (SharedBlocks.METAL_BLOCKS.containsKey(metal.id())) {
                add(SharedBlocks.METAL_BLOCKS.get(metal.id()).get(), "Block of " + name);
            }
            // INGOTS / NUGGETS index matches SharedMetals.ALL ordering — pinned by
            // TinkerItemTagsProvider and ingotsAndNuggetsAreParallelByIndex.
            add(SharedItems.INGOTS.get(i).get(), name + " Ingot");
            add(SharedItems.NUGGETS.get(i).get(), name + " Nugget");
        }

        // Decoratives.
        add(SharedBlocks.GLOW.get(), "Glow");
        add(SharedBlocks.FIREWOOD.get(), "Firewood");
        add(SharedBlocks.LAVAWOOD.get(), "Lavawood");

        // Slimeballs (parity with vanilla "Slime Ball" capitalisation).
        add(SharedItems.SLIMEBALL_BLUE.get(), "Blue Slime Ball");
        add(SharedItems.SLIMEBALL_PURPLE.get(), "Purple Slime Ball");
        add(SharedItems.SLIMEBALL_BLOOD.get(), "Blood Slime Ball");
        add(SharedItems.SLIMEBALL_MAGMA.get(), "Magma Slime Ball");

        // Miscellaneous items.
        add(SharedItems.BACON.get(), "Bacon");
        add(SharedItems.MUDBRICK.get(), "Mud Brick");
        add(SharedItems.BUCKET_BLOOD.get(), "Bucket of Blood");

        // Blood fluid description id. Set explicitly here because Fluid#getDescriptionId is
        // a separate key from the BucketItem above ({@code fluid.sconstruct.blood} vs
        // {@code item.sconstruct.blood_bucket}).
        add("fluid.sconstruct.blood", "Blood");

        // World pulse: four coloured slime fluids. Iterate WorldFluids.ALL so a new colour
        // lights up here automatically (matches the SharedMetals iteration pattern above).
        for (SlimeFluidSet fluid : WorldFluids.ALL) {
            String displayColor = slimeColorDisplayName(fluid);
            // LiquidBlock auto-derives "block.sconstruct.<path>" from the registered name.
            add(fluid.block().get(), displayColor + " Slime");
            // BucketItem auto-derives "item.sconstruct.<path>_bucket" from the registered name.
            add(fluid.bucket().get(), "Bucket of " + displayColor + " Slime");
            // Fluid descriptionId is explicit; the FluidType ctor sets it to
            // "fluid.sconstruct.slime_<color>" and there is no add(FluidType, String) helper.
            add("fluid.sconstruct.slime_" + slimeColorId(fluid), "Liquid " + displayColor + " Slime");
        }

        // World pulse: four bouncy slime blocks. Iterate WorldBlocks.SLIME_BLOCKS keyed by
        // SlimeColor so a new colour added in the enum + WorldBlocks lights up here with no
        // edit.
        for (Map.Entry<SlimeColor, ? extends net.neoforged.neoforge.registries.DeferredBlock<?>> entry : WorldBlocks.SLIME_BLOCKS.entrySet()) {
            String colorId = entry.getKey().id();
            String display = Character.toUpperCase(colorId.charAt(0)) + colorId.substring(1);
            add(entry.getValue().get(), display + " Slime Block");
        }

        // World pulse: four plant sets, four entries each (dirt + grass + leaves + sapling).
        // Iterate WorldBlocks.PLANT_SETS so a new colour lights up the lang entries with one
        // line in WorldBlocks plus a single SlimeColor constant.
        for (Map.Entry<SlimeColor, slimeknights.sconstruct.port1211.world.block.SlimePlantSet> entry : WorldBlocks.PLANT_SETS.entrySet()) {
            String colorId = entry.getKey().id();
            String display = Character.toUpperCase(colorId.charAt(0)) + colorId.substring(1);
            slimeknights.sconstruct.port1211.world.block.SlimePlantSet set = entry.getValue();
            add(set.dirt().get(), display + " Slime Dirt");
            add(set.grass().get(), display + " Slime Grass");
            add(set.leaves().get(), display + " Slime Leaves");
            add(set.sapling().get(), display + " Slime Sapling");
        }

        // World pulse: four slime log blocks plus their stripped variants — 8 entries total.
        for (Map.Entry<SlimeColor, net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock>> entry : WorldBlocks.SLIME_LOGS.entrySet()) {
            String colorId = entry.getKey().id();
            String display = Character.toUpperCase(colorId.charAt(0)) + colorId.substring(1);
            add(entry.getValue().get(), display + " Slime Log");
        }
        for (Map.Entry<SlimeColor, net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock>> entry : WorldBlocks.STRIPPED_SLIME_LOGS.entrySet()) {
            String colorId = entry.getKey().id();
            String display = Character.toUpperCase(colorId.charAt(0)) + colorId.substring(1);
            add(entry.getValue().get(), "Stripped " + display + " Slime Log");
        }

        // World pulse: slime mob entities. entity.<modid>.<path> is the conventional auto-key
        // for entity types; LanguageProvider has no add(EntityType, String) overload so the
        // raw key is written explicitly.
        add(slimeknights.sconstruct.port1211.world.WorldEntities.BLUESLIME.get(), "Blueslime");
        add(slimeknights.sconstruct.port1211.world.WorldEntities.HUGESLIME.get(), "Huge Slime");
    }

    private static String slimeColorId(SlimeFluidSet fluid) {
        // Path is "sconstruct:slime_<color>" — strip the "slime_" prefix to recover the colour
        // segment for display-name and lang-key derivation.
        String path = fluid.source().getId().getPath();
        if (!path.startsWith("slime_")) {
            throw new IllegalStateException("expected SlimeFluidSet source path to start with 'slime_', got '" + path + "'");
        }
        return path.substring("slime_".length());
    }

    private static String slimeColorDisplayName(SlimeFluidSet fluid) {
        String id = slimeColorId(fluid);
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    /**
     * Display name for a metal id; falls back to a title-cased version of the raw id. The
     * Metal record's canonical constructor rejects blank ids, so the empty-string guard here
     * is defence-in-depth — if a caller ever invokes this directly with an empty string the
     * fall through returns it verbatim instead of throwing StringIndexOutOfBoundsException.
     */
    private static String metalDisplayName(String id) {
        String override = METAL_DISPLAY_NAMES.get(id);
        if (override != null) {
            return override;
        }
        if (id.isEmpty()) {
            return id;
        }
        String[] words = id.split("_");
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
            }
        }
        return String.join(" ", words);
    }
}
