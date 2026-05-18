package slimeknights.sconstruct.port1211.smeltery.client.particle;

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
 * Client-only render of the {@code sconstruct:melting_bubble} particle (SMTCON-167) — a small,
 * short-lived bubble that rises a little off molten metal cooling in a casting table or basin and
 * pops (fades out) at the end of its brief life (~12-20 ticks). Spawned server-side by
 * {@code AbstractCastingBlockEntity}'s casting tick via {@code ServerLevel#sendParticles}, so this
 * class never resolves on a dedicated server.
 */
@OnlyIn(Dist.CLIENT)
public final class MeltingBubbleParticle extends TextureSheetParticle {

    /** Smallest random lifetime in ticks. */
    private static final int MIN_LIFETIME = 12;

    /** Lifetime spread in ticks added on top of {@link #MIN_LIFETIME}. */
    private static final int LIFETIME_SPREAD = 8;

    private MeltingBubbleParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.friction = 0.86F;
        this.xd = xSpeed * 0.1D + (this.random.nextDouble() - 0.5D) * 0.008D;
        this.yd = 0.015D + this.random.nextDouble() * 0.01D;
        this.zd = zSpeed * 0.1D + (this.random.nextDouble() - 0.5D) * 0.008D;
        this.lifetime = MIN_LIFETIME + this.random.nextInt(LIFETIME_SPREAD);
        this.quadSize *= 0.3F + this.random.nextFloat() * 0.25F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        // Pop: fade out over the final quarter of the bubble's short life.
        float fadeStart = this.lifetime * 0.75F;
        if (this.age > fadeStart) {
            this.setAlpha(1.0F - (this.age - fadeStart) / (this.lifetime - fadeStart));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Translucent so the per-tick alpha pop-out renders correctly rather than clipping.
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
            MeltingBubbleParticle particle = new MeltingBubbleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
