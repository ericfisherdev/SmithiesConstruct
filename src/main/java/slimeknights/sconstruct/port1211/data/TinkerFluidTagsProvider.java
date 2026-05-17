package slimeknights.sconstruct.port1211.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.TinkerTags;
import slimeknights.sconstruct.port1211.smeltery.MoltenFluidSet;
import slimeknights.sconstruct.port1211.smeltery.SmelteryFluids;
import slimeknights.sconstruct.port1211.world.SlimeFluidSet;
import slimeknights.sconstruct.port1211.world.WorldFluids;

/**
 * Fluid-tag data provider. Phase 1 shipped this as a stub; Phase 3 fills it with the
 * {@code c:slime} parent tag — a common-namespace umbrella over the four slime fluids
 * (source + flowing forms) so cross-mod recipes targeting "any slime liquid" find every
 * variant under one tag.
 *
 * <p>Phase 2+ smeltery work will append molten-metal fluid tags ({@code #molten_metals},
 * {@code #castable_in_smeltery}, ...) here without disturbing the slime entries.
 */
public final class TinkerFluidTagsProvider extends FluidTagsProvider {

    /** Common-namespace prefix for cross-mod tag interop. */
    private static final String COMMON_NAMESPACE = "c";

    /** Parent fluid tag for every coloured slime variant, exposed under the {@code c:} common namespace. */
    private static final TagKey<Fluid> COMMON_SLIME = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(COMMON_NAMESPACE, "slime"));

    /** Parent fluid tag collecting every molten-metal fluid, under the {@code c:} common namespace. */
    private static final TagKey<Fluid> COMMON_MOLTEN_METAL = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(COMMON_NAMESPACE, "molten_metal"));

    public TinkerFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // c:slime collects every slime fluid (source + flowing) so addon mods or recipes
        // targeting "any slime fluid" pick all four colours with one tag. Drift here would
        // partially break that interop contract — pin every variant explicitly.
        for (SlimeFluidSet fluid : WorldFluids.ALL) {
            tag(COMMON_SLIME).add(fluid.source().get(), fluid.flowing().get());
        }

        // SMTCON-129: c:molten_metal collects every molten-metal fluid (source + flowing) so
        // cross-mod recipes targeting "any molten metal" resolve all 20. Each molten metal is
        // also a valid smeltery fuel alongside vanilla lava — TinkerTags.Fluids.SMELTERY_FUEL
        // is what the controller's fuel logic reads.
        for (MoltenFluidSet molten : SmelteryFluids.ALL) {
            tag(COMMON_MOLTEN_METAL).add(molten.source().get(), molten.flowing().get());
            tag(TinkerTags.Fluids.SMELTERY_FUEL).add(molten.source().get(), molten.flowing().get());
        }
        tag(TinkerTags.Fluids.SMELTERY_FUEL).add(Fluids.LAVA, Fluids.FLOWING_LAVA);
    }
}
