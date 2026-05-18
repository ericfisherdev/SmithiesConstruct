package slimeknights.sconstruct.smeltery.recipe;

import java.util.Objects;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A fluid-side analogue of vanilla's item {@link net.minecraft.world.item.crafting.Ingredient}
 * (SMTCON-121). NeoForge ships an ingredient abstraction for items but not for fluids, so the
 * smeltery's casting and alloy recipes need this: a set of acceptable {@link Fluid}s plus a
 * minimum {@code amount} in millibuckets.
 *
 * <p>{@link #test} accepts a {@link FluidStack} when the stack's fluid is one of {@link #fluids()}
 * <em>and</em> the stack holds at least {@link #amount()} millibuckets — so a recipe can demand,
 * for example, "at least 288 mB of any fluid in the {@code molten_iron} tag".
 *
 * @param fluids the set of fluids this ingredient accepts, typically a fluid tag
 * @param amount the minimum millibuckets of fluid the ingredient requires
 */
public record FluidIngredient(HolderSet<Fluid> fluids, int amount) {

    /** Datapack codec — {@code {"fluid": "#tag-or-id", "amount": 288}}. */
    public static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(builder -> builder
            .group(RegistryCodecs.homogeneousList(Registries.FLUID).fieldOf("fluid").forGetter(FluidIngredient::fluids), ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidIngredient::amount))
            .apply(builder, FluidIngredient::new));

    /** Network codec — syncs a fluid ingredient to clients. */
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.holderSet(Registries.FLUID), FluidIngredient::fluids,
            ByteBufCodecs.VAR_INT, FluidIngredient::amount, FluidIngredient::new);

    /** Validates the fluid set and the amount. */
    public FluidIngredient {
        Objects.requireNonNull(fluids, "fluids");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be a positive millibucket value: " + amount);
        }
    }

    /**
     * Whether {@code stack} satisfies this ingredient — its fluid is one of {@link #fluids()}
     * and it holds at least {@link #amount()} millibuckets. An empty stack never matches.
     *
     * @param stack the fluid stack to test
     * @return {@code true} if the stack's fluid and amount both satisfy this ingredient
     */
    public boolean test(FluidStack stack) {
        return !stack.isEmpty() && stack.getAmount() >= amount && fluids.contains(stack.getFluidHolder());
    }
}
