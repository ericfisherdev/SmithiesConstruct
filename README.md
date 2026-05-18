# Smithies' Construct

A NeoForge **1.21.1** port of [Tinkers' Construct](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct) (originally by SlimeKnights), rebuilt from the mod's 1.12.2 release. Active port code lives under `src/main/java/slimeknights/sconstruct/port1211/` (mod id `sconstruct`); the legacy 1.12 codebase is preserved in-repo as frozen reference.

> ⚠️ **Worlds and inventories from the 1.12 release of this mod cannot be migrated.**
> The port moved tool data from raw NBT to vanilla `DataComponents` and materials to datapack
> registries — the on-disk formats are not compatible. Save your old 1.12 world separately and
> start fresh on 1.21.1. Loading a 1.12 save with this mod will not carry over Tinkers' tools,
> materials, or smeltery contents.

Modify all the things, then do it again! Melt down any metals you find.

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | NeoForge 21.1.95 or newer (`[21.1.0,22)`) |
| Java | 21 |
| Patchouli | required — ships the in-game manual |

## In-game documentation

Mod content — materials, tools, the smeltery, modifiers — is documented in the **"Materials and You"**
Patchouli book (`tconstruct:materialsandyou`), craftable in-game. The book is the canonical content
reference; this README only covers installation and building from source.

## Installing

1. Install [NeoForge 21.1.95+](https://neoforged.net/) for Minecraft 1.21.1.
2. Ensure a Java 21 runtime is selected in your launcher.
3. Drop the Smithies' Construct jar and [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli)
   into your `mods/` folder.

## Building from source

This is a NeoForge ModDevGradle project. Use the Gradle wrapper from the repository root:

* Build the mod jar: `./gradlew build` → output in `build/libs/`.
* Run the dev client: `./gradlew runClient`.
* Run the dev server: `./gradlew runServer`.
* Run the GameTest suite: `./gradlew runGameTestServer`.
* Run the unit tests: `./gradlew test`.

A Java 21 JDK is required to build.

## IMC

Tinkers' Construct supports several IMC messages so other mods can integrate themselves. The upstream
[Wiki IMC page](https://github.com/SlimeKnights/TinkersConstruct/wiki/IMC) describes them; anything not
expressible via IMC integrates through the `library` API package.

## Credits

Smithies' Construct is a port of **Tinkers' Construct**, created by the **SlimeKnights** team
(mDiyo, boni, and contributors). The original 1.12.2 codebase this port is built from remains
the work of its authors — all credit for the mod's design and content belongs to them. This
repository ports that work to NeoForge 1.21.1.

## Licenses

Code, textures, and binaries are licensed under the [MIT License](https://tldrlegal.com/license/mit-license).
You are allowed to use the mod in your modpack. Modpacks that include this mod are responsible
for their own user support queries.

Any alternate licenses are noted where appropriate.
