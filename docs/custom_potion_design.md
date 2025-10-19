# Custom Potion System Design

## Vision
- Reimagine potions as reusable tools that reward exploration while keeping early-game options accessible.
- Provide clear upgrade paths from overworld-crafted brews to nether-enhanced variants that justify the danger of nether expeditions.
- Replace opaque vanilla potion behavior with configurable systems that integrate tightly with existing managers (e.g., `TablistManager`, `ItemRegistry`).

## Core Mechanics
- **Reusable items:** Each potion is a durable custom item (likely reskinned tools) that auto-drinks on right click and never returns glass bottles.
- **Durability as charges:** Item durability tracks remaining uses. Every use reduces durability; when it reaches 0 the item breaks.
- **Instant effects:** On use, apply the configured potion effect immediately, playing custom audio/particle feedback.
- **Custom effect routing:** Route effect logic through a bespoke handler so we can override vanilla potion application and feed data into UI (tab list, HUD).

## Progression: Overworld vs Nether
- **Overworld brews:** Common ingredients, weaker effects (lower amplifier/duration), higher durability (more charges). Serves early- to mid-game sustain.
- **Nether brews:** Require nether-exclusive ingredients, grant stronger/longer effects, but with lower durability to highlight potency over longevity.
- **Recipe pairing:** Every potion species defines two recipes (overworld + nether) with shared output item but different metadata for stats.
- **Example:** Swiftness I (Overworld) = Honey Bottle + Sugar; Swiftness II (Nether) = Nether Wart + Weeping Vines.
- **Balance knobs:** Charges, amplifier, duration, cooldown between uses, and potential side effects can all differ per dimension tier.

## Brewing & Crafting Flow
- **Custom GUI:** Build an intuitive brewing workstation UI that replaces vanilla brewing stands. Should visualize ingredient slots, output preview, and charges/effect summary.
- **Ingredient sourcing:** Ensure every nether-exclusive item features in at least one potion/culinary recipe to maintain exploration value.
- **Unlock cadence:** Consider gating nether recipes behind discoveries, quests, or advancement triggers to pace power spikes.
- **Automation hooks:** Decide whether hoppers/redstone can interact with the station or if brewing remains manual.

## Itemization & Presentation
- **Resource pack integration:** Assign unique model overrides per potion variant (dimension, strength, special types) so players can identify them at a glance.
- **Lore & tooltips:** Use lore lines to communicate effect strength, duration, remaining charges, and cooldown information.
- **Compatibility touchpoints:** Reuse existing registry helpers (e.g., `ItemRegistry`, `HeirloomManager`) to avoid duplicating item creation flows.

## Effect Catalog (Initial Targets)
- Baseline: Swiftness, Strength, Regeneration, Fire Resistance, Night Vision, Water Breathing.
- Utility additions: Splash Potion of Extinguishing (fire put-out), Potion of Oxygen Recovery (ties into mining oxygen), maybe Potion of Stability (reduces knockback), Potion of Clarity (temporary night-vision + anti-withering).
- For each effect define: overworld & nether recipes, amplifier/duration, charges, cooldown, side-effects (if any).

## UX Enhancements
- **Tablist integration:** Extend `TablistManager` to show active potion effects with remaining time/charges, replacing vanilla HUD icons.
- **Feedback loops:** Configure custom sounds/particles per potion tier; possibly scale intensity with amplifier.
- **Fail states:** Handle cases where players try to use a depleted potion or do not meet requirements (e.g., cooldown still active).

## Technical Implementation Notes
- Define base potion item class under `utils` (e.g., `CustomPotionItem`) handling durability, effect application, and serialization.
- Command extensions for giving/testing potions should live under `commands` for admin tooling.
- Persist remaining charges/effect tier using NBT or persistent data containers to survive restarts.
- Register brewing recipes via custom recipe manager; ensure they load from config for easy tweaking.
- Replace vanilla potion consumption by intercepting relevant events (e.g., `PlayerInteractEvent`, `PlayerItemConsumeEvent`).

## Balance Considerations
- Establish target playtime to first nether potion; adjust overworld recipe power so nether upgrades feel meaningful.
- Analyze stacking: prevent players from chaining multiple potions too quickly (global/shared cooldowns or diminishing returns).
- Monitor PvP impact; nether variants should feel powerful but not oppressive.
- Consider integrating potion crafting with other sub-systems (e.g., nether rebalance, custom mobs dropping ingredients).

## Open Questions & Next Steps
- Where should the custom brewing workstation reside in-world (standalone block, station in settlements, etc.)?
- Do we need potion-specific achievements/progression tracking?
- How will we surface recipe discovery (in-game book, quests, NPCs)?
- Should splash/area variants share the same durability model or use ammo-style charges?
- Next tasks: flesh out recipe lists per potion, design GUI wireframes, prototype base `CustomPotionItem`, assess nether terrain tweaks to supply ingredients.
