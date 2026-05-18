package slimeknights.sconstruct.smeltery.multiblock;

/**
 * The functional smeltery component blocks the {@link SmelteryStructureValidator} catalogues
 * while walking the multiblock shell. Plain seared structural blocks (brick, stone, glass) are
 * <em>not</em> components — they carry no behaviour and need no per-position bookkeeping; only
 * the four blocks below expose a capability or drive the controller and so must be located by
 * position when the smeltery assembles.
 */
public enum ComponentType {
    /** The smeltery controller — the multiblock's brain block. Exactly one per smeltery. */
    CONTROLLER,
    /** A seared tank (IO, in, or gauge) — extra molten-metal storage folded into the shell. */
    TANK,
    /** A seared drain — the output spout exposing the smeltery's {@code IFluidHandler}. */
    DRAIN,
    /** A seared chute — the item input forwarding stacks into the controller's melting slots. */
    CHUTE
}
