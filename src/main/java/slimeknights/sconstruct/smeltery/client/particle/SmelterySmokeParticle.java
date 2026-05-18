package slimeknights.sconstruct.smeltery.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only render of the {@code sconstruct:smeltery_smoke} particle (SMTCON-167) — a slow grey
 * puff that drifts up off a hot, assembled smeltery controller. The particle rises gently with a
 * slight horizontal wobble, grows a touch over its life, and fades to transparent before it dies
 * (~40-60 ticks). Spawned server-side by {@code SmelteryControllerBlockEntity#serverTick} via
 * {@code ServerLevel#sendParticles}, so this class never resolves on a dedicated server.
 */
@OnlyIn(Dist.CLIENT)
public final class SmelterySmokeParticle extends TextureSheetParticle {

    /** Smallest random lifetime in ticks. */
    private static final int MIN_LIFETIME = 40;

    /** Lifetime spread in ticks added on top of {@link #MIN_LIFETIME}. */
    private static final int LIFETIME_SPREAD = 20;

    private SmelterySmokeParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        // Damp out any inbound velocity so the puff rises under its own slow drift, not the
        // spread the spawner passed in.
        this.friction = 0.92F;
        this.xd = xSpeed * 0.1D + (this.random.nextDouble() - 0.5D) * 0.01D;
        this.yd = 0.02D + this.random.nextDouble() * 0.015D;
        this.zd = zSpeed * 0.1D + (this.random.nextDouble() - 0.5D) * 0.01D;
        this.lifetime = MIN_LIFETIME + this.random.nextInt(LIFETIME_SPREAD);
        this.quadSize *= 0.6F + this.random.nextFloat() * 0.4F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        // Cool grey, slightly varied so a cluster of puffs does not look uniform.
        float grey = 0.32F + this.random.nextFloat() * 0.12F;
        this.setColor(grey, grey, grey);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        // Slight horizontal wobble and a gentle grow as the puff climbs.
        this.xd += (this.random.nextDouble() - 0.5D) * 0.004D;
        this.zd += (this.random.nextDouble() - 0.5D) * 0.004D;
        this.quadSize += 0.003F;
        // Fade out over the final third of the lifetime.
        float fadeStart = this.lifetime * 0.66F;
        if (this.age > fadeStart) {
            this.setAlpha(1.0F - (this.age - fadeStart) / (this.lifetime - fadeStart));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Translucent so the per-tick alpha fade-out renders correctly rather than clipping.
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Sprite-set {@link ParticleProvider} bound by {@code SmelteryParticles}. */
    @OnlyIn(Dist.CLIENT)
    public static final class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            SmelterySmokeParticle particle = new SmelterySmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
