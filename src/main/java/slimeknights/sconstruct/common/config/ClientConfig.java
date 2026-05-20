package slimeknights.sconstruct.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only {@link ModConfigSpec} for the port-1.21.1 build (SMTCON-229). Holds rendering-side
 * knobs the player can tune without touching the gameplay state — currently a single quad-budget
 * cap for the smeltery interior renderer. Separate from {@link Config} because that one is
 * {@code STARTUP} (read once at boot to gate pulse registration) while client values live for
 * the duration of the game session and need to be reloadable.
 *
 * <p>Registered as {@code ModConfig.Type.CLIENT} from {@link slimeknights.sconstruct.SConstruct},
 * so a dedicated server never reads this file — the constants exist there but their values are
 * effectively defaults the server never queries.
 *
 * <p>The TOML written to {@code <gameDir>/config/sconstruct-client.toml} on first launch groups
 * every render knob under a {@code [render]} category. Toggling a value there takes effect
 * mid-game; NeoForge reloads CLIENT configs without restarting the JVM.
 */
public final class ClientConfig {

    /** Spec category that holds every render-side knob. */
    private static final String RENDER_CATEGORY = "render";

    /** Compiled spec; pass this to {@code ModContainer#registerConfig}. */
    public static final ModConfigSpec SPEC;

    /**
     * Upper bound on the total quad count {@code SmelteryRenderer} submits per frame for the
     * melting items floating inside the bowl. Once the running total exceeds this value the
     * renderer breaks out of the per-slot loop; with the rotation cursor advancing each frame,
     * a smeltery whose contents exceed the budget shows different items each frame rather than
     * always the first N. Default of {@value #DEFAULT_MAX_ITEM_QUADS} matches upstream Tinkers'
     * Construct 1.18.2's {@code Config.CLIENT.maxSmelteryItemQuads}.
     */
    public static final ModConfigSpec.IntValue MAX_SMELTERY_ITEM_QUADS;

    /** Default quad budget — matches upstream TC 1.18.2 and gives ~100 ore-model items of headroom. */
    public static final int DEFAULT_MAX_ITEM_QUADS = 10_000;

    /** Lower clamp on the configured budget so a misconfigured zero does not stop all rendering. */
    public static final int MIN_MAX_ITEM_QUADS = 100;

    /** Upper clamp on the configured budget so an absurd value cannot starve the GPU through a typo. */
    public static final int MAX_MAX_ITEM_QUADS = 1_000_000;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Client-side render knobs for the smeltery interior view.").push(RENDER_CATEGORY);
        MAX_SMELTERY_ITEM_QUADS = builder
                .comment("Quad budget for the smeltery interior item renderer. Items beyond the budget", "are skipped for the frame; the renderer rotates which items render each frame",
                        "so a smeltery over the budget cycles its display rather than freezing on the first N.")
                .defineInRange("max_smeltery_item_quads", DEFAULT_MAX_ITEM_QUADS, MIN_MAX_ITEM_QUADS, MAX_MAX_ITEM_QUADS);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
