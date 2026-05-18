package slimeknights.sconstruct.smeltery.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.smeltery.SmelteryComponents;

/**
 * Seared drain block (SMTCON-112) — the smeltery's output spout. Eventually this exposes an
 * {@code IFluidHandler} on its facing side so the player can pull molten metal out of the
 * assembled smeltery (into a casting basin, table, or external tank). The fluid-handler
 * capability and the spout pour behaviour land in SMTCON-118; at this registration layer the
 * block is a plain horizontally-directional {@link SmelteryComponentBlock} carrying the shared
 * controller-proxy block entity.
 */
public class SearedDrainBlock extends SmelteryComponentBlock {

    /** Save-format codec — {@link BaseEntityBlock} requires one; the properties-only form suffices. */
    public static final MapCodec<SearedDrainBlock> CODEC = simpleCodec(SearedDrainBlock::new);

    public SearedDrainBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<?> beType() {
        return SmelteryComponents.DRAIN_BE.get();
    }
}
