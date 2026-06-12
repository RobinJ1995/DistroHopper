#!/usr/bin/env python3
"""Generate the elementary (Pantheon) theme drawables from values measured off
the baseline screenshots in `etc/theme baselines/pantheon/` (elementary OS 7+).

Colours and opacities were recovered from the white/black wallpaper
screenshot pairs (alpha = 1 - (C_white - C_black) / 255, colour = C_black / alpha):

  dock                #FAFAFA at 48% opacity, ~6dp corner radius, floats 8dp
                      off the screen edge
  search field fill   #FFFFFF, fully opaque, 1px #C8C8C8 border, ~4dp radius
  search border       focused state does not appear in the baselines;
                      approximated with the elementary accent blue #3689E6
  search glyph/hint   #666666, magnifier in the left cap
  running indicator   small dark dot below the icon (Plank tints it per app,
                      which is not expressible; approximated as #3A3A3A)

The BFB (app grid), preferences cog and trash icons are kept as-is.
The popover's pointer arrow and the dock's per-app icon tinting are not
expressible with the theme engine and are omitted.

Run from the repository root:  python3 etc/generate_theme_elementary_assets.py
Requires Pillow.
"""

import os
from PIL import Image, ImageDraw

RES = "app/src/main/res"
ETC = "etc"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
SS = 8  # supersampling factor for crisp antialiased shapes

DOCK = (0xFA, 0xFA, 0xFA, 122)
FIELD = (0xFF, 0xFF, 0xFF, 255)
FIELD_BORDER = (0xC8, 0xC8, 0xC8, 255)
FIELD_BORDER_FOCUSED = (0x36, 0x89, 0xE6, 255)
GLYPH = (0x66, 0x66, 0x66, 255)
RUNNING = (0x3A, 0x3A, 0x3A, 255)
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
	# Plank-style dock: translucent light surface, rounded on all corners (it
	# floats off the screen edge via launcher_margin), same image for every
	# dockable edge.
	content = rounded_rect((32, 32), 6, DOCK, scale)
	w = content.size[0]
	mid = (w // 2 - 1, w // 2 + 1)
	pad = (int(round(4 * scale)), w - int(round(4 * scale)))
	patch = nine_patch(content, mid, mid, pad, pad)
	for edge in ("top", "right", "bottom", "left"):
		save(patch, dpi_path(f"theme_elementary_res_launcher_background_{edge}.9.png", density))


# The dash background (the applications popover with its baked shadow) is NOT
# generated: drawable/theme_elementary_res_dash_background.9.png is the
# original hand-made asset, kept verbatim.


def panel_bfb(density, scale):
	# The panel's Applications button shows a white magnifier next to the
	# label, as in the baseline screenshots.
	size_dp = 18
	s = int(round(size_dp * scale))
	img = Image.new("RGBA", (s * SS, s * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	magnifier(draw, s * SS / 2, s * SS / 2, size_dp * scale * SS, (255, 255, 255, 255))
	save(img.resize((s, s), Image.LANCZOS),
		dpi_path("theme_elementary_res_panel_bfb.png", density))


def dash_search_background(density, scale):
	# elementary's bordered search entry, magnifier in the fixed left cap. A
	# transparent inset (wider horizontally, to keep the field clear of the
	# popover's edges) provides spacing; the vertical stretch zones live in
	# that inset so the border and the magnifier never distort.
	for name, border in (("default", FIELD_BORDER), ("focused", FIELD_BORDER_FOCUSED)):
		inset_x_dp, inset_y_dp, field_h_dp, r_dp = 12, 4, 32, 4
		w_dp = 104
		h_dp = field_h_dp + 2 * inset_y_dp
		w, h = int(round(w_dp * scale)), int(round(h_dp * scale))
		img = Image.new("RGBA", (w * SS, h * SS), CLEAR)
		draw = ImageDraw.Draw(img)
		ix, iy = inset_x_dp * scale * SS, inset_y_dp * scale * SS
		draw.rounded_rectangle((ix, iy, w * SS - 1 - ix, h * SS - 1 - iy),
			radius=r_dp * scale * SS, fill=FIELD, outline=border,
			width=int(round(1.5 * scale * SS)))
		magnifier(draw, (inset_x_dp + 16) * scale * SS, (h_dp / 2) * scale * SS,
			15 * scale * SS, GLYPH)
		content = img.resize((w, h), Image.LANCZOS)
		sx = (int(round(48 * scale)), int(round(56 * scale)))  # right of the magnifier
		sy = [(0, int(round(3 * scale))), (h - int(round(3 * scale)), h)]
		pad_x = (int(round(42 * scale)), w - int(round(20 * scale)))  # text right of icon
		pad_y = (int(round(12 * scale)), h - int(round(12 * scale)))
		save(nine_patch(content, sx, sy, pad_x, pad_y),
			dpi_path(f"theme_elementary_res_dash_search_background_{name}.9.png", density))


def app_running(density, scale):
	# Plank marks running apps with a small dot below the icon. The engine
	# draws this in a narrow strip beside the icon instead.
	size_dp, d_dp = 6, 5
	s = int(round(size_dp * scale))
	img = Image.new("RGBA", (s * SS, s * SS), CLEAR)
	draw = ImageDraw.Draw(img)
	c, r = size_dp * scale * SS / 2, d_dp * scale * SS / 2
	draw.ellipse((c - r, c - r, c + r, c + r), fill=RUNNING)
	save(img.resize((s, s), Image.LANCZOS),
		dpi_path("theme_elementary_res_launcher_app_running.png", density))


def etc_sources():
	"""Flat (non-9-patch) renders kept in etc/ alongside the other theme sources."""
	save(rounded_rect((160, 56), 6, DOCK, 1.0), f"{ETC}/theme_elementary_launcher_background.png")


def main():
	for density, scale in DENSITIES.items():
		launcher_background(density, scale)
		dash_search_background(density, scale)
		app_running(density, scale)
		panel_bfb(density, scale)
	etc_sources()


if __name__ == "__main__":
	main()
