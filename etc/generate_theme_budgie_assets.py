#!/usr/bin/env python3
"""Generate the Budgie theme drawables from values measured off the baseline
screenshots in `etc/theme baselines/budgie/` (Solus Budgie, Arc-Dark).

Colours and opacities were recovered from the white/black wallpaper screenshot
pairs (alpha = 1 - (C_white - C_black) / 255, colour = C_black / alpha). Every
surface came out fully opaque, matching Budgie's solid Arc-Dark panels:

  launcher / dock     #20222A, fully opaque, flat, full-bleed against the
                      screen edge (no rounding, no float)
  menu (dash)         #383C4A (Arc-Dark), fully opaque, gentle 8dp corners;
                      a floating card baked inside a transparent margin (so it
                      never fills the screen) with a small "ear" (speech-bubble
                      tail) pointing at the BFB and stopping just short of the
                      launcher, leaving a subtle gap, like elementary's popover
  search section      the menu's top strip: same #383C4A fill, a magnifier in
                      the left cap and a #52555F separator line below it that
                      spans the full card width, dividing it from the grid
  running indicator   Arc accent blue #5294E2; Budgie underlines the running
                      app, the engine draws the marker in the narrow strip
                      beside the icon instead (same approximation as cosmic)

The BFB reuses the pre-revamp GNOME "show applications" 3x3 grid (recovered
from etc/theme_gnome_launcher_bfb.png in git history), which closely matches
Budgie's own 3x3-dot menu button; it is reproduced procedurally for crisp
per-density output. The launcher preferences icon is the teal "tweak tool"
cog, and the theme-picker card uses the official Budgie bird logo
(etc/theme_budgie_logo.svg, from Wikimedia Commons).

The menu's category sidebar (All / Accessories / ...) and the account/power
row at the bottom are not expressible with the theme engine (the dash is an
app grid + search) and are omitted.

Per-edge ear: the launcher supports every screen edge, so the dash is rendered
once per launcher edge with the ear on the side that faces the launcher,
offset toward the BFB (the launcher's first icon). WallpaperColourApplier
selects the variant by the launcher edge via the theme's dash_background_edge
array.

Run from the repository root:  python3 etc/generate_theme_budgie_assets.py
Requires Pillow.
"""

import os
import cairosvg
from PIL import Image, ImageDraw

RES = "app/src/main/res"
ETC = "etc"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
SS = 8  # supersampling factor for crisp antialiased shapes

DOCK = (0x20, 0x22, 0x2A, 255)       # launcher / dock, status bar at top
MENU = (0x38, 0x3C, 0x4A, 255)       # dash / menu surface (Arc-Dark)
SEPARATOR = (0x52, 0x55, 0x5F, 255)  # search section divider
GLYPH = (0xBA, 0xC0, 0xCA, 255)      # search magnifier / hint
ACCENT = (0x52, 0x94, 0xE2, 255)     # Arc accent blue (running indicator)
BFB = (0xFF, 0xFD, 0xFA, 255)        # menu-button grid (near-white)
BLACK = (0, 0, 0, 255)
CLEAR = (0, 0, 0, 0)

# Dash popover geometry (dp). The dash is a floating card: it sits inside a
# baked transparent margin (so it never fills the whole screen), with a small
# "ear" whose tip stops just short of the launcher, leaving a subtle gap.
DASH_R = 6          # corner radius (a gentle round, not a pill)
EAR_W = 26          # ear base width
EAR_H = 12          # ear depth (how far the detached tail pokes out)
PANEL_EAR_GAP = 2   # gap between the panel's edge and the detached ear
EAR_GAP = 6         # gap between the ear's tip and the launcher
EAR_OFFSET = 40     # ear apex from the launcher-edge corner (~BFB centre)
DASH_MARGIN = 16    # floating gap on the sides away from the launcher


