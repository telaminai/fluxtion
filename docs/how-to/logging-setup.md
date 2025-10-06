# How-to: Configure logging in Fluxtion examples

This guide explains the logging framework used by these examples and points to a runnable sample app that demonstrates
the configuration.

## Logging framework

- The examples use Log4j2 for application logging.
- We route java.util.logging (JUL) through Log4j2 by setting the system property:
    - `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`
- The sample includes the `log4j-jul` bridge dependency so JUL logs are handled by Log4j2.
- Note: In the sample code, the line that sets this property is present but commented out. Prefer setting it via JVM
  args so it applies before class initialization.

## Configuration file

- Log4j2 is configured using a YAML file named `log4j2.yaml` located at the module root of the sample app.
- The configuration defines:
    - Console appender (minimal pattern)
    - File appenders writing to `data/log/app.log`, `data/log/error.log`
    - A YAML-like event audit file at `data/log/auditLog.yaml`
- Logger levels are set for the `com.telamin` package and the root logger.

## Example application

See the sample app: Log Config Demo.

- Source: `sample-apps/log-config-demo`
- Main class: `com.telamin.fluxtion.example.sampleapps.logging.LogConfigDemoRunner`
- Config file: `sample-apps/log-config-demo/log4j2.yaml` (copied from the reference module)
- Input data: `sample-apps/log-config-demo/data/input.txt`

## Running the example

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

- Build the module with Maven
- Run the app with JVM args:
    - `--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED`
    - `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`
    - `-Dlog4j.configurationFile=log4j2.yaml`

## How it works

- The `LogConfigDemoRunner` subscribes to a file event feed and logs each line using Log4j2 (`@Log4j2`).
- With the JUL bridge activated, any JUL-based loggers used by dependencies will also be routed to Log4j2.

## Customizing

- Edit `log4j2.yaml` to change patterns, appenders, or log levels.
- You can point `-Dlog4j.configurationFile` to any other Log4j2 YAML/XML/JSON configuration if desired.

## Related files

- Reference config template: `reference/src/main/resources/log4j2.yaml`
- Sample app README: see the navigation link "Log Config Demo README".
