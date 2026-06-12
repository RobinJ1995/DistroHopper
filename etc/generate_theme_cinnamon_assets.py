#!/usr/bin/env python3
"""Generate the Cinnamon theme drawables from values measured off the baseline
screenshots in `etc/theme baselines/cinnamon/` (Linux Mint, Mint-Y dark).

Colours and opacities were recovered from the white/black wallpaper
screenshot pairs (alpha = 1 - (C_white - C_black) / 255, colour = C_black / alpha):

  panel               #1C1C20, fully opaque, square, 1px #222226 desktop-facing edge
  menu surface        #222226, fully opaque, ~4dp corner radius, thin dark border
  search field fill   #303036, fully opaque, ~4dp radius, 32dp tall
  search border       #1F9EDE focused (the unfocused state does not appear in
                      the baselines; approximated as #55555E)
  search magnifier    #E1E1E1, right-hand cap
  running indicator   #1F9EDE bar under the window-list button
  running cell        #303036 highlight behind the running app's icon

The Mint logo BFB and the preferences icon are kept as-is (still current).

Run from the repository root:  python3 etc/generate_theme_cinnamon_assets.py
Requires Pillow.
"""

import os
from PIL import Image, ImageDraw

RES = "app/src/main/res"
ETC = "etc"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
SS = 8  # supersampling factor for crisp antialiased shapes

PANEL = (0x1C, 0x1C, 0x20, 255)
PANEL_EDGE = (0x22, 0x22, 0x26, 255)
MENU = (0x22, 0x22, 0x26, 255)
MENU_BORDER = (0, 0, 0, 128)
FIELD = (0x30, 0x30, 0x36, 255)
FIELD_BORDER = (0x55, 0x55, 0x5E, 255)
FIELD_BORDER_FOCUSED = (0x1F, 0x9E, 0xDE, 255)
GLYPH = (0xE1, 0xE1, 0xE1, 255)
RUNNING = (0x1F, 0x9E, 0xDE, 255)
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
	# Flat Mint panel: opaque near-black, square corners, a 1px lighter line
	# along the desktop-facing edge. One variant per dockable edge, with the
	# edge line on the side that faces the screen content.
	w_dp = 24
	w = int(round(w_dp * scale))
	edge_px = max(int(round(scale)), 1)
	for edge in ("top", "right", "bottom", "left"):
		img = Image.new("RGBA", (w, w), PANEL)
		draw = ImageDraw.Draw(img)
		line = {"top": (0, w - edge_px, w - 1, w - 1), "bottom": (0, 0, w - 1, edge_px - 1),
			"left": (w - edge_px, 0, w - 1, w - 1), "right": (0, 0, edge_px - 1, w - 1)}[edge]
		draw.rectangle(line, fill=PANEL_EDGE)
		mid = (w // 2 - 1, w // 2 + 1)
		pad = (int(round(2 * scale)), w - int(round(2 * scale)))
		save(nine_patch(img, mid, mid, pad, pad),
			dpi_path(f"theme_cinnamon_res_launcher_background_{edge}.9.png", density))


def dash_background(density, scale):
	# The Mint menu surface: flat dark. (The desktop menu has a border and
	# rounded corners, but the dash fills the screen here, where the border
	# looks clunky and the rounding leaves pixel gaps against the launcher.)
	content = rounded_rect((32, 32), 0, MENU, scale)
	w = content.size[0]
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (int(round(8 * scale)), w - int(round(8 * scale)))
	save(nine_patch(content, mid, mid, pad, pad),
		dpi_path("theme_cinnamon_res_dash_background.9.png", density))


def dash_search_background(density, scale):
	# Mint's bordered search entry, magnifier in the fixed right cap. A small
	# transparent inset provides spacing; the vertical stretch zones live in
	# that inset so the border and the magnifier never distort.
	for name, border in (("default", FIELD_BORDER), ("focused", FIELD_BORDER_FOCUSED)):
		inset_dp, field_h_dp, r_dp = 4, 32, 4
		w_dp = 88
		h_dp = field_h_dp + 2 * inset_dp
		w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
		img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
		draw = ImageDraw.Draw(img)
		i = inset_dp * scale * SS
		draw.rounded_rectangle((i, i, w * SS - 1 - i, h * SS - 1 - i),
			radius=r_dp * scale * SS, fill=FIELD, outline=border,
			width=int(round(1.5 * scale * SS)))
		magnifier(draw, (w_dp - inset_dp - 16) * scale * SS, (h_dp / 2) * scale * SS,
			15 * scale * SS, GLYPH)
		content = img.resize((w, h), Image.LANCZOS)
		sx = (int(round(12 * scale)), int(round(20 * scale)))  # left of the magnifier
		sy = [(0, int(round(3 * scale))), (h - int(round(3 * scale)), h)]
		pad_x = (int(round(12 * scale)), w - int(round(34 * scale)))  # text left of icon
		pad_y = (int(round(12 * scale)), h - int(round(12 * scale)))
		save(nine_patch(content, sx, sy, pad_x, pad_y),
			dpi_path(f"theme_cinnamon_res_dash_search_background_{name}.9.png", density))


def app_running(density, scale):
	# Mint underlines the running app's window-list button with a blue bar.
	# The engine draws this in a narrow strip beside the icon, so the bar is
	# rendered vertically.
	w_dp, h_dp, bar_w_dp, bar_h_dp = 6, 48, 3, 40
	w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
	img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	x0 = (w_dp - bar_w_dp) / 2 * scale * SS
	y0 = (h_dp - bar_h_dp) / 2 * scale * SS
	draw.rounded_rectangle((x0, y0, w * SS - 1 - x0, h * SS - 1 - y0),
		radius=bar_w_dp / 2 * scale * SS, fill=RUNNING)
	save(img.resize((w, h), Image.LANCZOS),
		dpi_path("theme_cinnamon_res_launcher_app_running.png", density))


def etc_sources():
	"""Flat (non-9-patch) renders kept in etc/ alongside the other theme sources."""
	save(rounded_rect((160, 40), 0, PANEL, 1.0), f"{ETC}/theme_cinnamon_launcher_background.png")
	save(rounded_rect((160, 160), 0, MENU, 1.0), f"{ETC}/theme_cinnamon_dash_background.png")


def main():
	for density, scale in DENSITIES.items():
		launcher_background(density, scale)
		dash_background(density, scale)
		dash_search_background(density, scale)
		app_running(density, scale)
	etc_sources()


if __name__ == "__main__":
	main()
