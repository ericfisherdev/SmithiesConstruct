package slimeknights.sconstruct.port1211.tools.item;

import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.data.ToolBroken;
import slimeknights.sconstruct.port1211.common.data.ToolMaterials;
import slimeknights.sconstruct.port1211.common.data.ToolModifiers;
import slimeknights.sconstruct.port1211.common.data.ToolPersistentData;
import slimeknights.sconstruct.port1211.common.data.ToolStats;
import slimeknights.sconstruct.port1211.tools.ToolDefinition;
import slimeknights.sconstruct.port1211.tools.ToolHelper;
import slimeknights.sconstruct.port1211.tools.modifier.ModifierHookDispatcher;
import slimeknights.sconstruct.port1211.tools.modifier.ToolEvents;

/**
 * Abstract base item for every Smithies' Construct tool. Extends vanilla {@link DiggerItem} so
 * the Tool data component, attribute-modifier dispatch, and damage-tick plumbing stay aligned
 * with the rest of the 1.21 item ecosystem, then layers SMTCON's component-driven stat / broken
 * model on top via four overrides:
 *
 * <ul>
 *   <li>{@link #getDestroySpeed} — returns {@code 0} on a broken tool; otherwise reads the cached
 *       {@link ToolStats#miningSpeed} when the block sits in the tool's mining tag, falling
 *       through to the vanilla {@code 1.0F} for blocks the tier alone doesn't speed up.</li>
 *   <li>{@link #isCorrectToolForDrops} — returns {@code false} on a broken tool; otherwise gates
 *       the vanilla "in mining tag" answer behind a harvest-level check against the
 *       vanilla {@link BlockTags#NEEDS_STONE_TOOL}/{@link BlockTags#NEEDS_IRON_TOOL}/
 *       {@link BlockTags#NEEDS_DIAMOND_TOOL} ladder.</li>
 *   <li>{@link #hurtEnemy} — declines the hit on a broken tool so the attack-cooldown sound /
 *       Knockback chain doesn't run; intact tools delegate to vanilla so their attack-damage
 *       attribute (set by {@link ToolHelper#rebuildStats}) drives the actual damage roll.</li>
 *   <li>{@link #damageItem} — when a durability tick would push {@code damageValue} to
 *       {@code maxDamage}, flips the {@link TinkerDataComponents#TOOL_BROKEN} flag and keeps
 *       {@code damageValue} just below the ceiling so {@link ItemStack#hurtAndBreak} never
 *       reaches its {@link ItemStack#shrink} branch. A broken tinker tool persists in the
 *       inventory so it can be repaired at the smeltery — vanilla's shrink-on-break behaviour
 *       would otherwise erase the per-stack material / modifier state.</li>
 * </ul>
 *
 * <p>Construction pins the five tool data components — {@link ToolMaterials#empty},
 * {@link ToolModifiers#empty}, {@link ToolStats#zero}, {@link ToolPersistentData#empty}, and
 * {@link ToolBroken#intact} — onto the {@link Item.Properties#component} stack so a freshly-built
 * tool reads as a meaningful zero rather than crashing every accessor that branches on a missing
 * component. The {@link #definition} record is held as a public final field so subclasses and the
 * tool-building UI can read part-slot metadata without reflecting through a getter.
 *
 * <p>Subclasses (Pickaxe, Shovel, …) supply their own {@link ToolDefinition} constants and stay
 * stateless beyond that — the per-stack identity lives entirely in the data component map.
 *
 * <p>SMTCON-79 instantiates the first concrete subclasses; SMTCON-77's {@link ToolHelper#rebuildStats}
 * is the recompute hook that swaps the zero defaults out for the real material-derived snapshot.
 */
public class ToolCore extends DiggerItem {

