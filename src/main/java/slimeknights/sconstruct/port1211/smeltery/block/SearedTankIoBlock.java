package slimeknights.sconstruct.port1211.smeltery.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Seared tank IO block (SMTCON-112) — the larger smeltery storage tank. Eventually this is a
 * 4000mb fluid container that both accepts and dispenses molten metal through its exposed
 * {@code IFluidHandler}, letting the player buffer the smeltery's output. The tank capacity and
 * the fluid-handler capability wiring land in SMTCON-116; at this registration layer the block
 * is a plain horizontally-directional {@link SmelteryComponentBlock} carrying the shared
 * controller-proxy block entity.
 */
public class SearedTankIoBlock extends SmelteryComponentBlock {

    /** Save-format codec — {@link BaseEntityBlock} requires one; the properties-only form suffices. */
    public static final MapCodec<SearedTankIoBlock> CODEC = simpleCodec(SearedTankIoBlock::new);

    public SearedTankIoBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<?> beType() {
        return SmelteryComponents.TANK_IO_BE.get();
    }
}
