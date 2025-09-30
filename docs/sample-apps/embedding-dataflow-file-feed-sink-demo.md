# Guide: Build, deploy, and run a sample app that embeds a DataFlow

This guide explains how to build, deploy, and run a simple Fluxtion application that embeds a DataFlow. The application
reads lines from a file, processes them through a DataFlow pipeline, and writes the transformed output to another file.

We will use the ready-made sample from this repository as the reference implementation:

- Sample module: `sample-apps/file-feed-sink-demo`
- Main class: `com.telamin.fluxtion.example.sampleapps.FileFeedSinkDemo`

The sample wires together these Fluxtion components:

- FileEventFeed: streams lines from an input file as events
- DataFlow: subscribes to the feed, logs and transforms each event (upper-case)
- FileMessageSink: writes the transformed result to an output file

For background, the “getting-started” tutorial [TutorialPart5](../getting-started/tutorial-part-5.md) demonstrates a similar pipeline.

## 1. Prerequisites

- Java 21+
- Maven 3.8+ (build time only)

## 2. Project layout (sample)

Within this repository, navigate to the sample module:

- `sample-apps/file-feed-sink-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/FileFeedSinkDemo.java` – main
  app
- `sample-apps/file-feed-sink-demo/data/input.txt` – example input data
- `sample-apps/file-feed-sink-demo/data/output.txt` – output file (created when the app runs)
- `sample-apps/file-feed-sink-demo/start.sh` – helper script to build and run the app
- `sample-apps/file-feed-sink-demo/stop.sh` – stops the background run started by `start.sh --bg`

## 3. Build: create a deployable fat-jar

The sample is configured to build a shaded (fat) jar that includes all runtime dependencies and the correct
`Main-Class`.

From the sample module directory:

```bash
cd sample-apps/file-feed-sink-demo
mvn -DskipTests package
```

This produces:

```
target/file-feed-sink-demo-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Why a fat-jar? It’s a single, self-contained artifact that you can copy to another machine and run with `java -jar` — no
Maven needed on the target.

## 4. Run locally

You can run the app in several ways.

A. Using the helper script (foreground):

```bash
./start.sh
```

Reads `./data/input.txt` and writes `./data/output.txt`.

B. Using the helper script (background) with overrides:

```bash
./start.sh --bg --input ./data/input.txt --output ./data/output.txt
# Later stop it:
./stop.sh
```

C. Running the fat-jar directly:

```bash
java --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
     -jar target/file-feed-sink-demo-1.0-SNAPSHOT-jar-with-dependencies.jar \
     ./data/input.txt ./data/output.txt
```

Note: the `--add-opens` option is required because the runtime uses `jdk.internal.misc.Unsafe` via Agrona.

## 5. Observe the pipeline

To see the DataFlow acting on new input lines in real time, you can tail the output and append to the input.

- Terminal 1: start the app (foreground or background)

```bash
./start.sh --bg
```

- Terminal 2: tail the output file

```bash
tail -f ./data/output.txt
```

- Terminal 3: append new lines to the input file

```bash
echo "new event one" >> ./data/input.txt
echo "another line"   >> ./data/input.txt
```

As you append, the FileEventFeed sees new lines, the DataFlow runs (upper-casing them), and the FileMessageSink pushes
the results into `data/output.txt`. You will see the transformed lines appear in real time in the `tail` output.

## 6. Deploy to another machine

Because the app is packaged as a fat-jar, deployment is straightforward:

1) Build on your development machine:

```bash
cd sample-apps/file-feed-sink-demo
mvn -DskipTests package
```

2) Copy the fat-jar and any desired data files to the target machine:

```bash
scp target/file-feed-sink-demo-1.0-SNAPSHOT-jar-with-dependencies.jar user@server:/opt/fluxtion-demo/
scp -r data user@server:/opt/fluxtion-demo/
```

3) On the target machine, run with Java 21+:

```bash
cd /opt/fluxtion-demo
java --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
     -jar file-feed-sink-demo-1.0-SNAPSHOT-jar-with-dependencies.jar \
     ./data/input.txt ./data/output.txt
```

## 7. Troubleshooting

- Missing `--add-opens` flag: If you see an error about `jdk.internal.misc.Unsafe`, add
  `--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED` to your `java -jar` command line.
- File paths: Prefer absolute or carefully chosen relative paths for input/output when deploying to different
  directories.
- Permissions: Ensure the process can read the input file and write to the output directory.

## 8. Where to look in the source

- Sample app module (this repository): `sample-apps/file-feed-sink-demo`
- Main application code:
  `sample-apps/file-feed-sink-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/FileFeedSinkDemo.java`
- Build configuration (fat-jar): `sample-apps/file-feed-sink-demo/pom.xml` (via `maven-shade-plugin`)

---

If you maintain the separate `fluxtion` documentation site/repo, you can copy or adapt this guide into:
`<fluxtion repo>/docs/sample-apps/` and reference this sample at
`<fluxtion-examples repo>/sample-apps/file-feed-sink-demo`.
