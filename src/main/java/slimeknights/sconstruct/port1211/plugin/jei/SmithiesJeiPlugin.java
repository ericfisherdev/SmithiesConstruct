package slimeknights.sconstruct.port1211.plugin.jei;

import net.minecraft.resources.ResourceLocation;

import slimeknights.sconstruct.port1211.SConstruct;
import slimeknights.sconstruct.port1211.plugin.jei.category.AlloyCategory;
import slimeknights.sconstruct.port1211.plugin.jei.category.CastingBasinCategory;
import slimeknights.sconstruct.port1211.plugin.jei.category.CastingTableCategory;
import slimeknights.sconstruct.port1211.plugin.jei.category.MeltingCategory;
import slimeknights.sconstruct.port1211.plugin.jei.category.ModifierCategory;
import slimeknights.sconstruct.port1211.plugin.jei.category.PartBuilderCategory;
import slimeknights.sconstruct.port1211.plugin.jei.category.ToolBuildingCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;

/**
 * The mod's JEI integration entry point (SMTCON-150). JEI discovers this class through the
 * {@link JeiPlugin} annotation — no manual registration is needed — and calls back into the
 * {@code IModPlugin} hooks during JEI's startup.
 *
 * <p>{@link #registerCategories} registers the recipe categories. SMTCON-151 adds
 * {@link MeltingCategory}; SMTCON-152 adds {@link CastingTableCategory} and
 * {@link CastingBasinCategory}; SMTCON-153 adds {@link AlloyCategory}; SMTCON-154 adds
 * {@link ToolBuildingCategory}; SMTCON-155 adds {@link PartBuilderCategory} and
 * {@link ModifierCategory}; the drying-rack category follows in SMTCON-156.
 * {@code registerRecipes} and {@code registerRecipeCatalysts} keep their default no-op bodies
 * until SMTCON-157 wires the recipe lists and the catalyst blocks.
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
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new MeltingCategory(guiHelper), new CastingTableCategory(guiHelper), new CastingBasinCategory(guiHelper), new AlloyCategory(guiHelper),
                new ToolBuildingCategory(guiHelper), new PartBuilderCategory(guiHelper), new ModifierCategory(guiHelper));
    }
}
