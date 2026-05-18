# Mantle Dependency Audit

Mantle has no 1.21.x release, so the port cannot depend on it. This document
enumerates every `slimeknights.mantle.*` import found in the legacy 1.12 source
tree (68 unique types across 13 subpackages) and records the disposition for
each one. It is the canonical reference for any porter who hits a Mantle import
in a Phase 2+ pulse — look the import up here before reaching for a workaround.

**Generated from:** `grep -rh '^import slimeknights\.mantle' src/main/java --include='*.java' | grep -v "sconstruct" | sort -u`
**Re-run after each phase merge** to catch drift; this file is hand-maintained
and will go stale if pulses are ported without updating it.

## Legend

| Category | Meaning |
|---|---|
| **Vendored** | A direct replacement now lives under `sconstruct/`. Call sites switch to the replacement class. |
| **Dropped** | No replacement is needed in 1.21.1 — vanilla, NeoForge, or another mod supersedes it. The legacy usage is rewritten against the modern API. |
| **Pending** | Replacement will arrive in a named later phase. Do not vendor speculatively; wait for the owning pulse to land so the surface stays scoped to actual use. |

## Vendored

| Mantle import | Replacement | Notes |
|---|---|---|
| `slimeknights.mantle.pulsar.pulse.Pulse` | `sconstruct.common.pulse.Pulse` | Subsystem interface; lifecycle hooks redesigned for NeoForge event bus instead of Pulsar's annotation scanner |
| `slimeknights.mantle.pulsar.control.PulseManager` | `sconstruct.common.pulse.PulseLoader` | Boots pulses gated by `Config.PULSES.<flag>` |
| `slimeknights.mantle.network.NetworkWrapper` | `sconstruct.common.TinkerNetwork` | Wraps NeoForge `PayloadRegistrar`; no SimpleNetworkWrapper analogue in modern NeoForge |
| `slimeknights.mantle.util.LocUtils` | `sconstruct.lib.util.Util.rl` | One-liner `ResourceLocation` constructor; reused in thousands of legacy call sites |
| (implicit `Lazy<T>` pattern) | `sconstruct.lib.util.Lazy` | Mantle had no public `Lazy`; vendored under the Mantle umbrella because the pattern shows up across pulses |
| (implicit safe-client pattern) | `sconstruct.lib.client.SafeClient` | Replaces `slimeknights.mantle.client.SafeClientAccess`; dist-aware `Minecraft`/`ClientLevel`/`Player` accessors |

## Dropped

