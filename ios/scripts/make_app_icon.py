#!/usr/bin/env python3
"""Draws the 1024 px App Store icon: the amber reticle from the Android launcher icon on the ground colour.

Run: python3 ios/scripts/make_app_icon.py   (needs Pillow)
"""
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "Alokchitri/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png"
SS = 4                      # supersampling for smooth edges
SIZE = 1024 * SS
K = SIZE / 108 * 1.3        # Android's 108-unit canvas, enlarged: iOS icons have no adaptive-icon safe zone
GROUND, AMBER = (0x0E, 0x10, 0x14), (0xFF, 0xB5, 0x47)


def p(v):  # Android units around the centre (54) -> pixels
    return SIZE / 2 + (v - 54) * K


img = Image.new("RGB", (SIZE, SIZE), GROUND)
d = ImageDraw.Draw(img)
w = 3.84 * K
r = 19.2 * K
c = SIZE / 2
d.ellipse([c - r - w / 2, c - r - w / 2, c + r + w / 2, c + r + w / 2], outline=AMBER, width=round(w))
dot = 7.68 * K
d.ellipse([c - dot, c - dot, c + dot, c + dot], fill=AMBER)
for (x1, y1, x2, y2) in [(54, 34.32, 54, 43.44), (54, 64.56, 54, 73.68), (34.32, 54, 43.44, 54), (64.56, 54, 73.68, 54)]:
    d.line([p(x1), p(y1), p(x2), p(y2)], fill=AMBER, width=round(w))
    # Round cap on the inner end only; the outer end sits under the ring.
    x, y = min([(x1, y1), (x2, y2)], key=lambda q: abs(q[0] - 54) + abs(q[1] - 54))
    d.ellipse([p(x) - w / 2, p(y) - w / 2, p(x) + w / 2, p(y) + w / 2], fill=AMBER)

img.resize((1024, 1024), Image.LANCZOS).save(OUT)
print("Wrote", OUT)
