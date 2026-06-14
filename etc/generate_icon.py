#!/usr/bin/env python3
"""
Generate DistroHopper icon assets from the original XCF source.

Usage:
    python3 etc/generate_icon.py                   # uses etc/icon_source.xcf
    python3 etc/generate_icon.py path/to/icon.xcf  # explicit path

Requires: pip install cairosvg gimpformats Pillow numpy
"""

import math
import os
import sys
from pathlib import Path

import numpy as np

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

def make_adaptive_bg(swirl_bg: Image.Image, size: int = 108) -> Image.Image:
    """
    Scale the swirl circle to `size`×`size`, then fill the transparent corners.

    Near the rim the angular blend window is narrow (preserving swirl stripes);
    further into the corners it widens with a Gaussian taper so the stripes merge
    smoothly into a uniform orange — avoiding the cross/swastika artefact that
    appears with straight single-ray projection.
    """
    img = swirl_bg.resize((size, size), Image.LANCZOS).convert('RGBA')
    arr = np.array(img, dtype=np.float64)

    cx = cy = size / 2.0
    r  = cx

    ys, xs   = np.meshgrid(np.arange(size), np.arange(size), indexing='ij')
    dx       = (xs - cx).astype(np.float64)
    dy       = (ys - cy).astype(np.float64)
    dist     = np.sqrt(dx * dx + dy * dy)
    angles   = np.arctan2(dy, dx)

    # Angular blur width: 0° at the rim → 30° at the farthest corner pixel
    max_extra = (math.sqrt(2) - 1.0) * r            # rim-to-corner distance ≈ 22 px
    extra     = np.clip(dist - r, 0.0, max_extra)
    sigma     = (extra / max_extra) * (math.pi / 6)  # per-pixel, 0..π/6

    sample_r = r - 2.0   # sample slightly inside the circle to avoid AA fringe

    n       = 9
    t_vals  = np.linspace(-2.0, 2.0, n)
    gauss_w = np.exp(-0.5 * t_vals ** 2)
    gauss_w /= gauss_w.sum()

    result = np.zeros((size, size, 4), dtype=np.float64)
    for t, w in zip(t_vals, gauss_w):
        sa   = angles + t * sigma
        s_xi = np.clip((cx + np.cos(sa) * sample_r).astype(np.int32), 0, size - 1)
        s_yi = np.clip((cy + np.sin(sa) * sample_r).astype(np.int32), 0, size - 1)
        result += arr[s_yi, s_xi] * w

    # Fade from original swirl (well inside) → corner fill (at rim and beyond).
    # Gate by arr's alpha so transparent/AA edge pixels never bleed dark values in.
    alpha_norm = arr[:, :, 3:4] / 255.0   # 1 inside circle, 0 outside

    t = np.clip((dist - r * 0.80) / (r * 0.20), 0.0, 1.0)
    t = t * t * (3.0 - 2.0 * t)           # smoothstep
    dist_weight = t[:, :, np.newaxis]

    # Outside the circle (alpha=0) always use result; inside, use distance fade
    eff_weight = np.maximum(dist_weight, 1.0 - alpha_norm)

    out = arr * (1.0 - eff_weight) + result * eff_weight
    out[:, :, 3] = 255
    return Image.fromarray(out.astype(np.uint8))


def make_adaptive_fg(face: Image.Image, offset: tuple, size: int = 108) -> Image.Image:
    """
    Place the face layer at the correct proportional position on a `size`×`size`
    transparent canvas.
    """
    scale      = size / XCF_SRC_WIDTH
    fw         = int(round(face.width  * scale))
    fh         = int(round(face.height * scale))
    fx         = int(round(offset[0]   * scale))
    fy         = int(round(offset[1]   * scale))
    face_scaled = face.resize((fw, fh), Image.LANCZOS)
    fg          = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    fg.paste(face_scaled, (fx, fy), face_scaled)
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

DEFAULT_XCF = ROOT / 'etc' / 'icon_source.xcf'

ADAPTIVE_DENSITIES = {
    'mdpi':    108,
    'hdpi':    162,
    'xhdpi':   216,
    'xxhdpi':  324,
    'xxxhdpi': 432,
}

def main():
    xcf_path = sys.argv[1] if len(sys.argv) > 1 else str(DEFAULT_XCF)
    cache_ok  = all(p.exists() for p in CACHE.values())

    if Path(xcf_path).exists():
        if not HAS_GIMP:
            if cache_ok:
                print("gimpformats unavailable — using existing /tmp cache")
            else:
                sys.exit(
                    "gimpformats not installed and no /tmp cache found.\n"
                    "Run: pip install cairosvg gimpformats Pillow numpy"
                )
        else:
            extract_from_xcf(xcf_path)
    elif not cache_ok:
        sys.exit(f"XCF not found at {xcf_path} and no /tmp cache.")
    else:
        print(f"XCF not found at {xcf_path} — using /tmp cache")

    face, swirl_bg, composite, offset = load_layers()

    print('\n=== Adaptive icon layers (density-qualified) ===')
    for density, px in ADAPTIVE_DENSITIES.items():
        folder = RES / f'drawable-{density}'
        folder.mkdir(exist_ok=True)
        bg_img  = make_adaptive_bg(swirl_bg, px)
        fg_img  = make_adaptive_fg(face, offset, px)
        bg_path = folder / 'ic_launcher_background.png'
        fg_path = folder / 'ic_launcher_foreground.png'
        bg_img.save(str(bg_path), optimize=True)
        fg_img.save(str(fg_path), optimize=True)
        print(f"  drawable-{density}/  bg={bg_path.stat().st_size//1024}KB"
              f"  fg={fg_path.stat().st_size//1024}KB  ({px}×{px})")

    # Remove old unqualified PNG drawables if present
    for name in ('ic_launcher_background.png', 'ic_launcher_foreground.png'):
        p = RES / 'drawable' / name
        if p.exists():
            p.unlink()
            print(f"  removed drawable/{name}")

    print('\n=== Adaptive icon XMLs (API 26+) ===')
    anydpi = RES / 'mipmap-anydpi-v26'
    anydpi.mkdir(exist_ok=True)
    for name in ('ic_launcher.xml', 'ic_launcher_round.xml'):
        (anydpi / name).write_text(ADAPTIVE_ICON_XML, encoding='utf-8')
        print(f'  mipmap-anydpi-v26/{name}')

    print('\n=== Legacy mipmap PNGs (kept from original — include drop shadow) ===')
    print('  Skipped: mipmap-*/ic_launcher.png and ic_launcher-web.png are preserved as-is.')
    print('  To regenerate them, remove this block and restore the save_png calls.')

    print('\nDone.')


if __name__ == '__main__':
    main()
