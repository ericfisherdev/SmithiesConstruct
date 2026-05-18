package slimeknights.sconstruct.common;

import java.util.List;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.SConstruct;

/**
 * Registration hub for the mod's custom {@link SoundEvent}s (SMTCON-166), ported from the
 * legacy {@code sounds.json}. Each event is registered against {@link TinkerRegistries#SOUND_EVENTS}
 * at the resource path matching its name, so {@code sconstruct:frypan_hit} and friends resolve
 * the OGG files under {@code assets/sconstruct/sounds/} that {@code SmithiesSoundProvider}
 * wires up.
 *
 * <p>The {@link DeferredHolder} fields are {@code public static final} so call sites address a
 * sound by its typed handle. {@link #ALL} is the iteration surface the sound-definitions
 * provider walks so a new sound lights up datagen by appending one field and one list entry.
 */
public final class SmithiesSounds {

    /** Frying-pan melee hit — the legacy {@code frypan_hit} sound. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FRYPAN_HIT = register("frypan_hit");
    /** Squeaky-toy hit — the legacy {@code toy_squeak} sound. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TOY_SQUEAK = register("toy_squeak");
    /** Lumber-axe / saw swing — the legacy {@code little_saw} sound. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LITTLE_SAW = register("little_saw");
    /** Slimesling launch — the legacy {@code slimesling} sound. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SLIMESLING = register("slimesling");
    /** Wood-block tool hit — the legacy {@code wood_hit} sound (five OGG variants). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOD_HIT = register("wood_hit");
    /** Stone-block tool hit — the legacy {@code stone_hit} sound (three OGG variants). */
    public static final DeferredHolder<SoundEvent, SoundEvent> STONE_HIT = register("stone_hit");
    /** Crossbow reload — the legacy {@code crossbow_reload} sound. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CROSSBOW_RELOAD = register("crossbow_reload");
    /** Crossbow finished charging — the legacy {@code charged} sound. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGED = register("charged");
    /** Crossbow fires — the legacy {@code discharge} sound. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DISCHARGE = register("discharge");

    /** Insertion-ordered view of every registered sound event — the datagen iteration surface. */
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ALL = List.of(FRYPAN_HIT, TOY_SQUEAK, LITTLE_SAW, SLIMESLING, WOOD_HIT, STONE_HIT, CROSSBOW_RELOAD, CHARGED, DISCHARGE);

    private SmithiesSounds() {
    }

    /** Forces class load so the static field initialisers register every sound event. */
    public static void init() {
        Objects.requireNonNull(ALL);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, name);
        return TinkerRegistries.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
