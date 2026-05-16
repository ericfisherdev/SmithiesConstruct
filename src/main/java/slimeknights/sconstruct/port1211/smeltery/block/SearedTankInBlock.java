package slimeknights.sconstruct.port1211.smeltery.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Seared tank in block (SMTCON-118) — the smaller smeltery tank, a
 * {@value slimeknights.sconstruct.port1211.smeltery.block.entity.SearedTankBE#CAPACITY_IN} mB
 * fluid container. As a {@link SearedTankBlock} it carries a real
 * {@link slimeknights.sconstruct.port1211.smeltery.block.entity.SearedTankBE} with its own
 * storage, accepts bucket interactions, and spills its contents when broken.
 */
public class SearedTankInBlock extends SearedTankBlock {

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
