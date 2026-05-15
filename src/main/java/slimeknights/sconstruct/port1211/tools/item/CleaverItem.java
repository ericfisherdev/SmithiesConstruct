package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Heavy 4-part melee weapon bound to {@link ToolDefinition#CLEAVER}. Damage is governed by the
 * underlying ToolStats; the per-swing behaviour layered here is the legacy 1.12 "behead" chance
 * — every kill against a humanoid mob has a small chance to drop the corresponding mob head at
 * the death position. The base chance is 5% (matching the legacy {@code 1/20} roll); modifiers
 * that boost beheading will scale this in a future ticket via the modifier hook.
 */
public class CleaverItem extends ToolCore {

    /** Base chance to drop a head on a successful kill — legacy 1.12 {@code 1/20} baseline. */
    public static final float BASE_BEHEAD_CHANCE = 0.05F;

    public CleaverItem(Item.Properties properties) {
        super(properties, ToolDefinition.CLEAVER);
    }

    @Override
    @SuppressWarnings("PMD.CloseResource") // ServerLevel pattern-match — Level is not AutoCloseable, PMD false-positive.
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean handled = super.hurtEnemy(stack, target, attacker);
        if (handled && target.isDeadOrDying() && target.level() instanceof ServerLevel level && attacker instanceof Player) {
            tryDropHead(level, target);
        }
        return handled;
    }

    /**
     * Roll the behead chance once per kill and, on success, pop the corresponding vanilla mob
     * head at the target's position. Only the three vanilla humanoid mobs that ship with a
     * recognised head item participate — zombie, skeleton, and wither skeleton — since vanilla
     * doesn't ship a generic "Player Head" with a dropped-by-player profile attached and the
     * other recognised heads (creeper, dragon) drop from their own death events.
     */
    private static void tryDropHead(ServerLevel level, LivingEntity target) {
        RandomSource random = level.getRandom();
        if (random.nextFloat() >= BASE_BEHEAD_CHANCE) {
            return;
        }
        ResourceLocation headId = headIdFor(target);
        if (headId == null) {
            return;
        }
        BuiltInRegistries.ITEM.getOptional(headId).ifPresent(head -> Block.popResource(level, target.blockPosition(), new ItemStack(head)));
    }

    /**
     * Map a kill victim to the vanilla head item id it should drop. Returns {@code null} for
     * targets without a recognised head — those swings get the regular sword treatment instead
     * of an unintended drop.
     */
    private static ResourceLocation headIdFor(LivingEntity target) {
        if (target instanceof WitherSkeleton) {
            return ResourceLocation.withDefaultNamespace("wither_skeleton_skull");
        }
        if (target instanceof Skeleton) {
            return ResourceLocation.withDefaultNamespace("skeleton_skull");
        }
        if (target instanceof Zombie) {
            return ResourceLocation.withDefaultNamespace("zombie_head");
        }
        return null;
    }
}
