package slimeknights.sconstruct.world;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import slimeknights.sconstruct.common.pulse.Pulse;
import slimeknights.sconstruct.shared.SharedTabs;
import slimeknights.sconstruct.world.block.SlimePlantSet;
import slimeknights.sconstruct.world.client.WorldClientFluidTypes;
import slimeknights.sconstruct.world.client.WorldEntityRenderers;

/**
 * Phase-3 world subsystem. Owns the slime-island content stack — Phase-3 first pass registers
 * the four slime fluids via {@link WorldFluids}; later tasks add slime entities, slime dirt /
 * leaves / saplings, and the slime-island worldgen feature.
 *
 * <p>The pulse is gated by the {@code world} flag in the COMMON config (default-enabled);
 * disabling it skips every {@link WorldFluids} registration, leaving the mod with no slime-fluid
 * surfaces. Operators who disable the pulse on a server must also disable it on every connecting
 * client or face a missing-registry-entry desync — STARTUP config flags are not network-synced
 * (see {@code Config} javadoc).
 *
 * <p>{@link #register} touches {@link WorldFluids} and {@link WorldBlocks} so their static
 * field initialisers run before the registry events fire, subscribes the creative-tab content
 * listener so the four slime fluid buckets and four coloured slime blocks show up in the shared
 * {@link SharedTabs#GENERAL} tab, and — on a physical client — wires the
 * {@link WorldClientFluidTypes} registration for per-fluid tints and fog colours.
 */
public final class TinkerWorldPulse implements Pulse {

    @Override
    public String id() {
        return "world";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void register(IEventBus modBus) {
        // Touch the content classes so their field initialisers run and TinkerRegistries.{FLUID_TYPES,
        // FLUIDS, BLOCKS, ITEMS, ENTITY_TYPES, STRUCTURE_TYPES, STRUCTURE_PIECE_TYPES} see every
        // entry before their registry events fire.
        WorldFluids.init();
        WorldBlocks.init();
        WorldEntities.init();
        WorldEntities.register(modBus);
        WorldStructures.init();

        modBus.addListener(TinkerWorldPulse::populateCreativeTab);

        // Client-side fluid rendering metadata (textures, tints, fog) must subscribe before the
        // common-setup phase, so it goes in register() guarded by dist. Routing through a static
        // factory means the WorldClientFluidTypes class never loads on a dedicated server — the
        // JVM doesn't resolve types inside an unreached branch.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            WorldClientFluidTypes.register(modBus);
            WorldEntityRenderers.register(modBus);
        }
    }

    @SubscribeEvent
    private static void populateCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // GENERAL is the catch-all and receives every world-pulse surface. The slime-fluid
        // buckets are also a materials-type surface, so they additionally route into the themed
        // MATERIALS tab; world blocks and plants stay GENERAL-only.
        if (SharedTabs.GENERAL.getKey().equals(event.getTabKey())) {
            WorldFluids.acceptBuckets(event::accept);
            WorldBlocks.acceptBlockItems(event::accept);
            WorldBlocks.acceptPlantItems(event::accept);
        }
        else if (SharedTabs.MATERIALS.getKey().equals(event.getTabKey())) {
            WorldFluids.acceptBuckets(event::accept);
        }
    }

    @Override
    public void clientSetup(FMLClientSetupEvent event) {
        // Leaves and saplings ship with transparent pixels in their sprites; the default SOLID
        // render layer would render those pixels as black. Cutout is the right layer for
        // alpha-tested foliage (matches vanilla oak/birch leaves). Done inside enqueueWork
        // because ItemBlockRenderTypes is not thread-safe — FMLClientSetupEvent fires on the
        // mod-loading thread, which isn't the render thread.
        event.enqueueWork(TinkerWorldPulse::registerPlantRenderTypes);
    }

    private static void registerPlantRenderTypes() {
        for (SlimePlantSet set : WorldBlocks.PLANT_SETS.values()) {
            ItemBlockRenderTypes.setRenderLayer(set.leaves().get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(set.sapling().get(), RenderType.cutout());
        }
    }
}
