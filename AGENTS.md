# Repository Guidelines

## Project Structure & Module Organization
- Core source code lives in `src/main/java/goat/projectLinearity`. Commands sit under `commands`, gameplay managers inside `libs`, and world generation in `world` with subpackages for structures and sectors.
- Resources such as `plugin.yml` and schematics are in `src/main/resources`, mirroring Bukkit’s expected layout.
- Legacy decompiled references remain inside `cfr-src`; treat them as read-only context.

## Build, Test, and Development Commands
- `mvn -q -DskipTests compile` builds the plugin JAR; ensure `JAVA_HOME` points to a supported JDK before running.
- `mvn package` produces the distributable JAR in `target/`. Delete old artifacts before packaging to avoid stale copies.
- No automated test suite is currently hooked up; manual playtesting on a Paper/Bukkit server is the primary validation path.

## Coding Style & Naming Conventions
- Java sources use 4-space indentation, braces on the same line, and descriptive camelCase identifiers.
- Classes belonging to the plugin are grouped by feature; add new managers under `libs` and related commands in `commands`.
- Favor concise comments that explain intent over restating code. Preserve existing lore/message formatting (e.g., `ChatColor` usage).

## Testing Guidelines
- Manual verification: spin up a dev server, copy the built JAR to `plugins/`, and use in-game commands (`/i`, `/setgild`, custom anvil UI) to exercise new features.
- If you add automated tests later, colocate them under `src/test/java` following the same package structure.

## Commit & Pull Request Guidelines
- Use imperative, descriptive commit messages (e.g., “Add custom anvil manager”).
- Pull requests should summarize the feature or fix, list testing performed, and reference any relevant tickets. Include screenshots or console logs when UI or command behavior changes.

## Agent-Specific Notes
- Avoid touching IDE metadata under `.idea/` unless intentionally updating project settings.
- Respect existing registries (e.g., `ItemRegistry`, `HeirloomManager`) and reuse helper methods instead of duplicating logic.
