package slimeknights.sconstruct.tools.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.IQuadTransformer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.tools.material.client.MaterialClientCache;

/**
 * Unit coverage for {@link ToolBakedModel#tintForMaterials} — the pure tinting seam of the
 * per-material baked tool model (SMTCON-105). Verifies that a base quad is recoloured only
 * when its {@code tintIndex} addresses a real part slot, and is otherwise passed through
 * untouched (overlay layers, the broken sprite).
 */
class ToolBakedModelTest {

    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "iron");

    /** Reset the process-wide {@link MaterialClientCache} so a populated entry cannot leak between tests. */
    @AfterEach
    void clearCache() {
        MaterialClientCache.populate(Map.of());
    }

    /** A minimal BakedQuad with the requested tint index — vertex data is irrelevant to the tint seam. */
    private static BakedQuad quad(int tintIndex) {
        return new BakedQuad(new int[IQuadTransformer.STRIDE * 4], tintIndex, Direction.NORTH, null, false);
    }

    @Test
    void tintsOnlyQuadsAddressingAValidPartSlot() {
        MaterialClientCache.populate(Map.of(IRON, 0xFF112233));
        List<ResourceLocation> oneMaterial = List.of(IRON);
        BakedQuad layer0 = quad(0);
        BakedQuad overlay = quad(-1);
        // Tint index past the single-element part list — exercises the out-of-range branch.
        BakedQuad outOfRange = quad(oneMaterial.size());

        List<BakedQuad> out = ToolBakedModel.tintForMaterials(List.of(layer0, overlay, outOfRange), oneMaterial);

        assertEquals(3, out.size(), "tinting must preserve quad count");
        assertNotSame(layer0, out.get(0), "tintIndex 0 addresses part slot 0 — quad is recoloured");
        assertSame(overlay, out.get(1), "tintIndex -1 carries no material — passed through");
        assertSame(outOfRange, out.get(2), "tintIndex beyond the part count — passed through");
    }

    @Test
    void emptyBaseQuadListYieldsEmptyResult() {
        assertEquals(List.of(), ToolBakedModel.tintForMaterials(List.of(), List.of(IRON)));
    }
}
