#!/usr/bin/env python3
"""Generate the GNOME theme drawables from values measured off the baseline
screenshots in `etc/theme baselines/gnome/`.

Colours and proportions were recovered from the white/black wallpaper
screenshot pairs (alpha = 1 - (C_white - C_black) / 255, colour = C_black / alpha):

  dock surface        #38383B, fully opaque, corner radius ~0.24 x dock thickness
  overview backdrop   #222226, fully opaque
  search pill         #404045, fully opaque, fully rounded ends
  search icon/hint    #C0C0C2
  running indicator   #FAFAFB dot
  app grid button     3x3 round #FAFAFB dots, pitch 0.31 x icon, dot 0.16 x icon
  top panel           #000000, fully opaque

Run from the repository root:  python3 etc/generate_theme_gnome_assets.py
Requires Pillow.
"""

import os
from PIL import Image, ImageDraw

RES = "app/src/main/res"
ETC = "etc"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
SS = 8  # supersampling factor for crisp antialiased shapes

DOCK = (0x38, 0x38, 0x3B, 255)
BACKDROP = (0x22, 0x22, 0x26, 255)
PILL = (0x40, 0x40, 0x45, 255)
HINT = (0xC0, 0xC0, 0xC2, 255)
DOT = (0xFA, 0xFA, 0xFB, 255)
BLACK = (0, 0, 0, 255)
CLEAR = (0, 0, 0, 0)


def rounded_rect(size_dp, radius_dp, colour, scale):
	"""Render an antialiased rounded rectangle at the given density scale."""
	w, h = int(round(size_dp[0] * scale)), int(round(size_dp[1] * scale))
	r = radius_dp * scale
	img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	draw.rounded_rectangle((0, 0, w * SS - 1, h * SS - 1), radius=r * SS, fill=colour)
	return img.resize((w, h), Image.LANCZOS)


def nine_patch(content, stretch_x, stretch_y, padding_x=None, padding_y=None):
	"""Wrap rendered content in a source-format 9-patch marker border.

	stretch/padding ranges are (start, end) pixel ranges in content coordinates,
	end exclusive. Padding defaults to the stretch region when omitted (matching
	aapt's behaviour for missing padding lines).
	"""
	w, h = content.size
	out = Image.new("RGBA", (w + 2, h + 2), CLEAR)
	out.paste(content, (1, 1))
	px = out.load()
	for x in range(*stretch_x):
		px[x + 1, 0] = BLACK
	for y in range(*stretch_y):
		px[0, y + 1] = BLACK
	if padding_x:
		for x in range(*padding_x):
			px[x + 1, h + 1] = BLACK
	if padding_y:
		for y in range(*padding_y):
			px[w + 1, y + 1] = BLACK
	return out


def save(img, *paths):
	for path in paths:
		os.makedirs(os.path.dirname(path), exist_ok=True)
		img.save(path)
		print(path, img.size)


def dpi_path(name, density):
	return f"{RES}/drawable-{density}/{name}"


def launcher_background(density, scale):
	# The dash floats off the screen edge (launcher_margin), so every corner is
	# rounded and the same drawable serves the left, right and bottom edges.
	content = rounded_rect((40, 40), 18, DOCK, scale)
	w, h = content.size
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (int(round(6 * scale)), w - int(round(6 * scale)))
	patch = nine_patch(content, mid, mid, pad, pad)
	for edge in ("left", "right", "bottom"):
		save(patch, dpi_path(f"theme_gnome_res_launcher_background_{edge}.9.png", density))


