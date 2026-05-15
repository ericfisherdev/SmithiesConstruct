package slimeknights.sconstruct.port1211.tools.block.entity;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.port1211.common.data.TinkerDataComponents;
import slimeknights.sconstruct.port1211.tools.PartBuilderRegistry;
import slimeknights.sconstruct.port1211.tools.PartType;
import slimeknights.sconstruct.port1211.tools.inventory.PartBuilderMenu;
import slimeknights.sconstruct.port1211.tools.item.MaterialItem;
import slimeknights.sconstruct.port1211.tools.item.PatternItem;
import slimeknights.sconstruct.port1211.tools.item.ToolParts;
import slimeknights.sconstruct.port1211.tools.material.Material;

/**
 * Block entity backing the Part Builder (SMTCON-92). Owns a three-slot
 * {@link ItemStackHandler} (pattern idx {@value #PATTERN_SLOT}, material idx
 * {@value #MATERIAL_SLOT}, output idx {@value #OUTPUT_SLOT}) and re-derives the output every
 * time an input changes.
 *
 * <p>Crafting rule — when the pattern slot holds a typed pattern (a {@code PatternItem} with
 * the {@link TinkerDataComponents#TINKER_PATTERN_PART} component present) and the material
 * slot holds an item that matches some registered {@link Material}'s
 * {@link Material#repairTag()}, the output is built by looking up the
 * {@link slimeknights.sconstruct.port1211.tools.item.ToolParts#get(PartType)} item for the
 * pattern's part type and stamping it with that material's id on the
 * {@link TinkerDataComponents#PART_MATERIAL} component. First match wins; if no material
 * matches, the output is cleared.
 *
 * <p>Pull-the-output (a craft) decrements both pattern + material by one — see
 * {@link #consumeInputs()} — and the BE then re-derives the output for the next craft if
 * both inputs still have stock.
 *
 * <p>TODO(SMTCON-future): the "reusable gold pattern" path is unimplemented — the pattern is
 * unconditionally consumed on each take. Future ticket will branch on the pattern item id
 * to skip the {@code shrink(1)} on gold-pattern variants.
 *
 * <p>{@link MenuProvider} so the block's {@code useWithoutItem} can pass the BE directly to
 * {@code player.openMenu(...)}.
 */
public class PartBuilderBlockEntity extends BlockEntity implements MenuProvider {

    /** Inventory slot index for the typed-pattern input. */
    public static final int PATTERN_SLOT = 0;
    /** Inventory slot index for the material input. */
    public static final int MATERIAL_SLOT = 1;
    /** Inventory slot index for the resolved part output (read-only via the menu). */
    public static final int OUTPUT_SLOT = 2;
    /** Three-slot handler — pattern + material + output. */
    public static final int SLOTS = 3;

    private static final String TAG_INVENTORY = "Inventory";

