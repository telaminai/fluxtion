#!/usr/bin/env python3
"""Keep every user-facing Fluxtion version and note in the docs and READMEs on one source of truth.

The source of truth is the `extra:` block of mkdocs.yml:

    fluxtion_runtime_version        fluxtion-runtime, fluxtion-builder-api
    fluxtion_builder_version        fluxtion-builder, fluxtion-bom, fluxtion-starter-core
    fluxtion_maven_plugin_version   fluxtion-maven-plugin
    fluxtion_repository             the Maven repository that serves the releases
    fluxtion_compiler_url           the hosted compiler's public address (sign-in, CI, token route)
    fluxtion_repository_note        which artifacts are where (Repsy vs Maven Central)
    fluxtion_remote_build_note      what a build sends, and the compiler sign-in it needs
    fluxtion_first_build_note       what the starter's first build needs: hosted generation, the one-time sign-in
    fluxtion_sign_in_line           where to learn to sign in
    fluxtion_sign_in_how_to         the public how-to URL (its #anchor must exist)

Pages rendered by mkdocs use the macros directly ({{fluxtion_builder_version}}). Files mkdocs does
not render - README.md files on GitHub and docs/claude.txt - carry literal versions and generated
note blocks (<!-- fluxtion:NAME --> ... <!-- /fluxtion:NAME -->), which this script checks and,
with --write, stamps.

Checks (exit 1 on any finding):
  1. A literal `com.telamin.fluxtion:<artifact>:<version>` coordinate, a literal <version> inside a
     <dependency>/<plugin>/<parent> naming a com.telamin.fluxtion artifact, or a
     <fluxtion*.version> property names a version other than the source of truth or an artifact
     it does not manage; or a legacy coordinate (com.fluxtion.dataflow, dataflow-maven-plugin,
     the restclient) appears.
  2. A `{{ ... fluxtion_* ... }}` macro names a variable mkdocs.yml does not define.
  3. A JBang `//DEPS com.telamin.fluxtion...` line (indented or not) has no `//REPOS` naming the
     repository above it, or a file showing a Fluxtion Maven/Gradle dependency never declares the
     repository (`<url>...</url>` / `maven("...")`).
  4. README.md or docs/claude.txt lacks a note block, or its text differs from mkdocs.yml.
  5. The anchor of fluxtion_sign_in_how_to is not an id in docs/home/quickstart-aot.md, or that page
     lost the retired anchor #1-getting-an-api-key, or README.md lost #cloud-generation-and-your-api-key;
     builders before the managed-only release print both. Ids inside HTML comments do not count.
  6. A README.md, a docs page or text file (docs/not-used excepted) or mkdocs.yml names the retired
     API-key route (RETIRED_ROUTE: the marketplace's name, the key-file name, the key property or the
     gateway host). Sign-in is the only route. A compiler.* address other than fluxtion_compiler_url, or
     a Fluxtion coordinate in mkdocs.yml's notes off the pinned version, is also a finding. The generated
     error pages (docs/troubleshooting/errors, copied from the compiler) are outside files(), so no check
     here reads them: their text is what a build prints.
  7. Sign-in rule: no page in that scope gives the pre-1.0.90 token-route instructions (FLUXTION_REMOTE_AUTH,
     fluxtion.remote.auth/host, "cannot yet be issued", "until fluxtion-builder 1.0.90"); the remote-build
     note has no backticks (it is verbatim the playground's) and names FLX-1023; and every FLX-nnnn code the
     docs mention has its page under docs/troubleshooting/errors.

--site=<dir> additionally (after `mkdocs build -d <dir>`):
  8. The built quickstart page carries both the sign-in anchor and the retired one as element ids, and the
     FLX-1023 error page is built (every builder from 1.0.90 links to it).

--online additionally (network):
  9. Each pinned version exists: Repsy for the runtime/builder line, Maven Central for the plugin.
     A missing version fails.
  10. Compares versions and notes with the deployed playground llms.txt; a mismatch is a warning
     only, because the two sites deploy independently.

Usage: python3 scripts/check-doc-versions.py [--write] [--site=<built site dir>]
       [--online [--llms=<local llms.txt instead of the site>]]
Standard library only; run from anywhere.
"""
import pathlib
import re
import sys
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
CENTRAL = "https://repo1.maven.org/maven2"
PLAYGROUND_LLMS = "https://fluxtion-playground.dev/llms.txt"

