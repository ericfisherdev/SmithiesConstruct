package slimeknights.sconstruct.port1211.tools.block.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import slimeknights.sconstruct.port1211.tools.ToolDefinition;
import slimeknights.sconstruct.port1211.tools.ToolStationLogic;
import slimeknights.sconstruct.port1211.tools.ToolStationRegistry;
import slimeknights.sconstruct.port1211.tools.inventory.ToolStationMenu;
import slimeknights.sconstruct.port1211.tools.item.ToolCore;
import slimeknights.sconstruct.port1211.tools.item.ToolItems;

/**
 * Block entity backing the Tool Station (SMTCON-93). Owns a 6-input + 1-output
 * {@link ItemStackHandler} and re-derives the output whenever an input slot changes.
 *
 * <p>Build path — when the 6 input slots hold {@code MaterialItem} stacks whose part-types
 * match a registered {@link ToolDefinition} positionally and every input carries a stamped
 * {@code PART_MATERIAL}, the output is the freshly-assembled tool. See
 * {@link ToolStationLogic#tryBuild} for the matching rule.
 *
 * <p>Modify path — when input slot 0 holds an existing {@link ToolCore} stack, the output is a
 * copy of that stack. The actual modifier-item ingestion / application is a follow-up ticket
 * (the modifier-item registry hasn't landed yet); for SMTCON-93 the BE only surfaces the
 * "passthrough" form so the menu / screen wiring is exercised end-to-end. The copy explicitly
 * preserves every data component the tool carries via {@link ItemStack#copy()}.
 *
 * <p>{@link ToolForgeBlockEntity} overrides {@link #acceptsAdvancedTools} to broaden the
 * candidate roster from {@link ToolDefinition#ALL_BASIC} to {@link ToolDefinition#ALL_ADVANCED}.
 * Subclassing rather than parameterising keeps each station's BE type a distinct registry
 * entry (vanilla constraint — a BE type is bound to its block class), which downstream
 * datagen / capability registration needs.
 */
public class ToolStationBlockEntity extends BlockEntity implements MenuProvider {

    /** Width of the input slot grid (6 slots, indices 0..5). */
    public static final int INPUT_SLOTS = 6;
    /** Output slot index. */
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    /** Total slot count owned by the BE (6 inputs + 1 output). */
    public static final int SLOTS = INPUT_SLOTS + 1;

    private static final String TAG_INVENTORY = "Inventory";

