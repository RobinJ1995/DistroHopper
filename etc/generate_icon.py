#!/usr/bin/env python3
"""
Generate DistroHopper icon assets from the original XCF source.
Usage: python3 etc/generate_icon.py [path/to/distrohopper.xcf]

If no XCF path is supplied the script re-uses the cached layer PNGs that
were previously exported to /tmp by this same script.
"""

import math
import os
import sys
from pathlib import Path

try:
    from gimpformats.gimpXcfDocument import GimpDocument
    HAS_GIMP = True
except ImportError:
    HAS_GIMP = False

from PIL import Image
import cairosvg

ROOT = Path(__file__).parent.parent
RES  = ROOT / 'app/src/main/res'

# ─── Extract layers from XCF (or load cached PNGs) ───────────────────────────

CACHE = {
    'face':    Path('/tmp/dh_layer_face.png'),
    'swirl_bg': Path('/tmp/dh_layer_swirl_bg.png'),
    'composite': Path('/tmp/dh_composite.png'),
}

XCF_SRC_WIDTH = 1472   # original canvas size

def extract_from_xcf(xcf_path: str):
    """Export the key layers from the XCF to our cache paths."""
    doc = GimpDocument(xcf_path)
    layers = doc._layers
    # Layer order (top→bottom): Clipboard(face), Layer(swirl), Background copy(orange circle), Background(blank)
    by_name = {l.name: l for l in layers}

    face_layer   = by_name.get('Clipboard')
    swirl_layer  = by_name.get('Layer')
    orange_layer = by_name.get('Background copy')

    W = H = doc.width
    assert W == XCF_SRC_WIDTH, f"Unexpected canvas size {W}"

    # ── Full composite (all visible layers, bottom→top) ──
    canvas = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    for layer in reversed(layers):
        img = layer.image
        if img is None:
            continue
        img = img.convert('RGBA')
        tmp = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        tmp.paste(img, (layer.xOffset, layer.yOffset))
        canvas = Image.alpha_composite(canvas, tmp)
    canvas.save(str(CACHE['composite']))

    # ── Background only (orange circle + swirl, no face) ──
    bg = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    for layer in [orange_layer, swirl_layer]:
        if layer is None:
            continue
        img = layer.image.convert('RGBA')
        tmp = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        tmp.paste(img, (layer.xOffset, layer.yOffset))
        bg = Image.alpha_composite(bg, tmp)
    bg.save(str(CACHE['swirl_bg']))

    # ── Face only (Clipboard layer at its offset) ──
    if face_layer:
        face_img = face_layer.image.convert('RGBA')
        # Store the face alongside its offset in the filename
        face_img.save(str(CACHE['face']))
        offset_file = Path('/tmp/dh_face_offset.txt')
        offset_file.write_text(f"{face_layer.xOffset},{face_layer.yOffset}")

    print(f"Extracted layers from {xcf_path}")


def load_layers():
    """Load cached layer PNGs (call extract_from_xcf first if missing)."""
    for key, path in CACHE.items():
        if not path.exists():
            raise FileNotFoundError(
                f"{path} missing — run with an XCF path to regenerate: "
                f"python3 etc/generate_icon.py path/to/icon.xcf"
            )
    face      = Image.open(str(CACHE['face'])).convert('RGBA')
    swirl_bg  = Image.open(str(CACHE['swirl_bg'])).convert('RGBA')
    composite = Image.open(str(CACHE['composite'])).convert('RGBA')

    offset_file = Path('/tmp/dh_face_offset.txt')
    ox, oy = (359, 420)   # fallback defaults
    if offset_file.exists():
        parts = offset_file.read_text().split(',')
        ox, oy = int(parts[0]), int(parts[1])

    return face, swirl_bg, composite, (ox, oy)


# ─── Build adaptive icon layers (108×108) ────────────────────────────────────

