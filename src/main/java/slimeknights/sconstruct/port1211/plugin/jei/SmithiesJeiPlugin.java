package slimeknights.sconstruct.port1211.plugin.jei;

import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.SConstruct;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;

/**
 * The mod's JEI integration entry point (SMTCON-150). JEI discovers this class through the
 * {@link JeiPlugin} annotation — no manual registration is needed — and calls back into the
 * {@code IModPlugin} hooks during JEI's startup.
 *
 * <p>This is the skeleton: it only declares the plugin {@link #getPluginUid() UID}. Every other
 * {@code IModPlugin} hook keeps its default no-op body for now. The real registrations land in
 * later tickets, each overriding one hook here:
 * <ul>
 *   <li>{@code registerCategories} — the melting / casting / alloy / tool-building / part-builder
 *       / modifier / drying-rack categories (SMTCON-151..156);</li>
 *   <li>{@code registerRecipes} and {@code registerRecipeCatalysts} — the recipe lists and the
 *       catalyst blocks that open each category (SMTCON-157).</li>
 * </ul>
 */
@JeiPlugin
public class SmithiesJeiPlugin implements IModPlugin {

    /** The plugin UID JEI keys this plugin's registrations under — {@code sconstruct:jei_plugin}. */
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }
}