    /**
     * Vanilla {@link DiggerItem} requires a non-null block tag to feed
     * {@link Tiers#createToolProperties}. Tools that don't mine — sword, bow, mattock — pass
     * this never-matching tag so the Tool component's {@code minesAndDrops} rule applies to no
     * block. The tag is intentionally never populated by any data pack (the
     * {@link #isCorrectToolForDrops} override is the authoritative answer for non-mining tools).
     */
    public static final TagKey<Block> EMPTY_MINING_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "empty_mining_tag"));

    /**
     * {@link ToolDefinition} the tool was built against — part slots, mining tag, modifier-slot
     * baseline. Held as a {@code public final} field rather than a getter because subclasses and
     * downstream pulses (tool station UI, datagen, JEI categories) read it on every tick; an
     * accessor would add an unnecessary indirection on a hot path.
     */
    public final ToolDefinition definition;

    /**
     * @param properties vanilla properties stack; SMTCON's default data components are layered
     *     on top before the {@link Item} constructor pins them as the item's defaults.
     * @param definition the tool's part-slot / modifier-slot metadata; the mining tag may be
     *     {@code null} for non-digging tools (sword, bow), in which case
     *     {@link #EMPTY_MINING_TAG} is passed to vanilla.
     */
    public ToolCore(Item.Properties properties, ToolDefinition definition) {
        super(Tiers.WOOD, resolveMiningTag(definition), withDefaultToolComponents(properties));
        this.definition = definition;
    }

    /**
     * Extract the mining tag from a non-null {@link ToolDefinition}. Kept as a static helper so
     * the {@code super(...)} call site never dereferences a null definition — a {@code null}
     * argument raises {@link NullPointerException} here with a {@code "definition"} message
     * before the {@link DiggerItem} constructor runs, rather than a bare implicit NPE on
     * {@code definition.miningTag()}.
     */
    private static TagKey<Block> resolveMiningTag(ToolDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return definition.miningTag() != null ? definition.miningTag() : EMPTY_MINING_TAG;
    }

    /**
     * Layer SMTCON's five tool data components onto the vanilla properties stack so a freshly
     * constructed tool item reads as a meaningful zero rather than null on every accessor.
     */
    private static Item.Properties withDefaultToolComponents(Item.Properties properties) {
        Objects.requireNonNull(properties, "properties");
        return properties.component(TinkerDataComponents.TOOL_MATERIALS.get(), ToolMaterials.empty()).component(TinkerDataComponents.TOOL_MODIFIERS.get(), ToolModifiers.empty())
                .component(TinkerDataComponents.TOOL_STATS.get(), ToolStats.zero()).component(TinkerDataComponents.TOOL_PERSISTENT_DATA.get(), ToolPersistentData.empty())
                .component(TinkerDataComponents.TOOL_BROKEN.get(), ToolBroken.intact());
    }

    /**
     * Broken tools mine at speed zero (the legacy "broken stick" effect); otherwise the cached
     * {@link ToolStats#miningSpeed} replaces the vanilla tier baseline whenever the block is in
     * the tool's mining tag. Vanilla returns {@code 1.0F} for blocks outside the tag — keep that
     * pass-through so a pickaxe mining dirt still gets the {@code 1.0F} fall-through rather than
     * the tool's full mining speed.
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return ToolBehavior.resolveDestroySpeed(stack, super.getDestroySpeed(stack, state));
    }

    /**
     * Broken tools never drop blocks; otherwise the vanilla "tool is correct for block" answer
     * is gated behind the harvest-level ladder. Vanilla's {@link BlockTags} ladder caps at
     * diamond — netherite blocks fall through to the diamond check since vanilla doesn't ship a
     * {@code NEEDS_NETHERITE_TOOL} tag.
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // Read the mining-tag answer directly off the BlockState rather than delegating to
        // super.isCorrectToolForDrops. The {@link Tiers#WOOD} baseline passed to
        // {@link DiggerItem} carries {@link BlockTags#INCORRECT_FOR_WOODEN_TOOL} as forbidden
        // blocks — letting vanilla gate the answer here would lock every higher-tier SMTCON
        // tool out of iron / diamond / netherite blocks before our harvest-level ladder gets
        // to swing. The mining-tag membership is the authoritative "tool is correct for this
        // block" answer; the harvest-level check downstream of it is what governs tiering.
        return ToolBehavior.isCorrectForBlock(stack, state, state.is(resolveMiningTag(definition)));
    }

    /**
     * Broken tools decline the swing so neither the vanilla durability tick nor the attack-speed
     * cooldown sound fires; intact tools delegate to {@link DiggerItem#hurtEnemy} which returns
     * {@code true} and lets the attribute-driven damage roll proceed.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!ToolBehavior.canHurtEnemy(stack) || !super.hurtEnemy(stack, target, attacker)) {
            return false;
        }
        // Dispatch onAttack to every applied modifier in insertion order. The dispatcher
        // skips on the client side and on broken tools — the canHurtEnemy guard above already
        // handles the broken case, but the dispatcher's defence-in-depth covers a stack whose
        // broken flag flipped between the guard and the dispatch (e.g. by a previous modifier
        // hook on the same swing).
        if (attacker instanceof Player player) {
            // baseDamage feeds the OnHitContext for modifiers that scale their side effects
            // (fiery duration, knockback magnitude) against the actual strike. Read off the
            // cached ToolStats.attackDamage — the same value the ATTRIBUTE_MODIFIERS component
            // (set by AttributeBuilder in SMTCON-77's rebuildStats) feeds the player's
            // ATTACK_DAMAGE attribute, so the modifier hooks see the same figure vanilla
            // applied to the actual damage roll.
            float baseDamage = ToolHelper.getStats(stack).attackDamage();
            ModifierHookDispatcher.dispatchOnAttack(stack, new ToolEvents.OnHitContext(player, target, target.level(), baseDamage));
        }
        return true;
    }

    /**
     * Vanilla {@code Item.mineBlock} fires once per successful block break. The {@code super}
     * call drives the damage-per-block durability tick via the Tool data component; afterwards
     * the dispatcher routes {@link slimeknights.sconstruct.port1211.tools.modifier.Modifier#onMine}
     * to every applied modifier. The broken-state / client-side / unknown-id guards live in the
     * dispatcher itself.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean handled = super.mineBlock(stack, level, state, pos, miningEntity);
        if (miningEntity instanceof Player player) {
            ModifierHookDispatcher.dispatchOnMine(stack, new ToolEvents.OnMineContext(player, state, pos, level));
        }
        return handled;
    }

    /**
     * Intercept the durability tick to mark the tool broken without letting vanilla shrink the
     * stack. A tick that would push {@code damageValue} to {@code maxDamage} flips the
     * {@link TinkerDataComponents#TOOL_BROKEN} flag, pins {@code damageValue} at {@code maxDamage - 1}
     * (one tick below the shrink threshold so {@link ItemStack#hurtAndBreak} never reaches its
     * {@link ItemStack#shrink} branch), and returns {@code 0} so vanilla applies no further
     * damage on this tick. The persisting empty-durability tool stays in the inventory and can
     * be repaired at the smeltery — vanilla's shrink-on-break behaviour would otherwise erase
     * the per-stack material / modifier state.
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ToolBehavior.processDurabilityTick(stack, amount);
    }

    /**
     * Resolve {@link net.neoforged.neoforge.common.extensions.IItemExtension#canPerformAction
     * canPerformAction} against the tool's static {@link ToolDefinition#abilities} set. Broken
     * tools decline every ability so vanilla's right-click handlers (axe-strip, shovel-flatten,
     * sword-sweep, hoe-till) short-circuit before the side effect runs.
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility action) {
        return ToolBehavior.canPerformAction(stack, action, definition.abilities());
    }
}