def nine_patch(content, stretch_x, stretch_y, padding_x=None, padding_y=None):
	"""Wrap rendered content in a source-format 9-patch marker border.

	stretch/padding are (start, end) pixel ranges in content coordinates (end
	exclusive), or lists of such ranges. Padding defaults to the stretch
	region when omitted (matching aapt's behaviour for missing padding lines).
	"""
	def ranges(spec):
		return spec if isinstance(spec, list) else [spec]

	w, h = content.size
	out = Image.new("RGBA", (w + 2, h + 2), CLEAR)
	out.paste(content, (1, 1))
	px = out.load()
	for r in ranges(stretch_x):
		for x in range(*r):
			px[x + 1, 0] = BLACK
	for r in ranges(stretch_y):
		for y in range(*r):
			px[0, y + 1] = BLACK
	if padding_x:
		for r in ranges(padding_x):
			for x in range(*r):
				px[x + 1, h + 1] = BLACK
	if padding_y:
		for r in ranges(padding_y):
			for y in range(*r):
				px[w + 1, y + 1] = BLACK
	return out


def magnifier(draw, cx, cy, size, colour):
	"""Draw a magnifying glass glyph centred on (cx, cy); all units are px."""
	r = size * 0.32
	stroke = max(int(round(size * 0.11)), 1)
	gx, gy = cx - size * 0.08, cy - size * 0.08
	draw.ellipse((gx - r, gy - r, gx + r, gy + r), outline=colour, width=stroke)
	hr = r * 0.7071
	draw.line((gx + hr, gy + hr, cx + size * 0.42, cy + size * 0.42),
		fill=colour, width=stroke)


def save(img, *paths):
	for path in paths:
		os.makedirs(os.path.dirname(path), exist_ok=True)
		img.save(path)
		print(path, img.size)


def dpi_path(name, density):
	return f"{RES}/drawable-{density}/{name}"


# ---------------------------------------------------------------------------
# Dash popover: rounded surface + ear, one variant per launcher edge.
# ---------------------------------------------------------------------------

def _dash_spec(edge):
	"""Geometry (in dp) for one edge variant: canvas size, the floating body
	rounded-rect bounds, the ear triangle, and the 9-patch stretch/padding
	markers. The body floats inside DASH_MARGIN on every side; on the
	launcher-facing side it is pulled in by the ear depth + gap, and the ear
	pokes out toward the launcher but stops EAR_GAP short of the canvas edge.
	Content spans the full body width so the search separator reaches both
	body edges; stretch zones sit on flat body, clear of the ear and corners."""
	M, R, EW, EH, EO = DASH_MARGIN, DASH_R, EAR_W, EAR_H, EAR_OFFSET
	EG, PG = EAR_GAP, PANEL_EAR_GAP
	w, h = (96, 84) if edge in ("top", "bottom") else (84, 96)
	inset = EH + EG + PG  # panel edge -> ear base (PG) -> ear tip -> launcher (EG)

	if edge == "bottom":
		body = (M, M, w - M, h - inset)
		base = h - EG - EH                       # ear base, PG below the panel
		ear = [(EO - EW / 2, base), (EO, h - EG), (EO + EW / 2, base)]
	elif edge == "top":
		body = (M, inset, w - M, h - M)
		base = EG + EH
		ear = [(EO - EW / 2, base), (EO, EG), (EO + EW / 2, base)]
	elif edge == "left":
		body = (inset, M, w - M, h - M)
		base = EG + EH
		ear = [(base, EO - EW / 2), (EG, EO), (base, EO + EW / 2)]
	else:  # right
		body = (M, M, w - inset, h - M)
		base = w - EG - EH
		ear = [(base, EO - EW / 2), (w - EG, EO), (base, EO + EW / 2)]

	bx0, by0, bx1, by1 = body
	cx, cy = (bx0 + bx1) / 2, (by0 + by1) / 2
	# Content fills the body width (full-width separator) and is inset by the
	# corner radius vertically so it clears the rounded corners.
	px = (bx0, bx1)
	py = (by0 + R, by1 - R)
	# Stretch on flat body, away from the corners and the ear's base span.
	if edge in ("top", "bottom"):
		sx = (bx1 - R - 6, bx1 - R - 4)   # right of the ear, left of the corner
		sy = (cy - 1, cy + 1)
	else:
		sy = (by1 - R - 6, by1 - R - 4)   # below the ear, above the corner
		sx = (cx - 1, cx + 1)
	return w, h, body, ear, sx, sy, px, py


