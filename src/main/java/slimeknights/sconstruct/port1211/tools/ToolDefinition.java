package slimeknights.sconstruct.port1211.tools;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Per-tool metadata that drives {@code ToolCore} behaviour and the tool-building UI: which
 * {@link PartType} slots the tool requires (positional, legacy order), the vanilla mining tag
 * that decides which blocks the tool is "right" for, the {@link ItemAbility ItemAbilities} the
 * item exposes (NeoForge 1.21 replacement for the legacy {@code ToolAction} type — not an enum,
 * so the ticket's {@code EnumSet<ToolAction>} suggestion is realised as a {@link Set} of
 * abilities), and the modifier-slot baseline every freshly-built tool starts with.
 *
 * <p>The legacy 1.12 line set every tool's baseline modifier count from
 * {@code ToolCore.DEFAULT_MODIFIERS = 3} (see
 * {@code src/main/java/slimeknights/tconstruct/library/tools/ToolCore.java}). The constants
 * below preserve that for byte-for-byte parity with player expectation.
 *
 * <p>Part lists are positional and match the legacy {@code PartMaterialType(...)} argument
 * order in {@code TinkerHarvestTools}, {@code TinkerMeleeWeapons}, and
 * {@code TinkerRangedWeapons}. Tools whose dig action is not gated by a vanilla mining tag
 * (mattock, swords, bows, arrows) record {@code null} for {@link #miningTag()} — null is the
 * documented "no mining tag" sentinel rather than a hidden default, so a caller that forgets
 * to handle it gets a fast NPE instead of silently digging the wrong block set.
 */
public record ToolDefinition(String id, List<PartType> parts, @Nullable TagKey<Block> miningTag, Set<ItemAbility> abilities, int baseModifierSlots) {

    /**
     * Legacy {@code ToolCore.DEFAULT_MODIFIERS} value — every tool starts with 3 free modifier
     * slots. Centralised here so a future cap-tier ticket only edits one constant.
     */
    public static final int DEFAULT_MODIFIER_SLOTS = 3;

    /**
     * Defensive copies so callers can't mutate the backing list / set after construction.
     * Records would otherwise leak the caller's collection identity and let an alias rewrite
     * the part roster post-build.
     */
    public ToolDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(abilities, "abilities");
        parts = List.copyOf(parts);
        abilities = Set.copyOf(abilities);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("ToolDefinition '" + id + "' must declare at least one part slot");
        }
        if (baseModifierSlots < 0) {
            throw new IllegalArgumentException("ToolDefinition '" + id + "' baseModifierSlots must be non-negative");
        }
    }

    /** Number of part slots the tool building UI must surface for this tool. */
    public int getPartCount() {
        return parts.size();
    }

    /**
     * Part-type required at the given slot index (0-based, matches legacy positional order).
     * Throws {@link IndexOutOfBoundsException} on an out-of-range index — the caller is
     * iterating with {@link #getPartCount()} or hard-coded against a known tool, so an
     * out-of-range index is a logic bug worth surfacing loudly.
     */
    public PartType getPartSlot(int index) {
        return parts.get(index);
    }

    // ----- Tool constants (audited against legacy TinkerTools / TinkerHarvestTools /
    // TinkerMeleeWeapons / TinkerRangedWeapons; see the per-tool comment for the legacy site). -----

    /** {@code Pickaxe}: handle + pickHead + binding ({@code TinkerHarvestTools:77}). */
    public static final ToolDefinition PICKAXE = new ToolDefinition("pickaxe", List.of(PartType.HANDLE, PartType.PICKHEAD, PartType.BINDING), BlockTags.MINEABLE_WITH_PICKAXE,
            ItemAbilities.DEFAULT_PICKAXE_ACTIONS, DEFAULT_MODIFIER_SLOTS);

    /** {@code Shovel}: handle + shovelHead + binding ({@code TinkerHarvestTools:78}). */
    public static final ToolDefinition SHOVEL = new ToolDefinition("shovel", List.of(PartType.HANDLE, PartType.SHOVELHEAD, PartType.BINDING), BlockTags.MINEABLE_WITH_SHOVEL,
            ItemAbilities.DEFAULT_SHOVEL_ACTIONS, DEFAULT_MODIFIER_SLOTS);

    /** {@code Hatchet}: handle + axeHead + binding ({@code TinkerHarvestTools:79}). */
    public static final ToolDefinition HATCHET = new ToolDefinition("hatchet", List.of(PartType.HANDLE, PartType.AXEHEAD, PartType.BINDING), BlockTags.MINEABLE_WITH_AXE,
            ItemAbilities.DEFAULT_AXE_ACTIONS, DEFAULT_MODIFIER_SLOTS);

    /**
     * {@code Mattock}: handle + axeHead + shovelHead ({@code TinkerHarvestTools:80}). Legacy
     * used a custom {@code "mattock"} harvest-class so vanilla mining tags don't model it
     * cleanly — null mining tag, ability set is the union of axe / shovel / hoe behaviours.
     */
    public static final ToolDefinition MATTOCK = new ToolDefinition("mattock", List.of(PartType.HANDLE, PartType.AXEHEAD, PartType.SHOVELHEAD), null,
            Set.of(ItemAbilities.AXE_DIG, ItemAbilities.SHOVEL_DIG, ItemAbilities.HOE_DIG, ItemAbilities.HOE_TILL, ItemAbilities.SHOVEL_FLATTEN, ItemAbilities.AXE_STRIP), DEFAULT_MODIFIER_SLOTS);

    /**
     * {@code Hammer}: toughHandle + hammerHead + largePlate + largePlate
     * ({@code TinkerHarvestTools:83}). Two large-plate slots intentionally — legacy passed two
     * separate {@code PartMaterialType.head(largePlate)} entries so each plate carries
     * independent material stats.
     */
    public static final ToolDefinition HAMMER = new ToolDefinition("hammer", List.of(PartType.TOUGHHANDLE, PartType.HAMMERHEAD, PartType.LARGEPLATE, PartType.LARGEPLATE),
            BlockTags.MINEABLE_WITH_PICKAXE, ItemAbilities.DEFAULT_PICKAXE_ACTIONS, DEFAULT_MODIFIER_SLOTS);

    /**
     * {@code LumberAxe}: toughHandle + broadAxeHead + largePlate + toughBinding
     * ({@code TinkerHarvestTools:85}).
     */
    public static final ToolDefinition LUMBER_AXE = new ToolDefinition("lumberaxe", List.of(PartType.TOUGHHANDLE, PartType.BROADAXEHEAD, PartType.LARGEPLATE, PartType.TOUGHBINDING),
            BlockTags.MINEABLE_WITH_AXE, ItemAbilities.DEFAULT_AXE_ACTIONS, DEFAULT_MODIFIER_SLOTS);

    /**
     * {@code Excavator}: toughHandle + shovelHead + largePlate + toughBinding
     * ({@code TinkerHarvestTools:84}). Heavy-shovel AOE counterpart to the lumber-axe; the
     * legacy 1.12 line used a dedicated {@code excavatorHead} part, but the port reuses
     * {@link PartType#SHOVELHEAD} so the existing shovel-head material library still feeds the
     * tool — the AOE behaviour lives in {@link slimeknights.sconstruct.port1211.tools.item.AoeToolCore}
     * rather than in the part roster.
     */
    public static final ToolDefinition EXCAVATOR = new ToolDefinition("excavator", List.of(PartType.TOUGHHANDLE, PartType.SHOVELHEAD, PartType.LARGEPLATE, PartType.TOUGHBINDING),
            BlockTags.MINEABLE_WITH_SHOVEL, ItemAbilities.DEFAULT_SHOVEL_ACTIONS, DEFAULT_MODIFIER_SLOTS);

    /**
     * {@code Scythe}: toughHandle + broadBlade + toughBinding + toughBinding
     * ({@code TinkerMeleeWeapons:80}). The legacy 1.12 line declared a dedicated
     * {@code scytheHead}; the port reuses {@link PartType#BROADBLADE} since the part-material
     * surface is shared with the cleaver. Mining ability set is empty — the scythe's AOE
     * effect is an attack-side override on {@link slimeknights.sconstruct.port1211.tools.item.ScytheItem},
     * not a vanilla-tag dig.
     */
    public static final ToolDefinition SCYTHE = new ToolDefinition("scythe", List.of(PartType.TOUGHHANDLE, PartType.BROADBLADE, PartType.TOUGHBINDING, PartType.TOUGHBINDING), null,
            ItemAbilities.DEFAULT_SWORD_ACTIONS, DEFAULT_MODIFIER_SLOTS);

    /** {@code BroadSword}: handle + swordBlade + wideGuard ({@code TinkerMeleeWeapons:75}). */
    public static final ToolDefinition BROADSWORD = new ToolDefinition("broadsword", List.of(PartType.HANDLE, PartType.SWORDBLADE, PartType.WIDEGUARD), null, ItemAbilities.DEFAULT_SWORD_ACTIONS,
            DEFAULT_MODIFIER_SLOTS);

    /**
     * {@code ShortBow}: bowLimb + bowLimb + bowString ({@code TinkerRangedWeapons:100}).
     * Two limb slots intentionally — top and bottom limbs carry independent stats in legacy.
     * No vanilla mining tag (bows aren't dig tools); ability set empty since NeoForge has no
     * "bow draw" ability and ranged shooting is driven by item-class behaviour.
     */
    public static final ToolDefinition SHORTBOW = new ToolDefinition("shortbow", List.of(PartType.BOWLIMB, PartType.BOWLIMB, PartType.BOWSTRING), null, Set.of(), DEFAULT_MODIFIER_SLOTS);

    /**
     * {@code CrossBow}: toughHandle + bowLimb + toughBinding + bowString
     * ({@code TinkerRangedWeapons:103}). Same ability-set rationale as {@link #SHORTBOW}.
     */
    public static final ToolDefinition CROSSBOW = new ToolDefinition("crossbow", List.of(PartType.TOUGHHANDLE, PartType.BOWLIMB, PartType.TOUGHBINDING, PartType.BOWSTRING), null, Set.of(),
            DEFAULT_MODIFIER_SLOTS);

    /**
     * {@code Arrow}: arrowShaft + arrow_head + fletching ({@code TinkerRangedWeapons:105}).
     * Ammunition rather than a held tool — no mining tag, no ability set.
     */
    public static final ToolDefinition ARROW = new ToolDefinition("arrow", List.of(PartType.ARROWSHAFT, PartType.ARROW_HEAD, PartType.FLETCHING), null, Set.of(), DEFAULT_MODIFIER_SLOTS);

    /**
     * Basic tool definitions buildable at the Tool Station (SMTCON-93) — three-part tools that
     * the legacy 1.12 Tool Station supports. Iteration surface for
     * {@code ToolStationLogic#tryBuild} when called from the base
     * {@code ToolStationBlockEntity}; the Tool Forge widens this to {@link #ALL_ADVANCED} so
     * hammer / lumberaxe / crossbow / arrow become buildable.
     *
     * <p>Note: arrow / shortbow / crossbow are ranged ammunition / weapons whose 1.12 line
     * required the Tool Forge — they're listed in {@link #ALL_ADVANCED} alongside the heavy
     * 4-part definitions. The Tool Station only handles the three "vanilla-equivalent"
     * harvest / melee tools the {@link slimeknights.sconstruct.port1211.tools.item.ToolItems}
     * registry currently exposes.
     */
    public static final List<ToolDefinition> ALL_BASIC = List.of(PICKAXE, SHOVEL, HATCHET, MATTOCK, BROADSWORD, SCYTHE);

    /**
     * Advanced tool definitions buildable at the Tool Forge (SMTCON-93) — the {@link #ALL_BASIC}
     * roster plus the heavy / ranged definitions whose legacy 1.12 line required the Tool Forge:
     * hammer, lumberaxe, shortbow, crossbow, arrow.
     */
    public static final List<ToolDefinition> ALL_ADVANCED = List.of(PICKAXE, SHOVEL, HATCHET, MATTOCK, BROADSWORD, SCYTHE, HAMMER, EXCAVATOR, LUMBER_AXE, SHORTBOW, CROSSBOW, ARROW);
}
