#!/usr/bin/env python3
"""Generate the Plasma theme drawables from values measured off the baseline
screenshots in `etc/theme baselines/kde/` (KDE Plasma, Breeze dark).

Colours and opacities were recovered from the white/black wallpaper
screenshot pairs (alpha = 1 - (C_white - C_black) / 255, colour = C_black / alpha):

  panel               #202326 at 85% opacity, floating ~5dp off the screen
                      edges with ~5dp rounded corners (Plasma 6 floating panel)
  Kickoff surface     #202326: a 48dp header band at 96% opacity holding the
                      search field, a 1px #4C4E51 separator, and the body at
                      92% opacity (slightly more opaque than the desktop's 85%
                      for phone legibility; Plasma blurs whatever is behind
                      both, and the dash gets the engine's whole-window blur
                      while open)
  search field fill   #141618, fully opaque, small radius, "Search..." at the
                      top of Kickoff with the magnifier in the left cap
  search border       Breeze outline grey #3F4347; the focused state does not
                      appear in the baselines and uses the Breeze accent #3DAEE9
  running indicator   #3DAEE9 bar on the cell edge, over a #2E6686 highlight
                      at 92% opacity behind the running app's icon

The BFB is the monochrome Plasma logo (etc/theme_plasma_logo.svg, from
https://kde.org/stuff/clipart/logo/plasma-logo-monochrome.svg), recoloured
white for the dark panel; the theme selection card uses the same render.
The preferences icon recreates the System Settings tile visible as the
panel's second icon in the baselines (dark tile, blue sliders, white knobs).

Run from the repository root:  python3 etc/generate_theme_plasma_assets.py
Requires Pillow and cairosvg.
"""

import os
import cairosvg
import numpy as np
from PIL import Image, ImageDraw

RES = "app/src/main/res"
ETC = "etc"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
SS = 8  # supersampling factor for crisp antialiased shapes

PANEL = (0x20, 0x23, 0x26, 216)
KICKOFF_HEADER = (0x20, 0x23, 0x25, 245)
KICKOFF_SEPARATOR = (0x4C, 0x4E, 0x51, 255)
KICKOFF = (0x20, 0x23, 0x26, 235)
FIELD = (0x14, 0x16, 0x18, 255)
FIELD_BORDER = (0x3F, 0x43, 0x47, 255)
FIELD_BORDER_FOCUSED = (0x3D, 0xAE, 0xE9, 255)
GLYPH = (0x9D, 0xA0, 0xA3, 255)
RUNNING = (0x3D, 0xAE, 0xE9, 255)
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
	# Plasma 6 floating panel: translucent, rounded on every corner (it
	# floats off the screen edge via launcher_margin), same image for every
	# dockable edge.
	content = rounded_rect((32, 32), 5, PANEL, scale)
	w = content.size[0]
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (int(round(3 * scale)), w - int(round(3 * scale)))
	patch = nine_patch(content, mid, mid, pad, pad)
	for edge in ("top", "right", "bottom", "left"):
		save(patch, dpi_path(f"theme_plasma_res_launcher_background_{edge}.9.png", density))


