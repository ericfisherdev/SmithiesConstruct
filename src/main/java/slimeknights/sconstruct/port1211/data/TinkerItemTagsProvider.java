package slimeknights.sconstruct.port1211.data;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.TinkerTags;
import slimeknights.sconstruct.port1211.shared.Metal;
import slimeknights.sconstruct.port1211.shared.SharedBlocks;
import slimeknights.sconstruct.port1211.shared.SharedItems;
import slimeknights.sconstruct.port1211.shared.SharedMetals;
import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.item.ToolCore;
import slimeknights.sconstruct.port1211.tools.item.ToolItems;
import slimeknights.sconstruct.port1211.tools.item.ToolParts;

/**
 * Item-tag data provider. Writes per-metal {@code c:ingots/<id>}, {@code c:nuggets/<id>}, and
 * {@code c:storage_blocks/<id>} entries for real-world metals so other mods using the
 * {@code c:} common-tag namespace can interoperate; fictional metals route to
 * {@code sconstruct:ingots/<id>} etc. to avoid polluting common tags with names the rest of
 * the ecosystem can't meaningfully consume (per plan/12 open question on ore-dict/common tags).
 *
 * <p>Additionally builds the slimeball tag tree:
 * <ul>
 *   <li>{@link TinkerTags.Items#SLIMEBALLS} ({@code sconstruct:slimeballs}) — parent tag.</li>
 *   <li>One child tag per coloured variant ({@code sconstruct:slimeballs/blue},
 *       {@code .../purple}, {@code .../blood}, {@code .../magma}). The parent tag references
 *       the children via {@code addTag}, so any item the children include is also visible to
 *       a recipe input targeting the parent — vanilla slimeball recipes that allow "any
 *       slimeball" only have to depend on {@code sconstruct:slimeballs}.</li>
 * </ul>
 *
 * <p>The realWorld/fictional split is driven entirely by {@link Metal#realWorld()}; no
 * per-metal table is duplicated here. {@code storage_blocks} is only emitted for metals that
 * have a block registered (lead + nickel are skipped per {@link SharedBlocks#skippedIds()}).
 */
public final class TinkerItemTagsProvider extends ItemTagsProvider {

    private static final String COMMON_NAMESPACE = "c";

    /**
     * Tool ids whose item belongs in NeoForge's {@code c:tools/mining_tool} tag — the harvest
     * tools. Keyed by registration path so the {@code addTags} loop can classify each entry of
     * {@link ToolItems#ALL_TOOLS} without a per-tool {@code instanceof} chain.
     */
    private static final Set<String> MINING_TOOL_IDS = Set.of("pickaxe", "shovel", "axe", "hammer", "excavator", "lumberaxe", "mattock");

    /** Tool ids that route to {@code c:tools/melee_weapon} — the close-combat tools. */
    private static final Set<String> MELEE_TOOL_IDS = Set.of("sword", "cleaver", "longsword", "rapier", "froe", "scythe");

    /** Tool ids that route to {@code c:tools/ranged_weapon} — the three bows. */
    private static final Set<String> RANGED_TOOL_IDS = Set.of("shortbow", "longbow", "crossbow");

    /** Tool id excluded from the broad {@code c:tools} tag — the arrow is ammunition, not a tool. */
    private static final String ARROW_ID = "tinker_arrow";

    /** Ranged-tool id that routes to {@code c:tools/crossbow}; the other ranged tools are bows. */
    private static final String CROSSBOW_ID = "crossbow";

    public TinkerItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
            ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Fail fast if the three driver lists fall out of step — silent mis-indexing here would
        // generate JSONs that point ingot tags at the wrong metal's item, which the snapshot
        // test would catch but only for the specific anchors it pins. An IllegalStateException
        // at the top of the loop turns "subtle mis-tag" into "datagen failure with a precise
        // message".
        if (SharedItems.INGOTS.size() != SharedMetals.ALL.size() || SharedItems.NUGGETS.size() != SharedMetals.ALL.size()) {
            throw new IllegalStateException(
                    "Shared metals/items table mismatch: metals=" + SharedMetals.ALL.size() + ", ingots=" + SharedItems.INGOTS.size() + ", nuggets=" + SharedItems.NUGGETS.size());
        }
        for (int i = 0; i < SharedMetals.ALL.size(); i++) {
            Metal metal = SharedMetals.ALL.get(i);
            String namespace = metal.realWorld() ? COMMON_NAMESPACE : SConstruct.MOD_ID;

            Item ingot = SharedItems.INGOTS.get(i).get();
            Item nugget = SharedItems.NUGGETS.get(i).get();
            tag(itemTag(namespace, "ingots/" + metal.id())).add(ingot);
            tag(itemTag(namespace, "nuggets/" + metal.id())).add(nugget);

            // Only metals with a registered storage block get the storage_blocks/<id> tag —
            // tagging lead/nickel under storage_blocks/lead would name a target item that
            // doesn't exist.
            DeferredBlock<Block> blockHolder = SharedBlocks.METAL_BLOCKS.get(metal.id());
            if (blockHolder != null) {
                tag(itemTag(namespace, "storage_blocks/" + metal.id())).add(blockHolder.get().asItem());
            }
        }

