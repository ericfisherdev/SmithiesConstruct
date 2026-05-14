package slimeknights.sconstruct.port1211.world.entity;

import javax.annotation.Nullable;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Boss-sized slime variant. Spawns as a rare island-encounter creature at size 4 — four times
 * vanilla's "large" slime size — and survives with {@code MAX_HEALTH 64.0} so a player can't
 * one-shot it through the bouncy melee phase. {@link #finalizeSpawn} forces the default size
 * to {@link #DEFAULT_SIZE} when the entity is spawned without a pre-set size override (e.g.
 * via {@code /summon} or worldgen); a slime split from a larger huge-slime keeps the size that
 * its parent's {@code dealtSize / 2} math produces, matching vanilla split mechanics.
 *
 * <p>Like {@link EntityBlueslime}, vanilla's size-driven attribute scaling multiplies
 * {@code MAX_HEALTH} by {@code size * size}, so a freshly summoned huge slime at size 4
 * resolves to {@code 64.0 * 4 * 4 = 1024} hp <em>if</em> the registered base were 64. To pin
 * the AC's literal "MAX_HEALTH 64" reading instead, the value here is the absolute spawn
 * health and the {@code setSize} call inside {@link #finalizeSpawn} passes
 * {@code updateHealth = false} so the scaler doesn't pile a square on top.
 */
public class EntityHugeSlime extends Slime {

    /** Default size for a freshly spawned huge slime; matches the AC's "size=4 default". */
    public static final int DEFAULT_SIZE = 4;

    public EntityHugeSlime(EntityType<? extends EntityHugeSlime> type, Level level) {
        super(type, level);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        // Force size 4 for a freshly summoned huge slime. setSize with updateHealth=false skips
        // the size-squared MAX_HEALTH multiplier so the registered 64.0 baseline survives —
        // vanilla would otherwise scale to 1024 hp at size 4.
        this.setSize(DEFAULT_SIZE, false);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    /**
     * Attribute baseline for the huge slime entity type. {@code MAX_HEALTH = 64.0} matches the
     * SMTCON-56 AC; the registered base is the absolute spawn health (see class Javadoc for the
     * {@code updateHealth=false} interaction).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 64.0D).add(Attributes.ATTACK_DAMAGE);
    }
}
