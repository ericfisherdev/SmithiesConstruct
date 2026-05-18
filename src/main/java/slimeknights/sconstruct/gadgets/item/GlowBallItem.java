package slimeknights.sconstruct.gadgets.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

import slimeknights.sconstruct.gadgets.entity.GadgetEntities;
import slimeknights.sconstruct.gadgets.entity.GlowBallEntity;

/**
 * Thrown glow-ball item — right-click to lob a {@link GlowBallEntity} that places a glow block
 * where it lands. Stacks like a snowball, and implements {@link ProjectileItem} so a dispenser
 * can fire it (wired by {@code GadgetDispenserBehaviors}).
 */
public class GlowBallItem extends Item implements ProjectileItem {

    /** Glow balls stack like snowballs / eggs. */
    public static final int STACK_SIZE = 16;

    /** Launch velocity in blocks/tick for a hand-thrown glow ball — matches the vanilla snowball. */
    private static final float LAUNCH_VELOCITY = 1.5F;

    public GlowBallItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            GlowBallEntity glowBall = new GlowBallEntity(GadgetEntities.GLOW_BALL.get(), player, level, stack.copyWithCount(1));
            glowBall.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, LAUNCH_VELOCITY, 1.0F);
            if (!level.addFreshEntity(glowBall)) {
                // Spawn refused — surface a sided fail so vanilla does not decrement the stack.
                return InteractionResultHolder.fail(stack);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new GlowBallEntity(GadgetEntities.GLOW_BALL.get(), level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
    }
}
