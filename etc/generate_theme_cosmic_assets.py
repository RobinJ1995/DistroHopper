#!/usr/bin/env python3
"""Generate the Cosmic theme drawables from values measured off the baseline
screenshots in `etc/theme baselines/cosmic/` (Pop!_OS COSMIC, dark).

Colours and opacities were recovered from the white/black wallpaper
screenshot pairs (alpha = 1 - (C_white - C_black) / 255, colour = C_black / alpha):

  panel               #1B1B1B, fully opaque, flat, full width
  dock                #1B1B1B (kept fully opaque and matched to the panel
                      and library surfaces), ~16dp corner radius, floating
                      4px off the screen edge
  app library (dash)  #1B1B1B, fully opaque, ~10dp corner radius, 1px
                      #444444 border, floating clear of every screen edge
  search pill         fill matches the library, fully rounded, fixed width
                      and centred (it does not span the dash); the baselines
                      capture the focused state with the COSMIC teal #63D0DF
                      border, the unfocused border is approximated as #4A4A4A
  search glyph        #E7E7E7, magnifier in the left cap
  running indicator   teal #63D0DF dot, over a soft #434343 highlight at 63%
                      opacity behind the running app's icon

The BFB and theme card use the COSMIC logo white mark
(etc/theme_cosmic_logo.svg, from Wikimedia Commons).

Run from the repository root:  python3 etc/generate_theme_cosmic_assets.py
Requires Pillow and cairosvg.
"""

import os
import cairosvg
from PIL import Image, ImageDraw

RES = "app/src/main/res"
ETC = "etc"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
SS = 8  # supersampling factor for crisp antialiased shapes

PANEL = (0x1B, 0x1B, 0x1B, 255)
DOCK = (0x1B, 0x1B, 0x1B, 255)
LIBRARY = (0x1B, 0x1B, 0x1B, 255)
SURFACE_BORDER = (0x44, 0x44, 0x44, 255)
FIELD = (0x1B, 0x1B, 0x1B, 255)
FIELD_BORDER = (0x4A, 0x4A, 0x4A, 255)
FIELD_BORDER_FOCUSED = (0x63, 0xD0, 0xDF, 255)
GLYPH = (0xE7, 0xE7, 0xE7, 255)
RUNNING = (0x63, 0xD0, 0xDF, 255)
BLACK = (0, 0, 0, 255)
CLEAR = (0, 0, 0, 0)


def rounded_rect(size_dp, radius_dp, colour, scale, outline=None, outline_width_dp=0):
	"""Render an antialiased rounded rectangle at the given density scale."""
	w, h = int(round(size_dp[0] * scale)), int(round(size_dp[1] * scale))
	img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	draw.rounded_rectangle((0, 0, w * SS - 1, h * SS - 1), radius=radius_dp * scale * SS,
		fill=colour, outline=outline,
		width=int(round(outline_width_dp * scale * SS)) if outline else 0)
	return img.resize((w, h), Image.LANCZOS)


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


def launcher_background(density, scale):
	# The floating COSMIC dock: translucent black, generously rounded on
	# every corner (it floats off the screen edge via launcher_margin), the
	# same image for every dockable edge.
	content = rounded_rect((44, 44), 16, DOCK, scale)
	w = content.size[0]
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (int(round(6 * scale)), w - int(round(6 * scale)))
	patch = nine_patch(content, mid, mid, pad, pad)
	for edge in ("top", "right", "bottom", "left"):
		save(patch, dpi_path(f"theme_cosmic_res_launcher_background_{edge}.9.png", density))


