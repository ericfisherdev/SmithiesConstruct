package slimeknights.sconstruct.tools.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import slimeknights.sconstruct.tools.entity.ShurikenEntity;
import slimeknights.sconstruct.tools.entity.ToolEntities;

/**
 * Thrown shuriken item — right-click spawns a {@link ShurikenEntity}, consuming one from the
 * stack (unless the player is in creative mode). Stack size matches the legacy 1.12 throwable
 * shuriken (4 per stack). The projectile's pickup behaviour returns the shuriken to the
 * player's inventory on contact, so a careful thrower can recover thrown stock.
 */
public class ShurikenItem extends Item {

    /** Legacy 1.12 shuriken stack size — pinned so material datapacks can't widen it accidentally. */
    public static final int STACK_SIZE = 4;

    /** Launch velocity in blocks/tick at full speed — matches the legacy thrown shuriken. */
    public static final float LAUNCH_VELOCITY = 1.5F;

    public ShurikenItem(Item.Properties properties) {
        super(properties.stacksTo(STACK_SIZE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            ShurikenEntity shuriken = new ShurikenEntity(ToolEntities.SHURIKEN.get(), player, level, stack.copyWithCount(1));
            shuriken.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, LAUNCH_VELOCITY, 1.0F);
            if (!level.addFreshEntity(shuriken)) {
                // Spawn refused (level capped, dimension teleport in flight) — surface a sided
                // fail so vanilla's use-handling doesn't decrement the stack or fire the stat.
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
}
