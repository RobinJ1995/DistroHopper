#!/usr/bin/env python3
"""
Generate modern DistroHopper icon assets.
Design: clean 12-segment orange pinwheel + goofy Tux eyes & beak.
All geometry is authored at 108×108 (Android adaptive icon canvas).
"""

import math
from pathlib import Path
import cairosvg

ROOT = Path(__file__).parent.parent
RES  = ROOT / 'app/src/main/res'
ETC  = ROOT / 'etc'

# ─── Colour palette ──────────────────────────────────────────────────────────
C_LIGHT   = '#FF6B2B'   # vivid orange  (pinwheel light blades)
C_DARK    = '#C83E08'   # burnt orange  (pinwheel dark blades)
C_PUPIL   = '#141414'
C_BEAK_HI = '#FFD84A'
C_BEAK_LO = '#E08610'


# ─── Helpers ─────────────────────────────────────────────────────────────────

def polar(cx, cy, r, deg):
    rad = math.radians(deg)
    return cx + r * math.cos(rad), cy + r * math.sin(rad)

def circle_path(cx, cy, r):
    """SVG/Android path for a full circle (two arcs)."""
    return (f'M{cx},{cy - r} '
            f'A{r},{r} 0 0 1 {cx},{cy + r} '
            f'A{r},{r} 0 0 1 {cx},{cy - r} Z')


# ─── Pinwheel segments ───────────────────────────────────────────────────────

def pinwheel_segments(cx=54.0, cy=54.0, r=54.0, n=12):
    """Return list of (colour, path_d) for n alternating pie slices."""
    sector = 360.0 / n
    out = []
    for i in range(n):
        colour = C_LIGHT if i % 2 == 0 else C_DARK
        a1 = -90 + i * sector
        a2 = a1 + sector
        x1, y1 = polar(cx, cy, r, a1)
        x2, y2 = polar(cx, cy, r, a2)
        d = f'M{cx},{cy} L{x1:.4f},{y1:.4f} A{r},{r} 0 0 1 {x2:.4f},{y2:.4f} Z'
        out.append((colour, d))
    return out


# ─── Face geometry (108×108 canvas) ──────────────────────────────────────────
# Safe zone for adaptive icons: x[18..90], y[18..90]

EY  = 47.0   # eye centre Y
LX  = 35.0   # left eye centre X
RX  = 73.0   # right eye centre X
ER  = 17.0   # eye radius

PLX = 38.5   # left pupil X (inward)
PRX = 69.5   # right pupil X (inward)
PY  = 51.5   # pupil Y (slightly down — goofy look)
PR  = 9.0    # pupil radius

BX, BY = 54.0, 67.5   # beak centre

BEAK_PATH = (
    f'M{BX-14},{BY-5} '
    f'C{BX-14},{BY-13} {BX+14},{BY-13} {BX+14},{BY-5} '
    f'C{BX+12},{BY+7}  {BX+4},{BY+12}  {BX},{BY+13} '
    f'C{BX-4},{BY+12}  {BX-12},{BY+7}  {BX-14},{BY-5} Z'
)


# ─── SVG generators ──────────────────────────────────────────────────────────

def background_svg():
    segs = pinwheel_segments()
    seg_xml = '\n  '.join(f'<path fill="{c}" d="{d}"/>' for c, d in segs)
    return f'''\
<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
  <defs>
    <radialGradient id="glow" cx="50%" cy="42%" r="54%">
      <stop offset="0%"   stop-color="white" stop-opacity="0.20"/>
      <stop offset="58%"  stop-color="white" stop-opacity="0"/>
      <stop offset="100%" stop-color="black" stop-opacity="0.13"/>
    </radialGradient>
  </defs>
  {seg_xml}
  <circle cx="54" cy="54" r="54" fill="url(#glow)"/>
</svg>'''


