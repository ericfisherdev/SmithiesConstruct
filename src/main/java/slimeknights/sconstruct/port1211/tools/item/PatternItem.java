package slimeknights.sconstruct.port1211.tools.item;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.tools.PartType;

/**
 * Single {@link Item} class backing both the blank pattern and the typed (stencil-table-output)
 * pattern variants (SMTCON-91). The two forms differ only by the presence of the
 * {@link TinkerDataComponents#TINKER_PATTERN_PART} data component on the {@link ItemStack}: an
 * absent component means "blank" (template for cycling); a present component carrying a
 * {@link PartType} means "typed for that part" (the stencil table's output, consumable by the
 * upcoming part builder).
 *
 * <p>Using one {@link Item} for both variants keeps the registry size constant (one item,
 * one model entry, one lang root) and lets the stencil table pivot a stack between forms by
 * mutating its component map rather than swapping the {@link ItemStack} for a different
 * {@link Item} instance. Same shape as {@link MaterialItem} for the per-stack
 * {@link TinkerDataComponents#PART_MATERIAL} component.
 *
 * <p>The rendered name pivots on the component: a typed pattern renders as
 * {@code "Pattern: <part>"} via the {@code item.sconstruct.pattern} lang key with the part's
 * display name substituted; a blank pattern falls back to the vanilla
 * {@code Item#getName(ItemStack)} which resolves the registered translation key (the
 * {@code item.sconstruct.blank_pattern} entry shipped by {@link TinkerLanguageProvider}).
 */
public class PatternItem extends Item {

    /** Translation key for the typed-pattern rendered name. Value is {@code "Pattern: %s"}. */
    private static final String TYPED_NAME_KEY = "item." + SConstruct.MOD_ID + ".pattern";

    public PatternItem(Properties properties) {
        super(properties);
    }

    /**
     * Build a typed-variant {@link ItemStack} of size 1 carrying the supplied {@link PartType}
     * as the {@link TinkerDataComponents#TINKER_PATTERN_PART} component.
     *
     * <p>Stencil-table output slot uses this on every "cycle" / "pull" transition to materialise
     * the typed pattern from the blank-pattern input. Splitting the stack-construction shape
     * into a single helper keeps the BE call sites obvious and avoids duplicating the
     * {@code Optional.of(part)} component wiring.
     */
    public static ItemStack makeTyped(Item baseItem, PartType part) {
        Objects.requireNonNull(part, "part");
        ItemStack stack = new ItemStack(baseItem);
        stack.set(TinkerDataComponents.TINKER_PATTERN_PART.get(), Optional.of(part));
        return stack;
    }

    /**
     * Read the typed {@link PartType} from the stack's component map. Returns
     * {@link Optional#empty()} when the component is absent (the blank variant) or when the
     * component is present-but-empty (defensive — a malformed component would otherwise NPE
     * downstream).
     */
    public static Optional<PartType> getPart(ItemStack stack) {
        Optional<PartType> component = stack.get(TinkerDataComponents.TINKER_PATTERN_PART.get());
        return component == null ? Optional.empty() : component;
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<PartType> typed = getPart(stack);
        if (typed.isPresent()) {
            Component partLabel = Component.translatable(typed.get().translationKey());
            return Component.translatable(TYPED_NAME_KEY, partLabel);
        }
        // Component absent — fall through to vanilla, which resolves the item's registered
        // descriptionId (the blank pattern's "item.sconstruct.blank_pattern" lang key).
        return super.getName(stack);
    }
}