    private final ItemStackHandler handler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            // Pattern or material changing rebuilds the output. Guarding on slot != OUTPUT_SLOT
            // prevents recursion: refreshOutput writes through setStackInSlot on OUTPUT_SLOT,
            // which would re-fire this callback if we didn't filter.
            if (slot != OUTPUT_SLOT) {
                refreshOutput();
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Output slot rejects every external insertion: contents are owned by the BE.
            // Menu layer also enforces, but the handler check defends against capability-API
            // neighbours (hoppers, droppers) that bypass the menu.
            if (slot == OUTPUT_SLOT) {
                return false;
            }
            if (slot == PATTERN_SLOT) {
                // Pattern slot only accepts typed patterns — the component must be present.
                return PatternItem.getPart(stack).isPresent();
            }
            // Material slot is permissive at the handler layer: the server-side refreshOutput
            // is the source of truth for whether the stack actually resolves to a known
            // material. A hopper feeding random items in just won't produce an output.
            return true;
        }
    };

    public PartBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(PartBuilderRegistry.PART_BUILDER_BE.get(), pos, state);
    }

    /** Backing handler — also exposed via the {@code Capabilities.ItemHandler.BLOCK} channel. */
    public ItemStackHandler getHandler() {
        return handler;
    }

    /**
     * Re-derive the output slot from the current pattern + material inputs. Clears the output
     * when either input is missing, when the pattern is not a typed pattern, or when no
     * registered {@link Material} matches the material stack. Called on every contents change
     * to either input slot.
     *
     * <p>Server-only: the {@link Material} datapack registry is only present on the server
     * {@link Level#registryAccess()}; clients see the cached server snapshot but
     * {@code refreshOutput} is invoked from the handler callback on the BE which only mutates
     * during server-side menu interaction.
     */
    void refreshOutput() {
        ItemStack pattern = handler.getStackInSlot(PATTERN_SLOT);
        ItemStack material = handler.getStackInSlot(MATERIAL_SLOT);
        if (pattern.isEmpty() || material.isEmpty()) {
            handler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        Optional<PartType> typed = PatternItem.getPart(pattern);
        if (typed.isEmpty()) {
            // Blank pattern (no typed component) — not valid input here.
            handler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        if (level == null) {
            // Defensive — BE is removed from world. Clear and bail.
            handler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        Optional<Holder<Material>> match = findMatchingMaterial(material);
        if (match.isEmpty()) {
            handler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        ItemStack outputStack = buildPartStack(typed.get(), match.get().value().id());
        handler.setStackInSlot(OUTPUT_SLOT, outputStack);
    }

    /**
     * Walk the {@code sconstruct:material} datapack registry through the {@link Level}'s
     * {@code registryAccess} and return the first {@link Material} whose {@link Material#repairTag()}
     * is present and contains the supplied material stack's item. Returns {@link Optional#empty()}
     * when no material matches; the caller treats that as "clear the output."
     *
     * <p>The lookup walks the live registry rather than the {@code MaterialRegistry} cache so
     * the part builder works even before the first {@code OnDatapackSyncEvent} fires (e.g. on
     * dedicated server first-boot before a player joins). The pass is linear in the registry
     * size — material rosters are small ({@code O(50)}) so the once-per-input-change scan is
     * negligible.
     */
    private Optional<Holder<Material>> findMatchingMaterial(ItemStack materialStack) {
        HolderLookup.RegistryLookup<Material> lookup = level.registryAccess().lookupOrThrow(Material.REGISTRY_KEY);
        return firstMaterialMatching(lookup.listElements().map(holder -> (Holder<Material>) holder), materialStack);
    }

    /**
     * Pure function over the supplied {@link Material} holder stream — returns the first holder
     * whose {@link Material#repairTag()} is present and contains the material stack's item, or
     * {@link Optional#empty()} if none match. Exposed package-private so unit tests can exercise
     * the matching rule against a hand-built holder list without a live {@link Level}.
     */
    static Optional<Holder<Material>> firstMaterialMatching(java.util.stream.Stream<Holder<Material>> holders, ItemStack materialStack) {
        return holders.filter(holder -> {
            Material mat = holder.value();
            return mat.repairTag().isPresent() && materialStack.is(mat.repairTag().get());
        }).findFirst();
    }

    /**
     * Build a {@link MaterialItem} stack for the supplied {@link PartType}, stamped with the
     * supplied material id on the {@link TinkerDataComponents#PART_MATERIAL} component. The
     * registered {@code MaterialItem} for the part type comes from {@link ToolParts#get}.
     */
    private static ItemStack buildPartStack(PartType part, net.minecraft.resources.ResourceLocation materialId) {
        ItemStack stack = new ItemStack(ToolParts.get(part).get());
        stack.set(TinkerDataComponents.PART_MATERIAL.get(), materialId);
        return stack;
    }

    /**
     * Consume one pattern + one material from the input slots. Called by the menu's output-slot
     * {@code onTake} hook after the player has drained the output. After the decrement,
     * {@link #refreshOutput} re-populates the output if both inputs still have stock.
     *
     * <p>TODO(SMTCON-future): pattern consumption here treats every pattern as single-use.
     * The gold-pattern path (reusable, durability-based, or unconsumed) is a future ticket;
     * branch on the pattern item id here when that lands.
     */
    public void consumeInputs() {
        ItemStack pattern = handler.getStackInSlot(PATTERN_SLOT);
        ItemStack material = handler.getStackInSlot(MATERIAL_SLOT);
        if (!pattern.isEmpty()) {
            pattern.shrink(1);
            handler.setStackInSlot(PATTERN_SLOT, pattern.isEmpty() ? ItemStack.EMPTY : pattern);
        }
        if (!material.isEmpty()) {
            material.shrink(1);
            handler.setStackInSlot(MATERIAL_SLOT, material.isEmpty() ? ItemStack.EMPTY : material);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_INVENTORY, handler.serializeNBT(provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(TAG_INVENTORY, Tag.TAG_COMPOUND)) {
            handler.deserializeNBT(provider, tag.getCompound(TAG_INVENTORY));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.sconstruct.part_builder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        Objects.requireNonNull(playerInventory, "playerInventory");
        Objects.requireNonNull(player, "player");
        return new PartBuilderMenu(containerId, playerInventory, this);
    }
}
