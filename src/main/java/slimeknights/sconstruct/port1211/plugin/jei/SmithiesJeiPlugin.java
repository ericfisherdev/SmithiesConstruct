package slimeknights.sconstruct.port1211.plugin.jei;

import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.plugin.jei.category.MeltingCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;

/**
 * The mod's JEI integration entry point (SMTCON-150). JEI discovers this class through the
 * {@link JeiPlugin} annotation — no manual registration is needed — and calls back into the
 * {@code IModPlugin} hooks during JEI's startup.
 *
 * <p>{@link #registerCategories} registers the recipe categories. SMTCON-151 adds the first,
 * {@link MeltingCategory}; the casting / alloy / tool-building / part-builder / modifier /
 * drying-rack categories follow in SMTCON-152..156. {@code registerRecipes} and
 * {@code registerRecipeCatalysts} keep their default no-op bodies until SMTCON-157 wires the
 * recipe lists and the catalyst blocks.
 */
@JeiPlugin
public class SmithiesJeiPlugin implements IModPlugin {

    /** The plugin UID JEI keys this plugin's registrations under — {@code sconstruct:jei_plugin}. */
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MeltingCategory(registration.getJeiHelpers().getGuiHelper()));
    }
}
