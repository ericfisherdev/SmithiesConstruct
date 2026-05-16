package slimeknights.sconstruct.port1211.smeltery.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Seared tank gauge block (SMTCON-112) — the translucent fluid-gauge panel. Eventually this
 * renders the smeltery's contained fluid as a visible level inside a glass-fronted frame, drawn
 * on the cutout / translucent render layer (its registration is given {@code noOcclusion()} so
 * neighbouring faces still draw behind it). The gauge rendering lands in SMTCON-116; at this
 * registration layer the block is a plain horizontally-directional {@link SmelteryComponentBlock}
 * carrying the shared controller-proxy block entity.
 */
public class SearedTankGaugeBlock extends SmelteryComponentBlock {

    /** Save-format codec — {@link BaseEntityBlock} requires one; the properties-only form suffices. */
    public static final MapCodec<SearedTankGaugeBlock> CODEC = simpleCodec(SearedTankGaugeBlock::new);

    public SearedTankGaugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<?> beType() {
        return SmelteryComponents.TANK_GAUGE_BE.get();
    }
}
