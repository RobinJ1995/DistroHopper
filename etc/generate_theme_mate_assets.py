#!/usr/bin/env python3
"""Generate the MATE theme drawables from values measured off the baseline
screenshots in `etc/theme baselines/mate/` (Ubuntu MATE, Yaru-MATE light).

Colours and opacities were recovered from the white/black wallpaper
screenshot pairs (alpha = 1 - (C_white - C_black) / 255, colour = C_black / alpha):

  panels              #EDEDED, fully opaque, flat, 28px tall (MATE has two:
                      the menu bar maps to the launcher, the other to the
                      panel, which sits on whichever horizontal edge the
                      launcher leaves free)
  menu surface        #FAFAFA, fully opaque (the Brisk menu)
  search field fill   #FFFFFF, 1px #C9C9C9 border, ~8dp radius, magnifier in
                      the left cap; the focused state does not appear in the
                      baselines and uses the Ubuntu MATE green #87A556
  menu button         dark Ubuntu MATE logo + "Menu" label (#3F3D3E); the
                      label is dropped on vertical edges, where it cannot fit
  running cell        a light raised button behind the window-list entry

The Ubuntu MATE logo (etc/theme_mate_logo.png, white on transparency, from
ubuntu-mate.community) is recoloured per use: dark on the light launcher,
white on the green theme card.

Run from the repository root:  python3 etc/generate_theme_mate_assets.py
Requires Pillow.
"""

import os
import cairosvg
import numpy as np
from PIL import Image, ImageDraw, ImageFont

RES = "app/src/main/res"
ETC = "etc"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
SS = 8  # supersampling factor for crisp antialiased shapes
FONT = "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf"

PANEL = (0xED, 0xED, 0xED, 255)
MENU = (0xED, 0xED, 0xED, 255)
APPS_PANEL = (0xDB, 0xDB, 0xDB, 255)
FIELD = (0xFF, 0xFF, 0xFF, 255)
FIELD_BORDER = (0xC9, 0xC9, 0xC9, 255)
FIELD_BORDER_FOCUSED = (0x87, 0xA5, 0x56, 255)
GLYPH = (0x8A, 0x8A, 0x8A, 255)
TEXT = (0x3F, 0x3D, 0x3E, 255)
LOGO = (0x3A, 0x3A, 0x3A, 255)
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


def logo(px, colour):
	"""The Ubuntu MATE logo at the given size, recoloured."""
	img = Image.open(f"{ETC}/theme_mate_logo.png").convert("RGBA")
	img = img.resize((px, px), Image.LANCZOS)
	a = np.asarray(img).copy()
	a[..., 0:3] = colour[0:3]
	return Image.fromarray(a)


def launcher_background(density, scale):
	# Flat opaque Yaru-MATE panel, identical on every edge.
	w = int(round(24 * scale))
	img = Image.new("RGBA", (w, w), PANEL)
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (max(int(round(scale)), 1), w - max(int(round(scale)), 1))
	patch = nine_patch(img, mid, mid, pad, pad)
	for edge in ("top", "right", "bottom", "left"):
		save(patch, dpi_path(f"theme_mate_res_launcher_background_{edge}.9.png", density))


