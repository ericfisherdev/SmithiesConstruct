package slimeknights.sconstruct.common;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registration hub for the mod's custom {@link ParticleType}s (SMTCON-167). Each type is a
 * {@link SimpleParticleType} registered against {@link TinkerRegistries#PARTICLE_TYPES} at the
 * resource path matching its name, so {@code sconstruct:smeltery_smoke} and
 * {@code sconstruct:melting_bubble} resolve the particle-definition JSONs under
 * {@code assets/sconstruct/particles/} and the textures under
 * {@code assets/sconstruct/textures/particle/}.
 *
 * <p>The {@link DeferredHolder} fields are {@code public static final} so call sites — chiefly the
 * smeltery block entities spawning the particles via {@code ServerLevel#sendParticles} — address a
 * particle by its typed handle. {@link #ALL} is the iteration surface for tests and future datagen.
 *
 * <p>This hub is common code: {@link SimpleParticleType} and {@link ParticleType} are common
 * classes, so the holder loads safely on a dedicated server. The particle <em>rendering</em>
 * classes are client-only and live under {@code smeltery.client.particle}.
 */
public final class SmithiesParticles {

    /** Slow grey smoke puff rising from a hot, assembled smeltery controller. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMELTERY_SMOKE = register("smeltery_smoke");

    /** Small short-lived bubble rising off molten metal cooling in a casting table or basin. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MELTING_BUBBLE = register("melting_bubble");

    /** Insertion-ordered view of every registered particle type — the iteration surface. */
    public static final List<DeferredHolder<ParticleType<?>, SimpleParticleType>> ALL = List.of(SMELTERY_SMOKE, MELTING_BUBBLE);

    private SmithiesParticles() {
    }

    /** Forces class load so the static field initialisers register every particle type. */
    public static void init() {
        Objects.requireNonNull(ALL);
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        // false: the particle obeys the client's particle-count limiter rather than overriding it,
        // so a busy smeltery cannot flood the screen past the player's particle setting.
        return TinkerRegistries.PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }
}
