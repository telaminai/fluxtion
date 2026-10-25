#!/usr/bin/env python3
"""Keep every user-facing Fluxtion version in the docs and READMEs on one source of truth.

The source of truth is the `extra:` block of mkdocs.yml:

    fluxtion_runtime_version   fluxtion-runtime, fluxtion-builder-api
    fluxtion_builder_version   fluxtion-builder, fluxtion-bom, fluxtion-starter-core
    fluxtion_maven_plugin_version   fluxtion-maven-plugin
    fluxtion_repository        the Maven repository that serves them

Pages rendered by mkdocs use the macros directly ({{fluxtion_builder_version}}). Files mkdocs does
not render - README.md files on GitHub and docs/claude.txt - carry literal versions, which this
script checks and, with --write, stamps.

Checks (exit 1 on any finding):
  1. A literal `com.telamin.fluxtion:<artifact>:<version>` coordinate, a Maven
     `<artifactId>fluxtion-*</artifactId><version>..</version>` pair or a `<fluxtion.version>`
     property names a version other than the source of truth, or an artifact it does not manage;
     or a legacy coordinate (com.fluxtion.dataflow, dataflow-maven-plugin, the restclient) appears.
  2. A `{{ fluxtion_* }}` macro names a variable mkdocs.yml does not define (it would render empty).
  3. A JBang `//DEPS com.telamin.fluxtion...` line has no `//REPOS` naming the repository above it,
     or a file showing a Fluxtion Maven/Gradle dependency never declares the repository
     (`<url>...</url>` / `maven("...")`) - Maven Central does not carry current releases.

Usage: python3 scripts/check-doc-versions.py [--write]
Standard library only; run from anywhere.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent

ARTIFACT_KEY = {
    "fluxtion-runtime": "fluxtion_runtime_version",
    "fluxtion-builder-api": "fluxtion_runtime_version",
    "fluxtion-builder": "fluxtion_builder_version",
    "fluxtion-bom": "fluxtion_builder_version",
    "fluxtion-starter-core": "fluxtion_builder_version",
    "fluxtion-maven-plugin": "fluxtion_maven_plugin_version",
}
# Coordinates from before the com.telamin.fluxtion line; they do not work with current releases.
LEGACY = re.compile(r"com\.fluxtion\.dataflow|dataflow-maven-plugin|fluxtion-generator-restclient|com\.fluxtion:fluxtion")
VERSION = r"\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?"
COORDINATE = re.compile(r"(com\.telamin\.fluxtion:)([a-z0-9-]+)(:)(" + VERSION + r")")
XML_PAIR = re.compile(
    r"(<artifactId>)(fluxtion-[a-z0-9-]+)(</artifactId>\s*<version>)(" + VERSION + r")(</version>)")
PROPERTY = re.compile(r"(<fluxtion\.version>)(" + VERSION + r")(</fluxtion\.version>)")
MACRO = re.compile(r"\{\{\s*(?:config\.extra\.)?(fluxtion_[a-z_]+)\s*\}\}")


def load_truth():
    text = (ROOT / "mkdocs.yml").read_text(encoding="utf-8")
    block = text.split("\nextra:", 1)[1]
    block = re.split(r"\n\S", block, maxsplit=1)[0]  # up to the next top-level key
    extra = dict(re.findall(r"^  ([a-z_]+):[ \t]*\"?([^\"\s#]+)", block, re.M))
    for key in ("fluxtion_runtime_version", "fluxtion_builder_version", "fluxtion_maven_plugin_version",
                "fluxtion_repository"):
        if key not in extra:
            sys.exit(f"mkdocs.yml extra: is missing {key}")
    return extra


def files():
    yield from sorted(ROOT.glob("README.md"))
    yield from sorted(ROOT.glob("*/README.md"))
    yield from sorted((ROOT / "docs").rglob("*.md"))
    yield from sorted((ROOT / "docs").rglob("*.txt"))


def check(path, text, truth):
    findings = []
    rel = path.relative_to(ROOT)

    def want(artifact):
        key = ARTIFACT_KEY.get(artifact)
        return truth[key] if key else None

    def line_of(pos):
        return text.count("\n", 0, pos) + 1

    def pin(m, artifact, current, rebuild):
        expected = want(artifact)
        if expected is None:
            findings.append(f"{rel}:{line_of(m.start())}: {artifact}:{current} is not a managed "
                            f"Fluxtion artifact (obsolete? remove it or add it to ARTIFACT_KEY)")
            return m.group(0)
        if current != expected:
            findings.append(f"{rel}:{line_of(m.start())}: {artifact} {current} -> {expected}")
        return rebuild(expected)

    out = COORDINATE.sub(lambda m: pin(m, m.group(2), m.group(4),
                                       lambda v: m.group(1) + m.group(2) + m.group(3) + v), text)
    out = XML_PAIR.sub(lambda m: pin(m, m.group(2), m.group(4),
                                     lambda v: m.group(1) + m.group(2) + m.group(3) + v + m.group(5)), out)
    out = PROPERTY.sub(lambda m: pin(m, "fluxtion-runtime", m.group(2),
                                     lambda v: m.group(1) + v + m.group(3)), out)

    for m in LEGACY.finditer(out):
        findings.append(f"{rel}:{line_of(m.start())}: legacy coordinate {m.group(0)!r} - use com.telamin.fluxtion artifacts")

    for m in MACRO.finditer(text):
        if m.group(1) not in truth:
            findings.append(f"{rel}:{line_of(m.start())}: macro {m.group(1)} is not defined in mkdocs.yml extra:")

    repo = truth["fluxtion_repository"]
    lines = text.split("\n")
    for i, line in enumerate(lines):
        if line.startswith("//DEPS") and "com.telamin.fluxtion" in line:
            window = lines[max(0, i - 5):i]
            if not any(l.startswith("//REPOS") and (repo in l or "fluxtion_repository" in l) for l in window):
                findings.append(f"{rel}:{i + 1}: //DEPS on a Fluxtion artifact without a //REPOS naming {repo}")
    repo_url = re.compile(r"<url>\s*(?:" + re.escape(repo) + r"|\{\{\s*fluxtion_repository\s*\}\})\s*</url>")
    if re.search(r"<artifactId>fluxtion-(bom|builder|runtime|builder-api)</artifactId>", text) \
            and not repo_url.search(text):
        findings.append(f"{rel}: shows a Fluxtion Maven dependency but no <repository><url>{repo}</url>")
    gradle_repo = re.compile(r"maven\s*\(\s*\"(?:" + re.escape(repo) + r"|\{\{\s*fluxtion_repository\s*\}\})\"")
    if re.search(r"\"com\.telamin\.fluxtion:", text) and re.search(r"implementation\(|compileOnly\(", text) \
            and not gradle_repo.search(text):
        findings.append(f"{rel}: shows a Gradle Fluxtion dependency but no maven(\"{repo}\") repository")
    return findings, out


def main():
    write = "--write" in sys.argv[1:]
    truth = load_truth()
    all_findings = []
    for path in files():
        text = path.read_text(encoding="utf-8")
        findings, out = check(path, text, truth)
        if write and out != text:
            path.write_text(out, encoding="utf-8")
            findings = check(path, out, truth)[0]
        all_findings += findings
    for f in all_findings:
        print(f)
    print(f"[check-doc-versions] runtime {truth['fluxtion_runtime_version']}, builder/BOM "
          f"{truth['fluxtion_builder_version']}: {len(all_findings)} finding(s)")
    return 1 if all_findings else 0


if __name__ == "__main__":
    sys.exit(main())
