# Ban Dream Architecture

This document is workspace-only and is not packaged into the mod jar.

## Runtime flow

1. Commands create `BanEntry` rules or pending actions.
2. `BanRules` validates, sorts, indexes, and writes rules to `config/bandream/bans.json`.
3. `RuleIndex` provides type-and-ID lookups without scanning the full rule list.
4. `BanEvents` handles item, entity, effect, enchantment, and dimension enforcement.
5. `BanStickEvents` handles Ban Stick target selection and confirmation prompts.
6. `BanEnforcement` applies inventory, effect, dimension, and entity cleanup.

## Rule storage

The JSON file stores two top-level collections:

- `bans`: blocked content rules.
- `playerWhitelist`: players exempt from player-facing restrictions.

Writes use a temporary file followed by an atomic move when the filesystem supports it.

## Scope matching

Global rules match every dimension. Dimension rules match only the exact dimension ID.
Player whitelist entries match by UUID first and then by case-insensitive player name.

## Enforcement boundaries

Player whitelist affects:

- inventory item deletion;
- use and interaction removal;
- enchantment cleanup;
- mob effect cleanup;
- dimension travel and forced dimension escape.

Entity spawning and structure generation remain world-level rules.

## GUI model

`BanListMenu` exposes a read-only 5-row container. The last row contains page and close
controls. `BanListScreen` renders a custom container-style panel and delegates item
tooltips to the vanilla screen implementation.

## Optional JEI integration

The common code emits only hidden item IDs. The client integration is compiled against
small compile-only API descriptors and uses reflection at runtime, so JEI is not a hard
dependency.
