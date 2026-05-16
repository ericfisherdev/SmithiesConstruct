package slimeknights.sconstruct.port1211.smeltery;

import java.util.Objects;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Five-part registration bundle for a single molten-metal fluid: the {@link FluidType}, the
 * static {@link BaseFlowingFluid.Source} / streaming {@link BaseFlowingFluid.Flowing} pair, the
 * in-world {@link LiquidBlock}, and the filled {@link BucketItem}. The smeltery-side counterpart
 * to the Phase-3 {@link slimeknights.sconstruct.port1211.world.SlimeFluidSet}; held in its own
 * {@code smeltery} package because the smeltery pulse owns the full lifecycle of every molten
 * metal surface.
 *
 * <p>All five components are stored as {@link DeferredHolder}s so a {@code MoltenFluidSet} can
 * be constructed before any of the underlying registries have fired — the holders only resolve
 * during the registry event chain. Downstream callers (the client fluid-type extensions, the
 * lang provider, melting / casting recipe datagen) iterate the holders and call {@code .get()}
 * after the events complete.
 */
public record MoltenFluidSet(DeferredHolder<FluidType, FluidType> type, DeferredHolder<Fluid, BaseFlowingFluid.Source> source, DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
        DeferredBlock<LiquidBlock> block, DeferredItem<BucketItem> bucket) {

    public MoltenFluidSet {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(flowing, "flowing");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(bucket, "bucket");
    }
}
