#!/usr/bin/env python3
"""Convert a PMD XML report to Checkstyle XML so reviewdog can parse it.

reviewdog's built-in parsers do not include PMD, but Checkstyle is supported.
This script reads a PMD report (path argument), writes Checkstyle XML to stdout.

Used by `.github/workflows/ci.yml` lint job:

    python3 scripts/ci/pmd_to_checkstyle.py build/reports/pmd/main.xml \\
        | reviewdog -f=checkstyle -filter-mode=added -fail-on-error=true ...
"""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from xml.sax.saxutils import escape, quoteattr

PMD_NS = "{http://pmd.sourceforge.net/report/2.0.0}"


def severity_for(priority: str) -> str:
    """PMD priority 1=highest, 5=lowest. Map to Checkstyle severity."""
    try:
        p = int(priority)
    except (TypeError, ValueError):
        return "warning"
    if p <= 2:
        return "error"
    if p <= 4:
        return "warning"
    return "info"


def convert(pmd_xml_path: Path) -> str:
    tree = ET.parse(pmd_xml_path)
    root = tree.getroot()

    out: list[str] = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<checkstyle version="8.0">',
    ]

    for file_el in root.findall(f"{PMD_NS}file"):
        file_name = file_el.get("name", "")
        if not file_name:
            continue
        out.append(f"  <file name={quoteattr(file_name)}>")
        for v in file_el.findall(f"{PMD_NS}violation"):
            line = v.get("beginline", "1")
            column = v.get("begincolumn", "1")
            rule = v.get("rule", "PMD")
            ruleset = v.get("ruleset", "")
            priority = v.get("priority", "3")
            message = (v.text or rule).strip()
            decorated = f"[{rule}] {message}" if rule else message
            source = f"pmd.{ruleset.lower().replace(' ', '')}.{rule}" if ruleset else f"pmd.{rule}"
            out.append(
                f'    <error line="{escape(line)}" column="{escape(column)}" '
                f'severity="{severity_for(priority)}" '
                f"message={quoteattr(decorated)} "
                f"source={quoteattr(source)}/>"
            )
        out.append("  </file>")

    out.append("</checkstyle>")
    return "\n".join(out) + "\n"


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(f"usage: {argv[0]} <pmd-report.xml>", file=sys.stderr)
        return 2
    pmd_xml_path = Path(argv[1])
    if not pmd_xml_path.is_file():
        print(f"error: report not found: {pmd_xml_path}", file=sys.stderr)
        return 1
    sys.stdout.write(convert(pmd_xml_path))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
