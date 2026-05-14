package slimeknights.sconstruct.port1211.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Pinned-behaviour tests for {@link SlimeFluidSet}. The record's only contract is that every
 * component is non-null at construction; the helper {@link WorldFluids#slimeFluid} relies on
 * that to fail fast at class load if a future refactor drops one of the five registrations.
 */
class SlimeFluidSetTest {

    @Test
    void canonicalConstructorRejectsNullComponents() {
        // Walk each of the five record components and verify the canonical constructor throws
        // NPE when that single field is null and the rest are populated. SLIMEBLUE is a fully
        // constructed set used here as a template — never null after WorldFluids class-load.
        SlimeFluidSet template = WorldFluids.SLIMEBLUE;
        assertThrows(NullPointerException.class, () -> new SlimeFluidSet(null, template.source(), template.flowing(), template.block(), template.bucket()));
        assertThrows(NullPointerException.class, () -> new SlimeFluidSet(template.type(), null, template.flowing(), template.block(), template.bucket()));
        assertThrows(NullPointerException.class, () -> new SlimeFluidSet(template.type(), template.source(), null, template.block(), template.bucket()));
        assertThrows(NullPointerException.class, () -> new SlimeFluidSet(template.type(), template.source(), template.flowing(), null, template.bucket()));
        assertThrows(NullPointerException.class, () -> new SlimeFluidSet(template.type(), template.source(), template.flowing(), template.block(), null));
    }

    @Test
    void recordComponentsExposeTheUnderlyingHolders() {
        // The accessor return value must be the exact holder passed to the constructor, not a
        // copy or unwrapped value. Downstream code (creative-tab listener, client extensions,
        // lang provider) relies on holder identity to look up resolved registry entries.
        SlimeFluidSet set = WorldFluids.SLIMEBLUE;
        assertNotNull(set.type());
        assertNotNull(set.source());
        assertNotNull(set.flowing());
        assertNotNull(set.block());
        assertNotNull(set.bucket());

        // Reconstructing through the canonical constructor with the same holders yields an
        // equal record value — equality is structural over the five components.
        SlimeFluidSet rebuilt = new SlimeFluidSet(set.type(), set.source(), set.flowing(), set.block(), set.bucket());
        assertEquals(set, rebuilt);
        assertSame(set.type(), rebuilt.type());
    }
}
