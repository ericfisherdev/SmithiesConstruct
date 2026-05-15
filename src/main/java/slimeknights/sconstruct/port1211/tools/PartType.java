package slimeknights.sconstruct.port1211.tools;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import com.mojang.serialization.Codec;

import slimeknights.sconstruct.port1211.SConstruct;

import io.netty.buffer.ByteBuf;

/**
 * Atomic taxonomy of every tool part the mod can build a tool from — the closed set of
 * slot-types a {@code ToolDefinition} can require and a {@code MaterialItem} can advertise.
 *
 * <p>The vanilla 1.20.5+ {@link Codec} / {@link StreamCodec} pair is derived from the
 * {@link StringRepresentable} {@link #id()} so a part type round-trips by name in JSON / NBT
 * (datapack-readable) and over the network (registry-free, no integer ordinal coupling). The
 * lang key built from {@link #id()} resolves to a human label like {@code "Pickaxe Head"} and
 * is consumed by {@link slimeknights.sconstruct.port1211.tools.item.MaterialItem#getName} when
 * formatting the per-stack name {@code "<material> <part>"}.
 *
 * <p>The order below mirrors the legacy 1.12 part roster grouping (basic heads, tough variants,
 * ranged) and is part of the public surface — adding a value at the end is non-breaking, but
 * reordering changes the {@link Enum#ordinal()} and would break any persistence that ever
 * relied on it. Persistence here goes through the string codec, so reordering is still
 * forward-compatible at the data layer; ordinal coupling is forbidden by contract.
 */
public enum PartType implements StringRepresentable {

    PICKHEAD("pickhead"), AXEHEAD("axehead"), SHOVELHEAD("shovelhead"), SWORDBLADE("swordblade"), BROADAXEHEAD("broadaxehead"), BROADBLADE("broadblade"), HAMMERHEAD("hammerhead"), HANDLE(
            "handle"), BINDING("binding"), TOUGHHANDLE("toughhandle"), TOUGHBINDING("toughbinding"), BOWLIMB(
                    "bowlimb"), BOWSTRING("bowstring"), ARROWSHAFT("arrowshaft"), ARROW_HEAD("arrow_head"), FLETCHING("fletching"), WIDEGUARD("wideguard"), LARGEPLATE("largeplate");

    /** JSON / NBT round-trip codec; encodes / decodes by {@link #id()}. */
    public static final Codec<PartType> CODEC = StringRepresentable.fromEnum(PartType::values);

    /**
     * Network codec; encodes / decodes by {@link #id()}. Routed through
     * {@link ByteBufCodecs#STRING_UTF8} rather than the registry-id stream codec because part
     * types are not a registry — the enum is a pure value taxonomy and the wire format must
     * survive registry-less contexts (datagen, unit tests, modlist diff displays).
     */
    public static final StreamCodec<ByteBuf, PartType> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(PartType::byId, PartType::id);

    private final String id;

    PartType(String id) {
        this.id = id;
    }

    /**
     * Stable lowercase identifier used as the codec serialised form and as the lang-key suffix.
     * Hyphen-free so it stays a valid resource-path component; the {@link #ARROW_HEAD} value
     * keeps its underscore so the legacy {@code arrow_head} part id round-trips byte-for-byte.
     */
    public String id() {
        return id;
    }

    /**
     * Translation key for the human-readable part label, e.g. {@code part.sconstruct.pickhead}.
     * Concatenated rather than {@code String.format}-built to keep the static call sites in
     * {@code MaterialItem.getName} allocation-light during tooltip rendering.
     */
    public String translationKey() {
        return "part." + SConstruct.MOD_ID + "." + id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /**
     * Lookup helper used by {@link #STREAM_CODEC}. Throws {@link IllegalArgumentException} on an
     * unknown id rather than returning null — a corrupt stream payload is a protocol error, not
     * a value the caller should be expected to handle.
     */
    public static PartType byId(String id) {
        for (PartType value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown PartType id: " + id);
    }
}
