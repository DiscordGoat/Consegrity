# Adding Schematics to Thaw

This document explains, end to end, how Thaw loads and pastes WorldEdit schematics at runtime, how those schematics are packaged into the plugin jar, and how you can add new structures or reuse the approach in other projects.

The implementation uses WorldEdit’s Clipboard API to paste `.schem` files that are bundled under `src/main/resources/schematics/` at build time. Thaw then triggers loot population for certain structure families (e.g., bungalows, capsules) after placement.

## Prerequisites

- Server: Paper/Spigot 1.21.x.
- Runtime dependency: WorldEdit (server plugin) installed. Thaw references the WorldEdit API with `scope: provided`, so the WorldEdit plugin must be present at runtime.
- Build: Maven (Java 21). Schematics are included as resources during packaging.

Tip (plugin.yml): In projects that always require WorldEdit at runtime, declare a dependency to ensure load order (soft dependency if optional):

```yml
depend: [WorldEdit]
# or
softdepend: [WorldEdit]
```

## Where Schematics Live

- Put your files in `src/main/resources/schematics/`.
- File format: use WorldEdit’s modern `.schem` format (recommended). Thaw’s loader currently searches for `schematics/<name>.schem`.
  - The Maven build is configured to include both `*.schem` and `*.schematic`, but the runtime loader defaults to `.schem`. If you need legacy `.schematic`, update the loader accordingly.

Examples (from this repo):

- `src/main/resources/schematics/stronghold.schem`
- `src/main/resources/schematics/fire.schem`
- `src/main/resources/schematics/brown.schem`

## How Thaw Pastes Schematics

Core class: `SchemManager`.

- Loads classpath resource `schematics/<name>.schem` via `JavaPlugin#getResource`.
- Uses WorldEdit’s `ClipboardFormats.findByAlias(ext)` to pick a reader and parse a `Clipboard`.
- Creates an `EditSession` for the target world and builds a paste operation anchored at the provided Bukkit `Location` (converted to a BlockVector).
- Runs the operation synchronously and flushes the session.
- By default, `ignoreAirBlocks(false)` is used, so air in the schematic overwrites existing blocks.

Pasting anchor and offset

- The paste location corresponds to the schematic’s internal origin. When you save a schematic in WorldEdit, that origin is defined by where and how you copied it.
- To control how a structure lines up when pasting, set the origin at save time (see “Preparing Schematics” below).

Threading and performance

- Pasting occurs on the main thread. For large structures, consider chunk-gating or FAWE/async strategies in other projects. For small/medium structures this approach is typical and safe.

## Build and Packaging Details

Thaw’s `pom.xml` ensures `.schem`/`.schematic` are included verbatim (no filtering):

```xml
<resources>
  <resource>
    <directory>src/main/resources</directory>
    <filtering>true</filtering>
    <excludes>
      <exclude>**/*.schem</exclude>
      <exclude>**/*.schematic</exclude>
    </excludes>
  </resource>
  <resource>
    <directory>src/main/resources</directory>
    <filtering>false</filtering>
    <includes>
      <include>**/*.schem</include>
      <include>**/*.schematic</include>
    </includes>
  </resource>
</resources>
```

This ensures binary schematic files are not altered by resource filtering and are packaged in the final jar under `/schematics/`.

## Preparing Schematics (WorldEdit)

Recommended flow to save a `.schem` that pastes correctly at a target block:

1. Build your structure in-game.
2. Stand at the block you want to serve as the paste anchor (origin). Many teams choose the structure’s “bottom center” or “entry” block.
3. Select the region (`//pos1`, `//pos2` or `//wand` and click corners).
4. Copy the selection with origin at your feet: `//copy` (your current position becomes the schematic origin).
5. Save as schem: `//schem save <name>` (e.g., `//schem save fire`).
6. Exported files are typically in `<worldedit/schematics>` on disk. Copy the resulting `<name>.schem` into `src/main/resources/schematics/`.

Notes

