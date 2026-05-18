package slimeknights.sconstruct.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.tools.ToolDefinition;

/**
 * Vanilla-equivalent sword tinker tool. Concrete {@link ToolCore} subclass bound to the
 * {@link ToolDefinition#BROADSWORD} 3-slot recipe (handle + sword-blade + wide-guard).
 *
 * <p>Sword-specific {@link net.neoforged.neoforge.common.ItemAbility} handling (sword-dig,
 * sword-sweep) is driven entirely by the {@link ToolDefinition#abilities} set
 * ({@link net.neoforged.neoforge.common.ItemAbilities#DEFAULT_SWORD_ACTIONS}) — the base
 * {@link ToolCore#canPerformAction} override reads from there, so no per-class override is
 * needed. See {@link PickaxeItem} for the rationale behind one subclass per tool slot.
 */
public class SwordItem extends ToolCore {

    public SwordItem(Item.Properties properties) {
        super(properties, ToolDefinition.BROADSWORD);
    }
}