        // Copy the block-side slime tags (sconstruct:slimegrass, sconstruct:slimelogs) to
        // the item registry so "any sconstruct slime log" / "any slime grass" recipes don't
        // have to maintain a parallel item list — copy() asks vanilla to re-emit the same
        // entry set under the matching item tag.
        copy(TinkerTags.Blocks.SLIMEGRASS, itemTag(SConstruct.MOD_ID, "slimegrass"));
        copy(TinkerTags.Blocks.SLIMELOGS, itemTag(SConstruct.MOD_ID, "slimelogs"));

        // Slimeballs: parent + 4 colour children. Parent references children via addTag so a
        // recipe input asking for "any sconstruct slimeball" only depends on the parent.
        TagKey<Item> blue = itemTag(SConstruct.MOD_ID, "slimeballs/blue");
        TagKey<Item> purple = itemTag(SConstruct.MOD_ID, "slimeballs/purple");
        TagKey<Item> blood = itemTag(SConstruct.MOD_ID, "slimeballs/blood");
        TagKey<Item> magma = itemTag(SConstruct.MOD_ID, "slimeballs/magma");
        tag(blue).add(SharedItems.SLIMEBALL_BLUE.get());
        tag(purple).add(SharedItems.SLIMEBALL_PURPLE.get());
        tag(blood).add(SharedItems.SLIMEBALL_BLOOD.get());
        tag(magma).add(SharedItems.SLIMEBALL_MAGMA.get());
        tag(TinkerTags.Items.SLIMEBALLS).addTag(blue).addTag(purple).addTag(blood).addTag(magma);

        // SMTCON-107: route every tool into the NeoForge c:tools category tags so other mods'
        // "any tool" / "any bow" recipes and predicates pick up the Smithies tools. Classify by
        // registration path against the explicit category sets above — driving it off the id
        // rather than a ToolCore subclass instanceof keeps the categorisation auditable in one
        // place and survives a tool class hierarchy refactor.
        for (net.neoforged.neoforge.registries.DeferredItem<? extends ToolCore> holder : ToolItems.ALL_TOOLS) {
            String id = holder.getId().getPath();
            Item item = holder.get();
            // The arrow is consumed ammunition, not a wielded tool — keep it out of c:tools.
            if (!ARROW_ID.equals(id)) {
                tag(Tags.Items.TOOLS).add(item);
            }
            if (MINING_TOOL_IDS.contains(id)) {
                tag(Tags.Items.MINING_TOOL_TOOLS).add(item);
            }
            if (MELEE_TOOL_IDS.contains(id)) {
                tag(Tags.Items.MELEE_WEAPON_TOOLS).add(item);
            }
            if (RANGED_TOOL_IDS.contains(id)) {
                tag(Tags.Items.RANGED_WEAPON_TOOLS).add(item);
                // The vanilla-shaped bow tags are sub-categories of ranged_weapon: shortbow /
                // longbow behave as bows, the crossbow as a crossbow. Predicates that ask
                // specifically for "a bow" (e.g. piglin aggro) consult these.
                if (CROSSBOW_ID.equals(id)) {
                    tag(Tags.Items.TOOLS_CROSSBOW).add(item);
                }
                else {
                    tag(Tags.Items.TOOLS_BOW).add(item);
                }
            }
        }

        // SMTCON-107: per-part-type item tag (sconstruct:tool_parts/<id>). Iterate
        // PartType.values() so a new part type lights up its own tag with no edit here; the
        // matching MaterialItem comes from ToolParts.get(part) which itself throws if the part
        // registration is missing.
        for (PartType part : PartType.values()) {
            tag(itemTag(SConstruct.MOD_ID, "tool_parts/" + part.id())).add(ToolParts.get(part).get());
        }
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
