package slimeknights.sconstruct.port1211.smeltery.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import slimeknights.sconstruct.port1211.common.SmithiesParticles;
import slimeknights.sconstruct.port1211.smeltery.client.particle.MeltingBubbleParticle;
import slimeknights.sconstruct.port1211.smeltery.client.particle.SmelterySmokeParticle;

/**
 * Client-only registrar that binds the mod's custom particle types (SMTCON-167) to their
 * sprite-set {@link net.minecraft.client.particle.ParticleProvider}s. Subscribes a single
 * {@link RegisterParticleProvidersEvent} listener which fires once during client setup.
 *
 * <p>Each particle is registered with {@link RegisterParticleProvidersEvent#registerSpriteSet}
 * because both have a matching particle-definition JSON under {@code assets/sconstruct/particles/}
 * that supplies the texture sprite list.
 *
 * <p>{@link #register(IEventBus)} is invoked from {@code TinkerSmelteryPulse#register} under a
 * {@code Dist.CLIENT} guard so this class — and the client-only particle classes it references —
 * never resolves on a dedicated server.
 */
public final class SmelteryParticles {

    private SmelteryParticles() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SmelteryParticles::onRegisterParticleProviders);
    }

    private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SmithiesParticles.SMELTERY_SMOKE.get(), SmelterySmokeParticle.Provider::new);
        event.registerSpriteSet(SmithiesParticles.MELTING_BUBBLE.get(), MeltingBubbleParticle.Provider::new);
    }
}
