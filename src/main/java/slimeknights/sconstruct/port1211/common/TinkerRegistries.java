package slimeknights.sconstruct.port1211.common;

import java.util.List;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import slimeknights.sconstruct.port1211.SConstruct;

/**
 * Central hub that owns every {@link DeferredRegister} the mod needs. A pulse that wants to
 * register content pulls the relevant register from here rather than allocating its own;
 * concentrating all {@code DeferredRegister.create(...)} calls in a single file removes the risk
 * of two subsystems registering against the same vanilla registry with different mod ids and
 * makes registration ordering trivial to reason about — {@link #registerAll(IEventBus)} attaches
 * each one to the mod bus in a single deterministic pass.
 *
 * <p>Empty registers (no content yet) still produce a valid mod boot: {@code DeferredRegister}
 * holds an empty list and the registry event fires with no entries.
 *
 * <p>Adding a new registry: declare a {@code public static final DeferredRegister<...>} field,
 * include it in the {@link #ALL} list, and {@link #registerAll(IEventBus)} picks it up
 * automatically — no other call sites need updating.
 */
public final class TinkerRegistries {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SConstruct.MOD_ID);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SConstruct.MOD_ID);

    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SConstruct.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SConstruct.MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, SConstruct.MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, SConstruct.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, SConstruct.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, SConstruct.MOD_ID);

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, SConstruct.MOD_ID);

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, SConstruct.MOD_ID);

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, SConstruct.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SConstruct.MOD_ID);

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, SConstruct.MOD_ID);

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, SConstruct.MOD_ID);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, SConstruct.MOD_ID);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> STRUCTURE_PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE,
            SConstruct.MOD_ID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SConstruct.MOD_ID);

    /**
     * Every register the hub owns, in attachment order. Listed here so {@link
     * #registerAll(IEventBus)} (and {@link #registryCount()} for tests) doesn't need to be
     * updated when new registries are added — just add the field above and append it here.
     */
    private static final List<DeferredRegister<?>> ALL = List.of(ITEMS, BLOCKS, DATA_COMPONENTS, BLOCK_ENTITY_TYPES, ENTITY_TYPES, MENU_TYPES, RECIPE_TYPES, RECIPE_SERIALIZERS, MOB_EFFECTS,
            PARTICLE_TYPES, SOUND_EVENTS, CREATIVE_TABS, FLUID_TYPES, FLUIDS, STRUCTURE_TYPES, STRUCTURE_PIECE_TYPES, ATTACHMENT_TYPES);

    private TinkerRegistries() {
    }

    /**
     * Attach every {@link DeferredRegister} owned by this hub to the supplied mod event bus.
     * Must be called exactly once during mod construction — typically from the {@code SConstruct}
     * constructor — before any pulse boots. Calling it twice would subscribe each register's
     * internal listeners twice, double-firing every registry event.
     */
    public static void registerAll(IEventBus modBus) {
        ALL.forEach(register -> register.register(modBus));
    }

    /** Test hook: total number of {@link DeferredRegister} fields the hub owns. */
    static int registryCount() {
        return ALL.size();
    }
}