def foreground_svg():
    return f'''\
<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
  <defs>
    <radialGradient id="eg" cx="38%" cy="30%" r="70%">
      <stop offset="0%"   stop-color="#FFFFFF"/>
      <stop offset="100%" stop-color="#CCCCCC"/>
    </radialGradient>
    <linearGradient id="bkg" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%"   stop-color="{C_BEAK_HI}"/>
      <stop offset="100%" stop-color="{C_BEAK_LO}"/>
    </linearGradient>
    <filter id="sh" x="-50%" y="-50%" width="200%" height="200%">
      <feDropShadow dx="0" dy="1.5" stdDeviation="2.8"
                    flood-color="#000" flood-opacity="0.30"/>
    </filter>
  </defs>

  <!-- Left eye -->
  <circle cx="{LX}" cy="{EY}" r="{ER}" fill="url(#eg)" filter="url(#sh)"/>
  <circle cx="{PLX}" cy="{PY}" r="{PR}" fill="{C_PUPIL}"/>
  <circle cx="{LX - 5.5}" cy="{EY - 7}" r="3.5" fill="white" opacity="0.85"/>

  <!-- Right eye -->
  <circle cx="{RX}" cy="{EY}" r="{ER}" fill="url(#eg)" filter="url(#sh)"/>
  <circle cx="{PRX}" cy="{PY}" r="{PR}" fill="{C_PUPIL}"/>
  <circle cx="{RX + 5.5}" cy="{EY - 7}" r="3.5" fill="white" opacity="0.85"/>

  <!-- Beak -->
  <path d="{BEAK_PATH}" fill="url(#bkg)" filter="url(#sh)"/>
</svg>'''


def composite_svg():
    """Background + foreground merged with circle clip, for legacy PNG rendering."""
    segs = pinwheel_segments()
    seg_xml = '\n    '.join(f'<path fill="{c}" d="{d}"/>' for c, d in segs)
    return f'''\
<?xml version="1.0" encoding="utf-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
  <defs>
    <radialGradient id="glow" cx="50%" cy="42%" r="54%">
      <stop offset="0%"   stop-color="white" stop-opacity="0.20"/>
      <stop offset="58%"  stop-color="white" stop-opacity="0"/>
      <stop offset="100%" stop-color="black" stop-opacity="0.13"/>
    </radialGradient>
    <radialGradient id="eg" cx="38%" cy="30%" r="70%">
      <stop offset="0%"   stop-color="#FFFFFF"/>
      <stop offset="100%" stop-color="#CCCCCC"/>
    </radialGradient>
    <linearGradient id="bkg" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%"   stop-color="{C_BEAK_HI}"/>
      <stop offset="100%" stop-color="{C_BEAK_LO}"/>
    </linearGradient>
    <filter id="sh" x="-50%" y="-50%" width="200%" height="200%">
      <feDropShadow dx="0" dy="1.5" stdDeviation="2.8"
                    flood-color="#000" flood-opacity="0.30"/>
    </filter>
    <clipPath id="circ">
      <circle cx="54" cy="54" r="54"/>
    </clipPath>
  </defs>
  <g clip-path="url(#circ)">
    {seg_xml}
    <circle cx="54" cy="54" r="54" fill="url(#glow)"/>
    <!-- Left eye -->
    <circle cx="{LX}" cy="{EY}" r="{ER}" fill="url(#eg)" filter="url(#sh)"/>
    <circle cx="{PLX}" cy="{PY}" r="{PR}" fill="{C_PUPIL}"/>
    <circle cx="{LX - 5.5}" cy="{EY - 7}" r="3.5" fill="white" opacity="0.85"/>
    <!-- Right eye -->
    <circle cx="{RX}" cy="{EY}" r="{ER}" fill="url(#eg)" filter="url(#sh)"/>
    <circle cx="{PRX}" cy="{PY}" r="{PR}" fill="{C_PUPIL}"/>
    <circle cx="{RX + 5.5}" cy="{EY - 7}" r="3.5" fill="white" opacity="0.85"/>
    <!-- Beak -->
    <path d="{BEAK_PATH}" fill="url(#bkg)" filter="url(#sh)"/>
  </g>
</svg>'''


# ─── Android Vector Drawable generators ──────────────────────────────────────

