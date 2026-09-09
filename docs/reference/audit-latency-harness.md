# Reference: The audit latency harness

Every audit figure in this documentation came out of one harness, and the harness is more interesting
than most of the figures. It exists because a benchmark that cannot prove what it measured is worth less
than no benchmark — a claim this project has had to make good on repeatedly.

The kit lives with the analyser, in `tools/bench/latency-kit/`.

## What it is

| script | what it answers |
|---|---|
| `measure.sh` | what does this arm cost — **or a refusal** |
| `compare-arms.sh` | do two arms differ, holding everything else equal |
| `validate-controls.sh` | is this machine still producing the recorded numbers |
| `describe-control.sh` | what *exactly* was a figure taken against |
| `index-binaries.sh` | which binary is which |

`control-bands.tsv` holds the bands; `RECORDED-BASELINES.md` holds the measurements and the
configuration that produced each.

## It refuses more than it reports

That is the design, not a limitation. `measure.sh` declines to print a number when:

- **the machine is not idle** — load average above 4 on ten cores
- **the arm is not repeatable** — three batches of six, gated on the coefficient of variation of the
  batch minima: 2% native, 6% JIT
- **the result carries no harness version or runtime identity** — every `RESULT` line stamps both

The refusals earn their keep. Read the JIT threshold in particular: measured repeatability on this graph
is **0.05% native and 5.49% JIT**. A JIT difference under about 6% is not a difference, and the harness
will not let you report one.

## What the benchmark itself asserts

The harness in the benchmark refuses to print a `RESULT` line unless it can prove what it ran:

- the requested clock mode and record type **actually took effect** — resolved values are echoed and
  cross-checked against what was asked for
- **the audit sink saw records**, and the records were non-empty
- **the graph produced the expected checksum**, so two arms did the same work

Each of those assertions exists because its absence produced a plausible wrong number:

| the assertion | the failure it now catches |
|---|---|
| clock mode took effect | `-D` placed *after* the main class is a program argument, not a system property. Native parses it; the JVM ignores it. Two arms silently ran different configurations. |
| the sink saw records | `LOW_LATENCY_AUDIT` once disabled node registration and therefore the audit log. The benchmark got much faster because it was measuring nothing. |
| the checksum matches | An arm was compared against a different arm's graph. |
| harness version stamped | An h5 binary passed an h3 band. Comparing a declared value against another declared value checks nothing — the version is read from the **binary's own output**. |

## Native: build three times, and use the mean

The single most important fact for anyone measuring a native image here: **rebuilding the same
configuration moves an audited figure by up to ±8 ns.** The PGO profile decides which regime a build
lands in, and profile *collection* is itself a measurement — two collections of one workload minutes
apart differ in over a thousand call-count contexts.

So an effect smaller than about 8 ns cannot be established by one build per arm, however repeatable that
one binary is. Build three, and:

- **minimise *within* a build** — that removes measurement noise, and is what `measure.sh` does
- **average *across* builds** — minimising across builds samples the lucky tail of the lottery and
  reports a figure no deployment will see

A useful non-parametric check with three builds each: if all three of one arm fall below all three of the
other, that is 9 of 9 pairings, p ≈ 0.05 under a no-effect null. Anything less and the arms are not
separated.

## Change one thing

Known inputs that move an audited figure, to be varied one at a time:

| input | effect |
|---|---|
| processor escaping the loop method | **4.3× on native**, nothing on JIT |
| `-H:-SpawnIsolates` | −24 ns audited |
| record format, text vs binary | 3.2× sparse, **9.5× dense** |
| audit density | the variable behind most of the spread |
| GC epsilon vs serial | 0–14 ns |
| PGO profile SHA | decides which regime a build lands in |
| build lottery, same config rebuilt | **±8 ns audited** |

## Profile before you optimise — and measure before you believe the profile

Both halves matter, and this project learned the second one the hard way.

Two rounds of harness discipline — interleaving, build lotteries, runtime digests, refusing unrepeatable
results — never found that **56% of the audited path was resolving names that never change**. One JFR
profile did, and fixing what it named took 24% off the JIT figure.

But the same profile also showed `EventLogger.info` as the leaf frame in **71% of samples** on the
no-audit control. Building an arm with the audit call sites physically deleted put the real cost at
**1.40 ns across 11.75 call sites**. HotSpot had inlined the node's arithmetic *into* `info`, and the
sampler reported the inlined frame as the leaf.

A sampling profiler gives you **proportions, never magnitudes**, and its attribution is only as honest as
the inlining underneath it. It is also safepoint-biased, which under-reports exactly the tight
straight-line code a latency path is made of.

**Use a profile to generate hypotheses. Use a differential measurement to size them.** In the round that
produced these figures, four hypotheses came from profiles; three were real and one was a phantom, and
only the measurement could tell them apart.