ARTIFACT_KEY = {
    "fluxtion-runtime": "fluxtion_runtime_version",
    "fluxtion-builder-api": "fluxtion_runtime_version",
    "fluxtion-builder": "fluxtion_builder_version",
    "fluxtion-bom": "fluxtion_builder_version",
    "fluxtion-starter-core": "fluxtion_builder_version",
    "fluxtion-maven-plugin": "fluxtion_maven_plugin_version",
}
PROPERTY_KEY = {
    "fluxtion.version": "fluxtion_runtime_version",
    "fluxtion.runtime.version": "fluxtion_runtime_version",
    "fluxtion.api.version": "fluxtion_runtime_version",
    "fluxtion.builder.version": "fluxtion_builder_version",
    "fluxtion.bom.version": "fluxtion_builder_version",
    "fluxtion.maven-plugin.version": "fluxtion_maven_plugin_version",
}
NOTE_BLOCKS = ("repository-note", "remote-build-note")
NOTE_FILES = ("README.md", "docs/claude.txt")
REQUIRED = ("fluxtion_runtime_version", "fluxtion_builder_version", "fluxtion_maven_plugin_version",
            "fluxtion_repository", "fluxtion_compiler_url", "fluxtion_repository_note", "fluxtion_remote_build_note",
            "fluxtion_first_build_note", "fluxtion_sign_in_line", "fluxtion_sign_in_how_to")

VERSION = r"\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?"
COORDINATE = re.compile(r"(com\.telamin\.fluxtion:)([a-z0-9-]+)(:)(" + VERSION + r")")
XML_BLOCK = re.compile(r"<(dependency|plugin|parent|path)>(.*?)</\1>", re.S)
PROPERTY = re.compile(r"(<(fluxtion[.a-z-]*\.version)>)(" + VERSION + r")(</\2>)")
MACRO = re.compile(r"\{\{(.*?)\}\}", re.S)
MACRO_NAME = re.compile(r"\b(fluxtion_[a-z_]+)\b")
LEGACY = re.compile(r"com\.fluxtion\.dataflow|dataflow-maven-plugin|fluxtion-generator-restclient|com\.fluxtion:fluxtion")


def load_truth():
    """Parse mkdocs.yml `extra:` (plain and `>-` folded scalars) without a YAML dependency;
    mkdocs.yml carries a !!python/name tag that a safe YAML loader rejects."""
    text = (ROOT / "mkdocs.yml").read_text(encoding="utf-8")
    block = text.split("\nextra:", 1)[1]
    block = re.split(r"\n\S", block, maxsplit=1)[0]
    extra, lines, i = {}, block.split("\n"), 0
    while i < len(lines):
        m = re.match(r"^  ([a-z_]+):[ \t]*(.*?)\s*$", lines[i])
        i += 1
        if not m:
            continue
        key, value = m.group(1), m.group(2)
        if value in (">-", ">"):
            parts = []
            while i < len(lines) and (lines[i].startswith("    ") or not lines[i].strip()):
                if lines[i].strip():
                    parts.append(lines[i].strip())
                i += 1
            extra[key] = " ".join(parts)
        else:
            value = re.sub(r"\s+#.*$", "", value).strip().strip('"')
            extra[key] = value
    missing = [k for k in REQUIRED if k not in extra]
    if missing:
        sys.exit(f"mkdocs.yml extra: is missing {', '.join(missing)}")
    return extra


def note_text(name, truth):
    if name == "repository-note":
        return truth["fluxtion_repository_note"]
    return (f"**Generation is remote.** {truth['fluxtion_remote_build_note']} {truth['fluxtion_sign_in_line']}\n\n"
            f"**First build:** {truth['fluxtion_first_build_note']}")


