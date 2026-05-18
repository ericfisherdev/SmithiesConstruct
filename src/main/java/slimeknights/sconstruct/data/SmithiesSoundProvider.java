package slimeknights.sconstruct.data;

import java.util.function.Supplier;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.SmithiesSounds;

/**
 * Generates {@code assets/sconstruct/sounds.json} (SMTCON-166) — one definition per
 * {@link SmithiesSounds} event, each pointing at its OGG file(s) under
 * {@code assets/sconstruct/sounds/} and a {@code subtitles.sconstruct.*} subtitle key.
 *
 * <p>Two events draw a random variant each play: {@code wood_hit} from five OGGs and
 * {@code stone_hit} from three, matching the legacy {@code sounds.json}.
 */
public class SmithiesSoundProvider extends SoundDefinitionsProvider {

    public SmithiesSoundProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, SConstruct.MOD_ID, existingFileHelper);
    }

    @Override
    public void registerSounds() {
        single(SmithiesSounds.FRYPAN_HIT, "frypan_hit");
        single(SmithiesSounds.TOY_SQUEAK, "toy_squeak");
        single(SmithiesSounds.LITTLE_SAW, "little_saw");
        single(SmithiesSounds.SLIMESLING, "slimesling");
        variants(SmithiesSounds.WOOD_HIT, "wood_hit", 5);
        variants(SmithiesSounds.STONE_HIT, "stone_hit", 3);
        single(SmithiesSounds.CROSSBOW_RELOAD, "crossbow_reload");
        single(SmithiesSounds.CHARGED, "charged");
        single(SmithiesSounds.DISCHARGE, "discharge");
    }

    /** Registers a sound event backed by a single OGG file at {@code sounds/<name>.ogg}. */
    private void single(Supplier<SoundEvent> event, String name) {
        add(event, definition().subtitle(subtitleKey(name)).with(sound(soundFile(name))));
    }

    /**
     * Registers a sound event backed by {@code count} numbered OGG variants
     * ({@code sounds/<name>1.ogg} … {@code sounds/<name><count>.ogg}); the game picks one at
     * random each play.
     */
    private void variants(Supplier<SoundEvent> event, String name, int count) {
        SoundDefinition definition = definition().subtitle(subtitleKey(name));
        for (int variant = 1; variant <= count; variant++) {
            definition.with(sound(soundFile(name + variant)));
        }
        add(event, definition);
    }

    private static String subtitleKey(String name) {
        return "subtitles." + SConstruct.MOD_ID + "." + name;
    }

    private static ResourceLocation soundFile(String path) {
        return ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, path);
    }
}
