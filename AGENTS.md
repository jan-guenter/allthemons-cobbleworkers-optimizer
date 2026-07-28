# Agent guide

This repository is a narrow, server-only compatibility/performance add-on. Do
not broaden its compatibility range or backport closed-source Cobbleworkers
2.x behavior without a fresh exact-version investigation.

## Supported stack and evidence lock

| Component | Version / evidence |
| --- | --- |
| All the Mons | `1.1.1`, pack commit `94a224acf6eace3edf7ea64e6033b458f5bda288` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.234` |
| Cobblemon | `1.7.3+1.21.1`, commit `e6fda6137e00cfe6035c863a3736ba30fc236696` |
| Cobbleworkers | `1.8.0+1.7.0`, commit `2986bdeb76622f57cc8bd2c2fa203ef091ab4195` |
| All The Mons core | `0.3.0` |

Exact shipped SHA-256 values:

```text
Cobblemon-neoforge-1.7.3+1.21.1.jar
962d75df4fb649d94863a7a7d130d4d2b3de4da9b3cae4c44b1ce90f37ec0ed5

cobbleworkers-neoforge-1.8.0+1.7.0.jar
fd766eb47efd1d2d310e1830fadeac25d338cb6b5e1cd6a31214bae27608c3cc

allthemons-0.3.0.jar
35ed3ef101b9c0b9fbe5843c7a6a3f62b40748f2b62ea14fd49ca494102a879e
```

The `maven.modrinth:cobbleworkers:zigPFURt` build dependency is intentional.
The human version is duplicated by Fabric and NeoForge on Modrinth; the
version ID resolves the exact NeoForge JAR and hash above.

## Exact target contract

Production Mixins are required, Java 21, `remap = false`, and use full method
descriptors with `require = 1`.

```text
accieo.cobbleworkers.jobs.WorkerDispatcher
  tickAreaScan(
    Lnet/minecraft/world/level/Level;
    Lnet/minecraft/core/BlockPos;)V
  tickPokemon(
    Lnet/minecraft/world/level/Level;
    Lnet/minecraft/core/BlockPos;
    Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V
  private static final workers:Ljava/util/List;

accieo.cobbleworkers.interfaces.Worker
  shouldRun(
    Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)Z
  tick(
    Lnet/minecraft/world/level/Level;
    Lnet/minecraft/core/BlockPos;
    Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)V

accieo.cobbleworkers.cache.CobbleworkersCacheManager
  getTargets(
    Lnet/minecraft/core/BlockPos;
    Laccieo/cobbleworkers/enums/JobType;)Ljava/util/Set;
```

`WorkerDispatcherMixin` cancels only a high-level dispatcher invocation. It
redirects the one `Worker.shouldRun` and one `Worker.tick` call site solely to
measure and invoke the same receiver. `CobbleworkersCacheManagerMixin` reads
the returned set size and does not mutate it.

The deterministic phase includes `Level.dimension().location()`, packed
Pasture position, and Pokémon UUID. Interval one must remain an unconditional
stock pass-through.

## All The Mons core composition

The exact 0.3.0 bytecode was audited:

- `MixinWorkerDispatcher` uses `ModifyExpressionValue` on the
  `CollectionsKt.listOf` call in `WorkerDispatcher.<clinit>` and appends new
  `HeartRipper`, `EggHandler`, and `EggHatcher` instances.
- `MixinJobType` injects after the `$VALUES` write in `JobType.<clinit>` and
  appends `HEART_RIPPER`, `EGG_HANDLER`, and `EGG_HATCHER`.
- `MixinCobbleworkersInventoryUtils` overwrites `blockValidator`,
  `findClosestInventory`, and `handleDepositing`. Its tag-backed inventory set
  excludes the Cobblemon Pasture block.
- `MixinPokemonPastureBlockEntity` injects at return from `checkPokemon` for
  breeding/drop work and also persists `breedCooldown` in
  `saveAdditional`/`loadAdditional`.

Never shadow or rebuild the dispatcher worker list. The startup
`CompatibilitySelfCheck` must continue to prove that the three exact custom
worker class names are present in the final list. Instrumentation must use the
runtime enum name so custom job types remain visible.

## Invariants

- Dedicated-server deployment must permit an unmodified pack client.
- Register no blocks, items, entities, menus, recipes, payloads, channels,
  screens, or client hooks.
- Write no required state into worlds, Pastures, Pokémon, claims, caches, or
  other pack objects.
- Removing the JAR and restarting must restore stock behavior.
- Every behavioral change needs its own config switch; interval `1` restores
  stock scheduling.
- Ordinary entity and path-navigation ticks must not be cancelled.
- Preserve all upstream and All The Mons worker jobs.
- Do not redistribute dependency JARs or third-party source.
- Fail clearly on changed exact signatures rather than widening ranges.
- Do not claim staging, unmodified-client, Spark, or production-world results
  that were not actually run.

## Deferred unsafe phases

Cobbleworkers 1.8.0 keys `pastureCaches`, deferred `activeScans` and
`lastScanCompletion`, block claims, recently expired targets, and structure
locations without a dimension. Their callable APIs also omit the dimension.
A safe dimension fix therefore cannot patch one map in isolation. It must
cover every upstream caller, the three All The Mons jobs, lifecycle cleanup,
and identical-coordinate multi-dimension tests.

Version 0.1.0 therefore does not implement or claim cross-dimension upstream
cache/scan/claim isolation or stale-claim cleanup.

Species checks are spread across many job singleton implementations and use
translated display text. Do not globally redirect `Species.getTranslatedName`
or silently reinterpret legacy config values. A canonical-ID phase needs an
explicit migration model and per-job regression tests.

Do not move guards, add job enablement, change claims, add stale-owner cleanup,
or reproduce berry/fossil/2.x fixes until a failing exact-1.8.0 test and exact
bytecode support the change. Cobbleworkers 2.x is closed-source and its release
notes are not an implementation specification.

Do not optimize Cobblemon's Pasture ticker in this project. The exact All The
Mons `checkPokemon` injection must first be isolated in a separate optional
Mixin group and tested with breeding and bird drops.

## Build and test

Use Java 21 and the committed Gradle 8.10.2 wrapper:

```bash
./gradlew --no-daemon clean check build
```

Inspect the production JAR, not the sources JAR:

```bash
unzip -l build/libs/allthemons-cobbleworkers-optimizer-0.1.0.jar
unzip -p build/libs/allthemons-cobbleworkers-optimizer-0.1.0.jar \
  META-INF/neoforge.mods.toml
```

The production JAR must contain project classes, the Mixin config,
`META-INF/neoforge.mods.toml`, and project metadata only. It must not contain
Cobblemon, Cobbleworkers, All The Mons, research JARs, or client resources.

Pure unit tests are required for scheduler hashing/cadence and metrics. Before
changing runtime behavior, add focused exact-version tests. Staging must cover
intervals 1/5/10/20; every worker family; the three pack jobs; identical
coordinates in separate dimensions; Pasture/Pokémon removal and unload;
restart; nearby inventories and fossil analyzer; and non-English environments
where species matching is involved.

## Release procedure

1. Update code, focused tests, `README.md`, `AGENTS.md`, and `CHANGELOG.md`.
2. Increase `mod_version` in `gradle.properties` using semantic versioning.
3. Run `./gradlew --no-daemon clean check build`.
4. Inspect the production JAR and verify that no dependency/source JAR was
   bundled.
5. Run the required dedicated-server, unmodified-client, job-family, and Spark
   staging checks; record only results actually observed.
6. Commit and merge through a pull request. The version-policy workflow
   requires a greater stable `x.y.z` `mod_version`.
7. Create and push tag `v<mod_version>` exactly.
8. Confirm the release workflow repeats checks, publishes the `mod` Maven
   publication, generates SHA-256 checksums, and creates the GitHub Release.

Never create a tag whose value differs from `mod_version`.

## Required All the Mons pack-update procedure

Treat every pack update as a fresh compatibility investigation:

1. Check out the exact new pack tag/commit. Record Minecraft, NeoForge, every
   relevant dependency JAR filename, mod ID, version, SHA-256, and source tag
   or commit.
2. Diff the complete mod list, pack configs, KubeJS scripts, and datapacks.
   Pay special attention to Cobblemon Pasture settings and inventory tags.
3. Obtain the exact new binary JARs. Inspect `neoforge.mods.toml`, every Mixin
   configuration, target bytecode, and all implementations/callers of patched
   methods. Use `git show` against the exact Cobblemon commit when its worktree
   is sparse.
4. Re-audit the All The Mons core JAR every time. Decompile its
   `WorkerDispatcher`, `JobType`, inventory-utility, and
   `PokemonPastureBlockEntity` Mixins and enumerate every pack-added job.
5. Update compile/runtime dependencies and strict metadata ranges only after
   source and bytecode review. If Modrinth duplicates a version across
   loaders, pin and hash the exact NeoForge version ID.
6. Update exact descriptors, startup assertions, and tests so changed
   signatures fail loudly; do not add a broad compatibility range.
7. Run unit tests, meaningful game/integration tests, a dedicated-server
   startup, and an unmodified-client connection test on a staging copy.
8. Repeat controlled before/after Spark profiles with equivalent world,
   players, loaded chunks, duration, and activity. Record median, p95/p99,
   maximum, and named hot paths.
9. Update compatibility tables, evidence hashes, `README.md`, `AGENTS.md`, and
   changelog/release notes; increase `mod_version`; merge; then create the
   exactly matching `v*` tag.
