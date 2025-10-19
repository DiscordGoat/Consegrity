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

# Brewing & Crafting Flow
- **Custom GUI:** Build an intuitive brewing workstation UI that replaces vanilla brewing stands. Should visualize ingredient slots, output preview, and charges/effect summary.
- **Ingredient sourcing:** Ensure every nether-exclusive item features in at least one potion/culinary recipe to maintain exploration value.
- **Unlock cadence:** Consider gating nether recipes behind discoveries, quests, or advancement triggers to pace power spikes.
- **Automation hooks:** Decide whether hoppers/redstone can interact with the station or if brewing remains manual.

## UI Concepts
- **Potion-first catalog (suggested):** Players browse a categorized list of potions, select one, then slot ingredients. Exiting preserves ingredients; explicit back-button cancels and refunds/drops items. Helps new players focus on known brews while leaving space for mastery via color-unlocked states.
- **Ingredient-driven grid:** Present ingredient slots up front with contextual recipe hints that unlock as players discover components. Output preview updates live, ideal for free-form experimentation but risks information overload without guardrails.
- **Quest/recipe book hybrid:** Tie UI to a recipe book panel showing discovered recipes, requirements, and potential upgrades. Keeps workstation clean while giving players a reference journal that scales with progression.
- **Modular workbench:** Split UI into tabs (brewing, infusion, enhancement). Each tab highlights a specific step (base potion, nether infusion, charge tuning). Supports future systems like potion augmentation without redesigning the interface.
- **NPC-assisted brewing:** Embed UI in dialogue with an alchemist NPC who walks players through ingredient choices. Facilitates tutorialization and allows gating advanced recipes behind reputation/quests.
- **Minimal quick-brew wheel:** Provide a radial or list-based quick select for frequently brewed potions, with recent recipes auto-filled. Useful for late-game players who want speed over depth.

## Itemization & Presentation
- **Resource pack integration:** Assign unique model overrides per potion variant (dimension, strength, special types) so players can identify them at a glance.
- **Lore & tooltips:** Use lore lines to communicate effect strength, duration, remaining charges, and cooldown information.
- **Compatibility touchpoints:** Reuse existing registry helpers (e.g., `ItemRegistry`, `HeirloomManager`) to avoid duplicating item creation flows.

## Enchantment Upgrades
- **Enchanted tiers:** Potions support an `Enchanted I-III` modifier applied via enchanting table.
- **Stacking rules:** Base overworld/nether potency stacks with enchanted tiers (e.g., nether potency II + Enchanted III = potency III).
- **Tier effects:**  
  - `Enchanted I`: doubles remaining charges/durability.  
  - `Enchanted II`: inherits Tier I bonus and doubles effect duration.  
  - `Enchanted III`: inherits Tier II bonuses and increases potency by +1 level (capped by safety rules per effect).
- **Progression pacing:** Gate higher tiers behind late-game resources (e.g., lapis, enchanted blaze rods) to keep nether exploration relevant.
- **Balance levers:** Adjust enchant cost, success rate, and compatibility (e.g., limit certain potions to max tier II) to prevent runaway stacking.
- **UI integration:** Enchanted state should surface in potion catalog (color shift, icon badge) and in lore lines for clarity.

## Effect Catalog (Initial Targets)
- Baseline: Swiftness, Slowness, Strength, Regeneration, Fire Resistance, Night Vision, Water Breathing.
- Swiftness prototype: reapplies standard Speed every 2s for the effect owner (players and mobs) so uptime is maintained without stacking.
- Slowness prototype: reapplies Slowness every 2s; potency doubles against non-player mobs to keep the effect threatening.
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
- Add a dedicated `CustomPotionEffectManager` to persist effect metadata and feed the tablist with active potion summaries while players are online.
- Route right-click and thrown potion behaviour through a usage listener so charges, sounds, particles, and splash debug output remain consistent.
- Implement enchantment hooks either via vanilla enchanting tables (custom weighting) or a bespoke altar to apply Enchanted I-III upgrades, enforcing stacking rules and caps centrally.

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
