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
| `BanRules.blockBanMode` with a global block rule | 33 ns |
| `BanRules.isDimensionBanned` with no dimension rules | 4 ns |

These values cover rule lookup after ID and dimension caches are warm. They do not
include the surrounding Minecraft inventory, chunk or capability work.

## Estimated server load

The estimates below use a 20 TPS server and a 50 ms tick budget. Actual cost depends on
rule count, NBT rules, modded pipes, entity count and chunk load.

| Scenario | Estimated additional load |
| --- | --- |
| No active item/block/entity rules, 100 players | under 0.01 ms per tick |
| Item bans, 100 players, normal inventories | roughly 0.01-0.03 ms per tick |
| Item bans with many NBT rules, 100 players | roughly 0.1-0.5 ms per tick |
| Block bans, 1,000 loaded hoppers | roughly 0.1-0.5 ms per tick |
| Block bans, 10,000 loaded hoppers | roughly 1-5 ms per tick |
| Curios checks, 100 players | usually below 0.01 ms per tick |

The item scan runs every five ticks and still performs an immediate scan on item pickup.
Curios and enchantment checks run with the ten-tick full check. Effect checks run every
twenty ticks and skip entities without active effects.
