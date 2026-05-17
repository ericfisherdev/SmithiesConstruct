package slimeknights.sconstruct.port1211.gadgets.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import slimeknights.sconstruct.port1211.world.block.SlimeColor;

/**
 * The slimesling — a charge-and-release launcher that flings the holder along their look
 * vector. Right-click starts the charge; the longer it is held (up to {@link #MAX_CHARGE_TICKS})
 * the harder the release launches the player. One slimesling item is registered per
 * {@link SlimeColor}, each with its own launch profile and on-release side effect:
 *
 * <ul>
 *   <li>{@link SlimeColor#BLUE} — a plain bounce launch, the baseline profile.</li>
 *   <li>{@link SlimeColor#PURPLE} — a long-range launch: the same charge throws the player
 *       noticeably farther.</li>
 *   <li>{@link SlimeColor#MAGMA} — the baseline launch, and sets the player on fire for
 *       {@link #MAGMA_FIRE_SECONDS} seconds.</li>
 *   <li>{@link SlimeColor#BLOOD} — the baseline launch, and heals the player
 *       {@link #BLOOD_HEAL_HEALTH} health on release.</li>
 * </ul>
 *
 * <p>The charge duration is read straight from vanilla's use-item system — {@code releaseUsing}
 * receives the ticks left, so {@code chargeTicks = getUseDuration - timeLeft}. No per-stack
 * data component is needed to track the charge; this mirrors how {@code BowToolCore} derives
 * draw power. The launch resets the player's fall distance so a charged hop does not deal fall
 * damage on landing, matching the cushioning a slime sling implies.
 */
public class SlimeSlingItem extends Item {

    /** Charge ticks that map to a full-power launch; holding longer does not launch harder. */
    private static final int MAX_CHARGE_TICKS = 20;

    /** Use-duration ceiling — large enough that the player never naturally finishes the charge. */
    private static final int USE_DURATION = 72000;

    /** Minimum charge fraction below which the release launches nothing and costs no durability. */
    private static final float MIN_LAUNCH_FRACTION = 0.1F;

    /** Full-charge launch speed for the baseline (blue / magma / blood) profile, in blocks/tick. */
    private static final double BASE_LAUNCH_POWER = 1.6D;

    /** Full-charge launch speed for the long-range purple profile, in blocks/tick. */
    private static final double LONG_RANGE_LAUNCH_POWER = 2.4D;

    /** Seconds the magma slimesling sets the player on fire on release. */
    private static final float MAGMA_FIRE_SECONDS = 3.0F;

    /** Health the blood slimesling restores on release — one heart. */
    private static final float BLOOD_HEAL_HEALTH = 2.0F;

    private final SlimeColor color;

    /**
     * @param properties item properties; callers supply the shared durability + max-stack-size
     *     budget so every colour wears the same.
     * @param color      the slime colour this slimesling launches with — selects the launch
     *     power profile and the on-release side effect.
     */
    public SlimeSlingItem(Item.Properties properties, SlimeColor color) {
        super(properties);
        this.color = color;
    }

    /** The slime colour this slimesling launches with. */
    public SlimeColor color() {
        return color;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // BOW pulls the item toward the body — a readable "winding up the sling" charge pose.
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        float fraction = chargeFraction(timeLeft);
        if (fraction < MIN_LAUNCH_FRACTION) {
            // A flick too short to charge launches nothing and costs no durability.
            return;
        }
        launch(player, fraction);
        if (!level.isClientSide()) {
            // Server-side playSound broadcasts to every tracking client, including this player —
            // calling it client-side too would double the sound for the launcher.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
            applyColorEffect(player);
            stack.hurtAndBreak(1, player, slotFor(player.getUsedItemHand()));
        }
    }

    /** The launch power ceiling for this colour — purple throws farther, the rest share a baseline. */
    private double launchPower() {
        return color == SlimeColor.PURPLE ? LONG_RANGE_LAUNCH_POWER : BASE_LAUNCH_POWER;
    }

    /** Charge fraction in {@code [0, 1]} from the ticks the player held the sling. */
    private float chargeFraction(int timeLeft) {
        int charge = Math.min(USE_DURATION - timeLeft, MAX_CHARGE_TICKS);
        return charge / (float) MAX_CHARGE_TICKS;
    }

    /**
     * Flings {@code player} along their look vector at {@code fraction} of this colour's launch
     * power. {@code hurtMarked} forces the new velocity to sync to the client, and the fall
     * distance is reset so the launch itself never converts into fall damage on landing.
     */
    private void launch(Player player, float fraction) {
        Vec3 launch = player.getLookAngle().scale(fraction * launchPower());
        player.setDeltaMovement(launch);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    /** Applies this colour's on-release side effect — fire for magma, healing for blood. */
    private void applyColorEffect(Player player) {
        switch (color) {
        case MAGMA -> player.igniteForSeconds(MAGMA_FIRE_SECONDS);
        case BLOOD -> player.heal(BLOOD_HEAL_HEALTH);
        default -> {
            // BLUE and PURPLE launch with no extra side effect.
        }
        }
    }

    /** The {@link EquipmentSlot} backing {@code hand}, for charging durability to the right slot. */
    private static EquipmentSlot slotFor(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }
}
