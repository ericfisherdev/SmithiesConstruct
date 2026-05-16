package slimeknights.sconstruct.port1211.smeltery.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import slimeknights.sconstruct.port1211.smeltery.block.entity.SmelteryComponentBlockEntity;

/**
 * Abstract base for the five non-controller smeltery component blocks — the tank IO, tank in,
 * gauge, drain, and chute (SMTCON-112). At this registration layer the five components are
 * behaviourally identical: each is a horizontally-directional {@link BaseEntityBlock} carrying a
 * {@link SmelteryComponentBlockEntity} (the controller-position proxy). The per-component
 * behaviour — tank capacities, the drain's {@code IFluidHandler} output spout, the chute's item
 * input — lands in SMTCON-116/118 and will be added by overriding capability/interaction hooks
 * on the concrete subclasses, not here.
 *
 * <p>This class is {@code abstract} rather than taking a {@code BlockEntityType} supplier in its
 * constructor for one concrete reason: {@link BaseEntityBlock} mandates a {@link com.mojang.serialization.MapCodec}
 * via {@link #codec()}, and the only ergonomic way to build one is {@code simpleCodec(Ctor::new)},
 * which requires a {@code (Properties)}-only constructor. A constructor carrying an extra
 * supplier argument breaks {@code simpleCodec}. Splitting into five thin concrete subclasses —
 * each with a {@code (Properties)} constructor, its own {@code CODEC = simpleCodec(Self::new)},
 * and a one-line {@link #beType()} override — keeps the codec contract satisfied per subclass
 * while all the shared placement / rotation / block-entity wiring stays here exactly once.
 *
 * <p>Subclasses supply only two things: their {@link #codec()} and their {@link #beType()}. The
 * directional state, placement orientation, world-edit rotate / mirror handling, and BE creation
 * are all final-shaped here.
 */
public abstract class SmelteryComponentBlock extends BaseEntityBlock {

    /**
     * Horizontal facing of the component, reused from vanilla so structure / world-edit tooling
     * recognises it. {@code BlockStateProperties.HORIZONTAL_FACING} is the same property
     * {@code HorizontalDirectionalBlock.FACING} exposes — declared directly because
     * {@link BaseEntityBlock} does not extend {@code HorizontalDirectionalBlock}.
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected SmelteryComponentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * The registered {@link BlockEntityType} this component block backs. Supplied by each
     * concrete subclass — one of the five {@code SmelteryComponents} component BE-type holders —
     * so {@link #newBlockEntity} can stamp the shared {@link SmelteryComponentBlockEntity} with
     * the type matching this block.
     */
    protected abstract BlockEntityType<?> beType();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Face the component toward the player: getHorizontalDirection() is the way the player is
        // looking, so the opposite is the block's front facing them — the standard furnace-style
        // placement orientation.
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // One BE class backs all five component types — pass this subclass's registered type in
        // so the constructed BE is bound to the matching BlockEntityType.
        return new SmelteryComponentBlockEntity(beType(), pos, state);
    }
}
