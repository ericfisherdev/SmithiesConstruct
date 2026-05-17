package slimeknights.sconstruct.port1211.gadgets;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.common.TinkerRegistries;
import slimeknights.sconstruct.port1211.shared.SharedItems;

/**
 * Registration hub for the Phase-6 gadget {@link ArmorMaterial}s. SMTCON-136 registers one:
 * {@link #SLIME}, the material backing the four slime armor pieces.
 *
 * <p>Slime armor is light: modest per-slot defense, no toughness, and no innate knockback
 * resistance — the full-set knockback bonus is applied dynamically by {@code GadgetEvents}
 * rather than baked into the material. It repairs at a vanilla anvil with a blue slimeball.
 */
public final class GadgetArmorMaterials {

    /** Enchantability — between leather (15) and iron (9); slime takes enchantments readily. */
    private static final int ENCHANTMENT_VALUE = 12;

    /** Per-slot armor-point defense for slime armor — a light set, a little above leather. */
    private static final Map<ArmorItem.Type, Integer> DEFENSE = new EnumMap<>(Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 3, ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1));

    /** The slime armor material — light defense, repaired with a blue slimeball at an anvil. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SLIME = TinkerRegistries.ARMOR_MATERIALS.register("slime",
            () -> new ArmorMaterial(DEFENSE, ENCHANTMENT_VALUE, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(SharedItems.SLIMEBALL_BLUE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "slime"))), 0.0F, 0.0F));

    private GadgetArmorMaterials() {
    }

    /** Forces class load so the static field initialiser registers the slime armor material. */
    public static void init() {
        // Touching a holder forces the static initialiser chain — same pattern as GadgetItems#init.
        SLIME.getId();
    }
}
