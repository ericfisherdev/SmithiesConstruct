package slimeknights.sconstruct.common.data;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

/**
 * Cached, fully-resolved combat / mining / durability stats for a built tool. Third of the five
 * tool data components (SMTCON-20).
 *
 * <p>The stat values are derived from the tool's {@link ToolMaterials} and {@link ToolModifiers}
 * but are heavy enough to compute that we cache the result on the ItemStack's
 * {@code DataComponentMap}. Anything that needs per-tick access (tooltip rendering, attribute
 * modifier resolution, damage-calculation event handlers) reads the cached snapshot instead of
 * recomputing.
 *
 * <p>Persistent via {@link #CODEC} (a {@link RecordCodecBuilder} for the nine flat fields) and
 * network-synchronised via {@link #STREAM_CODEC} (a hand-rolled {@link StreamCodec} — vanilla's
 * {@code StreamCodec.composite} arity tops out at six fields, so a sequential encode/decode is
 * cleaner than nesting composites for nine).
 *
 * <p>{@link #zero()} returns a singleton of an all-zero snapshot — the safe sentinel for "stats
 * not yet computed" so callers never see {@code null}.
 */
public record ToolStats(int maxDurability, float attackDamage, float attackSpeed, float miningSpeed, int harvestLevel, int freeModifiers, float drawSpeed, float bowRange, float projectileBonus) {

    /**
     * Compact constructor — guards against non-finite floats and negative counts. Every creation
     * path (direct {@code new}, decoded from {@link #CODEC}, decoded from {@link #STREAM_CODEC})
     * hits this validation, so a malformed save file or a hostile network payload cannot inject
     * a {@code NaN} damage value that would poison every arithmetic / comparison downstream.
     *
     * <p>The float fields all model magnitudes — damage, speed, range, etc. — where NaN and
     * infinities have no physical meaning and would corrupt cached stat math. The two int count
     * fields ({@code maxDurability}, {@code freeModifiers}) similarly cannot meaningfully be
     * negative; {@code harvestLevel} is left unconstrained because it doubles as a modded
     * "mining tier" identifier that some addons negate as a sentinel.
     */
    public ToolStats {
        if (!Float.isFinite(attackDamage) || !Float.isFinite(attackSpeed) || !Float.isFinite(miningSpeed) || !Float.isFinite(drawSpeed) || !Float.isFinite(bowRange)
                || !Float.isFinite(projectileBonus)) {
            throw new IllegalArgumentException("ToolStats float fields must be finite; got attackDamage=" + attackDamage + ", attackSpeed=" + attackSpeed + ", miningSpeed=" + miningSpeed
                    + ", drawSpeed=" + drawSpeed + ", bowRange=" + bowRange + ", projectileBonus=" + projectileBonus);
        }
        if (maxDurability < 0 || freeModifiers < 0) {
            throw new IllegalArgumentException("ToolStats count fields must be non-negative; got maxDurability=" + maxDurability + ", freeModifiers=" + freeModifiers);
        }
    }

    /** Persistence codec. Used by {@code DataComponentType.Builder#persistent}. */
    public static final Codec<ToolStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.INT.fieldOf("max_durability").forGetter(ToolStats::maxDurability),
            Codec.FLOAT.fieldOf("attack_damage").forGetter(ToolStats::attackDamage), Codec.FLOAT.fieldOf("attack_speed").forGetter(ToolStats::attackSpeed),
            Codec.FLOAT.fieldOf("mining_speed").forGetter(ToolStats::miningSpeed), Codec.INT.fieldOf("harvest_level").forGetter(ToolStats::harvestLevel),
            Codec.INT.fieldOf("free_modifiers").forGetter(ToolStats::freeModifiers), Codec.FLOAT.fieldOf("draw_speed").forGetter(ToolStats::drawSpeed),
            Codec.FLOAT.fieldOf("bow_range").forGetter(ToolStats::bowRange), Codec.FLOAT.fieldOf("projectile_bonus").forGetter(ToolStats::projectileBonus)).apply(instance, ToolStats::new));

    /**
     * Network codec. Written sequentially because {@code StreamCodec.composite} arity caps at six
     * fields in vanilla 1.21.1; nesting composites for nine fields produces unreadable nested-Pair
     * builders, so an explicit anonymous implementation is the lesser evil here.
     */
    public static final StreamCodec<ByteBuf, ToolStats> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ToolStats decode(ByteBuf buf) {
            return new ToolStats(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, ToolStats stats) {
            ByteBufCodecs.VAR_INT.encode(buf, stats.maxDurability());
            ByteBufCodecs.FLOAT.encode(buf, stats.attackDamage());
            ByteBufCodecs.FLOAT.encode(buf, stats.attackSpeed());
            ByteBufCodecs.FLOAT.encode(buf, stats.miningSpeed());
            ByteBufCodecs.VAR_INT.encode(buf, stats.harvestLevel());
            ByteBufCodecs.VAR_INT.encode(buf, stats.freeModifiers());
            ByteBufCodecs.FLOAT.encode(buf, stats.drawSpeed());
            ByteBufCodecs.FLOAT.encode(buf, stats.bowRange());
            ByteBufCodecs.FLOAT.encode(buf, stats.projectileBonus());
        }
    };

    private static final ToolStats ZERO = new ToolStats(0, 0f, 0f, 0f, 0, 0, 0f, 0f, 0f);

    /**
     * Canonical zero-valued snapshot — the safe sentinel for "stats not yet computed". Returns
     * the same instance every call so callers that need a placeholder never allocate.
     */
    public static ToolStats zero() {
        return ZERO;
    }
}
