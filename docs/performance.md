# Ban Dream Performance Notes

This document is workspace-only and is not packaged into the mod jar.

## Microbenchmark

Environment:

- Windows 11
- JDK 17.0.20.1
- Forge 47.4.20 dedicated server
- 50,000 warm-up calls and 200,000 measured calls per case

Measured rule-query cost on the development machine:

| Query | Approximate cost |
| --- | ---: |
| `BanRules.findItem` with a global item rule | 20 ns |
| `BanRules.isBlockBanned` with a global block rule | 33 ns |
| `BanRules.isDimensionBanned` with no dimension rules | 4 ns |

These values cover rule lookup after ID and dimension caches are warm. They do not
include the surrounding Minecraft inventory, chunk or capability work.

## Estimated server load

The estimates below use a 20 TPS server and a 50 ms tick budget. Actual cost depends on
rule count, NBT rules, modded pipes, entity count and chunk load.

| Scenario | Estimated additional load |
| --- | --- |
| No active item/block/entity rules, 100 players | under 0.01 ms per tick |
| Item bans, 100 players, normal inventories | usually under 0.01 ms per tick |
| Item bans with many NBT rules, 100 players | only uses/interactions, no periodic scan |
| Block bans, 1,000 loaded hoppers | roughly 0.1-0.5 ms per tick |
| Block bans, 10,000 loaded hoppers | roughly 1-5 ms per tick |
| Block behavior checks | one rule lookup per relevant tick or update |
| Curios checks, 100 players | only on curio changes or enchantment refresh |

Player inventory insertion is no longer checked at all, because item bans do not modify
ItemStack data. Item use is checked at `ItemStack` interaction and callback entry points.
Block bans are checked at central server tick/update entry points instead of scanning
world blocks. The main additional cost is one block-ID rule lookup for each relevant
block entity tick or update, plus capability checks for item transport.
