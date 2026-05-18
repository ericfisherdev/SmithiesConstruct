package slimeknights.sconstruct.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.tools.ToolDefinition;

/**
 * Heavy 4-part bow tuned for higher damage and range at the cost of a slower draw. The draw
 * window is twice the vanilla bow's so the player sees a visibly slower bow-pull animation;
 * the full-draw velocity is bumped to {@link #FULL_DRAW_VELOCITY} so the arrow travels farther
 * and hits harder than a shortbow shot.
 */
public class LongbowItem extends BowToolCore {

    /** Full-draw tick count — 2x the vanilla bow so the longbow draw is visibly slower. */
    public static final int DRAW_TICKS = BowToolCore.VANILLA_DRAW_TICKS * 2;

    /** Full-draw arrow velocity — 1.5x the vanilla bow ceiling for greater range and damage. */
    public static final float FULL_DRAW_VELOCITY = BowToolCore.VANILLA_FULL_DRAW_VELOCITY * 1.5F;

    public LongbowItem(Item.Properties properties) {
        super(properties, ToolDefinition.LONGBOW, DRAW_TICKS, FULL_DRAW_VELOCITY);
    }
}
