package slimeknights.sconstruct.port1211.tools.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.common.data.ToolMaterials;
import slimeknights.sconstruct.port1211.tools.material.client.MaterialClientCache;

/**
 * Per-material baked tool model (SMTCON-105). Wraps the JSON-defined base item model for a
 * {@code ToolCore} and, at render time, assembles a quad list whose layers are tinted by the
 * tool's {@link ToolMaterials} data component — so an iron pickaxe renders a different colour
 * than a cobalt one without a separate model file per material combination.
 *
 * <p><strong>Render path.</strong> Vanilla item rendering resolves a stack-specific model
 * through {@link #getOverrides()} before reading its quads. {@link ToolItemOverrides#resolve}
 * reads the stack's {@link ToolMaterials}, keys a cache on the immutable part-material list,
 * and returns a {@link MaterialVariantModel} whose quads are pre-tinted once and reused on
 * every subsequent frame — satisfying the "no per-frame allocations in {@code getQuads}"
 * acceptance criterion. {@link #getQuads} on this wrapper itself returns the untinted base
 * quads: it is the fallback for the no-stack-context call vanilla makes while the override is
 * still being resolved.
 *
 * <p><strong>Tinting.</strong> {@link #tintForMaterials} maps each base quad's
 * {@code tintIndex} to a part slot — layer {@code N} of the base model is part slot {@code N}
 * — and recolours the quad with that part's material colour from {@link MaterialClientCache}.
 * Quads with a tint index outside the part range (overlays, the broken sprite) are passed
 * through untouched.
 *
 * <p><strong>Texture variants.</strong> This task ships per-material <em>tint</em>; per-material
 * <em>texture</em> sprites (the {@code head_cactus} / {@code head_paper} family) need a
 * material render-info field that the {@code sconstruct:material} schema does not yet carry —
 * that, and the tool texture assets themselves, land with the tools datagen ticket (SMTCON-107).
 */
public class ToolBakedModel extends BakedModelWrapper<BakedModel> {

    /**
     * Shared random for the no-context base-quad pull; item models ignore the seed. Thread-safe
     * because item models are read from multiple client render threads concurrently — a plain
     * {@code create()} source throws when touched off-thread.
     */
    private static final RandomSource RANDOM = RandomSource.createThreadSafe();

    private final ToolItemOverrides overrides;

    public ToolBakedModel(BakedModel base) {
        super(base);
        this.overrides = new ToolItemOverrides(base);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    /**
     * Resolve the cached, per-material tinted quad list for the {@code null}-side render layer
     * of {@code stack}. Routes through the same {@link ToolItemOverrides} cache the live render
     * path uses, so a repeat call for an identical material set returns the identical list
     * instance rather than rebuilding it.
     */
    public List<BakedQuad> getRenderQuads(ItemStack stack) {
        return overrides.resolve(this, stack, null, null, 0).getQuads(null, null, RANDOM);
    }

    /**
     * Recolour {@code base} quads by part-material colour. A quad whose {@code tintIndex}
     * addresses a valid part slot is recoloured with that material's cached colour; every other
     * quad is returned as-is (same instance). Pure function — the unit-test seam for the
     * tinting contract.
     */
    static List<BakedQuad> tintForMaterials(List<BakedQuad> base, List<ResourceLocation> parts) {
        List<BakedQuad> out = new ArrayList<>(base.size());
        for (BakedQuad quad : base) {
            int tintIndex = quad.getTintIndex();
            if (tintIndex >= 0 && tintIndex < parts.size()) {
                int color = MaterialClientCache.getColor(parts.get(tintIndex));
                out.add(QuadTransformers.applyingColor(color).process(quad));
            }
            else {
                out.add(quad);
            }
        }
        return List.copyOf(out);
    }

    /**
     * {@link ItemOverrides} that swaps the wrapped base model for a per-material
     * {@link MaterialVariantModel}, keyed and cached on the tool's part-material list.
     */
    private static final class ToolItemOverrides extends ItemOverrides {

        private final BakedModel base;
        private final Map<List<ResourceLocation>, MaterialVariantModel> variants = new ConcurrentHashMap<>();

        private ToolItemOverrides(BakedModel base) {
            this.base = base;
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            ToolMaterials materials = stack.getOrDefault(TinkerDataComponents.TOOL_MATERIALS.get(), ToolMaterials.empty());
            if (materials.parts().isEmpty()) {
                // Unbuilt tool (creative-tab pull, /give): no materials to tint by — render the
                // base model untouched rather than caching a degenerate all-default variant.
                return base;
            }
            return variants.computeIfAbsent(materials.parts(), parts -> new MaterialVariantModel(base, parts));
        }
    }

    /**
     * Base model with its quads pre-tinted for one fixed part-material list. Built once when a
     * material combination is first seen and cached by {@link ToolItemOverrides}; every
     * {@link #getQuads} call thereafter returns the already-assembled list.
     */
    private static final class MaterialVariantModel extends BakedModelWrapper<BakedModel> {

        private final Map<Direction, List<BakedQuad>> tintedBySide = new EnumMap<>(Direction.class);
        private final List<BakedQuad> tintedNullSide;

        private MaterialVariantModel(BakedModel base, List<ResourceLocation> parts) {
            super(base);
            for (Direction side : Direction.values()) {
                tintedBySide.put(side, tintForMaterials(base.getQuads(null, side, RANDOM), parts));
            }
            this.tintedNullSide = tintForMaterials(base.getQuads(null, null, RANDOM), parts);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return side == null ? tintedNullSide : tintedBySide.getOrDefault(side, List.of());
        }

        @Override
        public ItemOverrides getOverrides() {
            // Already resolved — returning EMPTY stops vanilla re-entering the override chain.
            return ItemOverrides.EMPTY;
        }
    }
}
