package slimeknights.tconstruct.port1211.data;

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
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.tconstruct.port1211.TConstruct;
import slimeknights.tconstruct.port1211.common.TinkerTags;
import slimeknights.tconstruct.port1211.shared.Metal;
import slimeknights.tconstruct.port1211.shared.SharedBlocks;
import slimeknights.tconstruct.port1211.shared.SharedItems;
import slimeknights.tconstruct.port1211.shared.SharedMetals;

/**
 * Item-tag data provider. Writes per-metal {@code c:ingots/<id>}, {@code c:nuggets/<id>}, and
 * {@code c:storage_blocks/<id>} entries for real-world metals so other mods using the
 * {@code c:} common-tag namespace can interoperate; fictional metals route to
 * {@code tconstruct:ingots/<id>} etc. to avoid polluting common tags with names the rest of
 * the ecosystem can't meaningfully consume (per plan/12 open question on ore-dict/common tags).
 *
 * <p>Additionally builds the slimeball tag tree:
 * <ul>
 *   <li>{@link TinkerTags.Items#SLIMEBALLS} ({@code tconstruct:slimeballs}) — parent tag.</li>
 *   <li>One child tag per coloured variant ({@code tconstruct:slimeballs/blue},
 *       {@code .../purple}, {@code .../blood}, {@code .../magma}). The parent tag references
 *       the children via {@code addTag}, so any item the children include is also visible to
 *       a recipe input targeting the parent — vanilla slimeball recipes that allow "any
 *       slimeball" only have to depend on {@code tconstruct:slimeballs}.</li>
 * </ul>
 *
 * <p>The realWorld/fictional split is driven entirely by {@link Metal#realWorld()}; no
 * per-metal table is duplicated here. {@code storage_blocks} is only emitted for metals that
 * have a block registered (lead + nickel are skipped per {@link SharedBlocks#skippedIds()}).
 */
public final class TinkerItemTagsProvider extends ItemTagsProvider {

    private static final String COMMON_NAMESPACE = "c";

    public TinkerItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
            ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, TConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (int i = 0; i < SharedMetals.ALL.size(); i++) {
            Metal metal = SharedMetals.ALL.get(i);
            String namespace = metal.realWorld() ? COMMON_NAMESPACE : TConstruct.MOD_ID;

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

        // Slimeballs: parent + 4 colour children. Parent references children via addTag so a
        // recipe input asking for "any tconstruct slimeball" only depends on the parent.
        TagKey<Item> blue = itemTag(TConstruct.MOD_ID, "slimeballs/blue");
        TagKey<Item> purple = itemTag(TConstruct.MOD_ID, "slimeballs/purple");
        TagKey<Item> blood = itemTag(TConstruct.MOD_ID, "slimeballs/blood");
        TagKey<Item> magma = itemTag(TConstruct.MOD_ID, "slimeballs/magma");
        tag(blue).add(SharedItems.SLIMEBALL_BLUE.get());
        tag(purple).add(SharedItems.SLIMEBALL_PURPLE.get());
        tag(blood).add(SharedItems.SLIMEBALL_BLOOD.get());
        tag(magma).add(SharedItems.SLIMEBALL_MAGMA.get());
        tag(TinkerTags.Items.SLIMEBALLS).addTag(blue).addTag(purple).addTag(blood).addTag(magma);
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