def panel_background(density, scale):
	# The COSMIC panel: flat opaque dark, full width.
	w = int(round(24 * scale))
	img = Image.new("RGBA", (w, w), PANEL)
	mid = (w // 2 - 1, w // 2 + 1)
	save(nine_patch(img, mid, mid, (0, w), (0, w)),
		dpi_path("theme_cosmic_res_panel_background.9.png", density))


def dash_background(density, scale):
	# The app library: an opaque rounded surface floating inside a baked
	# transparent margin, clear of every screen edge as in the baselines.
	# the inset matches the dock's launcher_margin, so their edges line up
	inset_dp, r_dp = 4, 10
	size_dp = 64
	s = int(round(size_dp * scale))
	i = int(round(inset_dp * scale))
	img = Image.new("RGBA", (s * SS, s * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	draw.rounded_rectangle((i * SS, i * SS, s * SS - 1 - i * SS, s * SS - 1 - i * SS),
		radius=r_dp * scale * SS, fill=LIBRARY, outline=SURFACE_BORDER,
		width=max(int(round(scale * SS)), 1))
	content = img.resize((s, s), Image.LANCZOS)
	mid = (s // 2 - 1, s // 2 + 1)
	pad = (i + int(round(8 * scale)), s - i - int(round(8 * scale)))
	save(nine_patch(content, mid, mid, pad, pad),
		dpi_path("theme_cosmic_res_dash_background.9.png", density))


def dash_search_background(density, scale):
	# The library's search pill. The fixed width and centring come from the
	# dash_search_width theme dimension; the asset itself is a normal
	# stretchable pill. The vertical stretch zones live in the transparent
	# inset above and below, so the rounded ends and the magnifier never
	# distort; the horizontal zone sits between the magnifier and the right
	# cap.
	for name, border in (("default", FIELD_BORDER), ("focused", FIELD_BORDER_FOCUSED)):
		pill_h_dp, inset_y_dp = 36, 6
		w_dp = 96
		h_dp = pill_h_dp + 2 * inset_y_dp
		w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
		img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
		draw = ImageDraw.Draw(img)
		iy = inset_y_dp * scale * SS
		draw.rounded_rectangle((0, iy, w * SS - 1, h * SS - 1 - iy),
			radius=pill_h_dp / 2 * scale * SS, fill=FIELD, outline=border,
			width=int(round(1.5 * scale * SS)))
		magnifier(draw, 22 * scale * SS, (h_dp / 2) * scale * SS,
			16 * scale * SS, GLYPH)
		content = img.resize((w, h), Image.LANCZOS)
		sx = (int(round(44 * scale)), int(round(52 * scale)))
		sy = [(0, int(round(3 * scale))), (h - int(round(3 * scale)), h)]
		pad_x = (int(round(40 * scale)), w - int(round(20 * scale)))
		pad_y = (int(round(14 * scale)), h - int(round(14 * scale)))
		save(nine_patch(content, sx, sy, pad_x, pad_y),
			dpi_path(f"theme_cosmic_res_dash_search_background_{name}.9.png", density))


def app_running(density, scale):
	# COSMIC marks running apps with a small teal dot below the icon. The
	# engine draws this in a narrow strip beside the icon instead.
	size_dp, d_dp = 6, 5
	s = int(round(size_dp * scale))
	img = Image.new("RGBA", (s * SS, s * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	c, r = size_dp * scale * SS / 2, d_dp * scale * SS / 2
	draw.ellipse((c - r, c - r, c + r, c + r), fill=RUNNING)
	save(img.resize((s, s), Image.LANCZOS),
		dpi_path("theme_cosmic_res_launcher_app_running.png", density))


def logo_white(px):
	cairosvg.svg2png(url=f"{ETC}/theme_cosmic_logo.svg", write_to="/tmp/cosmic_logo.png",
		output_width=px * 4, output_height=px * 4)
	img = Image.open("/tmp/cosmic_logo.png").convert("RGBA")
	# blank the TM mark in the bottom-right corner
	import numpy as np
	a = np.asarray(img).copy()
	hh, ww = a.shape[:2]
	a[int(hh * 0.72):, int(ww * 0.89):, 3] = 0
	return Image.fromarray(a).resize((px, px), Image.LANCZOS)


def bfb(density, scale):
	# The launcher button: the COSMIC logo white mark, per density.
	s = int(round(48 * scale))
	img = Image.new("RGBA", (s, s), CLEAR)
	l = logo_white(int(round(34 * scale)))
	l = l.crop(l.getbbox())
	img.alpha_composite(l, ((s - l.width) // 2, (s - l.height) // 2))
	save(img, dpi_path("theme_cosmic_res_launcher_bfb.png", density))


def launcher_preferences(density, scale):
	# The COSMIC Settings app icon (etc/theme_cosmic_launcher_preferences.png).
	s = int(round(48 * scale))
	img = Image.open(f"{ETC}/theme_cosmic_launcher_preferences.png").convert("RGBA")
	img = img.crop(img.getbbox())
	save(img.resize((s, s), Image.LANCZOS),
		dpi_path("theme_cosmic_res_launcher_preferences.png", density))


def card_logo():
	img = logo_white(512)
	img = img.crop(img.getbbox())
	w = round(img.width * 194 / img.height)
	save(img.resize((w, 194), Image.LANCZOS),
		f"{RES}/drawable-nodpi/theme_cosmic_res_card_logo.png")


def etc_sources():
	"""Flat (non-9-patch) renders kept in etc/ alongside the other theme sources."""
	save(rounded_rect((160, 44), 16, DOCK, 1.0), f"{ETC}/theme_cosmic_launcher_background.png")
	save(rounded_rect((160, 160), 10, LIBRARY, 1.0), f"{ETC}/theme_cosmic_dash_background.png")


def main():
	for density, scale in DENSITIES.items():
		launcher_background(density, scale)
		panel_background(density, scale)
		dash_background(density, scale)
		dash_search_background(density, scale)
		app_running(density, scale)
		bfb(density, scale)
		launcher_preferences(density, scale)
	card_logo()
	etc_sources()


if __name__ == "__main__":
	main()