def background_vector_xml():
    segs = pinwheel_segments()
    path_els = '\n    '.join(
        f'<path android:fillColor="{c}" android:pathData="{d}"/>'
        for c, d in segs
    )
    return f'''\
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    {path_els}
    <!-- Subtle centre highlight -->
    <path
        android:fillColor="#33FFFFFF"
        android:pathData="{circle_path(54, 54, 40)}"/>
</vector>'''


def foreground_vector_xml():
    return f'''\
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Left eye -->
    <path android:fillColor="#FFFFFF"
          android:pathData="{circle_path(LX, EY, ER)}"/>
    <path android:fillColor="{C_PUPIL}"
          android:pathData="{circle_path(PLX, PY, PR)}"/>
    <path android:fillColor="#DDFFFFFF"
          android:pathData="{circle_path(LX - 5.5, EY - 7, 3.5)}"/>

    <!-- Right eye -->
    <path android:fillColor="#FFFFFF"
          android:pathData="{circle_path(RX, EY, ER)}"/>
    <path android:fillColor="{C_PUPIL}"
          android:pathData="{circle_path(PRX, PY, PR)}"/>
    <path android:fillColor="#DDFFFFFF"
          android:pathData="{circle_path(RX + 5.5, EY - 7, 3.5)}"/>

    <!-- Beak with gradient -->
    <path android:pathData="{BEAK_PATH}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="{BX}" android:startY="{BY - 13}"
                android:endX="{BX}"   android:endY="{BY + 13}"
                android:startColor="{C_BEAK_HI}"
                android:endColor="{C_BEAK_LO}"/>
        </aapt:attr>
    </path>
</vector>'''


ADAPTIVE_ICON_XML = '''\
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>'''


# ─── PNG rendering ────────────────────────────────────────────────────────────

def render_png(svg_str: str, out_path: Path, size: int):
    """Render SVG to PNG at the given pixel size."""
    png_bytes = cairosvg.svg2png(
        bytestring=svg_str.encode('utf-8'),
        output_width=size,
        output_height=size,
    )
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(png_bytes)
    kb = len(png_bytes) / 1024
    print(f'  {out_path.relative_to(ROOT)}  ({size}×{size}, {kb:.1f} KB)')


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    comp = composite_svg()
    bg   = background_svg()
    fg   = foreground_svg()

    print('=== SVG source files ===')
    (ETC / 'icon_bg.svg').write_text(bg,   encoding='utf-8')
    (ETC / 'icon_fg.svg').write_text(fg,   encoding='utf-8')
    (ETC / 'icon_composite.svg').write_text(comp, encoding='utf-8')
    print('  etc/icon_bg.svg, icon_fg.svg, icon_composite.svg')

    print('\n=== Legacy mipmap PNGs ===')
    densities = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
    for density, px in densities.items():
        render_png(comp, RES / f'mipmap-{density}' / 'ic_launcher.png', px)

    print('\n=== Web / store icon (512×512) ===')
    render_png(comp, ROOT / 'app/src/main/ic_launcher-web.png', 512)
    render_png(comp, ROOT / 'fastlane/metadata/android/en-US/images/icon.png', 512)

    print('\n=== Android drawable vectors ===')
    drawable = RES / 'drawable'
    drawable.mkdir(exist_ok=True)
    (drawable / 'ic_launcher_background.xml').write_text(background_vector_xml(), encoding='utf-8')
    print('  drawable/ic_launcher_background.xml')
    (drawable / 'ic_launcher_foreground.xml').write_text(foreground_vector_xml(), encoding='utf-8')
    print('  drawable/ic_launcher_foreground.xml')

    print('\n=== Adaptive icon XMLs (API 26+) ===')
    anydpi = RES / 'mipmap-anydpi-v26'
    anydpi.mkdir(exist_ok=True)
    (anydpi / 'ic_launcher.xml').write_text(ADAPTIVE_ICON_XML, encoding='utf-8')
    (anydpi / 'ic_launcher_round.xml').write_text(ADAPTIVE_ICON_XML, encoding='utf-8')
    print('  mipmap-anydpi-v26/ic_launcher.xml')
    print('  mipmap-anydpi-v26/ic_launcher_round.xml')

    print('\nAll done.')


if __name__ == '__main__':
    main()
