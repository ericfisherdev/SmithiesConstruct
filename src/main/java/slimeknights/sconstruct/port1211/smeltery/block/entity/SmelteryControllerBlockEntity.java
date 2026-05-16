package slimeknights.sconstruct.port1211.smeltery.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import slimeknights.sconstruct.port1211.smeltery.SmelteryComponents;

/**
 * Block entity for the smeltery controller (SMTCON-112). This is a deliberate empty skeleton:
 * the registration ticket only needs the controller block to carry a BE so that later tickets
 * have a typed home to attach behaviour to, without churning the block-entity-type registration.
 *
 * <p>Intentionally absent and arriving in later tickets:
 * <ul>
 *   <li><b>SMTCON-114</b> — the smeltery state machine and the structure-validation pass that
 *       scans the seared-block shell, stamps every {@link SmelteryComponentBlockEntity} with this
 *       controller's position, and tracks melt progress / fuel.</li>
 *   <li><b>SMTCON-125</b> — the controller GUI: this BE will implement
 *       {@link net.minecraft.world.MenuProvider} so the right-click hook in
 *       {@code SmelteryControllerBlock#useWithoutItem} can call {@code player.openMenu(...)}.</li>
 * </ul>
 *
 * <p>Until then it holds no fields and no logic — only the constructor, which reads its
 * registered type from {@link SmelteryComponents#SMELTERY_CONTROLLER_BE} the same way
 * {@code PatternChestBlockEntity} reads its type from {@code PatternChestRegistry}.
 */
public class SmelteryControllerBlockEntity extends BlockEntity {

    /**
     * @param pos   the block position, forwarded to {@link BlockEntity}
     * @param state the placed block state, forwarded to {@link BlockEntity}
     */
    public SmelteryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryComponents.SMELTERY_CONTROLLER_BE.get(), pos, state);
    }
}
