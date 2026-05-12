#!/usr/bin/env python3
"""Filter PMD reports to findings on lines added by a diff; fail if any remain.

Drop-in replacement for reviewdog's `-f=pmd -filter-mode=added` pipeline that
removes the external-binary dependency. Used identically by:

  * the pre-commit hook in scripts/git-hooks/pre-commit, fed `git diff --cached
    -U0` so it gates the staged change.
  * the CI lint job in .github/workflows/ci.yml, fed
    `git diff origin/${BASE_REF}...HEAD -U0` so it gates the whole PR diff.

Because both call sites run this exact same script, the local hook and the
remote CI gate are guaranteed to disagree only on inputs, never on logic.

Usage::

    git diff --cached -U0 | scripts/lint/pmd_diff_check.py REPORT [REPORT...]

Exit codes:
  * 0 — no PMD findings on added/modified lines.
  * 1 — at least one finding survived the diff filter.
  * 2 — bad invocation (missing report path).

When run with GITHUB_ACTIONS=true (set automatically by GitHub-hosted runners),
the script also emits ``::error file=...,line=...::...`` workflow commands so
each surviving finding appears as an inline annotation on the PR's checks tab.
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

try:
    # Hardened XML parser — guards against XXE and entity-expansion (billion laughs)
    # attacks. Installed via the project's requirements (and on CI runners) so the
    # remote gate is always hardened; locally the stdlib fallback below is used by
    # developers who haven't installed defusedxml yet.
    from defusedxml import ElementTree as ET
except ImportError:
    # PMD reports are produced locally by our own Gradle pipeline, so the stdlib
    # parser is acceptable as a fallback — modern Python disables external-entity
    # resolution by default, and an attacker capable of poisoning the report has
    # already compromised the build before this script ever runs.
    import xml.etree.ElementTree as ET  # noqa: S314

PMD_NS = "{http://pmd.sourceforge.net/report/2.0.0}"

HUNK_RE = re.compile(
    r"^@@ -\d+(?:,\d+)? \+(?P<start>\d+)(?:,(?P<count>\d+))? @@"
)
# Accept both default git prefixes (`+++ b/path`) and the `diff.noprefix=true` form
# (`+++ path`) so the gate behaves identically regardless of the developer's git config.
NEW_FILE_RE = re.compile(r"^\+\+\+ (?P<path>.+)$")


def _strip_diff_prefix(path: str) -> str:
    """Drop git's a/ or b/ prefix if present so paths match repo-relative form."""
    if path.startswith(("a/", "b/")):
        return path[2:]
    return path


def parse_added_lines(diff_text: str) -> dict[str, set[int]]:
    """Return {repo-relative path -> {added line numbers}} from a -U0 unified diff."""
    added: dict[str, set[int]] = {}
    current: str | None = None
    for line in diff_text.splitlines():
        if line.startswith("+++ "):
            m = NEW_FILE_RE.match(line)
            raw = m.group("path") if m else None
            # /dev/null marks a deletion — no new lines to consider.
            if raw in (None, "/dev/null"):
                current = None
            else:
                current = _strip_diff_prefix(raw)
                added.setdefault(current, set())
            continue
        if current and line.startswith("@@"):
            m = HUNK_RE.match(line)
            if not m:
                continue
            start = int(m.group("start"))
            count = int(m.group("count")) if m.group("count") is not None else 1
            if count == 0:
                continue
            for n in range(start, start + count):
                added[current].add(n)
    return added


def collect_findings(report_paths: list[Path]) -> list[dict]:
    """Flatten PMD report files into a list of {path, line, col, rule, message} dicts."""
    findings: list[dict] = []
    for report in report_paths:
        if not report.is_file() or report.stat().st_size == 0:
            continue
        for file_el in ET.parse(report).getroot().findall(f"{PMD_NS}file"):
            absolute = file_el.get("name", "")
            if not absolute:
                continue
            for v in file_el.findall(f"{PMD_NS}violation"):
                findings.append(
                    {
                        "path": absolute,
                        "line": int(v.get("beginline", "1")),
                        "col": int(v.get("begincolumn", "1")),
                        "rule": v.get("rule", "PMD"),
                        "ruleset": v.get("ruleset", ""),
                        "message": (v.text or v.get("rule", "")).strip(),
                    }
                )
    return findings


def relativise(path: str, repo_root: Path) -> str:
    """Make a PMD-reported absolute path relative to the repo root for diff lookup.

    Always returns POSIX-style forward slashes so comparisons against diff paths
    (which git always emits with forward slashes regardless of OS) match on Windows
    and WSL hosts as reliably as on Linux/macOS.
    """
    try:
        return Path(path).resolve().relative_to(repo_root).as_posix()
    except ValueError:
        # Outside the repo — keep the path but still normalise the slash style.
        return Path(path).as_posix()


def emit_github_annotation(f: dict, relative_path: str) -> None:
    summary = f["message"].replace("\n", " ").strip()
    sys.stdout.write(
        f"::error file={relative_path},line={f['line']},col={f['col']},"
        f"title=PMD {f['rule']}::{summary}\n"
    )


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        sys.stderr.write(
            f"usage: {argv[0]} REPORT [REPORT...] < unified.diff\n"
        )
        return 2

    repo_root = Path.cwd().resolve()
    in_github = os.environ.get("GITHUB_ACTIONS") == "true"

    added = parse_added_lines(sys.stdin.read())
    findings = collect_findings([Path(p) for p in argv[1:]])

    blocking: list[tuple[dict, str]] = []
    for f in findings:
        rel = relativise(f["path"], repo_root)
        if f["line"] in added.get(rel, set()):
            blocking.append((f, rel))

    if not blocking:
        return 0

    sys.stderr.write(
        f"\nPMD: {len(blocking)} finding(s) on lines added by this diff:\n"
    )
    for f, rel in blocking:
        sys.stderr.write(
            f"  {rel}:{f['line']}:{f['col']}  [{f['ruleset']} / {f['rule']}] {f['message']}\n"
        )
        if in_github:
            emit_github_annotation(f, rel)
    sys.stderr.write(
        "\nFix the findings (or commit with --no-verify and address in a follow-up PR).\n"
    )
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
