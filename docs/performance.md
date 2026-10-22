# Ban Dream Performance Notes

This document is workspace-only and is not packaged into the mod jar.

## Microbenchmark

Environment:

- Windows 11
- JDK 17.0.20.1
- Forge 47.4.20 dedicated server
- Development server, single-player test world, warm JVM
- 20,000 warm-up calls and 1,000,000 measured rule queries per case

Measured costs on the development machine:

| Query | Approximate cost |
| --- | ---: |
| `BanRules.isItemBanned` with no item rules | 23 ns |
| `BanRules.isItemBanned` with one global item rule | 20 ns |

These values cover rule lookup after ID and dimension caches are warm. They do not
include the surrounding Minecraft inventory, chunk or capability work.

Snapshot payloads now use one compressed NBT file per UUID. Only the UUID index remains
loaded in memory; original ItemStacks and block snapshots are read on demand. Write cost
therefore depends primarily on storage latency rather than the total snapshot count.
The earlier 10,000-snapshot in-memory benchmark no longer applies to this format.

## Estimated server load

The estimates below use a 20 TPS server and a 50 ms tick budget. Actual cost depends on
rule count, NBT rules, modded pipes, entity count and chunk load.

| Scenario | Estimated additional load |
| --- | --- |
| No active item/block/entity rules, 100 players | one fast-path predicate per relevant event |
| Item bans, 100 players, normal inventories | no periodic inventory scan |
| Item bans with many NBT rules, 100 players | only inventory/event boundaries |
| 10,000 retained item snapshots | UUID index in memory; payload stored as separate files |
| Block bans, 1,000 loaded hoppers | roughly 0.1-0.5 ms per tick |
| Block bans, 10,000 loaded hoppers | roughly 1-5 ms per tick |
| Block behavior checks | one rule lookup per relevant tick or update |
| Curios checks, 100 players | only on curio changes or enchantment refresh |
| Block scanner | global 8,192-cell, 128-redirect and 2 ms per-tick cap |

Player inventory insertion is checked only when a stack is inserted or a slot is written.
Item use is still checked at `ItemStack` interaction and callback entry points as a
pre-redirect guard. Item entities are checked once on join and once when rules change,
not every tick.
Block bans are checked at central server tick/update entry points instead of scanning
world blocks. The main additional cost is one block-ID rule lookup for each relevant
block entity tick or update, plus capability checks for item transport.