    private final ItemStackHandler handler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            // Input changes rebuild the output. Guard against re-entrance from the output slot
            // write inside refreshOutput.
            if (slot != OUTPUT_SLOT) {
                refreshOutput();
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Output slot rejects every external insertion — the BE owns the contents.
            return slot != OUTPUT_SLOT;
        }
    };

    public ToolStationBlockEntity(BlockPos pos, BlockState state) {
        this(ToolStationRegistry.TOOL_STATION_BE.get(), pos, state);
    }

    /**
     * Subclass-friendly constructor — {@link ToolForgeBlockEntity} feeds in the forge's BE
     * type so the same handler / menu / build logic backs both stations.
     */
    protected ToolStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Backing handler — also exposed via the {@code Capabilities.ItemHandler.BLOCK} channel. */
    public ItemStackHandler getHandler() {
        return handler;
    }

    /**
     * Whether the station can build "advanced" tools — the four / five-part hammer, lumberaxe,
     * shortbow, crossbow, arrow definitions in {@link ToolDefinition#ALL_ADVANCED}. The base
     * Tool Station returns {@code false} so only the basic three-part roster is accepted;
     * {@link ToolForgeBlockEntity} overrides to {@code true}.
     */
    public boolean acceptsAdvancedTools() {
        return false;
    }

    /**
     * Re-derive the output slot from the current inputs. Two paths:
     *
     * <ul>
     *   <li><strong>Modify</strong> — slot 0 holds an existing {@link ToolCore}: output is a
     *       copy of slot 0 (the data-component map is carried by {@link ItemStack#copy}).
     *       TODO(SMTCON-93 follow-up): consume modifier items in slots 1..5 and call
     *       {@link slimeknights.sconstruct.port1211.tools.ToolHelper#addModifier} for each;
     *       the modifier-item registry hasn't landed yet.</li>
     *   <li><strong>Build</strong> — otherwise: hand the inputs + the accepted candidate
     *       roster to {@link ToolStationLogic#tryBuild}. On match the helper returns the
     *       freshly-built tool stack with materials written; we run the stat rebuild here so
     *       the output preview carries the real durability / attack numbers.</li>
     * </ul>
     */
    void refreshOutput() {
        List<ItemStack> inputs = snapshotInputs();
        // Modify path: built tool in slot 0 takes priority over the build path. A built tool's
        // item slot prevents the inputs from being read as part stacks anyway — short-circuit
        // explicitly so the legacy "drop your tool in slot 0 to modify it" gesture still
        // produces an output preview.
        if (inputs.get(0).getItem() instanceof ToolCore) {
            handler.setStackInSlot(OUTPUT_SLOT, inputs.get(0).copy());
            return;
        }
        Map<String, ToolCore> defIdToTool = candidateMap();
        Optional<ItemStack> built = ToolStationLogic.tryBuild(inputs, defIdToTool.values().stream().map(t -> t.definition), def -> defIdToTool.get(def.id()));
        if (built.isEmpty()) {
            handler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        ItemStack output = built.get();
        // Stat rebuild is server-only (the materials datapack registry isn't on the client
        // side). We're inside an onContentsChanged callback which only fires server-side
        // through the menu / hopper insertion paths; defensive level / server check below keeps
        // the off-thread case clean.
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            ToolCore tool = (ToolCore) output.getItem();
            slimeknights.sconstruct.port1211.tools.ToolHelper.rebuildStats(output, level.getServer(), tool.definition);
        }
        handler.setStackInSlot(OUTPUT_SLOT, output);
    }

    /**
     * Snapshot of the 6 input slots in positional order. Returned as an unmodifiable view so
     * callers can't mutate the BE's backing handler through the snapshot.
     */
    List<ItemStack> snapshotInputs() {
        List<ItemStack> inputs = new ArrayList<>(INPUT_SLOTS);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            inputs.add(handler.getStackInSlot(i));
        }
        return List.copyOf(inputs);
    }

    /**
     * Map of accepted-definition id → its registered {@link ToolCore} item. Walks
     * {@link ToolItems#ALL_TOOLS} and includes only items whose definition id is in the
     * accepted roster ({@link ToolDefinition#ALL_BASIC} on the base, {@link ToolDefinition#ALL_ADVANCED}
     * on the forge). The map shape gives the {@code tryBuild} resolver an O(1) lookup back
     * from a matched definition to its tool item.
     */
    Map<String, ToolCore> candidateMap() {
        List<ToolDefinition> roster = acceptsAdvancedTools() ? ToolDefinition.ALL_ADVANCED : ToolDefinition.ALL_BASIC;
        Map<String, ToolCore> result = new HashMap<>();
        for (ToolDefinition def : roster) {
            result.put(def.id(), null);
        }
        Map<String, ToolCore> filtered = new HashMap<>();
        for (var item : ToolItems.ALL_TOOLS) {
            ToolCore tool = (ToolCore) item.get();
            if (result.containsKey(tool.definition.id())) {
                filtered.put(tool.definition.id(), tool);
            }
        }
        return filtered;
    }

    /**
     * Consume the build inputs after the player takes the output. Decrements every populated
     * input slot by one; the BE's {@link #refreshOutput} re-fires through the handler callback
     * and either re-stamps the output (if every input still has stock) or clears it.
     *
     * <p>Modify path uses {@link #consumeModifyInput} instead so slot 0 (the input tool) is
     * fully removed rather than decremented to a half-count tool stack.
     */
    public void consumeBuildInputs() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack slot = handler.getStackInSlot(i);
            if (!slot.isEmpty()) {
                slot.shrink(1);
                handler.setStackInSlot(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
            }
        }
    }

    /**
     * Consume the modify-path input — clears slot 0 (the input tool) outright; modifier-slot
     * consumption is a follow-up ticket. TODO(SMTCON-93 follow-up): once the modifier-item
     * registry lands, decrement slots 1..5 here as each contributes one application.
     */
    public void consumeModifyInput() {
        handler.setStackInSlot(0, ItemStack.EMPTY);
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
        return Component.translatable("container.sconstruct.tool_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        Objects.requireNonNull(playerInventory, "playerInventory");
        Objects.requireNonNull(player, "player");
        return new ToolStationMenu(containerId, playerInventory, this);
    }
}
