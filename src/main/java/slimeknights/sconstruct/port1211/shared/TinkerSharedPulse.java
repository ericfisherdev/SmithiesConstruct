package slimeknights.sconstruct.port1211.shared;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

import slimeknights.sconstruct.port1211.common.SmithiesSounds;
import slimeknights.sconstruct.port1211.common.pulse.Pulse;
import slimeknights.sconstruct.port1211.shared.client.SharedClientFluidTypes;

/**
 * Phase-2 shared subsystem. Owns the cross-pulse blocks, items, fluids, and creative tab —
 * every later pulse pulls content references from {@link SharedBlocks}, {@link SharedItems},
 * {@link SharedFluids}, and {@link SharedTabs}, so the shared pulse must boot before any of
 * them or their initialiser chains see an unpopulated registry.
 *
 * <p>The pulse is gated by the {@code shared} flag in the COMMON config (default-enabled);
 * disabling it skips every Phase-2 registration, leaving the mod with only Phase-1
 * infrastructure (registries, network, config, datagen).
 *
 * <p>{@link #register} touches each content class so its static {@link
 * net.neoforged.neoforge.registries.DeferredRegister} entries are populated before the
 * registry events fire, subscribes the creative-tab listener, and — on a physical client —
 * wires the {@link SharedClientFluidTypes} registration. Render-layer setup for the
 * {@link SharedBlocks#GLOW} cutout sprite lives in {@link #clientSetup} because
 * {@link ItemBlockRenderTypes} is a client-only API.
 */
public final class TinkerSharedPulse implements Pulse {

    @Override
    public String id() {
        return "shared";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void register(IEventBus modBus) {
        // Order matters: SharedFluids must load before SharedItems so SharedItems.BUCKET_BLOOD's
        // initialiser can resolve SharedFluids.BLOOD; SharedTabs must load after the content
        // classes so its tab population listener has every field available.
        SharedBlocks.init();
        SharedFluids.init();
        SharedItems.init();
        SharedTabs.init();
        SharedTabs.registerCreativeTabContents(modBus);

        // SMTCON-166: custom sound events. Touched here so the SOUND_EVENTS DeferredRegister
        // sees every entry before the registry freezes during mod construction.
        SmithiesSounds.init();

        // Client-side fluid rendering metadata (textures, tint, fog) must subscribe before the
        // common-setup phase, so it goes in register() guarded by dist. Routing through a
        // static factory means the SharedClientFluidTypes class never loads on a dedicated
        // server — the JVM doesn't resolve types inside an unreached branch.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SharedClientFluidTypes.register(modBus);
        }
    }

    @Override
    public void clientSetup(FMLClientSetupEvent event) {
        // Cutout render layer for glow. The block ships a partially-transparent sprite (the
        // legacy mod used TRANSLUCENT, but cutout matches the modern grass-block convention
        // and avoids the depth-sorting cost). Firewood and lavawood are opaque wood blocks
        // and stay on the default SOLID layer.
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(SharedBlocks.GLOW.get(), RenderType.cutout()));
    }
}
