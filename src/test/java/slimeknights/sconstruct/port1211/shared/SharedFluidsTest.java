package slimeknights.sconstruct.port1211.shared;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

import org.junit.jupiter.api.Test;

import slimeknights.sconstruct.port1211.SConstruct;

/**
 * Pinned-behaviour tests for the {@link SharedFluids#BLOOD} four-part registration. Verifies
 * the {@link FluidType} carries the legacy physical properties verbatim, the source/flowing
 * pair instantiates with the right kinds, the {@link net.minecraft.world.level.block.LiquidBlock}
 * destroys on piston push (PushReaction.DESTROY) so it doesn't bypass the no-loot setup, and
 * the matching {@link BucketItem} is single-stack with a vanilla-bucket craft remainder.
 */
class SharedFluidsTest {

    @Test
    void bloodTypeCarriesLegacyPhysicalProperties() {
        FluidType type = SharedFluids.BLOOD_TYPE.get();
        assertNotNull(type);
        assertAll(() -> assertEquals(1100, type.getDensity(), "density"), () -> assertEquals(1500, type.getViscosity(), "viscosity"), () -> assertEquals(310, type.getTemperature(), "temperature"),
                () -> assertEquals("fluid.sconstruct.blood", type.getDescriptionId(), "descriptionId"));
    }

    @Test
    void sourceAndFlowingAreRegisteredUnderTheTconstructNamespace() {
        assertNotNull(SharedFluids.BLOOD.get());
        assertNotNull(SharedFluids.BLOOD_FLOWING.get());
        assertAll(() -> assertEquals(SConstruct.MOD_ID, SharedFluids.BLOOD.getId().getNamespace()), () -> assertEquals("blood", SharedFluids.BLOOD.getId().getPath()),
                () -> assertEquals(SConstruct.MOD_ID, SharedFluids.BLOOD_FLOWING.getId().getNamespace()), () -> assertEquals("flowing_blood", SharedFluids.BLOOD_FLOWING.getId().getPath()));
    }

    @Test
    void sourceIsTheBaseFlowingSourceVariant() {
        // Two concrete subclasses live under BaseFlowingFluid: Source (still water-style) and
        // Flowing (the stream). A swap would silently break vanilla flow propagation — pin the
        // exact runtime type.
        org.junit.jupiter.api.Assertions.assertInstanceOf(BaseFlowingFluid.Source.class, SharedFluids.BLOOD.get());
        org.junit.jupiter.api.Assertions.assertInstanceOf(BaseFlowingFluid.Flowing.class, SharedFluids.BLOOD_FLOWING.get());
    }

    @Test
    void liquidBlockDestroysOnPistonPush() {
        // PushReaction.DESTROY + noLootTable means a piston shoves the column and the fluid
        // is gone with no item drop. Vanilla water/lava use the same combination; pinning it
        // here guards against a refactor that flips to PushReaction.IGNORE or BLOCK (which
        // would leave residual fluid on push).
        assertEquals("blood", SharedFluids.BLOOD_BLOCK.getId().getPath());
        assertEquals(SConstruct.MOD_ID, SharedFluids.BLOOD_BLOCK.getId().getNamespace());
        assertEquals(PushReaction.DESTROY, SharedFluids.BLOOD_BLOCK.get().defaultBlockState().getPistonPushReaction());
    }

    @Test
    void bloodBucketStacksToOneAndRemaindersAsAVanillaBucket() {
        BucketItem bucket = SharedItems.BUCKET_BLOOD.get();
        assertNotNull(bucket);
        assertAll(() -> assertEquals("blood_bucket", SharedItems.BUCKET_BLOOD.getId().getPath()), () -> assertEquals(SConstruct.MOD_ID, SharedItems.BUCKET_BLOOD.getId().getNamespace()),
                () -> assertEquals(1, bucket.components().getOrDefault(DataComponents.MAX_STACK_SIZE, Integer.valueOf(64)).intValue(), "stack size"),
                () -> assertSame(Items.BUCKET, bucket.getCraftingRemainingItem(), "craft remainder"));
    }

    @Test
    void bucketReferencesTheSourceFluid() {
        // The Fluid inside the BucketItem must be the source, not the flowing variant; a swap
        // would mean the bucket places a stream that immediately drains rather than a static
        // pool.
        assertSame(SharedFluids.BLOOD.get(), SharedItems.BUCKET_BLOOD.get().content);
    }
}