| Mantle import | Replaced by | Rationale |
|---|---|---|
| `slimeknights.mantle.pulsar.config.ForgeCFG` | NeoForge `ModConfigSpec` (see `sconstruct.common.config.Config`) | Pulsar's TOML/INI config writer superseded by first-party NeoForge config |
| `slimeknights.mantle.network.AbstractPacket` | NeoForge `CustomPacketPayload` + `StreamCodec` | New network stack is type-safe and codec-driven; the legacy base class has no analogue |
| `slimeknights.mantle.network.AbstractPacketThreadsafe` | NeoForge `CustomPacketPayload.handle(...)` runs on the main thread | Thread-hop is provided by the framework, not the packet base class |
| `slimeknights.mantle.client.book.*` (all 13 types under `book`, `book.data`, `book.data.content`, `book.data.element`, `book.repository`, `book.action`, `book.action.protocol`) | **Patchouli** (Phase 7 integration) | Decision D4 in the project planning notes; the in-house book framework is not being ported |
| `slimeknights.mantle.client.gui.book.GuiBook` | Patchouli | Same as above — book GUI is part of the dropped book stack |
| `slimeknights.mantle.client.gui.book.element.BookElement` | Patchouli | Book element hierarchy is dropped wholesale |
| `slimeknights.mantle.client.gui.book.element.ElementImage` | Patchouli | |
| `slimeknights.mantle.client.gui.book.element.ElementItem` | Patchouli | |
| `slimeknights.mantle.client.gui.book.element.ElementText` | Patchouli | |
| `slimeknights.mantle.client.gui.book.element.SizedBookElement` | Patchouli | |
| `slimeknights.mantle.client.CreativeTab` | Vanilla `CreativeModeTab.builder(...)` | Tabs are a first-party registry in 1.20+; legacy wrapper is unnecessary |
| `slimeknights.mantle.common.GuiHandler` | Vanilla `MenuType` + `MenuScreens.register` | `IGuiHandler` was removed in 1.13; modern menus open via `Player.openMenu` |
| `slimeknights.mantle.common.IInventoryGui` | Vanilla `MenuProvider` | Same migration path as `GuiHandler` |
| `slimeknights.mantle.item.ItemBlockMeta` | One vanilla `Item` per block variant | Item metadata system removed in 1.13; per-variant items are now the norm |
| `slimeknights.mantle.item.ItemMetaDynamic` | Vanilla `DataComponents` | Dynamic per-stack metadata is now expressed as DataComponent values |
| `slimeknights.mantle.item.ItemBlockSlab` | One `Item` per slab variant | Slab metadata fold collapses the same way as `ItemBlockMeta` |
| `slimeknights.mantle.item.ItemArmorTooltip` | Vanilla `Item.appendHoverText` | Tooltip override is now a single overridable method |
| `slimeknights.mantle.item.ItemTooltip` | Vanilla `Item.appendHoverText` | Same as above |
| `slimeknights.mantle.item.ItemEdible` | Vanilla `FoodProperties` builder via `Item.Properties.food(...)` | First-party food API replaces the wrapper |
| `slimeknights.mantle.property.PropertyString` | Vanilla `EnumProperty<E>` over a string-backed enum | `IUnlistedProperty` removed in 1.13 |
| `slimeknights.mantle.property.PropertyUnlistedDirection` | Vanilla `DirectionProperty` (listed) or model data via `ModelData` | Unlisted properties are gone; model state is now `ModelData` |
| `slimeknights.mantle.block.EnumBlock<E>` | One vanilla `Block` per variant, joined by a tag | Metadata-driven enum blocks collapse into per-variant blocks plus a `TagKey<Block>` for grouping (see [`TinkerTags`](../common/TinkerTags.java)) |
| `slimeknights.mantle.block.EnumBlockSlab` | Per-variant `SlabBlock` + tag | Same migration path as `EnumBlock` |
| `slimeknights.mantle.block.EnumBlockConnectedTexture` | Per-variant block + NeoForge connected-texture model | Texture connection is now a model-side concern |
| `slimeknights.mantle.block.BlockConnectedTexture` | NeoForge connected-texture model | Same as above |
| `slimeknights.mantle.block.BlockStairsBase` | Vanilla `StairBlock` | The legacy base predates a usable vanilla stair block; modern vanilla is sufficient |
| `slimeknights.mantle.client.model.BakedSimple` | NeoForge `BakedModelWrapper` + `IUnbakedGeometry` | Mantle's baked-model helpers predate NeoForge's geometry API |
| `slimeknights.mantle.client.model.BakedWrapper` | NeoForge `BakedModelWrapper` | Direct first-party equivalent |
| `slimeknights.mantle.client.model.BakedCompositeModel` | NeoForge composite model + `IUnbakedGeometry` | Composite models are now declarative in JSON |
| `slimeknights.mantle.client.model.TRSRBakedModel` | `Transformation` + `BakedModelWrapper` | Mojang's `Transformation` replaces TRSR |
| `slimeknights.mantle.util.RecipeMatch` | NeoForge `SizedIngredient` + custom `RecipeInput` | Quantity-aware matching is first-party |
| `slimeknights.mantle.util.RecipeMatchRegistry` | Tag-driven recipe inputs | Tags replace the registry-of-matchers pattern |
| `slimeknights.mantle.util.ItemStackList` | `NonNullList.withSize(int, ItemStack.EMPTY)` | Vanilla helper covers the only use case |
| `slimeknights.mantle.util.TagHelper` | Vanilla `CompoundTag` API + `DataComponents` | The 1.12 NBT helpers are obsolete; persistent state migrates to data components |

