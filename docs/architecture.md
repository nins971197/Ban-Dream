# Ban Dream Architecture

This document is workspace-only and is not packaged into the mod jar.

## Runtime flow

1. Commands create `BanEntry` rules or pending actions.
2. `BanRules` validates, sorts, indexes, and writes rules to `config/bandream/bans.json`.
3. `RuleIndex` provides type-and-ID lookups without scanning the full rule list.
4. `BanEvents` handles item, entity, effect, enchantment, and dimension enforcement.
5. `BlockBanEvents` handles block interaction, breaking, and placement rules.
6. `BlockBanSupport` answers block mode and item I/O queries for mixins and events.
7. `BanStickEvents` selects Ban Stick targets and opens server-validated option panels.
8. `BanActionHandler` maps client choices to ban or unban operations.
9. `BanEnforcement` applies inventory, effect, dimension, and entity cleanup.

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

## Block ban enforcement

`INTERACT` keeps placement and breaking available, cancels block right-click use, and
blocks item movement. `FULL` blocks placement, removes the block on either left-click or
right-click, and also blocks item movement.

Item movement is blocked at three layers:

- `CapabilityProvider` and `BaseContainerBlockEntity` mixins return an empty item handler
  while the block is banned, covering Forge-capability pipes and machines;
- `HopperBlockEntity` mixins stop hopper ticks, entity pickup, source extraction, and
  destination lookup for loaded hoppers;
- banned block entities are tracked weakly while loaded. Block rule changes invalidate
  and revive their capabilities so cached pipe connections are refreshed.

The item-handler guard applies to both block modes and to global or matching-dimension
rules. It is a world-level rule and does not consult the player whitelist.

## Performance guards

Rule types with no entries are skipped before registry and index work. Player inventory
scans run every five ticks, while item pickup triggers an immediate scan. Effect checks
run every twenty ticks, Curios checks run with the ten-tick full check, and block or
structure events exit before registry lookup when their rule type is empty. Hopper ticks
check only the source, self and actual output side.

## Ban Stick actions

`PendingActionManager` stores one short-lived action per player. The server sends an
`OpenChoicePacket`; the client displays a left-side option panel or a bottom-right
confirmation panel. The client returns only an action ID and option index. The server
validates permission, action expiry, and the stored target before writing rules.

## GUI model

`BanListMenu` exposes a read-only 5-row container for items or blocks. The last row
contains page and close controls. `BanListScreen` renders a custom container-style panel
and delegates item tooltips to the vanilla screen implementation. Both list types share
the same grouping, pagination, and read-only slot code; only the icon and tooltip lines
differ.

Clicking a content slot creates a list-entry action. The management panel can change the
ban mode or remove all rules for that ID; the list is reopened after the choice.

## Optional JEI integration

The common code emits only hidden item IDs. The client integration is compiled against
small compile-only API descriptors and uses reflection at runtime, so JEI is not a hard
dependency.

## Optional Curios integration

`CuriosCompat` is compiled against the Curios API but is only called when Curios is
loaded. Periodic player checks scan normal and cosmetic curio stacks, remove banned items,
and strip banned enchantments. Curios is not a required dependency.