def dash_background(density, scale):
	# The Kickoff surface, floating inside a baked transparent margin that
	# matches the panel's float: a more opaque header band (which the search
	# field sits vertically centred in) over the translucent body, divided
	# by Kickoff's separator line. The desktop popup's rounded corners stay
	# (the surface floats here, so they cost nothing), and the vertical
	# stretch zone lives in the body, so the header keeps its height.
	inset_dp, header_dp, r_dp = 5, 52, 5
	w_dp = 42
	body_dp = 38
	h_dp = inset_dp + header_dp + 1 + body_dp + inset_dp
	w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
	i = int(round(inset_dp * scale))
	header = int(round(header_dp * scale))
	sep = max(int(round(scale)), 1)
	img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	draw.rounded_rectangle((i * SS, i * SS, (w - i) * SS - 1, (h - i) * SS - 1),
		radius=r_dp * scale * SS, fill=KICKOFF)
	content = img.resize((w, h), Image.LANCZOS)
	draw = ImageDraw.Draw(content)
	# header band and separator over the surface (straight edges, so they are
	# drawn at full resolution after the downsample)
	band = content.crop((i, i, w - i, i + header)).copy()
	hdr = Image.new("RGBA", band.size, (0, 0, 0, 0))
	hdr.paste(Image.new("RGBA", band.size, KICKOFF_HEADER), (0, 0), band.split()[3])
	content.paste(hdr, (i, i))
	draw.rectangle((i, i + header, w - i - 1, i + header + sep - 1), fill=KICKOFF_SEPARATOR)
	mid_x = (w // 2 - 1, w // 2 + 1)
	stretch_y = (i + header + sep + int(round(4 * scale)), h - i - int(round(4 * scale)))
	pad_x = (i + int(round(8 * scale)), w - i - int(round(8 * scale)))
	pad_y = (i + int(round(2 * scale)), h - i - int(round(8 * scale)))
	save(nine_patch(content, mid_x, stretch_y, pad_x, pad_y),
		dpi_path("theme_plasma_res_dash_background.9.png", density))


def dash_search_background(density, scale):
	# Kickoff's search entry, magnifier in the fixed left cap. A transparent
	# inset provides spacing; the vertical stretch zones live in that inset
	# so the border and the magnifier never distort.
	for name, border in (("default", FIELD_BORDER), ("focused", FIELD_BORDER_FOCUSED)):
		# the larger bottom inset keeps the content below the header band at a
		# comfortable distance from the separator line
		inset_x_dp, inset_top_dp, inset_bottom_dp, field_h_dp, r_dp = 4, 4, 14, 32, 4
		w_dp = 88
		h_dp = field_h_dp + inset_top_dp + inset_bottom_dp
		w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
		img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
		draw = ImageDraw.Draw(img)
		ix, iy = inset_x_dp * scale * SS, inset_top_dp * scale * SS
		ib = inset_bottom_dp * scale * SS
		draw.rounded_rectangle((ix, iy, w * SS - 1 - ix, h * SS - 1 - ib),
			radius=r_dp * scale * SS, fill=FIELD, outline=border,
			width=int(round(1.5 * scale * SS)))
		magnifier(draw, (inset_x_dp + 16) * scale * SS,
			(inset_top_dp + field_h_dp / 2) * scale * SS, 15 * scale * SS, GLYPH)
		content = img.resize((w, h), Image.LANCZOS)
		sx = (int(round(40 * scale)), int(round(48 * scale)))  # right of the magnifier
		sy = [(0, int(round(3 * scale))), (h - int(round(3 * scale)), h)]
		pad_x = (int(round(34 * scale)), w - int(round(12 * scale)))  # text right of icon
		pad_y = (int(round(12 * scale)), h - int(round(22 * scale)))
		save(nine_patch(content, sx, sy, pad_x, pad_y),
			dpi_path(f"theme_plasma_res_dash_search_background_{name}.9.png", density))


def app_running(density, scale):
	# Plasma's task manager marks the running app's cell with an accent bar
	# on the desktop-facing edge. The engine draws this in a narrow strip
	# beside the icon, so the bar is rendered vertically.
	w_dp, h_dp, bar_w_dp, bar_h_dp = 6, 48, 3, 40
	w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
	img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	x0 = (w_dp - bar_w_dp) / 2 * scale * SS
	y0 = (h_dp - bar_h_dp) / 2 * scale * SS
	draw.rounded_rectangle((x0, y0, w * SS - 1 - x0, h * SS - 1 - y0),
		radius=bar_w_dp / 2 * scale * SS, fill=RUNNING)
	save(img.resize((w, h), Image.LANCZOS),
		dpi_path("theme_plasma_res_launcher_app_running.png", density))


def launcher_preferences(density, scale):
	# The System Settings tile, as seen as the panel's second icon in the
	# baselines: a dark rounded square with two blue slider rails and white
	# round knobs.
	size_dp = 48
	s = int(round(size_dp * scale))
	img = Image.new("RGBA", (s * SS, s * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	tile = (0x26, 0x29, 0x2C, 255)
	rail = (0x3D, 0xAE, 0xE9, 255)
	knob = (0xFC, 0xFC, 0xFC, 255)
	t0, t1 = 6 * scale * SS, (size_dp - 6) * scale * SS
	draw.rounded_rectangle((t0, t0, t1, t1), radius=4 * scale * SS, fill=tile)
	stroke = int(round(3 * scale * SS))
	knob_r = 4.5 * scale * SS
	x0, x1 = 13 * scale * SS, (size_dp - 13) * scale * SS
	for row_dp, knob_dp in ((20, 31), (28, 18)):
		y = row_dp * scale * SS
		draw.line((x0, y, x1, y), fill=rail, width=stroke)
		kx = knob_dp * scale * SS
		draw.ellipse((kx - knob_r, y - knob_r, kx + knob_r, y + knob_r), fill=knob)
	save(img.resize((s, s), Image.LANCZOS),
		dpi_path("theme_plasma_res_launcher_preferences.png", density))


def plasma_logo_white(px):
	"""Render the monochrome Plasma logo at the given size, recoloured white
	(the SVG's glyph colour is dark, made for light backgrounds)."""
	cairosvg.svg2png(url=f"{ETC}/theme_plasma_logo.svg", write_to="/tmp/plasma_logo.png",
		output_width=px, output_height=px)
	a = np.asarray(Image.open("/tmp/plasma_logo.png").convert("RGBA")).copy()
	a[..., 0:3] = 255  # keep the glyph's alpha, recolour it white
	return Image.fromarray(a)


def bfb():
	# The application menu button: the monochrome Plasma logo, per density.
	for density, scale in DENSITIES.items():
		save(plasma_logo_white(int(round(48 * scale))),
			dpi_path("theme_plasma_res_launcher_bfb.png", density))


def card_logo():
	img = plasma_logo_white(512)
	img = img.crop(img.getbbox())
	w = round(img.width * 194 / img.height)
	save(img.resize((w, 194), Image.LANCZOS),
		f"{RES}/drawable-nodpi/theme_plasma_res_card_logo.png")


def etc_sources():
	"""Flat (non-9-patch) renders kept in etc/ alongside the other theme sources."""
	save(rounded_rect((160, 48), 5, PANEL, 1.0), f"{ETC}/theme_plasma_launcher_background.png")
	save(rounded_rect((160, 160), 0, KICKOFF, 1.0), f"{ETC}/theme_plasma_dash_background.png")


def bfb_background_when_dash_opened(density, scale):
	# While the menu is open the Plasma Application Launcher is active; Breeze
	# marks the checked button with a translucent accent highlight (accent
	# border) behind the icon. 9-patch so it scales to the BFB's icon size.
	content = rounded_rect((40, 40), 6, (0x3D, 0xAE, 0xE9, 64), scale,
		outline=(0x3D, 0xAE, 0xE9, 160), outline_width_dp=1)
	w, h = content.size
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (int(round(4 * scale)), w - int(round(4 * scale)))
	save(nine_patch(content, mid, mid, pad, pad),
		dpi_path("theme_plasma_res_launcher_bfb_background_when_dash_opened.9.png", density))


def main():
	for density, scale in DENSITIES.items():
		launcher_background(density, scale)
		dash_background(density, scale)
		dash_search_background(density, scale)
		app_running(density, scale)
		launcher_preferences(density, scale)
		bfb_background_when_dash_opened(density, scale)
	bfb()
	card_logo()
	etc_sources()


if __name__ == "__main__":
	main()
