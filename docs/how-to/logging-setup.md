# How-to: Configure log4j2 in Fluxtion
---

This guide explains how logging is configured for Fluxtion and its examples. It covers how Log4j2 finds and loads its
configuration, how java.util.logging (JUL) is routed to Log4j2, and it documents a runnable example that you can try.

Fluxtion runtime logging usees no logging libraries but relies on java util logging (JUL) for its core logging. To
integrate
with Log4j2, we use the Log4j2 JUL bridge. Other logging libraries can be used as well, and will require a similar
bridging setup.

## Logging framework used

- Log framework: java util logging
- JUL bridge: log4j-jul (routes java.util.logging to Log4j2)
- Typical usage in example projects:
    - Prefer passing JVM system properties at launch so they apply before any classes are initialized.
    - The example code also shows how you could set the JUL bridge in code, but it is commented out.

### Key JVM properties

- Route JUL to Log4j2: <br/>`-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`
- Point Log4j2 at an explicit config file (optional when using standard names/locations): <br/>
  `-Dlog4j.configurationFile=log4j2.yaml`

## How Log4j2 finds its configuration

Log4j2 looks for its configuration in the following common ways (simplified):

- Explicit file via system property: if you pass
  <br/>`-Dlog4j.configurationFile=<path-or-classpath-url>`,
  <br/>Log4j2 uses that. This can be a file path (e.g. `log4j2.yaml`) or a classpath URL (e.g. `classpath:log4j2.yaml`).
- Default names on the classpath: if no property is set, Log4j2 scans the classpath for standard names such as
  <br/>`log4j2.xml`, `log4j2.yaml`/`yml`, or `log4j2.json`/`jsn`.
- Precedence: an explicit `-Dlog4j.configurationFile` takes precedence over default discovery.

In the example below we put `log4j2.yaml` at the module root and run the JVM with
`-Dlog4j.configurationFile=log4j2.yaml`
so it is unambiguous which file is used.

## What the example configuration contains (at a glance)

A reference copy of the config lives at: [log4j2.yaml]({{fluxtion_example_src}}/sample-apps/log-config-demo/log4j2.yaml)
(in the examples repository). The sample `log4j2.yaml` defines:

- Console appender with a minimal pattern for readability.
- File appenders writing to:
    - `data/log/app.log` (general application log)
- Logger levels:
    - Package `com.telamin` at DEBUG (noisy for demo purposes).
    - Root logger at INFO.

## Runnable example: Log Config Demo

- Source [examples repo]({{fluxtion_example_src}}/sample-apps/log-config-demo)

What the app does:

- Reads lines from `data/input.txt` using a simple file event feed.
- For each line, logs a message using Log4j2 (via Lombok's `@Log4j2`).
- With the JUL bridge active, any JUL-based logs from dependencies are routed to Log4j2 as well.

### How to run it

Requirements: Java 21+, Maven.

- Linux/macOS:
  ```bash
  cd sample-apps/log-config-demo
  ./run.sh
  ```
- Windows:
  ```bat
  cd sample-apps\log-config-demo
  run.bat
  ```

The scripts will:

- Build the module with Maven.
- Launch the app with JVM args:
    - `--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED`
    - `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`
    - `-Dlog4j.configurationFile=log4j2.yaml`

### Expected output

- Console shows messages like:
    - `flow in:alpha`
    - `flow in:beta`
    - `flow in:gamma`
- Logs are written under `sample-apps/log-config-demo/log/`:
    - `app.log` (general messages)

If you rerun after editing `data/input.txt`, the new lines are reprocessed and logged accordingly.

## Bridging java.util.logging (JUL) to Log4j2

To ensure any JUL-based logs are routed through Log4j2, pass:

```
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
```

Notes:

- This must be set before any JUL loggers are initialized, which is why setting it on the command line is preferred.
- The example also includes the line below (commented out) to illustrate the alternative in-code approach:
  ```java
  // System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
  ```

## Customizing for your project

- Change patterns, appenders, or log levels in `log4j2.yaml`.
- Move the config if you prefer, and update the launch arg `-Dlog4j.configurationFile` accordingly.
- If you package the config on the classpath under a standard name (e.g. `log4j2.xml` or `log4j2.yaml`), you can omit
  the
  `-Dlog4j.configurationFile` parameter and let Log4j2's discovery find it.

## Related links

- Example source [examples repo]({{fluxtion_example_src}}/sample-apps/log-config-demo)
- Sample log4j2 config [log4j2.yaml]({{fluxtion_example_src}}/sample-apps/log-config-demo/log4j2.yaml)
