# All the Mons Cobbleworkers Optimizer

A dedicated-server-only NeoForge add-on that reduces the frequency of
Cobbleworkers' expensive high-level decisions while leaving ordinary Pokémon
entity and path-navigation ticks alone. It targets the exact Cobbleworkers
stack in All the Mons 1.2.0 and preserves the pack's Heart Ripper, Egg Handler,
and Egg Hatcher jobs.

The add-on does not register content, payloads, packet channels, screens, or
client hooks. Do not install it on clients.

## Exact compatibility

| Component | Supported version |
| --- | --- |
| All the Mons | `1.2.0` |
| All The Mons core | `0.5.0` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.248` |
| Java | `21` |
| Cobblemon | `1.7.3+1.21.1` |
| Cobbleworkers | `1.8.0+1.7.0` |

These are hard dependency ranges, not broad compatibility claims. A startup
self-check also verifies the exact dispatcher signatures and confirms that all
three pack-added worker implementations are in the final worker registry.

## What it changes

Cobbleworkers calls its high-level area scanner once per Pasture per tick and
evaluates every worker job once per eligible pastured Pokémon per tick. This
add-on wraps those two dispatcher methods:

- worker decisions run at a configurable interval;
- area-scan dispatches run at a separately configurable interval;
- work is assigned a deterministic phase using the dimension, Pasture
  position, and, for decisions, Pokémon UUID;
- every executed dispatcher call and every job's eligibility/decision time is
  measured in memory;
- target-set sizes returned to upstream and pack-added jobs are sampled.

An interval of `1`, or disabling the relevant feature, restores stock
dispatcher scheduling. The wrapper calls the original composed dispatcher; it
does not copy or replace Cobbleworkers' private worker list.

Normal entity AI and active navigation still tick every game tick. At the
default decision interval of five ticks, a newly available task can wait up to
four additional ticks (about 200 ms at 20 TPS). The area scanner retains its
stock blocks-per-dispatch budget, so an initial or periodic full scan takes
roughly five times as many game ticks at the default scan interval.

## Installation

1. Stop the dedicated server and make a normal backup.
2. Put the production JAR in the server's `mods` directory only.
3. Keep the pack's existing Cobblemon, Cobbleworkers, and All The Mons core
   JARs unchanged.
4. Start the server and confirm the log contains
   `Compatibility self-check passed with 25 composed Cobbleworkers jobs`.
5. Inspect and tune
   `config/atmons_cobbleworkers_optimizer-common.toml`.

The production JAR for this pack version is
`allthemons-cobbleworkers-optimizer-0.1.1.jar`.

## Configuration

The generated common config uses these defaults:

| Key | Default | Effect |
| --- | ---: | --- |
| `enableWorkerDecisionThrottling` | `true` | Enables the worker decision interval |
| `workerDecisionIntervalTicks` | `5` | High-level decision cadence; `1` is stock |
| `enableAreaScanDispatchThrottling` | `true` | Enables the area-scan interval |
| `areaScanDispatchIntervalTicks` | `5` | Deferred scan dispatch cadence; `1` is stock |
| `staggerByPokemonAndPasture` | `true` | Spreads work across deterministic phases |
| `enableInstrumentation` | `true` | Collects process-local counters and timing |

Stop the server before editing the file. For a conservative first rollout, set
both intervals to `1`, gather diagnostic output, then try `5`.

## Diagnostics

Operators with permission level 2 can use:

```text
/atmons_cobbleworkers_optimizer stats
/atmons_cobbleworkers_optimizer stats reset
```

The report includes attempted, executed, and skipped dispatcher calls; total
dispatcher time; per-job eligibility checks and decision calls; failures seen
by the wrappers; and target-set sample average/maximum. Job names include the
All The Mons custom enum values when those jobs run.

Counters are approximate live diagnostics, exist only in memory, and reset on
restart or removal. They are not a substitute for a controlled Spark profile.

## Deliberately deferred work

This release does not rewrite Cobbleworkers' cache, deferred-scan, navigation
claim, cleanup timer, or structure-location maps. Their exact 1.8.0 public
methods accept `BlockPos` without a dimension. Replacing only some maps would
split state between upstream jobs and the three pack-added jobs, so a safe fix
requires a broader tested compatibility layer.

Accordingly, version 0.1.1 does **not** claim the plan's cross-dimension cache
isolation or stale-claim cleanup acceptance criteria.

Canonical species-ID matching and class-by-class early job guards are also
deferred. Eligibility is implemented independently in many worker singletons,
and legacy configured display names are ambiguous. The first release gathers
per-job evidence before attempting those transformations.

No Cobblemon Pasture-core optimization is included. All The Mons core 0.5.0
injects breeding and drop behavior into `checkPokemon`, persists its own
cooldown, and limits those actions to a real Pasture rather than its new Pika
Power block. This add-on does not alter that control flow.

The pack already overwrites Cobbleworkers inventory discovery with the
`allthemons:cobbleworkers_inventories` tag. The exact tag contains chest and
barrel tags plus the incubator and optional TM machine, and does not contain
`cobblemon:pasture`; this release preserves that filtering instead of stacking
another inventory overwrite.

## Rollout and rollback

Use a staging copy first:

1. Run both intervals at `1`, reset metrics, reproduce representative worker
   activity, and save the diagnostic output and a controlled Spark profile.
2. Repeat with intervals `5`, the same world/chunks/players/activity, and the
   same observation duration.
3. Verify every pack job family used by the server, identical Pasture
   coordinates in multiple dimensions, unload/restart behavior, and a fossil
   analyzer near a Pasture.
4. Connect with an unmodified All the Mons 1.2.0 client before production
   rollout.

To roll back, stop the server and remove this add-on's JAR. It writes no world,
entity, Pasture, claim, or cache data. Its config file may be left in place.
Stock Cobbleworkers scheduling returns on the next start.

## Verification status

The repository's automated gate is:

```bash
./gradlew --no-daemon clean check build
```

The All the Mons 1.2.0 compatibility investigation is recorded in
[`docs/compatibility/all-the-mons-1.2.0.md`](docs/compatibility/all-the-mons-1.2.0.md).
It verifies the exact pack manifest identities and JAR hashes, compares the
coremod's relevant Mixins and worker classes, reviews the NeoForge and
ModernFix changes, and records the tests actually run.

The gate passed locally on 2026-08-10 against NeoForge 21.1.248. The
production JAR was opened and verified to contain only project classes,
server-only Mixin metadata, mod metadata, and the license. Maven POM and
Gradle module metadata generation also passed for version 0.1.1. A disposable
dedicated server made from the official All the Mons 1.2.0 ServerFiles archive
reached `Done`, the runtime compatibility self-check found all 25 composed
Cobbleworkers jobs, and a `stop` command shut the server down cleanly. This was
a startup smoke test only; it did not include a client connection, live job
exercise, production-world behavior, or Spark profiling.

Pure-Java tests cover interval-one rollback, aligned and staggered scheduling,
dimension/Pasture/Pokémon key participation, phase stability, and metrics
aggregation/reset. The `check` lifecycle also opens the production JAR,
requires its metadata, Mixin config, license, and project classes, and rejects
bundled third-party or client entries.

Dedicated-server startup, a production-world test, unmodified-client
connection, full job-family GameTests, and equivalent Spark profiles remain
staging tasks. No result for those tests is claimed here.

## Maven

Releases publish the `mod` publication to GitHub Packages:

```text
io.github.jan-guenter:allthemons-cobbleworkers-optimizer:0.1.1
```

Repository:

```text
https://maven.pkg.github.com/jan-guenter/allthemons-cobbleworkers-optimizer
```

GitHub Packages consumers need normal GitHub package credentials.

## License

The add-on code is available under the [MIT License](LICENSE). Cobblemon,
Cobbleworkers, and All The Mons remain under their own licenses and are
dependencies, not redistributed source or binaries.
