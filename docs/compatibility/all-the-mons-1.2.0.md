# All the Mons 1.2.0 compatibility audit

Audit date: 2026-08-10

This record covers only the compatibility surface of the Cobbleworkers
optimizer. It does not claim a complete semantic review of every mod in the
pack.

## Pack identity

| Evidence | Exact value |
| --- | --- |
| All the Mons tag | `1.2.0` |
| Pack commit | `c7bb230f21d14d26859d0b92548f089b3a493ad9` |
| CurseForge export file | project `1356598`, file `8572588`, `All the Mons-1.2.0.zip` |
| Export SHA-256 | `1d37df201daddecf5454115f5205cca15ca6ab84ed102bcc2e312f4c14876e5d` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.248` |

The export manifest contains 402 file records. Compared with 1.1.1, the
direct Cobbleworkers and Cobblemon records are unchanged; All The Mons core,
ModernFix, and NeoForge changed.

## Dependency and target audit

| Component | 1.1.1 | 1.2.0 | Result |
| --- | --- | --- | --- |
| Cobbleworkers | CF `1296055:7586428`, `1.8.0+1.7.0` | same | Exact file identity unchanged |
| Cobblemon | CF `687131:7553231`, `1.7.3+1.21.1` | same | Exact file identity unchanged |
| All The Mons core | CF `1361840:8427273`, `0.3.0` | CF `1361840:8561730`, `0.5.0` | Relevant bytecode re-audited |
| ModernFix | CF `790626:8360807`, `5.27.15` | CF `790626:8459650`, `5.27.20` | No direct optimizer targets |
| NeoForge | `21.1.234` | `21.1.248` | Build/runtime pin updated |

Exact runtime JAR hashes:

```text
Cobblemon-neoforge-1.7.3+1.21.1.jar
962d75df4fb649d94863a7a7d130d4d2b3de4da9b3cae4c44b1ce90f37ec0ed5

cobbleworkers-neoforge-1.8.0+1.7.0.jar
fd766eb47efd1d2d310e1830fadeac25d338cb6b5e1cd6a31214bae27608c3cc

allthemons-0.5.0.jar
b5ae8fa0d0e16d104e79d71b085dc64f3f00b845cd5f1dba21f91e7a686683c6

modernfix-neoforge-5.27.20+mc1.21.1.jar
6f367ea4002c39e3cfe27e0a0be1008880a4b35a8482a78a437cfb9d8ee38273
```

The unchanged Cobbleworkers JAR still exposes the exact patched members:

```text
WorkerDispatcher.tickAreaScan(Level, BlockPos): void
WorkerDispatcher.tickPokemon(Level, BlockPos, PokemonEntity): void
WorkerDispatcher.workers: private static final List
Worker.shouldRun(PokemonEntity): boolean
Worker.tick(Level, BlockPos, PokemonEntity): void
CobbleworkersCacheManager.getTargets(BlockPos, JobType): Set
```

The optimizer retains full descriptors and `require = 1`, so an unexpected
target change fails at startup instead of silently widening support.

## All The Mons core 0.5.0

The exact 0.3.0 and 0.5.0 JARs were decompiled with Vineflower 1.11.2 and the
following classes were compared:

- `MixinWorkerDispatcher`
- `MixinJobType`
- `MixinCobbleworkersInventoryUtils`
- `EggHandler`
- `EggHatcher`
- `HeartRipper`
- `MixinPokemonPastureBlockEntity`

The first six are semantically identical. The final dispatcher is still
composed by appending Heart Ripper, Egg Handler, and Egg Hatcher; the enum and
inventory behavior are unchanged. The two block tags used by the inventory
overwrite are also identical and still exclude `cobblemon:pasture`.

The Pasture Mixin now limits its breeding/bird-drop work to a real Cobblemon
Pasture and adds Pika Power placement behavior. Core 0.5.0 also adds tether
recovery Mixins around Cobblemon handler classes. Those changes do not target
`WorkerDispatcher` or `CobbleworkersCacheManager`, and this optimizer does not
modify the Pasture control flow.

## NeoForge and ModernFix

NeoForge's official 21.1.248 cumulative changelog lists versions 21.1.235
through 21.1.248. The loader/transformer-relevant entries are FancyModLoader
4.0.43 and ASM 9.10.1; the remainder are registry, data, client, and vanilla
bug fixes. None changes the mod-owned Cobbleworkers descriptors above. The
project is compiled against 21.1.248 and now requires `[21.1.248,21.2)`.

ModernFix changes from 5.27.15 to 5.27.20, and the pack adds
`stability_level=BETA` while retaining `mixin.perf.dynamic_resources=true`.
Both exact ModernFix JARs were unpacked and scanned. Neither contains a Mixin
target or literal for Cobbleworkers, `WorkerDispatcher`,
`PokemonPastureBlockEntity`, or the `accieo.cobbleworkers` package. Its new
Mixins target Minecraft/client classes and do not collide with the two
optimizer targets.

The exported `config/cobblemon/main.json` is byte-for-byte unchanged from
1.1.1. No Cobbleworkers-specific override was added.

## Outcome and verification

No scheduling or instrumentation code change is required. Version 0.1.1 only
updates the exact pack/core/loader pins and compatibility documentation.

Local verification for the release candidate:

```text
./gradlew --no-daemon clean check build
```

This command passed on 2026-08-10 with Java 21 and NeoForge 21.1.248. The
production JAR was also opened by `verifyProductionJar`, which rejected
bundled dependency or client entries, and the Maven POM plus Gradle module
metadata were generated successfully for version 0.1.1.

A disposable instance built from the official `ServerFiles-1.2.0.zip`
(SHA-256
`de112ed8d79b3ff027e399a5108b706f6a2db3be74b15d0db6f6b9d6ac268e6c`)
then passed a dedicated-server startup smoke test. The server loaded this mod
as version 0.1.1 alongside NeoForge 21.1.248, Cobbleworkers 1.8.0, Cobblemon
1.7.3, All The Mons core 0.5.0, and ModernFix 5.27.20. It reached
`Done (2.995s)`, logged `Compatibility self-check passed with 25 composed
Cobbleworkers jobs`, accepted `stop`, saved all dimensions, and exited with
status zero. No optimizer-target Mixin failure was logged.

This smoke test did not connect an unmodified client, exercise live worker job
families, use a production world, or run controlled Spark profiling; no result
for those checks is claimed here.

## Primary sources

- [All the Mons 1.2.0 pack changelog](https://github.com/AllTheMods/All-the-Mons/blob/1.2.0/changelogs/CHANGELOG-All%20The%20Mons-1.1.1-1.2.0.md)
- [All the Mons 1.2.0 CurseForge export](https://www.curseforge.com/minecraft/modpacks/all-the-mons/files/8572588)
- [All The Mons core 0.5.0 file and changelog](https://www.curseforge.com/minecraft/mc-mods/all-the-mons-coremod/files/8561730)
- [NeoForge 21.1.248 cumulative changelog](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.248/neoforge-21.1.248-changelog.txt)
- [ModernFix 5.27.20 exact file](https://www.curseforge.com/minecraft/mc-mods/modernfix/files/8459650)
- [Cobbleworkers source](https://github.com/Accieo/cobbleworkers/tree/2986bdeb76622f57cc8bd2c2fa203ef091ab4195)
