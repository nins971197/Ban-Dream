# Ban Dream Architecture

This document is workspace-only and is not packaged into the mod jar.

## Runtime flow

1. Commands create `BanEntry` rules or pending actions.
2. `BanRules` validates, sorts, indexes, and writes rules to `config/bandream/bans.json`.
3. `RuleIndex` provides type-and-ID lookups without scanning the full rule list.
4. `BanEvents` handles effect, enchantment, dimension, and fallback interaction enforcement.
5. `BlockBanEvents` handles block interaction and breaking cancellation.
6. `BlockBanSupport` answers item I/O queries for mixins and events.
7. `BanStickEvents` selects Ban Stick targets and opens server-validated option panels.
8. `BanActionHandler` maps client choices to ban or unban operations.
9. `BanEnforcement` applies effect, dimension, entity, and enchantment cleanup.
10. `ItemStackMixin` blocks item use and special item callbacks on the server.
11. `LevelMixin` and `ServerLevelMixin` suppress banned block ticks and updates.
12. `ServerPlayerGameModeMixin` blocks player interaction and destruction.

## Rule storage

The JSON file stores two top-level collections:

- `bans`: blocked content rules.
- `playerWhitelist`: players exempt from player-facing restrictions.

Writes use a temporary file followed by an atomic move when the filesystem supports it.

## Scope matching

Global rules match every dimension. Only entity rules may use `DIMENSION` scope; all
other ban types normalize to global scope, including old configurations.
Player whitelist entries match by UUID first and then by case-insensitive player name.

## Enforcement boundaries

Player whitelist affects:

- item use and interaction blocking;
- enchantment cleanup;
- mob effect cleanup;
- dimension travel and forced dimension escape.

Entity spawning and structure generation remain world-level rules.

## Block ban enforcement

Block rules replace the original block with `bakaban`. The complete original BlockState
and BlockEntity NBT are stored once in a global UUID-indexed snapshot store.
`BakabanBlockEntity` and dropped `bakaban` items store only the UUID. The marker may be
broken and placed elsewhere; restoration uses the marker's current position.

`BakabanScanner` scans loaded chunks for banned blocks and existing markers. On unban it
restores markers at their current positions and leaves unloaded chunks queued for their
next load. `ServerPlayerGameModeMixin` blocks right-click use, while destruction and
placement remain allowed. `LevelChunkTickerMixin`, `LevelMixin`, and `ServerLevelMixin`
continue to suppress internal block behavior before a marker is created.

Item movement is blocked at three layers:

- `bakaban` exposes no original item handler, so pipes and machines cannot access the
  original contents;
- `CapabilityProvider` and `BaseContainerBlockEntity` mixins return an empty item handler
  for any pre-redirection banned block still present in a loaded race window;
- `HopperBlockEntity` mixins stop hopper ticks, entity pickup, source extraction, and
  destination lookup for loaded hoppers;
- banned block entities are tracked weakly while loaded. Block rule changes invalidate
  and revive their capabilities so cached pipe connections are refreshed.

The item-handler guard applies to global block rules. It is a world-level rule and does
not consult the player whitelist.

## Performance guards

Rule types with no entries are skipped before registry and index work. Item use is
checked at the `ItemStack` interaction and callback boundaries, not by periodic
inventory scans. Effect additions are handled by `MobEffectEvent.Added`, and Curios
changes by `CurioChangeEvent`. Block behavior is checked at central tick and update
entry points. Hopper ticks check only the source, self and actual output side.

Measured query costs and rough server-load estimates are recorded in
`docs/performance.md`.

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

Clicking a content slot creates a list-entry action. The management panel can ban or
remove all rules for that ID; the list is reopened after the choice.

## Optional JEI integration

The common code emits only hidden item IDs. The client integration is compiled against
small compile-only API descriptors and uses reflection at runtime, so JEI is not a hard
dependency.

## Optional Curios integration

`CuriosCompat` is compiled against the Curios API but is only registered when Curios is
loaded. Curio change events can strip banned enchantments, but item bans never remove
curio stacks. Curios is not a required dependency.
