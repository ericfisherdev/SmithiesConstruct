package slimeknights.sconstruct.plugin.jei.category;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

import com.mojang.serialization.Lifecycle;

import slimeknights.sconstruct.data.modifier.TinkerModifierBootstrap;
import slimeknights.sconstruct.tools.modifier.Modifier;

/**
 * Builds the synthetic recipe list for the JEI {@link ModifierCategory} — one
 * {@link ModifierEntry} per built-in modifier. SMTCON-157 calls {@link #entries()} from the JEI
 * plugin's {@code registerRecipes} hook.
 *
 * <p>The list is harvested by replaying {@link TinkerModifierBootstrap#bootstrap} against a
 * collecting {@link BootstrapContext} — the bootstrap stays the single source of truth for the
 * roster, so a modifier added there appears in JEI with no edit here. Harvesting code-side also
 * sidesteps the {@code ModifierRegistry} datapack cache, which is server-only and never
 * populated on the JEI client.
 */
public final class ModifierCatalog {

    private ModifierCatalog() {
    }

    /** One {@link ModifierEntry} per modifier registered by {@link TinkerModifierBootstrap}. */
    public static List<ModifierEntry> entries() {
        List<ModifierEntry> collected = new ArrayList<>();
        TinkerModifierBootstrap.bootstrap(new CollectingContext(collected));
        return List.copyOf(collected);
    }

    /**
     * A {@link BootstrapContext} that records every registered {@link Modifier} into a sink list
     * instead of writing to a real registry. {@link TinkerModifierBootstrap#bootstrap} only ever
     * calls {@code register}; {@link #lookup} is unreachable for this roster and fails loudly if
     * a future modifier shape starts resolving cross-registry references.
     */
    private static final class CollectingContext implements BootstrapContext<Modifier> {

        private final List<ModifierEntry> sink;

        CollectingContext(List<ModifierEntry> sink) {
            this.sink = sink;
        }

        @Override
        public Holder.Reference<Modifier> register(ResourceKey<Modifier> key, Modifier value, Lifecycle lifecycle) {
            sink.add(new ModifierEntry(value));
            // The bootstrap discards every register() return; a real Holder.Reference would need
            // a registry owner this harvesting context deliberately does not have.
            return Holder.Reference.createStandAlone(NULL_OWNER, key);
        }

        @Override
        public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
            throw new UnsupportedOperationException("ModifierCatalog harvesting does not resolve registry lookups");
        }
    }

    /** Trivial {@link HolderOwner} for the stand-alone {@link Holder.Reference} returned above. */
    private static final HolderOwner<Modifier> NULL_OWNER = new HolderOwner<>() {
    };
}
