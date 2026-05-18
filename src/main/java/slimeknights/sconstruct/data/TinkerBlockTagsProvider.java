package slimeknights.sconstruct.data;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.TinkerTags;
import slimeknights.sconstruct.gadgets.GadgetBlocks;
import slimeknights.sconstruct.shared.Metal;
import slimeknights.sconstruct.shared.SharedBlocks;
import slimeknights.sconstruct.shared.SharedMetals;
import slimeknights.sconstruct.smeltery.CastingBlocks;
import slimeknights.sconstruct.smeltery.SearedBlocks;
import slimeknights.sconstruct.smeltery.SmelteryComponents;
import slimeknights.sconstruct.world.WorldBlocks;
import slimeknights.sconstruct.world.block.SlimePlantSet;

/**
 * Block-tag data provider. Writes the {@code minecraft:mineable/*} and {@code minecraft:needs_*_tool}
 * tag JSONs for every sconstruct block so vanilla's tool-tier system enforces the right pickaxe
 * (iron for most metals, diamond for cobalt/ardite/manyullyn) and the wood decoratives respond
 * to axes.
 *
 * <p>The mapping is driven entirely by {@link SharedMetals#ALL}'s {@code needsDiamond} flag —
 * no per-metal table is duplicated here. A new metal lights up the right tags by appending to
 * the driver and {@link SharedBlocks#METAL_BLOCKS}.
 *
 * <p>{@code BlockTagsProvider#tag} returns the same builder on repeat calls, so tagging a block
 * under both {@code MINEABLE_WITH_PICKAXE} and {@code NEEDS_IRON_TOOL} appends one entry to each
 * tag JSON — the per-tag JSON file lists every block tagged into it.
 */
public final class TinkerBlockTagsProvider extends BlockTagsProvider {

    public TinkerBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        Map<String, Metal> metalsById = indexMetalsById();
        for (Map.Entry<String, DeferredBlock<Block>> entry : SharedBlocks.METAL_BLOCKS.entrySet()) {
            Block block = entry.getValue().get();
            Metal metal = metalsById.get(entry.getKey());
            if (metal == null) {
                // SharedBlocks#metalBlock validates against SharedMetals.ALL at registration
                // time, so today this is unreachable. Pin it anyway so a future refactor that
                // weakens that guard fails datagen with a precise message instead of NPEing.
                throw new IllegalStateException("Block key '" + entry.getKey() + "' in SharedBlocks.METAL_BLOCKS has no matching Metal in SharedMetals.ALL");
            }
            // Every metal block is pickaxe-mined; the difference is what *tier* of pickaxe.
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            // Iron-tier for real-world metals; diamond-tier for the Nether trio (cobalt, ardite,
            // manyullyn). Membership is mutually exclusive — vanilla treats NEEDS_DIAMOND_TOOL
            // as strictly stricter than NEEDS_IRON_TOOL, so a block tagged into both would still
            // require diamond, but tagging both pollutes the tag JSON without changing behaviour.
            tag(metal.needsDiamond() ? BlockTags.NEEDS_DIAMOND_TOOL : BlockTags.NEEDS_IRON_TOOL).add(block);
        }
        // Decoratives: glow + the two firewood variants are wood-like and respond to axes. No
        // tier requirement — any axe (or hand) can mine them, matching the legacy hardness
        // settings on BlockGlow and BlockFirewood.
        tag(BlockTags.MINEABLE_WITH_AXE).add(SharedBlocks.GLOW.get(), SharedBlocks.FIREWOOD.get(), SharedBlocks.LAVAWOOD.get());

        // Phase-3 plant blocks: dirt and grass take shovel, leaves take hoe, saplings are
        // hand-mined; leaves also feed BlockTags.LEAVES (so vanilla compost / fire-spread /
        // decay machinery picks them up) and saplings feed BlockTags.SAPLINGS (so vanilla
        // bonemeal target detection treats them like oak/birch saplings).
        for (SlimePlantSet set : WorldBlocks.PLANT_SETS.values()) {
            tag(BlockTags.MINEABLE_WITH_SHOVEL).add(set.dirt().get(), set.grass().get());
            tag(BlockTags.MINEABLE_WITH_HOE).add(set.leaves().get());
            tag(BlockTags.LEAVES).add(set.leaves().get());
            tag(BlockTags.SAPLINGS).add(set.sapling().get());
        }

        // Phase-3 slime logs (normal + stripped): axe-mineable, feeding BlockTags.LOGS so
        // vanilla "any log" recipes (planks crafting, etc. in a future task) pick them up,
        // and the sconstruct:slimelogs parent tag for any "any sconstruct slime log" recipe
        // / interaction.
        for (DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> holder : WorldBlocks.ALL_LOGS) {
            net.minecraft.world.level.block.RotatedPillarBlock log = holder.get();
            tag(BlockTags.MINEABLE_WITH_AXE).add(log);
            tag(BlockTags.LOGS).add(log);
            tag(TinkerTags.Blocks.SLIMELOGS).add(log);
        }

        // Phase-3 slime grass parent tag — sconstruct:slimegrass collects every coloured
        // grass block so "any slime grass" lookups (spread rules, future bonemeal hooks)
        // can target one tag instead of enumerating per-colour fields.
        for (SlimePlantSet set : WorldBlocks.PLANT_SETS.values()) {
            tag(TinkerTags.Blocks.SLIMEGRASS).add(set.grass().get());
        }

        // SMTCON-129: smeltery structural tags. The six functional component blocks go in
        // SMELTERY_COMPONENT; every plain seared block is valid as both smeltery floor and
        // wall, so SMELTERY_FLOOR and SMELTERY_WALL each collect the whole seared roster. Each
        // smeltery block is also pickaxe-mineable, matching its stone-tier hardness.
        for (DeferredBlock<? extends Block> holder : SmelteryComponents.ALL) {
            tag(TinkerTags.Blocks.SMELTERY_COMPONENT).add(holder.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(holder.get());
        }
        for (DeferredBlock<? extends Block> holder : SearedBlocks.ALL) {
            tag(TinkerTags.Blocks.SMELTERY_FLOOR).add(holder.get());
            tag(TinkerTags.Blocks.SMELTERY_WALL).add(holder.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(holder.get());
        }
        for (DeferredBlock<? extends Block> holder : CastingBlocks.ALL) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(holder.get());
        }

        // SMTCON-143: Phase-6 gadget block tags.
        //   - stone ladder MUST be in BlockTags.CLIMBABLE — that membership (not the block class)
        //     is what makes a ladder climbable; it is also pickaxe-mined, matching its stone tier.
        //   - drying rack is a wooden block, axe-mined.
        //   - wooden hopper, dried clay, and dried clay brick are stone/pickaxe-mined.
        tag(BlockTags.CLIMBABLE).add(GadgetBlocks.STONE_LADDER.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GadgetBlocks.STONE_LADDER.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(GadgetBlocks.DRYING_RACK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GadgetBlocks.WOODEN_HOPPER.get(), GadgetBlocks.DRIED_CLAY.get(), GadgetBlocks.DRIED_CLAY_BRICK.get());
    }

    private static Map<String, Metal> indexMetalsById() {
        java.util.Map<String, Metal> map = new java.util.HashMap<>();
        for (Metal metal : SharedMetals.ALL) {
            map.put(metal.id(), metal);
        }
        return Map.copyOf(map);
    }
}
