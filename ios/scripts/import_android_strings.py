#!/usr/bin/env python3
"""Builds the iPhone app's string catalog from the Android app's strings.

Reads app/src/main/res/values/strings.xml (Bangla, the default) and values-en/strings.xml (English),
then applies ios/strings-ios.xml (iPhone-only wording), and writes ios/Alokchitri/Resources/Localizable.xcstrings.
Keys are the Android string names, so both apps share one reviewed set of Bangla text.

Run from anywhere:  python3 ios/scripts/import_android_strings.py
"""
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
OUT = ROOT / "ios/Alokchitri/Resources/Localizable.xcstrings"


def unescape_android(text: str) -> str:
    """Android resource escapes -> plain text, and %1$s -> %1$@ for Swift's String(format:)."""
    text = text.strip()
    if len(text) >= 2 and text[0] == '"' and text[-1] == '"':
        text = text[1:-1]
    out, i = [], 0
    while i < len(text):
        c = text[i]
        if c == "\\" and i + 1 < len(text):
            n = text[i + 1]
            out.append({"n": "\n", "t": "\t", "'": "'", '"': '"', "\\": "\\", "@": "@", "?": "?"}.get(n, n))
            i += 2
            continue
        out.append(c)
        i += 1
    text = "".join(out)
    text = re.sub(r"%(\d+)\$s", r"%\1$@", text)
    text = re.sub(r"%(\d+)\$d", r"%\1$lld", text)
    return text


def read(path: Path) -> dict:
    strings = {}
    for el in ET.parse(path).getroot().iter("string"):
        if el.get("translatable") == "false" and path.parent.name != "values":
            continue
        strings[el.get("name")] = unescape_android("".join(el.itertext()))
    return strings


def main() -> int:
    bn = read(RES / "values/strings.xml")
    en = read(RES / "values-en/strings.xml")
    for el in ET.parse(ROOT / "ios/strings-ios.xml").getroot().iter("string"):
        name = el.get("name")
        bn[name] = unescape_android(el.get("bn"))
        en[name] = unescape_android(el.get("en"))

    missing = sorted(set(bn) - set(en))
    if missing:
        print("warning: no English for:", ", ".join(missing), file=sys.stderr)

    catalog = {"sourceLanguage": "bn", "version": "1.0", "strings": {}}
    for key in sorted(bn):
        locs = {"bn": {"stringUnit": {"state": "translated", "value": bn[key]}}}
        if key in en:
            locs["en"] = {"stringUnit": {"state": "translated", "value": en[key]}}
        catalog["strings"][key] = {"extractionState": "manual", "localizations": locs}

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(catalog, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Wrote {len(catalog['strings'])} strings to {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