def make_adaptive_bg(swirl_bg: Image.Image) -> Image.Image:
    """
    Scale the swirl circle large enough that its radius covers every corner of
    the 108×108 canvas (radius ≥ half-diagonal ≈ 76.4 px), then centre-crop to
    108×108.  This makes the gradient+swirl fill the full rectangle with no
    plain-colour corners.
    """
    SIZE = 108
    # The circle must reach all four corners: radius ≥ 108/2 * sqrt(2) ≈ 76.4
    # Use a couple of extra pixels as margin.
    needed_radius = math.ceil(SIZE * math.sqrt(2) / 2) + 2   # 79
    scaled_size   = needed_radius * 2                          # 158

    scaled = swirl_bg.resize((scaled_size, scaled_size), Image.LANCZOS)

    # Centre-crop: every pixel in the 108×108 window is inside the circle
    x0 = (scaled_size - SIZE) // 2
    y0 = (scaled_size - SIZE) // 2
    return scaled.crop((x0, y0, x0 + SIZE, y0 + SIZE)).convert('RGBA')


def make_adaptive_fg(face: Image.Image, offset: tuple) -> Image.Image:
    """
    Place the face layer at the correct proportional position on a 108×108
    transparent canvas.  The face is perfectly centred in the XCF canvas, so
    it stays centred in the adaptive-icon safe zone (18–90 on both axes).
    """
    SIZE  = 108
    scale = SIZE / XCF_SRC_WIDTH
    fw    = int(round(face.width  * scale))
    fh    = int(round(face.height * scale))
    fx    = int(round(offset[0]   * scale))
    fy    = int(round(offset[1]   * scale))

    fg = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    fg.paste(face.resize((fw, fh), Image.LANCZOS), (fx, fy), face.resize((fw, fh), Image.LANCZOS))
    return fg


# ─── Android vector XML for the adaptive icon ────────────────────────────────
# We reference PNG drawables rather than vector XML so pixel-perfect quality
# from the original artwork is preserved.

ADAPTIVE_ICON_XML = '''\
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>'''


# ─── Helpers ─────────────────────────────────────────────────────────────────

def save_png(img: Image.Image, path: Path, size: int):
    out = img.resize((size, size), Image.LANCZOS)
    path.parent.mkdir(parents=True, exist_ok=True)
    out.save(str(path), optimize=True)
    kb = path.stat().st_size / 1024
    print(f"  {path.relative_to(ROOT)}  ({size}×{size}, {kb:.1f} KB)")


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    if len(sys.argv) > 1:
        xcf_path = sys.argv[1]
        if not HAS_GIMP:
            sys.exit("gimpformats is not installed. Run: pip install gimpformats")
        extract_from_xcf(xcf_path)

    face, swirl_bg, composite, offset = load_layers()

    print('\n=== Building adaptive icon layers ===')
    adaptive_bg = make_adaptive_bg(swirl_bg)
    adaptive_fg = make_adaptive_fg(face, offset)

    # Save to drawable/ as PNGs (highest quality, no lossy vector conversion)
    drawable = RES / 'drawable'
    drawable.mkdir(exist_ok=True)
    save_png(adaptive_bg, drawable / 'ic_launcher_background.png', 108)
    save_png(adaptive_fg, drawable / 'ic_launcher_foreground.png', 108)

    print('\n=== Adaptive icon XMLs (API 26+) ===')
    anydpi = RES / 'mipmap-anydpi-v26'
    anydpi.mkdir(exist_ok=True)
    for name in ('ic_launcher.xml', 'ic_launcher_round.xml'):
        (anydpi / name).write_text(ADAPTIVE_ICON_XML, encoding='utf-8')
        print(f'  mipmap-anydpi-v26/{name}')

    # Update adaptive icon XML to point at PNG drawables
    PNG_ADAPTIVE_XML = '''\
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>'''
    for name in ('ic_launcher.xml', 'ic_launcher_round.xml'):
        (anydpi / name).write_text(PNG_ADAPTIVE_XML, encoding='utf-8')

    print('\n=== Legacy mipmap PNGs ===')
    densities = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
    for density, px in densities.items():
        save_png(composite, RES / f'mipmap-{density}' / 'ic_launcher.png', px)

    print('\n=== Web / store icon (512×512) ===')
    save_png(composite, ROOT / 'app/src/main/ic_launcher-web.png', 512)
    save_png(composite, ROOT / 'fastlane/metadata/android/en-US/images/icon.png', 512)

    # Remove old vector drawables (replaced by PNG drawables)
    for old in ('ic_launcher_background.xml', 'ic_launcher_foreground.xml'):
        p = drawable / old
        if p.exists():
            p.unlink()
            print(f'  removed drawable/{old} (replaced by PNG)')

    print('\nDone.')


if __name__ == '__main__':
    main()
