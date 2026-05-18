package slimeknights.sconstruct.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.tools.ToolDefinition;

/**
 * Crossbow-shaped ranged weapon bound to {@link ToolDefinition#CROSSBOW}. The minimal port
 * here reuses the {@link BowToolCore} draw / release surface — the player nocks an arrow on
 * right-click and releases to fire — with a longer draw and a flatter, faster shot. Vanilla
 * crossbow loading semantics (charged-state, multi-shot) are deferred; the SMTCON crossbow
 * fires straight on release, like a heavy bow.
 */
public class CrossbowItem extends BowToolCore {

    /** Full-draw tick count — 1.5x vanilla bow draw to match the heavier wind-up. */
    public static final int DRAW_TICKS = BowToolCore.VANILLA_DRAW_TICKS * 3 / 2;

    /** Full-draw arrow velocity — 1.25x vanilla bow ceiling for a flatter, faster shot. */
    public static final float FULL_DRAW_VELOCITY = BowToolCore.VANILLA_FULL_DRAW_VELOCITY * 1.25F;

    public CrossbowItem(Item.Properties properties) {
        super(properties, ToolDefinition.CROSSBOW, DRAW_TICKS, FULL_DRAW_VELOCITY);
    }
}