def _dash_silhouette(edge, scale):
	"""Render the filled, floating dash silhouette (rounded body + ear) at the
	given density scale."""
	w, h, body, ear, _, _, _, _ = _dash_spec(edge)
	W, H = int(round(w * scale)), int(round(h * scale))
	img = Image.new("RGBA", (W * SS, H * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	f = scale * SS
	draw.rounded_rectangle([c * f for c in body], radius=DASH_R * f, fill=MENU)
	draw.polygon([(x * f, y * f) for x, y in ear], fill=MENU)
	return img.resize((W, H), Image.LANCZOS)


def dash_background(density, scale):
	# One 9-patch per launcher edge; index 0 (NONE) reuses the bottom variant.
	def emit(edge, name):
		w, h, _, _, sx, sy, px, py = _dash_spec(edge)
		content = _dash_silhouette(edge, scale)
		def rng(t):
			return (int(round(t[0] * scale)), int(round(t[1] * scale)))
		patch = nine_patch(content, rng(sx), rng(sy), rng(px), rng(py))
		save(patch, dpi_path(f"theme_budgie_res_dash_background_{name}.9.png", density))

	for edge in ("bottom", "top", "left", "right"):
		emit(edge, edge)


def dash_search_background(density, scale):
	# The menu's top section: a flat #383C4A strip with a magnifier in the left
	# cap and a separator line at the bottom (no pill/border, matching the
	# baseline). Full width comes from dash_search_width = 0; the horizontal
	# stretch zone sits right of the magnifier, the vertical zone on plain fill
	# below it so neither the magnifier nor the separator distort.
	w_dp, h_dp = 128, 46
	sep_dp = 1.5
	w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
	img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	draw.rectangle((0, 0, w * SS - 1, h * SS - 1), fill=MENU)
	draw.rectangle((0, h * SS - 1 - sep_dp * scale * SS, w * SS - 1, h * SS - 1), fill=SEPARATOR)
	magnifier(draw, 22 * scale * SS, (h_dp / 2 - 1) * scale * SS, 18 * scale * SS, GLYPH)
	content = img.resize((w, h), Image.LANCZOS)
	sx = (int(round(64 * scale)), int(round(66 * scale)))
	sy = (int(round(30 * scale)), int(round(34 * scale)))
	pad_x = (int(round(42 * scale)), w - int(round(16 * scale)))  # text right of magnifier
	pad_y = (int(round(10 * scale)), h - int(round(14 * scale)))  # above the separator
	save(nine_patch(content, sx, sy, pad_x, pad_y),
		dpi_path("theme_budgie_res_dash_search_background.9.png", density))


def app_running(density, scale):
	# Budgie underlines running apps in the accent blue; the engine fits this
	# drawable into the narrow strip beside the icon, so it reads as an accent
	# dot (same approximation as cosmic/elementary).
	size_dp, d_dp = 6, 5
	s = int(round(size_dp * scale))
	img = Image.new("RGBA", (s * SS, s * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	c, rr = size_dp * scale * SS / 2, d_dp * scale * SS / 2
	draw.ellipse((c - rr, c - rr, c + rr, c + rr), fill=ACCENT)
	save(img.resize((s, s), Image.LANCZOS),
		dpi_path("theme_budgie_res_launcher_app_running.png", density))


# ---------------------------------------------------------------------------
# BFB: the pre-revamp GNOME 3x3 "show applications" grid (matches Budgie's own
# menu button). Geometry measured off etc/theme_gnome_launcher_bfb.png (34px:
# 2px margin, three 6px rounded squares with 6px gaps).
# ---------------------------------------------------------------------------

def grid_icon(px, colour):
	"""Render the 3x3 rounded-square grid into a px-square image."""
	img = Image.new("RGBA", (px * SS, px * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	unit = px * SS / 34.0          # baseline grid is 34 units
	cell = 6 * unit                # square size
	radius = 1.6 * unit            # rounded corners
	for r in range(3):
		for c in range(3):
			x = (2 + c * 12) * unit
			y = (2 + r * 12) * unit
			draw.rounded_rectangle((x, y, x + cell, y + cell), radius=radius, fill=colour)
	return img.resize((px, px), Image.LANCZOS)


def bfb(density, scale):
	s = int(round(48 * scale))
	img = Image.new("RGBA", (s, s), CLEAR)
	g = grid_icon(int(round(34 * scale)), BFB)
	img.alpha_composite(g, ((s - g.width) // 2, (s - g.height) // 2))
	save(img, dpi_path("theme_budgie_res_launcher_bfb.png", density))


def bfb_background_when_dash_opened(density, scale):
	# While the menu is open the Budgie Menu button is active; Arc-Dark fills it
	# with the lighter menu-surface tint behind the icon. 9-patch so it scales to
	# the BFB's icon size.
	w = int(round(40 * scale))
	img = Image.new("RGBA", (w * SS, w * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	draw.rounded_rectangle((0, 0, w * SS - 1, w * SS - 1), radius=8 * scale * SS, fill=MENU)
	content = img.resize((w, w), Image.LANCZOS)
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (int(round(4 * scale)), w - int(round(4 * scale)))
	save(nine_patch(content, mid, mid, pad, pad),
		dpi_path("theme_budgie_res_launcher_bfb_background_when_dash_opened.9.png", density))


# ---------------------------------------------------------------------------
# Launcher preferences: the teal "tweak tool" settings cog
# (etc/theme_budgie_launcher_preferences.png).
# ---------------------------------------------------------------------------

def launcher_preferences(density, scale):
	s = int(round(48 * scale))
	src = Image.open(f"{ETC}/theme_budgie_launcher_preferences.png").convert("RGBA")
	src = src.crop(src.getbbox())
	side = int(round(44 * scale))
	scaled = src.resize((side, round(side * src.height / src.width)), Image.LANCZOS)
	img = Image.new("RGBA", (s, s), CLEAR)
	img.alpha_composite(scaled, ((s - scaled.width) // 2, (s - scaled.height) // 2))
	save(img, dpi_path("theme_budgie_res_launcher_preferences.png", density))


def card_logo():
	# Theme-picker card: the official Budgie bird logo (etc/theme_budgie_logo.svg,
	# from Wikimedia Commons), reduced to a flat white silhouette to match the
	# other themes' monochrome card marks. The bird's eye and beak details share
	# the disc colour, so dropping the disc turns them into clean cut-outs.
	import numpy as np
	px = 256
	cairosvg.svg2png(url=f"{ETC}/theme_budgie_logo.svg", write_to="/tmp/budgie_logo.png",
		output_width=px * 2, output_height=px * 2)
	a = np.asarray(Image.open("/tmp/budgie_logo.png").convert("RGBA")).astype(int)
	disc = np.array([0x40, 0x45, 0x52])
	bird = (a[:, :, 3] > 40) & (np.abs(a[:, :, :3] - disc).sum(2) > 60)  # not disc/eye/beak
	out = np.zeros_like(a)
	out[:, :, :3] = 255
	out[:, :, 3] = np.where(bird, a[:, :, 3], 0)
	img = Image.fromarray(out.astype("uint8"))
	img = img.crop(img.getbbox())  # tight bird silhouette, aspect preserved
	scale = px / max(img.width, img.height)
	img = img.resize((round(img.width * scale), round(img.height * scale)), Image.LANCZOS)
	save(img, f"{RES}/drawable-nodpi/theme_budgie_res_card_logo.png")


def etc_sources():
	"""Flat reference renders kept in etc/ alongside the other theme sources.
	(theme_budgie_launcher_preferences.png and theme_budgie_logo.svg are the
	original downloaded sources.)"""
	save(_dash_silhouette("bottom", 2.0), f"{ETC}/theme_budgie_dash_background.png")
	save(grid_icon(96, BFB), f"{ETC}/theme_budgie_launcher_bfb.png")


def main():
	for density, scale in DENSITIES.items():
		dash_background(density, scale)
		dash_search_background(density, scale)
		app_running(density, scale)
		bfb(density, scale)
		bfb_background_when_dash_opened(density, scale)
		launcher_preferences(density, scale)
	card_logo()
	etc_sources()


if __name__ == "__main__":
	main()
