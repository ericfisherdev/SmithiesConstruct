package slimeknights.sconstruct.port1211.smeltery.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Seared tank in block (SMTCON-112) — the smaller, input-only smeltery tank. Eventually this is
 * a 2000mb fluid container that only accepts molten metal (it does not dispense), used to feed
 * fluid into the smeltery. The capacity and the input-only fluid-handler capability land in
 * SMTCON-116; at this registration layer the block is a plain horizontally-directional
 * {@link SmelteryComponentBlock} carrying the shared controller-proxy block entity.
 */
public class SearedTankInBlock extends SmelteryComponentBlock {

    /** Save-format codec — {@link BaseEntityBlock} requires one; the properties-only form suffices. */
    public static final MapCodec<SearedTankInBlock> CODEC = simpleCodec(SearedTankInBlock::new);

    public SearedTankInBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<?> beType() {
        return SmelteryComponents.TANK_IN_BE.get();
    }
}
