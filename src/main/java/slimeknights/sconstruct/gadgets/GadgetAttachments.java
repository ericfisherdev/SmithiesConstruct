package slimeknights.sconstruct.gadgets;

import java.util.Objects;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;

import slimeknights.sconstruct.common.TinkerRegistries;

/**
 * Registration hub for the Phase-6 gadget {@link AttachmentType}s — the NeoForge replacement
 * for the legacy {@code ICapabilitySerializable} persistence the 1.12 gadgets used.
 *
 * <p>SMTCON-134 registers one: {@link #PIGGYBACK}, the per-player {@link PiggybackData} that
 * records who is riding whom. It is serialized with {@link PiggybackData#CODEC} so the ride
 * survives a logout, and {@code copyOnDeath()} carries it through a respawn.
 */
public final class GadgetAttachments {

    /** Per-player piggyback ride state — see {@link PiggybackData}. Persisted and copied on death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PiggybackData>> PIGGYBACK = TinkerRegistries.ATTACHMENT_TYPES.register("piggyback",
            () -> AttachmentType.builder(() -> PiggybackData.EMPTY).serialize(PiggybackData.CODEC).copyOnDeath().build());

    private GadgetAttachments() {
    }

    /** Forces class load so the static field initialiser registers the piggyback attachment. */
    public static void init() {
        Objects.requireNonNull(PIGGYBACK);
    }
}
