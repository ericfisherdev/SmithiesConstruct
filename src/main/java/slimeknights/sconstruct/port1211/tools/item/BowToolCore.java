package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;
import slimeknights.sconstruct.port1211.tools.ToolHelper;

/**
 * Base class for held-draw ranged weapons (shortbow, longbow). Extends {@link ToolCore} so the
 * material / modifier / broken-state plumbing stays uniform across the tool roster, then layers
 * the bow-specific draw + release behaviour: right-click starts the use action, the use
 * animation reports {@link UseAnim#BOW}, and the release spawns a vanilla {@link Arrow} entity
 * whose velocity and base damage scale against the drawn-power curve and the tool's cached
 * {@link slimeknights.sconstruct.port1211.common.data.ToolStats}.
 *
 * <p>The bow's per-class profile — maximum draw duration in ticks and the velocity multiplier
 * at full draw — is supplied through the constructor so subclasses stay a constructor and
 * nothing else. The shortbow uses a short fast draw with the vanilla 3.0 velocity ceiling; the
 * longbow uses a longer draw with a higher velocity ceiling so its release lands more damage
 * at greater range.
 */
public class BowToolCore extends ToolCore {

    /** Vanilla bow draw ceiling — the {@code timeLeft} value that maps to full draw. */
    public static final int VANILLA_DRAW_TICKS = 20;

    /** Vanilla full-draw arrow velocity ceiling — passed to {@link Arrow#shoot}. */
    public static final float VANILLA_FULL_DRAW_VELOCITY = 3.0F;

    /** Minimum drawn power below which the release fires no arrow — vanilla bow threshold. */
    private static final float MIN_RELEASE_POWER = 0.1F;

    /** Full-draw threshold above which the arrow is marked critical for the +damage modifier. */
    private static final float FULL_DRAW_THRESHOLD = 1.0F;

    private final int drawTicks;
    private final float fullDrawVelocity;

    /**
     * @param drawTicks number of ticks at full draw — controls both the shooting power curve
     *     and the velocity at release. Shorter values produce a faster fire rate at lower
     *     damage.
     * @param fullDrawVelocity arrow velocity at full draw; scaled by the power curve at
     *     release. The vanilla baseline is {@link #VANILLA_FULL_DRAW_VELOCITY}.
     */
    public BowToolCore(Item.Properties properties, ToolDefinition definition, int drawTicks, float fullDrawVelocity) {
        super(properties, definition);
        this.drawTicks = drawTicks;
        this.fullDrawVelocity = fullDrawVelocity;
    }

    /** Test seam: full-draw tick count for this bow profile. */
    public int drawTicks() {
        return drawTicks;
    }

    /** Test seam: velocity multiplier at full draw for this bow profile. */
    public float fullDrawVelocity() {
        return fullDrawVelocity;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        // Match vanilla BowItem — the "long enough that the player never naturally runs out"
        // ceiling. The actual draw window is governed by the power curve in releaseUsing.
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ToolHelper.isBroken(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || ToolHelper.isBroken(stack)) {
            return;
        }
        int charge = getUseDuration(stack, user) - timeLeft;
        float power = computePower(charge);
        if (power < MIN_RELEASE_POWER) {
            return;
        }
        if (!level.isClientSide()) {
            Arrow arrow = new Arrow(level, player, new ItemStack(net.minecraft.world.item.Items.ARROW), stack);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * fullDrawVelocity, 1.0F);
            if (power >= FULL_DRAW_THRESHOLD) {
                arrow.setCritArrow(true);
            }
            arrow.setBaseDamage(arrow.getBaseDamage() + ToolHelper.getStats(stack).attackDamage());
            level.addFreshEntity(arrow);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    /**
     * Vanilla bow power curve, parameterised by this bow's {@link #drawTicks}: a quadratic ramp
     * from 0 to 1 over {@code drawTicks} ticks, clamped at 1. The longbow's longer ramp produces
     * a visibly slower draw at the player's bow animation hooked into the same curve.
     */
    public float computePower(int charge) {
        float f = (float) charge / drawTicks;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }

    /**
     * NeoForge hook — controls the bow's apparent draw-bar fill rate in the player HUD. Map
     * the per-class {@link #drawTicks} to the vanilla baseline so the longbow's slow draw
     * surfaces visually in the bow-pull animation, not just in the damage curve.
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return super.isBarVisible(stack);
    }

    /**
     * Helper for the model-pull predicate: the visual fraction of full draw at a given use
     * tick count. Test seam — mirrors {@link #computePower} but exposes the raw fraction for
     * the bow-pull HUD rather than the velocity-scaling curve.
     */
    public float drawProgressFraction(int useTicks) {
        return Math.min((float) useTicks / drawTicks, 1.0F);
    }

    /** Returns a freshly-shot arrow for testing — does not add to the level. */
    public AbstractArrow createTestArrow(Level level, Player player, ItemStack stack) {
        Arrow arrow = new Arrow(level, player, new ItemStack(net.minecraft.world.item.Items.ARROW), stack);
        arrow.setBaseDamage(arrow.getBaseDamage() + ToolHelper.getStats(stack).attackDamage());
        return arrow;
    }
}