def render_block(name, truth):
    return (f"<!-- fluxtion:{name} - generated from mkdocs.yml extra: by scripts/check-doc-versions.py "
            f"--write; edit there -->\n{note_text(name, truth)}\n<!-- /fluxtion:{name} -->")


def files():
    skip = {"target", ".git", "node_modules", "site"}
    for p in sorted(ROOT.rglob("README.md")):
        if not skip.intersection(p.relative_to(ROOT).parts):
            yield p
    # docs/troubleshooting/errors/ holds the compiler's generated error pages, copied verbatim: their text is what a
    # build prints, so it names the versions of the builder that emits it, not this site's pins.
    generated = ROOT / "docs" / "troubleshooting" / "errors"
    for p in sorted((ROOT / "docs").rglob("*.md")) + sorted((ROOT / "docs").rglob("*.txt")):
        if generated not in p.parents:
            yield p


def check(path, text, truth):
    findings = []
    rel = path.relative_to(ROOT).as_posix()

    def line_of(pos, src):
        return src.count("\n", 0, pos) + 1

    def expected(key, label, pos, src):
        if key is None:
            findings.append(f"{rel}:{line_of(pos, src)}: {label} is not a managed Fluxtion artifact or property "
                            f"(obsolete? remove it or add it to ARTIFACT_KEY/PROPERTY_KEY)")
            return None
        return truth[key]

    def coordinate(m):
        want = expected(ARTIFACT_KEY.get(m.group(2)), f"{m.group(2)}:{m.group(4)}", m.start(), text)
        if want is None or want == m.group(4):
            return m.group(0)
        findings.append(f"{rel}:{line_of(m.start(), text)}: {m.group(2)} {m.group(4)} -> {want}")
        return m.group(1) + m.group(2) + m.group(3) + want

    out = COORDINATE.sub(coordinate, text)

    def xml_block(m):
        body = m.group(2)
        if not re.search(r"<groupId>\s*com\.telamin\.fluxtion\s*</groupId>", body):
            return m.group(0)
        artifact = re.search(r"<artifactId>\s*([^<\s]+)\s*</artifactId>", body)
        version = re.search(r"(<version>)\s*(" + VERSION + r")\s*(</version>)", body)
        if not artifact or not version:
            return m.group(0)
        want = expected(ARTIFACT_KEY.get(artifact.group(1)), f"{artifact.group(1)}:{version.group(2)}",
                        m.start(), out)
        if want is None or want == version.group(2):
            return m.group(0)
        findings.append(f"{rel}:{line_of(m.start(), out)}: {artifact.group(1)} <version> {version.group(2)} -> {want}")
        return m.group(0).replace(version.group(0), version.group(1) + want + version.group(3), 1)

    out = XML_BLOCK.sub(xml_block, out)

    def prop(m):
        want = expected(PROPERTY_KEY.get(m.group(2)), f"<{m.group(2)}>{m.group(3)}", m.start(), out)
        if want is None or want == m.group(3):
            return m.group(0)
        findings.append(f"{rel}:{line_of(m.start(), out)}: <{m.group(2)}> {m.group(3)} -> {want}")
        return m.group(1) + want + m.group(4)

    out = PROPERTY.sub(prop, out)

    for m in LEGACY.finditer(out):
        findings.append(f"{rel}:{line_of(m.start(), out)}: legacy coordinate {m.group(0)!r} - use com.telamin.fluxtion artifacts")

    # docs/not-used/ pages already fail macro rendering on Jekyll {% highlight %} tags, so their
    # {{ }} never render; checking their (dead) variable names would only add noise.
    for m in ([] if rel.startswith("docs/not-used/") else MACRO.finditer(text)):
        for name in MACRO_NAME.findall(m.group(1)):
            if name not in truth:
                findings.append(f"{rel}:{line_of(m.start(), text)}: macro {name} is not defined in mkdocs.yml extra:")

    repo = truth["fluxtion_repository"]
    lines = out.split("\n")
    for i, line in enumerate(lines):
        stripped = line.lstrip(" \t>-*")
        if stripped.startswith("//DEPS") and "com.telamin.fluxtion" in stripped:
            window = [w.lstrip(" \t>-*") for w in lines[max(0, i - 5):i]]
            if not any(w.startswith("//REPOS") and (repo in w or "fluxtion_repository" in w) for w in window):
                findings.append(f"{rel}:{i + 1}: //DEPS on a Fluxtion artifact without a //REPOS naming {repo}")
    repo_url = re.compile(r"<url>\s*(?:" + re.escape(repo) + r"|\{\{\s*fluxtion_repository\s*\}\})\s*</url>")
    if re.search(r"<artifactId>\s*fluxtion-(bom|builder|runtime|builder-api)\s*</artifactId>", out) \
            and not repo_url.search(out):
        findings.append(f"{rel}: shows a Fluxtion Maven dependency but no <repository><url>{repo}</url>")
    gradle_repo = re.compile(r"maven\s*\(\s*\"(?:" + re.escape(repo) + r"|\{\{\s*fluxtion_repository\s*\}\})\"")
    if re.search(r"\"com\.telamin\.fluxtion:", out) and re.search(r"implementation\(|compileOnly\(", out) \
            and not gradle_repo.search(out):
        findings.append(f"{rel}: shows a Gradle Fluxtion dependency but no maven(\"{repo}\") repository")

    if rel in NOTE_FILES:
        for name in NOTE_BLOCKS:
            pattern = re.compile(r"<!-- fluxtion:" + name + r"\b.*?<!-- /fluxtion:" + name + r" -->", re.S)
            m = pattern.search(out)
            want = render_block(name, truth)
            if not m:
                findings.append(f"{rel}: missing the <!-- fluxtion:{name} --> block")
            elif m.group(0) != want:
                findings.append(f"{rel}:{line_of(m.start(), out)}: {name} block differs from mkdocs.yml (run --write)")
                out = out[:m.start()] + want + out[m.end():]
    return findings, out


