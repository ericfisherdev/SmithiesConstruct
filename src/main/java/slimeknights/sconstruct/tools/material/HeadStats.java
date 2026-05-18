package slimeknights.sconstruct.tools.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Stats for any tool head — pickaxe head, axe head, sword blade, hammer head, broadaxe head,
 * broadblade. The baseline {@code (durability, harvestLevel, miningSpeed, attackDamage)} feeds
 * into the {@code StatsBuilder} (SMTCON-75) when a tool is rebuilt; multiple heads on a single
 * tool (broadaxe, broadblade) average / max according to per-tool definition rules.
 */
public record HeadStats(int durability, int harvestLevel, float miningSpeed, float attackDamage) implements MaterialStats {

    /** Map-codec form used by the {@link MaterialStatsCodecs} dispatch — head-specific fields
     *  live alongside the {@code type} discriminator inside the same JSON object. */
    public static final MapCodec<HeadStats> MAP_CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance
                    .group(Codec.INT.fieldOf("durability").forGetter(HeadStats::durability), Codec.INT.fieldOf("harvest_level").forGetter(HeadStats::harvestLevel),
                            Codec.FLOAT.fieldOf("mining_speed").forGetter(HeadStats::miningSpeed), Codec.FLOAT.fieldOf("attack_damage").forGetter(HeadStats::attackDamage))
                    .apply(instance, HeadStats::new));

    @Override
    public Type type() {
        return Type.HEAD;
    }
}
