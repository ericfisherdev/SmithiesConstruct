package slimeknights.sconstruct.port1211.tools.client.model;

import java.util.Map;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import slimeknights.sconstruct.port1211.tools.item.ToolCore;
import slimeknights.sconstruct.port1211.tools.item.ToolItems;

/**
 * Client-side wiring that swaps every registered {@code ToolCore} item's baked model for a
 * {@link ToolBakedModel} so tools render with per-material tinting (SMTCON-105).
 *
 * <p>{@link ModelEvent.ModifyBakingResult} fires once after the model bakery finishes, handing
 * a mutable {@code Map<ModelResourceLocation, BakedModel>}. For each tool in
 * {@link ToolItems#ALL_TOOLS} the handler replaces the inventory-variant entry with a
 * {@link ToolBakedModel} wrapping the JSON-defined base model — the wrapper delegates every
 * standard {@code BakedModel} method to that base and only diverges on the per-stack override
 * resolution.
 *
 * <p>Loaded only on the client side — {@code SConstruct} guards {@link #register(IEventBus)}
 * with a {@code Dist.CLIENT} check so this class never resolves on a dedicated server.
 */
public final class ToolModelEvents {

    private ToolModelEvents() {
    }

    /** Subscribe the {@link ModelEvent.ModifyBakingResult} handler on the mod bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(ToolModelEvents::onModifyBakingResult);
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (DeferredItem<? extends ToolCore> tool : ToolItems.ALL_TOOLS) {
            ModelResourceLocation key = ModelResourceLocation.inventory(tool.getId());
            BakedModel base = models.get(key);
            // A tool with no item-model JSON yet (its assets land in SMTCON-107) has no baking
            // entry — skip it rather than wrapping a null. The instanceof guard keeps the
            // handler idempotent if the event ever fires twice in one bake cycle.
            if (base != null && !(base instanceof ToolBakedModel)) {
                models.put(key, new ToolBakedModel(base));
            }
        }
    }
}