def slug(heading):
    s = re.sub(r"[^\w\s-]", "", heading.strip().lower())
    return re.sub(r"[\s]+", "-", s)


QUICKSTART_AOT = "docs/home/quickstart-aot.md"
QUICKSTART_AOT_URL = "https://telaminai.github.io/fluxtion/home/quickstart-aot/"
# Builders before the managed-only release print these two links in their missing-key message: they must keep
# landing on the sign-in explanation, not a 404, for as long as those builders are in use.
LEGACY_ANCHOR = "1-getting-an-api-key"
LEGACY_README_ANCHOR = "cloud-generation-and-your-api-key"
# The API-key route retired on 2026-11-05: the marketplace's name (any spacing), the key-file name (any
# separators), the key system property and the gateway host. Spelled in pieces so this file does not match itself.
RETIRED_ROUTE = re.compile(r"rapid[\s_-]*" + r"api|api[\s_.-]*key[\s_.-]*" + r"file|fluxtion\.api" + r"key\b"
                           r"|source[\s-]*" + r"gen\.p\.", re.I)
# The interim token-route instructions for builders before 1.0.90; from 1.0.90 one sign-in serves every build.
STALE_SIGN_IN = re.compile(r"FLUXTION_REMOTE_AUTH|fluxtion\.remote\.(?:auth|host)|cannot yet be issued"
                           r"|until fluxtion-builder 1\.0\.90", re.I)
ERRORS_DIR = "docs/troubleshooting/errors"
FLX_CODE = re.compile(r"\bFLX-\d{4}\b")
SIGN_IN_ERROR = "FLX-1023"
COMPILER_URL = re.compile(r"https?://compiler[\w.-]*", re.I)
HTML_COMMENT = re.compile(r"<!--.*?-->", re.S)


