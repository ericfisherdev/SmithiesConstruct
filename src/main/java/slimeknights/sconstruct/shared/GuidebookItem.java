package slimeknights.sconstruct.shared;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import vazkii.patchouli.api.PatchouliAPI;

/**
 * The in-game guidebook item — right-clicking it opens a Patchouli book GUI. SMTCON-158 wires
 * this for the "Materials and You" book; the same class backs any future book by passing a
 * different {@code bookId} at registration.
 *
 * <p>{@link PatchouliAPI#openBookGUI} is a server-initiated call (it needs a {@link ServerPlayer}
 * and pushes the open-book packet to that player's client), so {@link #use} only acts on the
 * logical server and reports a side-aware success that still swings the player's arm on the
 * client.
 */
public class GuidebookItem extends Item {

    /** The Patchouli book this copy opens — {@code data/<namespace>/patchouli_books/<path>/}. */
    private final ResourceLocation bookId;

    public GuidebookItem(Properties properties, ResourceLocation bookId) {
        super(properties.stacksTo(1));
        this.bookId = Objects.requireNonNull(bookId, "bookId");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            PatchouliAPI.get().openBookGUI(serverPlayer, bookId);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
