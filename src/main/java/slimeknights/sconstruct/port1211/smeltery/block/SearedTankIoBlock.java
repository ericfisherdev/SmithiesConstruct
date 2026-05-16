package slimeknights.sconstruct.port1211.smeltery.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Seared tank IO block (SMTCON-118) — the larger smeltery storage tank, a
 * {@value slimeknights.sconstruct.port1211.smeltery.block.entity.SearedTankBE#CAPACITY_IO} mB
 * fluid container that both accepts and dispenses molten metal through its exposed
 * {@code IFluidHandler}, letting the player buffer the smeltery's output. As a
 * {@link SearedTankBlock} it carries a real {@link slimeknights.sconstruct.port1211.smeltery.block.entity.SearedTankBE}
 * with its own storage, accepts bucket interactions, and spills its contents when broken.
 */
public class SearedTankIoBlock extends SearedTankBlock {

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
