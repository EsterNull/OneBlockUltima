# OneBlock Ultima

## Requirements

- **JDK 8** (required — ForgeGradle 3 / Gradle 4.10.3 do not work on newer JDKs, e.g. JDK 21 fails with `Unable to get mutable Windows environment variable map`).
- Minecraft Forge **1.12.2-14.23.5.2859** (resolved automatically by the build).
- Tested on Windows, but the build is cross-platform.

Set `JAVA_HOME` to your JDK 8 before running any Gradle task:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-1.8"
$env:Path = "C:\Program Files\Java\jdk-1.8\bin;$env:Path"
```

## Build

```powershell
gradlew build
```

Produces the reobfuscated release jar in `build/libs/OneBlockUltima-forge-1.12.x-<version>.jar` (`reobfJar` runs automatically after `jar`). Drop it into the `mods/` folder of a 1.12.2 Forge instance.

Other useful tasks:

| Task | Purpose |
| --- | --- |
| `gradlew runClient` | Launch the Minecraft client with the mod (dev environment). |
| `gradlew runServer` | Launch a dedicated server with the mod. |
| `gradlew test` | Run unit tests (JUnit 4, pure JVM, no client needed). |
| `gradlew genIntellijRuns` | Generate IntelliJ IDEA run configurations. |
| `gradlew genEclipseRuns` | Generate Eclipse run configurations. |
| `gradlew clean` | Clean `build/`. |

## IDE setup

1. Clone the repo and import `build.gradle` as a Gradle project (IntelliJ IDEA / Eclipse with the Gradle plugin).
2. Run `gradlew genIntellijRuns` (or `genEclipseRuns`) once.
3. Use the generated run configuration or `gradlew runClient`.

The IDE should use the **JDK 8** SDK as the project SDK.

## Project layout

```
src/
├── main/
│   ├── java/ru/defea/oneblockultima/   # mod code
│   │   ├── OneBlockUltima.java         # @Mod entry point, logger (getLogger())
│   │   ├── config/                     # ModSettings, BlockSetConfig, BlockPriceConfig (JSON)
│   │   ├── gui/                        # all screens (ViewFactory-based layout)
│   │   ├── guide/                      # GuideBook content (commands, recipes, sets)
│   │   ├── tile/                       # TileEntityOneBlockGenerator
│   │   ├── event/                      # ModEvents (spawning, break handling, claims)
│   │   ├── command/                    # chat commands
│   │   ├── network/                    # custom packets
│   │   ├── world/                      # One Block world type
│   │   └── util/                       # helpers (BlockUtil, ModelUtil)
│   └── resources/
│       ├── assets/oneblockultima/      # textures, models, lang (en_us, ru_ru)
│       ├── blocksets.json              # default block sets
│       ├── block_prices.json           # default price table
│       └── mcmod.info
└── test/
    └── java/ru/defea/oneblockultima/   # JUnit 4 unit tests
```

## Versioning & config

- Version lives in `build.gradle`, also injected into `mcmod.info` during `processResources`.
- Runtime config is written to the Forge config dir:
  - `config/oneblockultima_mod_settings.json` — mod settings.
  - `config/oneblockultima/blocksets.json` — block sets (created from bundled defaults on first launch).
  - `config/oneblockultima/block_prices.json` — price table.

## Logging / Debug mode

All mod logging goes through `OneBlockUltima.getLogger()` (a `java.lang.reflect.Proxy` wrapper). When the **Debug mode** toggle in Settings → Misc is off, every call is suppressed; when on, calls are forwarded to the mod logger. Don't use `System.out.println` for mod logs.

## Tests

Unit tests run in a pure JVM via `gradlew test` (no Minecraft client is launched):

```powershell
gradlew test
```

Perf/allocation tests print `[Perf]` lines through `System.out` by design.

## Short player overview

- Create a world with the **One Block** world type. A free generator can be claimed by right-clicking it.
- Break the block above the generator — it is replaced from the active set (blocks, mobs, fluids, chests, saplings).
- Coins (OBU) are earned by mining or selling (`/obuSell`, `/obuSellAll`); the mode is set in Settings → Manage Prices.
- Unlock and upgrade sets, meet unlock conditions, invite friends as generator members.
- Full mechanics are described in the in-game **Guide Book**.

### Commands

| Command | Description | Cheat |
| --- | --- | --- |
| `/obuSell` | Sell the generator-made item in your hand. | |
| `/obuSellAll` | Sell all generator-made items of the same type as the item in hand. | |
| `/inviteGeneratorMember <player>` | Invite a player to your generator. | |
| `/acceptGeneratorInvite` | Accept a generator invite. | |
| `/declineGeneratorInvite` | Decline a generator invite. | |
| `/addUltimaBalance <amount>` | Give yourself coins. | yes |
| `/setOwner <x> <y> <z> <player>` | Set the generator owner at coordinates. | yes |
