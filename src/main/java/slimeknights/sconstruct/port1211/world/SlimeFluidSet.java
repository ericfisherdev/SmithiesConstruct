package slimeknights.sconstruct.port1211.world;

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
 * Five-part registration bundle for a single coloured slime fluid: the {@link FluidType}, the
 * static {@link BaseFlowingFluid.Source} / streaming {@link BaseFlowingFluid.Flowing} pair, the
 * in-world {@link LiquidBlock}, and the filled {@link BucketItem}. Mirrors the four-part pattern
 * established by the Phase-2 blood fluid in {@code SharedFluids}; the bucket is bundled here
 * (rather than registered separately in a shared items class) because the world pulse owns the
 * full lifecycle of every slime fluid surface.
 *
 * <p>All five components are stored as {@link DeferredHolder}s so a {@code SlimeFluidSet} can be
 * constructed before any of the underlying registries have fired — the holders only resolve
 * during the registry event chain. Downstream callers (the creative-tab listener, the lang
 * provider, the client extensions) iterate the holders and call {@code .get()} after the events
 * complete.
 */
public record SlimeFluidSet(DeferredHolder<FluidType, FluidType> type, DeferredHolder<Fluid, BaseFlowingFluid.Source> source, DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
        DeferredBlock<LiquidBlock> block, DeferredItem<BucketItem> bucket) {

    public SlimeFluidSet {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(flowing, "flowing");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(bucket, "bucket");
    }
}