- If you need air to NOT overwrite existing blocks, you can change the code path to `.ignoreAirBlocks(true)`.
- If you need rotation/mirroring at paste time, WorldEdit’s `ClipboardHolder` supports transforms (not enabled in Thaw’s default paste flow).

## Using the Test Command

Thaw provides a dev command to validate schematics quickly:

- `/testschem <name>` pastes `schematics/<name>.schem` at your current location.
- Make sure your file naming matches exactly (Thaw lowercases the input in the command).

This is helpful to iterate on origin/offset correctness before enabling automatic generation.

## Automatic Spawning in Thaw

Thaw pastes specific families of structures during world events and scheduled tasks, then may populate nearby containers with loot.

- Bungalows
  - Thaw chooses a random bungalow name from an internal list and pastes it at queued locations.
  - After pasting, a short delay is used before populating nearby chests/furnaces to ensure tile entities exist.
  - To add new bungalow types: place the `.schem`, add the base name to the bungalow list, and (optionally) add a loot table mapping in `BungalowLootManager`.

- Time Capsules
  - Capsule names are registered in `CapsuleRegistry` and selected randomly.
  - Paste occurs underground at validated locations; nearby chests receive type-specific loot shortly after paste.
  - To add a new capsule: place the `.schem`, call `CapsuleRegistry.register("<name>")`, and add its loot population rules to `CapsuleLootManager`.

- CTM (Capture/Target Monument)
  - Pasted once per world/session when available locations are queued, followed by a `GenerateCTMEvent` so other systems can react.

Validation windows and placement safety

- For capsules (and similar underground spawns), Thaw validates locations (e.g., checks air at target, stone above, nearby cavities) to improve placement quality. Adjust or replicate this logic in other projects as needed.

## Adding a New Structure Type (Step-by-Step)

1. Create and export your schematic as described above.
2. Copy `<name>.schem` into `src/main/resources/schematics/`.
3. Reference it by base name (no extension) in code via `SchemManager.placeStructure("<name>", location)`.
4. If this structure needs loot:
   - Implement a loot populator (see `BungalowLootManager`/`CapsuleLootManager`).
   - Schedule loot population a few ticks after paste.
5. If the structure participates in a random pool: register the name in the corresponding list/registry.
6. Build with `mvn clean package` and (optionally) deploy the jar to your server’s `plugins/` directory.

## Reusing the Approach in Other Projects

- Copy or adapt `SchemManager` to your plugin, update the package, and ensure a WorldEdit dependency is available at runtime.
- Add the same Maven resource configuration to preserve binary schematics.
- For dev/testing convenience, wire a simple command like `/testschem <name>` that invokes your manager at the player’s location.
- If you need more control:
  - Toggle `ignoreAirBlocks(true)` to avoid terrain deletion.
  - Add transforms (rotation/mirror) using `ClipboardHolder`.
  - Use chunk-aware scheduling or FAWE for large schematics and performance-critical deployments.

## Troubleshooting

- “Schematic not found”
  - Ensure the file is at `src/main/resources/schematics/<name>.schem` and included in the final jar.
  - Confirm you’re using the base name without extension in calls/commands.

- “Unrecognized schematic format”
  - Use modern `.schem` files saved by current WorldEdit. If using legacy `.schematic`, update the loader to look for that extension or re-export in `.schem`.

- NoClassDefFoundError for WorldEdit classes
  - Install the WorldEdit plugin on the server and declare `depend`/`softdepend` in `plugin.yml` if needed.

- Loot not appearing
  - Ensure you schedule loot population a short time after placement, and verify container discovery radius/limits match your schematic layout.

## Quick Reference (API Surface)

- Manager: `SchemManager#placeStructure(String name, org.bukkit.Location loc)`
- Files: `src/main/resources/schematics/<name>.schem`
- Test: `/testschem <name>`
- Registries:
  - Bungalows: add to the internal list (see Thaw’s main class)
  - Capsules: `CapsuleRegistry.register("<name>")`
  - CTM: single `ctm.schem` pasted once when available

With these pieces, you can confidently add new structures to Thaw or port the pattern to your own plugins.

