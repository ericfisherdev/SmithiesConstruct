package slimeknights.sconstruct.port1211.gadgets.block;

import net.minecraft.util.StringRepresentable;

/**
 * The visible state of a drying rack, exposed as a block-state property so the rack's model
 * swaps as its contents progress:
 *
 * <ul>
 *   <li>{@link #EMPTY} — the rack holds nothing.</li>
 *   <li>{@link #DRYING} — the rack holds an item that is part-way through a drying recipe.</li>
 *   <li>{@link #DONE} — the rack holds an item that is not drying: a finished output, or an
 *       item with no drying recipe simply resting on the rack.</li>
 * </ul>
 */
public enum DryingState implements StringRepresentable {

    EMPTY("empty"), DRYING("drying"), DONE("done");

    private final String name;

    DryingState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