def page_ids(text):
    """Ids a reader can link to: heading slugs, explicit `{#id}` attributes and `<a id="...">` anchors.
    Commented-out markup is not an anchor."""
    text = HTML_COMMENT.sub("", text)
    ids = set()
    for h in re.findall(r"^#+\s+(.*)$", text, re.M):
        explicit = re.search(r"\{\s*#([\w-]+)[^}]*\}\s*$", h)
        ids.add(explicit.group(1) if explicit else slug(h))
    ids.update(re.findall(r"""<a\s+(?:id|name)=["']([^"']+)["']""", text))
    return ids


def check_anchor(truth):
    url = truth["fluxtion_sign_in_how_to"]
    anchor = url.split("#", 1)[1] if "#" in url else None
    page = ROOT / QUICKSTART_AOT
    ids = page_ids(page.read_text(encoding="utf-8"))
    findings = []
    if not url.startswith(QUICKSTART_AOT_URL) or anchor not in ids:
        findings.append(f"mkdocs.yml: fluxtion_sign_in_how_to {url} does not match an id in {QUICKSTART_AOT}")
    if LEGACY_ANCHOR not in ids:
        findings.append(f"{QUICKSTART_AOT}: lost the retired anchor #{LEGACY_ANCHOR}; released builders link to it")
    if LEGACY_README_ANCHOR not in page_ids((ROOT / "README.md").read_text(encoding="utf-8")):
        findings.append(f"README.md: lost the anchor #{LEGACY_README_ANCHOR}; released builders link to it")
    return findings


def retired_scope():
    yield ROOT / "mkdocs.yml"
    for p in files():
        if not p.relative_to(ROOT).as_posix().startswith("docs/not-used/"):
            yield p


def check_retired_route(truth):
    findings = []
    pages = {p.stem for p in (ROOT / ERRORS_DIR).glob("FLX-*.md")}
    for path in retired_scope():
        rel = path.relative_to(ROOT).as_posix()
        for n, line in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
            for code in sorted(set(FLX_CODE.findall(line))):
                if code not in pages:
                    findings.append(f"{rel}:{n}: {code} has no page in {ERRORS_DIR} (copy it from the compiler's "
                                    f"generated docs/errors)")
            s = STALE_SIGN_IN.search(line)
            if s:
                findings.append(f"{rel}:{n}: gives the pre-1.0.90 token-route instructions ({s.group(0)!r}); "
                                f"one sign-in serves every build")
            m = RETIRED_ROUTE.search(line)
            if m:
                findings.append(f"{rel}:{n}: names the retired API-key route ({m.group(0)!r}); "
                                f"describe compiler sign-in instead")
            for u in COMPILER_URL.findall(line):
                if u.rstrip(".") != truth["fluxtion_compiler_url"]:
                    findings.append(f"{rel}:{n}: compiler address {u} is not fluxtion_compiler_url "
                                    f"{truth['fluxtion_compiler_url']}")
    note = truth["fluxtion_remote_build_note"]
    if "`" in note:
        findings.append("mkdocs.yml: fluxtion_remote_build_note has backticks; keep it verbatim the playground's plain text")
    if SIGN_IN_ERROR not in note:
        findings.append(f"mkdocs.yml: fluxtion_remote_build_note does not name {SIGN_IN_ERROR}")
    # mkdocs.yml's notes are rendered verbatim, so a coordinate in them must name the pinned version
    extra = (ROOT / "mkdocs.yml").read_text(encoding="utf-8").split("\nextra:", 1)[1]
    for m in COORDINATE.finditer(extra):
        key = ARTIFACT_KEY.get(m.group(2))
        if key and truth[key] != m.group(4):
            findings.append(f"mkdocs.yml: {m.group(2)}:{m.group(4)} in extra: is not the pinned {truth[key]}")
    return findings


