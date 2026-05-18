package slimeknights.sconstruct.smeltery.block;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.MapCodec;

import slimeknights.sconstruct.smeltery.SmelteryComponents;

/**
 * Seared chute block (SMTCON-112) — the smeltery's item input. Eventually this accepts items
 * (ores, ingots) dropped or hopper-fed into it and forwards them into the controller's melting
 * inventory, letting the smeltery be automated. The item-input capability and the forwarding
 * logic land in SMTCON-118; at this registration layer the block is a plain horizontally-
 * directional {@link SmelteryComponentBlock} carrying the shared controller-proxy block entity.
 */
public class SearedChuteBlock extends SmelteryComponentBlock {

    /** Save-format codec — {@link BaseEntityBlock} requires one; the properties-only form suffices. */
    public static final MapCodec<SearedChuteBlock> CODEC = simpleCodec(SearedChuteBlock::new);

    public SearedChuteBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<?> beType() {
        return SmelteryComponents.CHUTE_BE.get();
    }
}
