package slimeknights.sconstruct.port1211.tools.item;

import net.minecraft.world.item.Item;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;

/**
 * Three-part bow tuned for fast fire rate at lower damage. Reaches full draw at
 * {@link #DRAW_TICKS} ticks (matches the vanilla bow draw) and fires at the vanilla
 * {@link BowToolCore#VANILLA_FULL_DRAW_VELOCITY} velocity ceiling.
 */
public class ShortbowItem extends BowToolCore {

    /** Full-draw tick count — matches vanilla bow for byte-for-byte fire-rate parity. */
    public static final int DRAW_TICKS = BowToolCore.VANILLA_DRAW_TICKS;

    /** Full-draw arrow velocity — matches the vanilla bow ceiling. */
    public static final float FULL_DRAW_VELOCITY = BowToolCore.VANILLA_FULL_DRAW_VELOCITY;

    public ShortbowItem(Item.Properties properties) {
        super(properties, ToolDefinition.SHORTBOW, DRAW_TICKS, FULL_DRAW_VELOCITY);
    }
}
