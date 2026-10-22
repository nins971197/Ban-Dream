# Ban Dream Architecture

This document is workspace-only and is not packaged into the mod jar.

## Runtime flow

1. Commands create `BanEntry` rules or pending actions.
2. `BanRules` validates, sorts, indexes, and writes rules to `config/bandream/bans.json`.
3. `RuleIndex` provides type-and-ID lookups without scanning the full rule list.
4. `BanEvents` handles effect, dimension, entity and item-entity enforcement.
5. `ItemRedirectService` creates and restores UUID-backed `999ban` item markers.
6. `ItemSnapshotStore` keeps original ItemStacks in world SavedData.
7. `BlockBanEvents` handles block interaction and placement.
8. `BlockBanSupport` answers item I/O queries for mixins and events.
9. `BanStickEvents` selects Ban Stick targets and opens server-validated option panels.
10. `BanActionHandler` maps client choices to ban or unban operations.
11. `BanEnforcement` applies player inventory, Curios, effect, dimension and entity cleanup.
12. `ItemStackMixin` blocks item callbacks while a redirect is pending.
13. `LevelMixin` and `ServerLevelMixin` suppress banned block ticks and updates.
14. `ServerPlayerGameModeMixin` blocks player interaction and destruction.

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

- item redirects and item callback blocking;
- Curios item redirects;
- enchantment cleanup;
- mob effect cleanup;
- dimension travel and forced dimension escape.

Entity spawning and structure generation remain world-level rules.

## Item ban enforcement

Item rules replace matching stacks with `999ban`. The complete original ItemStack is
stored once by UUID in `bandream_item_snapshots` SavedData. A `999ban` stack stores only
the UUID in its `SnapshotId` tag, so moving, dropping or storing the marker does not
duplicate the original item data.

`PlayerInventoryMixin` handles inventory insertion and slot writes. Player login,
dimension changes, inventory events and rule changes restore valid markers and redirect
remaining stacks. `EntityJoinLevelEvent` handles dropped or machine-spawned item
entities. Item markers become invulnerable and never expire. When item rules change,
loaded item entities are checked once rather than every tick.

Container stacks are redirected when they reach a player inventory or Curios slot.
Markers already stored inside a closed third-party container remain markers until the
mod's rule refresh can reach a supported inventory surface. Marker UUIDs are globally
unique; copying a marker with commands copies its UUID and does not duplicate the
underlying snapshot.

## Block ban enforcement

Block rules replace the original block with `bakaban`. The complete original BlockState
and BlockEntity NBT are stored once in a global UUID-indexed snapshot store.
`BakabanBlockEntity` and dropped `bakaban` items store only the UUID. The marker may be
broken and placed elsewhere; restoration uses the marker's current position.

`BakabanScanner` scans loaded chunks for banned blocks and existing markers. On unban it
restores markers at their current positions and leaves unloaded chunks queued for their
next load. Scanner work is shared across dimensions and capped by a two-millisecond
per-tick time budget in addition to cell and redirect limits. `ServerPlayerGameModeMixin`
blocks right-click use, while destruction and placement remain allowed.
`LevelChunkTickerMixin` prevents banned block entities from registering a ticker;
`LevelMixin` and `ServerLevelMixin` continue to suppress scheduled, random and fluid
behavior before a marker is created.

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

Rule types with no entries are skipped before registry and index work. Inventory
redirects run only on insertion, slot writes, login and rule changes, not on a timer.
Effect additions are handled by `MobEffectEvent.Added`, and Curios changes by
`CurioChangeEvent`. Block behavior is checked at central tick and update entry points.
Hopper ticks check only the source, self and actual output side.

Measured query costs and rough server-load estimates are recorded in
`docs/performance.md`.

## Ban Stick actions

`PendingActionManager` stores one short-lived action per player. Ban Stick clicks and
action choices require permission level 3. The server sends an
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
loaded. Curio change events redirect banned items to `999ban`, restore markers after
unban, and strip banned enchantments. Curios is not a required dependency.
