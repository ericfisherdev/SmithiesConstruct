package slimeknights.sconstruct.port1211.gadgets.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * A piece of slime armor. A thin {@link ArmorItem} subclass — the slot is carried by
 * {@link ArmorItem.Type}, so one class serves the helmet, chestplate, leggings, and boots.
 *
 * <p>The subclass exists purely as a marker: {@code GadgetEvents} keys its fall-damage
 * reduction (slime boots) and full-set knockback bonus off {@code instanceof SlimeArmorItem}
 * rather than comparing against four individual registered items.
 */
public class SlimeArmorItem extends ArmorItem {

    public SlimeArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties);
    }
}
