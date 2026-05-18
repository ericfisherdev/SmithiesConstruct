package slimeknights.sconstruct.plugin.patchouli.page;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Shared resolution and labelling helpers for the custom smeltery Patchouli pages
 * ({@link PageMelting}, {@link PageCasting}, {@link PageAlloy}). The pages store their inputs and
 * outputs as resource-location strings in the book JSON; these helpers turn those strings into
 * the {@link ItemStack}s and display {@link Component}s the pages render.
 *
 * <p>Resolution is lenient: an unknown or blank id yields an empty stack or a literal
 * {@code "?"} label rather than throwing, so a typo in a book JSON degrades to a visibly wrong
 * page instead of crashing the book GUI.
 */
final class RecipePageRender {

    /** Millibuckets in one bucket — the unit fluid amounts are quoted in on the page. */
    private static final int MB_PER_BUCKET = 1000;
    /** Ticks per second — melt and cooling times are stored in ticks, shown in seconds. */
    private static final int TICKS_PER_SECOND = 20;

    private RecipePageRender() {
    }

    /**
     * The item stack for {@code id}, or {@link ItemStack#EMPTY} when the id is blank or names no
     * registered item.
     */
    static ItemStack resolveItem(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /**
     * A {@code "<amount> mB <fluid>"} label for the fluid named by {@code id}. Falls back to a
     * literal {@code "?"} fluid name when the id is blank or names no registered fluid.
     */
    static Component fluidLabel(@Nullable String id, int amountMb) {
        Component name = Component.literal("?");
        ResourceLocation key = id == null ? null : ResourceLocation.tryParse(id);
        if (key != null) {
            Fluid fluid = BuiltInRegistries.FLUID.getOptional(key).orElse(Fluids.EMPTY);
            if (fluid != Fluids.EMPTY) {
                name = new FluidStack(fluid, Math.max(amountMb, 1)).getHoverName();
            }
        }
        return Component.literal(formatAmount(amountMb) + " ").append(name);
    }

    /** Formats a millibucket amount, collapsing whole-bucket amounts to {@code "N B"}. */
    private static String formatAmount(int amountMb) {
        if (amountMb > 0 && amountMb % MB_PER_BUCKET == 0) {
            return (amountMb / MB_PER_BUCKET) + " B";
        }
        return amountMb + " mB";
    }

    /**
     * Formats a tick duration as seconds with one decimal place — plain integer division would
     * truncate a sub-second recipe to {@code "0 s"} and round every recipe down by up to a tick.
     */
    static String formatSeconds(int ticks) {
        int wholeSeconds = ticks / TICKS_PER_SECOND;
        int tenths = (ticks % TICKS_PER_SECOND) * 10 / TICKS_PER_SECOND;
        return tenths == 0 ? wholeSeconds + " s" : wholeSeconds + "." + tenths + " s";
    }
}
