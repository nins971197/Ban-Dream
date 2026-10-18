# Ban Dream - Forge 1.12.2

Development environment based on the provided CleanroomMC `ForgeDevEnv` template.

- Gradle: 9.6.1
- Java: 25 runner, Java 8 source target
- RetroFuturaGradle: 2.0.2
- Forge: 14.23.5.2847
- Mappings: `stable_39`
- MixinBooter: `10.7` (kept pinned; not upgraded)
- Mod ID: `bandream`
- Display name: `Ban Dream`

## Tasks

```text
gradlew.bat build
gradlew.bat runClient
gradlew.bat runServer
```

The project is a compileable environment scaffold. Existing 1.12.2 feature code can be
added incrementally without changing the pinned MixinBooter version.
