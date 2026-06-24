# Agent guidance

This is a public repository. Do not add private repository names or locations,
commercial plans, credentials, entitlement details, unpublished artifact
coordinates, internal service URLs, or other confidential implementation context
to source, documentation, tests, commit messages, or generated artifacts.

If work is coordinated by a private project, keep its plans, status, decisions,
and hand-off notes in the private repository. Only copy the public implementation
requirements needed for an accepted open-source change into this repository.

## Working rules

- Inspect the worktree before editing and preserve unrelated or uncommitted work.
- At the start of a work session, check the branch, `git status --short`, and recent
  commits. When continuing work, inspect the relevant diff
  before rereading whole files.
- Search source, tests, POMs, resources, and relevant documentation first. Ignore
  `target/`, build outputs, generated reports, IDE metadata, caches, logs, and
  binaries by default.
- Inspect ignored/build-output directories only when a specific investigation
  requires compiled bytecode, packaged JAR contents, generated source, test reports,
  or build logs. Keep that inspection narrowly scoped.
- Never edit generated or compiled output directly. Change its source or build
  configuration and regenerate it.
- Use `rg` with explicit paths and exclusions. Avoid unrestricted repository dumps,
  broad `find` output, and reading large files in full when a symbol search or
  targeted line range is sufficient.
- Limit inspection to the active task and acceptance criterion. Stop once there is
  enough evidence; do not expand scope opportunistically.
- Capture or compare baselines against a clean base revision; do not overwrite
  active feature-branch work to obtain a baseline.
- Prefer Git object inspection for clean-baseline comparisons. Create a separate
  worktree only when the baseline must be built or executed.
- Use the Maven wrapper. Use Java 21 for verification; the default shell/IDE Java
  may not be suitable.
- Run module-specific builds and focused tests before a full reactor build. Use
  quiet Maven output first; rerun verbosely only to diagnose a failure, and retain
  only the relevant error section.
- Keep generated processors dependent on `fluxtion-runtime` only.
- Keep the public build independent of private repositories, credentials, and
  unpublished artifacts.

## Common commands

Repository build:

```shell
./mvnw -q test
```

Module-focused tests should use `-am` and, when filtering tests:

```shell
-Dsurefire.failIfNoSpecifiedTests=false
```
