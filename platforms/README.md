# Ban Dream Platform Workspaces

Each directory is an independent Gradle project with its own wrapper and toolchain.

| Target | Directory | Java | Gradle | Status |
| --- | --- | --- | --- | --- |
| Forge 1.12.2 | `forge-1.12.2` | 25 runner / 8 target | 9.6.1 | Built successfully |
| Cleanroom 1.12.2 | `cleanroom-1.12.2` | 25 / 8 reobf | 9.8.1 | Built successfully |
| NeoForge 1.20.1 | `neoforge-1.20.1` | 17 | 8.14.2 | Environment configured |
| NeoForge 1.21.1 | `neoforge-1.21.1` | 21 | 8.14.2 | Environment configured |

The two NeoForge projects use `maven.neoforged.net`. If dependency resolution fails,
check that the host is reachable and not being redirected by local DNS.

The 1.12.2 Forge project is based on the supplied CleanroomMC `ForgeDevEnv` template.
Its MixinBooter version is pinned to `10.7` and must not be upgraded as part of this port.
