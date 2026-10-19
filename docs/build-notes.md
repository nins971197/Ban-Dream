# Build Notes

This document is workspace-only and is not packaged into the mod jar or source jar.

## Root project

- Minecraft 1.20.1
- Forge 47.4.20
- Java 17
- Gradle 8.8
- Mixin 0.8.5

Command:

```text
gradlew.bat build --offline
```

## Forge 1.12.2

- Based on the supplied CleanroomMC ForgeDevEnv template.
- Gradle 9.6.1
- RetroFuturaGradle 2.0.2
- Java 25 runner
- Java 8 target
- MixinBooter 10.7, pinned and not upgraded

The dependency access-transformer switch is disabled by default because the RFG JST
stage requires a separate Azul JDK 21 installation in this workspace. The project
builds successfully with this setting.

## Cleanroom 1.12.2

- Cleanroom 0.7.9-alpha
- CleanroomGradle 0.22.0
- Gradle 9.8.1
- Java 25
- Java 8 required for the reobf task

Build completed successfully after supplying a Java 8 toolchain.

## NeoForge 1.20.1

- ModDevGradle Legacy 2.0.148
- Gradle 8.14.2
- Java 17
- NeoForge-compatible 47.1.x API

Configuration evaluation succeeds. Dependency resolution stops at the unavailable
`maven.neoforged.net` host in this workspace.

## NeoForge 1.21.1

- ModDevGradle 2.0.148
- Gradle 8.14.2
- Java 21
- NeoForge 21.1.176

Configuration and NeoForm setup start correctly. Dependency resolution stops at the
unavailable `maven.neoforged.net` host in this workspace.
