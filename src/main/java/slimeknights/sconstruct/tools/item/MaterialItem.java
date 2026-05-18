package slimeknights.sconstruct.tools.item;

import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.data.TinkerDataComponents;
import slimeknights.sconstruct.tools.PartType;

/**
 * The vanilla 1.20.5+ {@link Item} subclass that backs every tool-part item the mod ships.
 * Pairs a static {@link PartType} (baked into the item registration so {@code pick_head} and
 * {@code shovel_head} are distinct items even when made of the same material) with a dynamic
 * material identity stored on the {@link ItemStack} via the {@link TinkerDataComponents#PART_MATERIAL}
 * data component.
 *
 * <p>Material identity is per-stack, not per-item, because the part-builder pulse stamps the
 * material onto an existing stack by mutating its {@code DataComponentMap} rather than swapping
 * the ItemStack out for a different {@link Item} instance. That keeps the {@link Item} registry
 * size bounded to {@code PartType.values().length} regardless of how many materials the mod —
 * or any addon — registers downstream.
 *
 * <p>The default material on an unstamped stack is {@code tconstruct:wood}: the legacy 1.12
 * default that anchors every other material's stat baseline. Falling back rather than throwing
 * keeps the item usable in the creative-tab preview slot and in any code path that constructs
 * a part stack without first attaching the component.
 */
public class MaterialItem extends Item {

    /**
     * Default material identity returned by {@link #getMaterial(ItemStack)} when the stack has
     * no {@link TinkerDataComponents#PART_MATERIAL} component attached. The legacy
     * {@code tconstruct:wood} namespace is preserved so cross-mod material references resolved
     * against this default keep working when Smithies' Construct sits alongside any addon that
     * still ships materials under {@code tconstruct:*}.
     */
    public static final ResourceLocation DEFAULT_MATERIAL = ResourceLocation.fromNamespaceAndPath("tconstruct", "wood");

    /**
     * Translation-key suffix used to format {@code "<material> <part>"}. Resolves to the lang
     * value {@code "%s %s"} (English) at the resource pack layer; locales that flip the order
     * (some Asian languages) just edit the value to {@code "%2$s %1$s"} without touching code.
     */
    private static final String NAME_FORMAT_KEY = "item." + SConstruct.MOD_ID + ".material_part.format";

    private final PartType partType;

    public MaterialItem(Properties properties, PartType partType) {
        super(properties);
        this.partType = Objects.requireNonNull(partType, "partType");
    }

    /** Static {@link PartType} this item slot represents — fixed at registration time. */
    public PartType partType() {
        return partType;
    }

    /**
     * Read the per-stack material identity. Returns {@link #DEFAULT_MATERIAL} when the
     * {@link TinkerDataComponents#PART_MATERIAL} component is absent — see the class javadoc
     * for why this falls back rather than throws.
     */
    public ResourceLocation getMaterial(ItemStack stack) {
        ResourceLocation material = stack.get(TinkerDataComponents.PART_MATERIAL.get());
        return material != null ? material : DEFAULT_MATERIAL;
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation material = getMaterial(stack);
        Component materialLabel = Component.translatable("material." + material.getNamespace() + "." + material.getPath());
        Component partLabel = Component.translatable(partType().translationKey());
        return Component.translatable(NAME_FORMAT_KEY, materialLabel, partLabel);
    }
}