def check_site(site, truth):
    page = pathlib.Path(site) / "home/quickstart-aot/index.html"
    if not page.is_file():
        return [f"--site: {page} not found (run mkdocs build -d {site} first)"]
    html = HTML_COMMENT.sub("", page.read_text(encoding="utf-8"))
    want = [truth["fluxtion_sign_in_how_to"].split("#", 1)[1], LEGACY_ANCHOR]
    findings = [f"{page}: no element with id=\"{a}\"" for a in want
                if not re.search(r"""\bid=["']""" + re.escape(a) + r"""["']""", html)]
    error_page = pathlib.Path(site) / "troubleshooting/errors" / SIGN_IN_ERROR / "index.html"
    if not error_page.is_file():
        findings.append(f"{error_page}: the {SIGN_IN_ERROR} page was not built; every builder from 1.0.90 links to it")
    return findings


def fetch(url):
    for attempt in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "fluxtion-docs-check/1 (+github.com/telaminai/fluxtion)"})
            with urllib.request.urlopen(req, timeout=20) as r:
                return r.read().decode("utf-8", "replace")
        except Exception as e:  # noqa: BLE001 - report after retries
            if getattr(e, "code", None) == 404:
                return None
            if attempt == 2:
                raise
            time.sleep(2)


def online(truth, llms_file=None):
    findings, warnings = [], []
    repo = truth["fluxtion_repository"]
    pins = [(repo, a, truth[k]) for a, k in ARTIFACT_KEY.items() if a != "fluxtion-maven-plugin"]
    pins.append((CENTRAL, "fluxtion-maven-plugin", truth["fluxtion_maven_plugin_version"]))
    for base, artifact, version in pins:
        meta = fetch(f"{base}/com/telamin/fluxtion/{artifact}/maven-metadata.xml") or ""
        if f"<version>{version}</version>" not in meta:
            findings.append(f"online: com.telamin.fluxtion:{artifact}:{version} is not published at {base}")
    try:
        llms = pathlib.Path(llms_file).read_text(encoding="utf-8") if llms_file else (fetch(PLAYGROUND_LLMS) or "")
    except Exception as e:  # noqa: BLE001 - the cross-site comparison is advisory
        return findings, [f"online: could not fetch {PLAYGROUND_LLMS}: {e}"]

    def norm(s):
        return re.sub(r"\s+", " ", s.replace("`", "")).strip()

    for artifact in ("fluxtion-bom", "fluxtion-builder", "fluxtion-runtime"):
        m = re.search(r"com\.telamin\.fluxtion:" + artifact + r":(" + VERSION + r")", llms)
        want = truth[ARTIFACT_KEY[artifact]]
        if not m or m.group(1) != want:
            warnings.append(f"online: {PLAYGROUND_LLMS} shows {artifact} {m.group(1) if m else 'nothing'}, docs pin {want}")
    for key in ("fluxtion_remote_build_note", "fluxtion_first_build_note", "fluxtion_repository_note"):
        if norm(truth[key]) not in norm(llms):
            warnings.append(f"online: {PLAYGROUND_LLMS} does not carry {key} verbatim (update both sites together)")
    return findings, warnings


def main():
    args = sys.argv[1:]
    write, net = "--write" in args, "--online" in args
    truth = load_truth()
    all_findings = check_anchor(truth) + check_retired_route(truth)
    site = next((a.split("=", 1)[1] for a in args if a.startswith("--site=")), None)
    if site:
        all_findings += check_site(site, truth)
    for path in files():
        text = path.read_text(encoding="utf-8")
        findings, out = check(path, text, truth)
        if write and out != text:
            path.write_text(out, encoding="utf-8")
            findings = check(path, out, truth)[0]
        all_findings += findings
    warnings = []
    if net:
        llms_file = next((a.split("=", 1)[1] for a in args if a.startswith("--llms=")), None)
        f, warnings = online(truth, llms_file)
        all_findings += f
    for f in all_findings:
        print(f)
    for w in warnings:
        print(f"::warning::{w}")
    print(f"[check-doc-versions] runtime {truth['fluxtion_runtime_version']}, builder/BOM "
          f"{truth['fluxtion_builder_version']}, plugin {truth['fluxtion_maven_plugin_version']}: "
          f"{len(all_findings)} finding(s), {len(warnings)} warning(s)")
    return 1 if all_findings else 0


if __name__ == "__main__":
    sys.exit(main())