def panel_background(density, scale):
	# The second MATE panel: the same flat surface.
	w = int(round(24 * scale))
	img = Image.new("RGBA", (w, w), PANEL)
	mid = (w // 2 - 1, w // 2 + 1)
	save(nine_patch(img, mid, mid, (0, w), (0, w)),
		dpi_path("theme_mate_res_panel_background.9.png", density))


def dash_background(density, scale):
	# The Brisk menu: the base surface matches the panels, and everything
	# below the search field (the Applications label and the app grid) sits
	# on a darker rounded panel, mirroring the baselines' app list panel
	# (drawn with more contrast than the desktop's, which gets lost behind
	# the icon grid on a phone). The vertical stretch zone lives inside the
	# inner panel, so the zone above it keeps its height; the padding keeps
	# the dash content inside the panel's insets.
	top_dp, inset_dp, r_dp = 52, 16, 8
	w_dp, h_dp = 80, 160
	w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
	top, i = int(round(top_dp * scale)), int(round(inset_dp * scale))
	img = Image.new("RGBA", (w * SS, h * SS), PANEL)
	draw = ImageDraw.Draw(img)
	draw.rounded_rectangle((i * SS, top * SS, (w - i) * SS - 1, (h - i) * SS - 1),
		radius=r_dp * scale * SS, fill=APPS_PANEL)
	content = img.resize((w, h), Image.LANCZOS)
	mid_x = (w // 2 - 1, w // 2 + 1)
	stretch_y = (top + int(round(12 * scale)), h - i - int(round(12 * scale)))
	pad_x = (int(round(16 * scale)), w - int(round(16 * scale)))
	pad_y = (int(round(4 * scale)), h - int(round(24 * scale)))
	save(nine_patch(content, mid_x, stretch_y, pad_x, pad_y),
		dpi_path("theme_mate_res_dash_background.9.png", density))


def dash_search_background(density, scale):
	# Brisk's "Type to search..." entry, magnifier in the fixed left cap. A
	# transparent inset provides spacing; the vertical stretch zones live in
	# that inset so the border and the magnifier never distort.
	for name, border in (("default", FIELD_BORDER), ("focused", FIELD_BORDER_FOCUSED)):
		inset_x_dp, inset_top_dp, inset_bottom_dp, field_h_dp, r_dp = 0, 6, 18, 32, 4
		w_dp = 96
		h_dp = field_h_dp + inset_top_dp + inset_bottom_dp
		w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
		img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
		draw = ImageDraw.Draw(img)
		ix, iy = inset_x_dp * scale * SS, inset_top_dp * scale * SS
		ib = inset_bottom_dp * scale * SS
		draw.rounded_rectangle((ix, iy, w * SS - 1 - ix, h * SS - 1 - ib),
			radius=r_dp * scale * SS, fill=FIELD, outline=border,
			width=int(round(1.5 * scale * SS)))
		magnifier(draw, (inset_x_dp + 18) * scale * SS,
			(inset_top_dp + field_h_dp / 2) * scale * SS, 20 * scale * SS, GLYPH)
		content = img.resize((w, h), Image.LANCZOS)
		sx = (int(round(48 * scale)), int(round(56 * scale)))  # right of the magnifier
		sy = [(0, int(round(4 * scale))), (h - int(round(4 * scale)), h)]
		pad_x = (int(round(32 * scale)), w - int(round(8 * scale)))  # text right of icon
		pad_y = (int(round(14 * scale)), h - int(round(26 * scale)))
		save(nine_patch(content, sx, sy, pad_x, pad_y),
			dpi_path(f"theme_mate_res_dash_search_background_{name}.9.png", density))


def bfb(density, scale):
	# The menu button: dark logo plus the "Menu" label on horizontal edges,
	# just the logo on vertical edges (no room for the label there).
	logo_dp = 30
	s48 = int(round(48 * scale))

	# vertical variant: logo only, centred on a square canvas
	img = Image.new("RGBA", (s48, s48), CLEAR)
	l = logo(int(round(logo_dp * scale)), LOGO)
	img.alpha_composite(l, ((s48 - l.width) // 2, (s48 - l.height) // 2))
	save(img, dpi_path("theme_mate_res_launcher_bfb_vertical.png", density))

	# horizontal variant: logo + label
	w = int(round(84 * scale))
	img = Image.new("RGBA", (w, s48), CLEAR)
	img.alpha_composite(l, (int(round(4 * scale)), (s48 - l.height) // 2))
	font = ImageFont.truetype(FONT, int(round(17 * scale)))
	draw = ImageDraw.Draw(img)
	draw.text((int(round((4 + logo_dp + 5) * scale)), s48 / 2), "Menu",
		font=font, fill=TEXT, anchor="lm")
	save(img, dpi_path("theme_mate_res_launcher_bfb.png", density))


def card_logo():
	# Theme selection card: the white logo as provided.
	img = Image.open(f"{ETC}/theme_mate_logo.png").convert("RGBA")
	img = img.crop(img.getbbox())
	w = round(img.width * 194 / img.height)
	save(img.resize((w, 194), Image.LANCZOS),
		f"{RES}/drawable-nodpi/theme_mate_res_card_logo.png")


def panel_cog(density, scale):
	# Yaru's system-shutdown-symbolic, recoloured dark for the light panel.
	s = int(round(22 * scale))
	cairosvg.svg2png(url=f"{ETC}/theme_mate_panel_cog.svg", write_to="/tmp/mate_cog.png",
		output_width=s, output_height=s)
	a = np.asarray(Image.open("/tmp/mate_cog.png").convert("RGBA")).copy()
	a[..., 0:3] = TEXT[0:3]
	save(Image.fromarray(a), dpi_path("theme_mate_res_panel_cog.png", density))


def launcher_preferences(density, scale):
	# Yaru's Tweaks app icon.
	s = int(round(48 * scale))
	img = Image.open(f"{ETC}/theme_mate_launcher_preferences.png").convert("RGBA")
	save(img.resize((s, s), Image.LANCZOS),
		dpi_path("theme_mate_res_launcher_preferences.png", density))


def etc_sources():
	"""Flat (non-9-patch) renders kept in etc/ alongside the other theme sources."""
	save(rounded_rect((160, 28), 0, PANEL, 1.0), f"{ETC}/theme_mate_launcher_background.png")
	save(rounded_rect((160, 160), 0, MENU, 1.0), f"{ETC}/theme_mate_dash_background.png")


def bfb_background_when_dash_opened(density, scale):
	# While the menu is open the MATE "Menu" button is active; Yaru-MATE marks it
	# with a slightly darker rounded box (subtle border) behind the icon. 9-patch
	# so it scales to the BFB's icon size.
	content = rounded_rect((40, 40), 6, APPS_PANEL, scale, outline=FIELD_BORDER, outline_width_dp=1)
	w, h = content.size
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (int(round(4 * scale)), w - int(round(4 * scale)))
	save(nine_patch(content, mid, mid, pad, pad),
		dpi_path("theme_mate_res_launcher_bfb_background_when_dash_opened.9.png", density))


def main():
	for density, scale in DENSITIES.items():
		launcher_background(density, scale)
		panel_background(density, scale)
		dash_background(density, scale)
		dash_search_background(density, scale)
		bfb(density, scale)
		bfb_background_when_dash_opened(density, scale)
		panel_cog(density, scale)
		launcher_preferences(density, scale)
	card_logo()
	etc_sources()


if __name__ == "__main__":
	main()
