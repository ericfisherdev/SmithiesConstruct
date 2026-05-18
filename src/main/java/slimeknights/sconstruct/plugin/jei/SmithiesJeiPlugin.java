package slimeknights.sconstruct.plugin.jei;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.gadgets.GadgetBlocks;
import slimeknights.sconstruct.gadgets.recipe.GadgetRecipes;
import slimeknights.sconstruct.plugin.jei.category.AlloyCategory;
import slimeknights.sconstruct.plugin.jei.category.CastingBasinCategory;
import slimeknights.sconstruct.plugin.jei.category.CastingTableCategory;
import slimeknights.sconstruct.plugin.jei.category.DryingRackCategory;
import slimeknights.sconstruct.plugin.jei.category.MeltingCategory;
import slimeknights.sconstruct.plugin.jei.category.ModifierCatalog;
import slimeknights.sconstruct.plugin.jei.category.ModifierCategory;
import slimeknights.sconstruct.plugin.jei.category.PartBuilderCatalog;
import slimeknights.sconstruct.plugin.jei.category.PartBuilderCategory;
import slimeknights.sconstruct.plugin.jei.category.ToolBuildingCatalog;
import slimeknights.sconstruct.plugin.jei.category.ToolBuildingCategory;
import slimeknights.sconstruct.smeltery.CastingBlocks;
import slimeknights.sconstruct.smeltery.SmelteryComponents;
import slimeknights.sconstruct.smeltery.inventory.client.SmelteryControllerScreen;
import slimeknights.sconstruct.smeltery.recipe.CastingRecipe;
import slimeknights.sconstruct.smeltery.recipe.SmelteryRecipes;
import slimeknights.sconstruct.tools.PartBuilderRegistry;
import slimeknights.sconstruct.tools.ToolStationRegistry;
import slimeknights.sconstruct.tools.inventory.client.PartBuilderScreen;
import slimeknights.sconstruct.tools.inventory.client.ToolStationScreen;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

/**
 * The mod's JEI integration entry point (SMTCON-150). JEI discovers this class through the
 * {@link JeiPlugin} annotation — no manual registration is needed — and calls back into the
 * {@code IModPlugin} hooks during JEI's startup.
 *
 * <p>{@link #registerCategories} registers the eight recipe categories (SMTCON-151..156).
 * {@link #registerRecipes} feeds each category its recipe list — the smeltery and drying
 * categories read {@link RecipeHolder}s from the client recipe manager, the synthetic
 * tool-building / part-building / modifier categories take their entries from the matching
 * {@code *Catalog}. {@link #registerRecipeCatalysts} marks the workstation blocks that open
 * each category, and {@link #registerGuiHandlers} makes a click on each workstation screen jump
 * straight to its JEI category (SMTCON-157).
 */
@JeiPlugin
public class SmithiesJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The plugin UID JEI keys this plugin's registrations under — {@code sconstruct:jei_plugin}. */
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(SConstruct.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        IRecipeCategory<?>[] categories = { new MeltingCategory(guiHelper), new CastingTableCategory(guiHelper), new CastingBasinCategory(guiHelper), new AlloyCategory(guiHelper),
                new ToolBuildingCategory(guiHelper), new PartBuilderCategory(guiHelper), new ModifierCategory(guiHelper), new DryingRackCategory(guiHelper) };
        registration.addRecipeCategories(categories);
        // SMTCON-171 smoke check: surface the registered count in the client log so a dropped
        // category shows up as a number lower than the expected eight rather than silently.
        LOGGER.info("Smithies' Construct JEI plugin: registered {} recipe categories", categories.length);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MeltingCategory.TYPE, recipes(SmelteryRecipes.MELTING_TYPE.get()));

        // One casting recipe type backs both casting blocks; CastingRecipe#isBasin partitions it
        // in a single pass — true for basin recipes, false for table recipes.
        Map<Boolean, List<RecipeHolder<CastingRecipe>>> casting = recipes(SmelteryRecipes.CASTING_TYPE.get()).stream().collect(Collectors.partitioningBy(holder -> holder.value().isBasin()));
        registration.addRecipes(CastingTableCategory.TYPE, casting.get(false));
        registration.addRecipes(CastingBasinCategory.TYPE, casting.get(true));

        registration.addRecipes(AlloyCategory.TYPE, recipes(SmelteryRecipes.ALLOY_TYPE.get()));
        registration.addRecipes(DryingRackCategory.TYPE, recipes(GadgetRecipes.DRYING_TYPE.get()));

        registration.addRecipes(ToolBuildingCategory.TYPE, ToolBuildingCatalog.entries());
        registration.addRecipes(PartBuilderCategory.TYPE, PartBuilderCatalog.entries());
        registration.addRecipes(ModifierCategory.TYPE, ModifierCatalog.entries());
        LOGGER.info("Smithies' Construct JEI plugin: registered recipe lists for all categories");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(SmelteryComponents.SMELTERY_CONTROLLER.get(), MeltingCategory.TYPE, AlloyCategory.TYPE);
        registration.addRecipeCatalyst(CastingBlocks.CASTING_TABLE.get(), CastingTableCategory.TYPE);
        registration.addRecipeCatalyst(CastingBlocks.CASTING_BASIN.get(), CastingBasinCategory.TYPE);
        // Tools build and gain modifiers at both the Tool Station and the Tool Forge.
        registration.addRecipeCatalyst(ToolStationRegistry.TOOL_STATION.get(), ToolBuildingCategory.TYPE, ModifierCategory.TYPE);
        registration.addRecipeCatalyst(ToolStationRegistry.TOOL_FORGE.get(), ToolBuildingCategory.TYPE, ModifierCategory.TYPE);
        registration.addRecipeCatalyst(PartBuilderRegistry.PART_BUILDER.get(), PartBuilderCategory.TYPE);
        registration.addRecipeCatalyst(GadgetBlocks.DRYING_RACK.get(), DryingRackCategory.TYPE);
        LOGGER.info("Smithies' Construct JEI plugin: registered workstation recipe catalysts");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Click areas are GUI-relative. The smeltery melting grid sits at (62,17) and spans the
        // 3x3 18px-pitch slot grid; the tool-station Build / Modify buttons stack at (6,50); the
        // part-builder pattern -> output slot row runs across (26,33).
        registration.addRecipeClickArea(SmelteryControllerScreen.class, 62, 17, 54, 54, MeltingCategory.TYPE, AlloyCategory.TYPE);
        registration.addRecipeClickArea(ToolStationScreen.class, 6, 50, 56, 42, ToolBuildingCategory.TYPE, ModifierCategory.TYPE);
        registration.addRecipeClickArea(PartBuilderScreen.class, 26, 33, 110, 20, PartBuilderCategory.TYPE);
    }

    /**
     * Every {@link RecipeHolder} of {@code type} from the synced client recipe manager, or an
     * empty list when no server connection is active — JEI rebuilds its recipe lists on world
     * join, so the connection is normally present, but a defensive empty keeps a pre-join
     * rebuild from throwing. The recipe manager is read off the {@link ClientPacketListener}
     * rather than the {@code ClientLevel} so this method does not appear to leak a closeable.
     */
    private static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> recipes(net.minecraft.world.item.crafting.RecipeType<T> type) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            LOGGER.debug("JEI requested recipes for {} but no server connection is active — returning an empty list", type);
            return List.of();
        }
        return connection.getRecipeManager().getAllRecipesFor(type);
    }
}