def panel_background(density, scale):
	size = max(int(round(8 * scale)), 4)
	content = Image.new("RGBA", (size, size), BLACK)
	mid = (size // 2 - 1, size // 2 + 1)
	patch = nine_patch(content, mid, mid)
	save(patch, dpi_path("theme_gnome_res_panel_background.9.png", density))


def magnifier(draw, cx, cy, size, colour):
	"""Draw a magnifying glass glyph centred on (cx, cy); all units are px."""
	r = size * 0.32
	stroke = max(int(round(size * 0.11)), 1)
	gx, gy = cx - size * 0.08, cy - size * 0.08
	draw.ellipse((gx - r, gy - r, gx + r, gy + r), outline=colour, width=stroke)
	hr = r * 0.7071
	draw.line((gx + hr, gy + hr, cx + size * 0.42, cy + size * 0.42),
		fill=colour, width=stroke)


def dash_search_background(density, scale):
	# Fully rounded pill with the magnifier baked into the fixed left cap,
	# mirroring the overview search entry.
	w_dp, h_dp, r_dp = 72, 36, 18
	w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
	img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	draw.rounded_rectangle((0, 0, w * SS - 1, h * SS - 1), radius=r_dp * scale * SS, fill=PILL)
	magnifier(draw, 24 * scale * SS, (h_dp / 2) * scale * SS, 17 * scale * SS, HINT)
	content = img.resize((w, h), Image.LANCZOS)
	sx = (int(round(44 * scale)), int(round(50 * scale)))  # between icon and right cap
	sy = (int(round((h_dp / 2 - 1) * scale)), int(round((h_dp / 2 + 1) * scale)))
	pad_x = (int(round(38 * scale)), w - int(round(14 * scale)))  # text right of icon
	pad_y = (int(round(8 * scale)), h - int(round(8 * scale)))
	save(nine_patch(content, sx, sy, pad_x, pad_y),
		dpi_path("theme_gnome_res_dash_search_background.9.png", density))


def dot(size_dp, diameter_dp, scale):
	s = int(round(size_dp * scale))
	img = Image.new("RGBA", (s * SS, s * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	c, r = size_dp * scale * SS / 2, diameter_dp * scale * SS / 2
	draw.ellipse((c - r, c - r, c + r, c + r), fill=DOT)
	return img.resize((s, s), Image.LANCZOS)


def app_running(density, scale):
	# GNOME marks running apps with a small light dot next to the icon.
	save(dot(6, 5, scale), dpi_path("theme_gnome_res_launcher_app_running.png", density))


def bfb(density, scale):
	# "Show apps" button: 3x3 grid of round dots.
	size_dp, pitch_dp, d_dp = 48, 15.5, 7.7
	s = int(round(size_dp * scale))
	img = Image.new("RGBA", (s * SS, s * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	c = size_dp * scale * SS / 2
	r = d_dp * scale * SS / 2
	for i in (-1, 0, 1):
		for j in (-1, 0, 1):
			x, y = c + i * pitch_dp * scale * SS, c + j * pitch_dp * scale * SS
			draw.ellipse((x - r, y - r, x + r, y + r), fill=DOT)
	save(img.resize((s, s), Image.LANCZOS), dpi_path("theme_gnome_res_launcher_bfb.png", density))


def etc_sources():
	"""Flat (non-9-patch) renders kept in etc/ alongside the other theme sources."""
	save(rounded_rect((160, 160), 18, DOCK, 1.0), f"{ETC}/theme_gnome_launcher_background.png")
	save(Image.new("RGBA", (160, 28), BLACK), f"{ETC}/theme_gnome_panel_background.png")
	w, h = 320, 36
	img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	draw.rounded_rectangle((0, 0, w * SS - 1, h * SS - 1), radius=18 * SS, fill=PILL)
	magnifier(draw, 24 * SS, h / 2 * SS, 17 * SS, HINT)
	save(img.resize((w, h), Image.LANCZOS), f"{ETC}/theme_gnome_dash_search_background.png")
	save(Image.new("RGBA", (160, 90), BACKDROP), f"{ETC}/theme_gnome_dash_background.png")


def main():
	for density, scale in DENSITIES.items():
		launcher_background(density, scale)
		panel_background(density, scale)
		dash_search_background(density, scale)
		app_running(density, scale)
		bfb(density, scale)
	etc_sources()


if __name__ == "__main__":
	main()
