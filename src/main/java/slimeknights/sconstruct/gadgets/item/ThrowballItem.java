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
import slimeknights.sconstruct.gadgets.entity.ThrowballEntity;
import slimeknights.sconstruct.world.block.SlimeColor;

/**
 * Thrown throwball item — right-click spawns a {@link ThrowballEntity} that applies a
 * colour-specific area effect on impact (see {@link ThrowballEntity}). One throwball item is
 * registered per {@link SlimeColor}.
 *
 * <p>Implements {@link ProjectileItem} so a dispenser can fire the throwball: the dispenser
 * support is wired by {@code GadgetDispenserBehaviors} via
 * {@code DispenserBlock.registerProjectileBehavior}.
 */
public class ThrowballItem extends Item implements ProjectileItem {

    /** Throwballs stack like snowballs / eggs. */
    public static final int STACK_SIZE = 16;

    /** Launch velocity in blocks/tick for a hand-thrown throwball — matches the vanilla snowball. */
    private static final float LAUNCH_VELOCITY = 1.5F;

    private final SlimeColor color;

    /**
     * @param properties item properties; the caller supplies the shared stack-size budget.
     * @param color      the slime colour this throwball carries — selects the on-impact effect.
     */
    public ThrowballItem(Item.Properties properties, SlimeColor color) {
        super(properties);
        this.color = color;
    }

    /** The slime colour this throwball carries — drives the {@link ThrowballEntity} impact effect. */
    public SlimeColor color() {
        return color;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            ThrowballEntity throwball = new ThrowballEntity(GadgetEntities.THROWBALL.get(), player, level, stack.copyWithCount(1));
            throwball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, LAUNCH_VELOCITY, 1.0F);
            if (!level.addFreshEntity(throwball)) {
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
        return new ThrowballEntity(GadgetEntities.THROWBALL.get(), level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
    }
}
