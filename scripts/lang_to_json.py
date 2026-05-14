#!/usr/bin/env python3
"""Convert legacy ``<locale>.lang`` files (Forge 1.12 ``key=value`` format) to
modern ``<locale>.json`` (Minecraft 1.13+ JSON format).

One-time migration script used by SMTCON-48. Reads every ``*.lang`` under the
legacy ``resources/assets/tconstruct/lang/`` directory and emits one
``<locale>.json`` per file under
``src/main/resources/assets/tconstruct/lang/``. Keys are preserved verbatim —
the legacy ``tile.tconstruct.X.name`` format will not match the modern
``block.tconstruct.X`` keys emitted by ``TinkerLanguageProvider``, but
preserving the original translations gives future per-locale mapping passes
the source material they need to work from.

Run from the project root::

    python3 scripts/lang_to_json.py

Idempotent — re-running overwrites the JSONs in place. Skips ``en_us.lang``
and ``en_ud.lang`` (English baseline lives in ``TinkerLanguageProvider``).
"""

from __future__ import annotations

import json
from pathlib import Path


SOURCE_DIR = Path("resources/assets/tconstruct/lang")
DEST_DIR = Path("src/main/resources/assets/tconstruct/lang")
# en_us is owned by TinkerLanguageProvider — skip it here.
# en_ud (upside-down English) is a joke locale shipped by vanilla and listed in the SMTCON-48
# AC's 12-locale set, so it stays.
SKIP_LOCALES = {"en_us"}


def parse_lang(text: str) -> dict[str, str]:
    """Parse Forge 1.12 ``key=value`` lang text into an ordered dict.

    Empty lines and ``#``-prefixed comments are skipped. Lines without an ``=``
    separator are skipped silently (the legacy files contain a few of these).
    Keys are stripped of leading/trailing whitespace; values keep their
    whitespace verbatim because some translations encode hard spaces with
    leading-space conventions.
    """
    out: dict[str, str] = {}
    for raw in text.splitlines():
        line = raw.lstrip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        out[key.strip()] = value
    return out


def main() -> int:
    if not SOURCE_DIR.is_dir():
        raise SystemExit(f"source directory not found: {SOURCE_DIR}")
    DEST_DIR.mkdir(parents=True, exist_ok=True)

    converted = 0
    for src in sorted(SOURCE_DIR.glob("*.lang")):
        locale = src.stem
        if locale in SKIP_LOCALES:
            continue
        text = src.read_text(encoding="utf-8")
        entries = parse_lang(text)
        dest = DEST_DIR / f"{locale}.json"
        # ensure_ascii=False keeps multi-byte characters (Chinese, Japanese,
        # Korean, Cyrillic, German umlauts) intact in the on-disk JSON rather
        # than escaping them to \uXXXX — Minecraft loads JSON as UTF-8, so the
        # native form is both correct and human-readable.
        dest.write_text(json.dumps(entries, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(f"{locale}: {len(entries)} entries -> {dest}")
        converted += 1
    print(f"\nConverted {converted} locale(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
