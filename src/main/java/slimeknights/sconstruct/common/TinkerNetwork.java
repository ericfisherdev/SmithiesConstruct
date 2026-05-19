package slimeknights.sconstruct.common;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import slimeknights.sconstruct.SConstruct;
import slimeknights.sconstruct.common.network.StencilTablePartPayload;
import slimeknights.sconstruct.common.network.ToolStationActionPayload;
import slimeknights.sconstruct.smeltery.network.SmelteryFluidUpdatePayload;
import slimeknights.sconstruct.smeltery.network.SmelteryFuelUpdatePayload;
import slimeknights.sconstruct.smeltery.network.SmelteryMeltingUpdatePayload;
import slimeknights.sconstruct.smeltery.network.SmelteryStructureUpdatePayload;

/**
 * Central registration point for the mod's network payloads. Phase 1 ships only the registrar
 * itself — no payload types are wired yet — so {@link #onRegisterPayloads} just creates a
 * version-{@value #VERSION} {@link PayloadRegistrar} on every boot, proving the
 * {@link RegisterPayloadHandlersEvent} reaches us and giving Phase 2+ pulses a stable spot to
 * call {@code reg.playToClient(...)}/{@code .playToServer(...)} on as their custom packets
 * come online.
 *
 * <p>The {@code @EventBusSubscriber(modid = MOD_ID, bus = MOD)} annotation hooks the static
 * handler below to the mod bus during mod construction; no manual {@code addListener} call is
 * needed from {@link SConstruct}. Versioning the registrar with the string {@code "1"} lets us
 * bump the network protocol later (string "2", "3", …) without breaking older clients on the
 * same major MC version — NeoForge refuses connection when the versions mismatch.
 */
@EventBusSubscriber(modid = SConstruct.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TinkerNetwork {

    /** Payload-registrar protocol version. Bump to "2" before adding breaking packet changes. */
    public static final String VERSION = "1";

    private static final Logger LOGGER = LogUtils.getLogger();

    private TinkerNetwork() {
    }

    /**
     * Mod-bus handler for {@link RegisterPayloadHandlersEvent}. Delegates to {@link #configure}
     * so unit tests can drive the same logic with a mocked event without bringing up FML.
     */
    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        configure(event);
    }

    /**
     * Wire payloads into {@code event}'s registrar. Package-private for tests; production code
     * always reaches it via {@link #onRegisterPayloads}.
     *
     * <p>Phase 1 makes no further registrar calls — the empty body intentionally proves the
     * event reaches this class. Phase 2+ pulses append their {@code .playToClient(...)} /
     * {@code .playToServer(...)} calls here.
     */
    static void configure(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        if (registrar == null) {
            throw new IllegalStateException("PayloadRegistrar from RegisterPayloadHandlersEvent was null — NeoForge contract violated");
        }
        LOGGER.info("SConstruct: network payload registrar version {} created", VERSION);
        // SMTCON-91: client→server cycle-button packet for the Stencil Table.
        registrar.playToServer(StencilTablePartPayload.TYPE, StencilTablePartPayload.STREAM_CODEC, StencilTablePartPayload::handleServer);
        // SMTCON-94: client→server action-button packet for the Tool Station / Tool Forge.
        registrar.playToServer(ToolStationActionPayload.TYPE, ToolStationActionPayload.STREAM_CODEC, ToolStationActionPayload::handleServer);
        // SMTCON-124: server→client smeltery sync packets — tank contents, heat, and assembled
        // shape — pushed to chunk trackers by the controller without resending its full BE NBT.
        registrar.playToClient(SmelteryFluidUpdatePayload.TYPE, SmelteryFluidUpdatePayload.STREAM_CODEC, SmelteryFluidUpdatePayload::handleClient);
        registrar.playToClient(SmelteryFuelUpdatePayload.TYPE, SmelteryFuelUpdatePayload.STREAM_CODEC, SmelteryFuelUpdatePayload::handleClient);
        registrar.playToClient(SmelteryStructureUpdatePayload.TYPE, SmelteryStructureUpdatePayload.STREAM_CODEC, SmelteryStructureUpdatePayload::handleClient);
        registrar.playToClient(SmelteryMeltingUpdatePayload.TYPE, SmelteryMeltingUpdatePayload.STREAM_CODEC, SmelteryMeltingUpdatePayload::handleClient);
    }
}