## Pending

| Mantle import | Target phase | Replacement plan |
|---|---|---|
| `slimeknights.mantle.client.gui.GuiElement` | Phase 6 (smeltery UI) / Phase 9 (rendering) | Vanilla `AbstractWidget` + custom render helpers under `sconstruct.lib.client.gui` if reuse warrants it |
| `slimeknights.mantle.client.gui.GuiElementScalable` | Phase 6 / Phase 9 | Custom widget or 9-slice helper alongside `GuiElement` replacement |
| `slimeknights.mantle.client.gui.GuiModule` | Phase 6 | Vanilla `AbstractContainerScreen` composition |
| `slimeknights.mantle.client.gui.GuiMultiModule` | Phase 6 | Same as `GuiModule` — multi-module screens use composed sub-screens |
| `slimeknights.mantle.client.gui.GuiWidget` | Phase 6 / Phase 9 | Vanilla `AbstractWidget` |
| `slimeknights.mantle.client.gui.GuiWidgetSlider` | Phase 6 | Vanilla `AbstractSliderButton` |
| `slimeknights.mantle.client.gui.GuiWidgetTabs` | Phase 6 | Custom tab widget over `AbstractWidget`; only the smeltery and tool station need it |
| `slimeknights.mantle.inventory.BaseContainer` | Phase 5 (tools) / Phase 6 (smeltery) | Vanilla `AbstractContainerMenu` |
| `slimeknights.mantle.inventory.ContainerMultiModule` | Phase 6 | Composed `AbstractContainerMenu` — evaluate vendoring a small helper if reuse repeats |
| `slimeknights.mantle.inventory.IContainerCraftingCustom` | Phase 5 | Vanilla `CraftingMenu` extension point |
| `slimeknights.mantle.inventory.SlotCraftingCustom` | Phase 5 | Vanilla `ResultSlot` or a small subclass |
| `slimeknights.mantle.inventory.SlotOut` | Phase 5 / Phase 6 | Output-only `Slot` subclass (~5 lines); vendor in `sconstruct.lib.inventory` only if it appears 3+ times |
| `slimeknights.mantle.multiblock.IMasterLogic` | Phase 6 (smeltery) | In-house smeltery multiblock controller; do **not** vendor a generic multiblock framework |
| `slimeknights.mantle.multiblock.IServantLogic` | Phase 6 | Per-servant block-entity protocol on the smeltery controller |
| `slimeknights.mantle.multiblock.MultiServantLogic` | Phase 6 | Same as `IServantLogic` |
| `slimeknights.mantle.tileentity.MantleTileEntity` | Phase 5 / Phase 6 | Vanilla `BlockEntity`; the Mantle base added save/load helpers that the modern API already provides |
| `slimeknights.mantle.tileentity.TileInventory` | Phase 5 / Phase 6 | `BaseContainerBlockEntity` + `IItemHandler` capability |
| `slimeknights.mantle.block.BlockInventory` | Phase 5 / Phase 6 | `BaseEntityBlock` + capability registration on the matching `BlockEntityType` |

## Maintenance

- **When porting a pulse:** scan the pulse's legacy directory for Mantle imports and confirm each one already appears here. If a new import surfaces (e.g. a transitive helper not used elsewhere), add a row before the port lands.
- **When a Pending row's phase lands:** move the row to Vendored or Dropped with the concrete replacement filled in. Do not leave stale "pending" entries — they mislead the next porter.
- **Vendoring budget:** the foundation plan caps `sconstruct.lib.*` at roughly 20 files. Before adding a Vendored row, confirm the helper is referenced in ≥3 call sites; otherwise inline the logic and skip the wrapper.
