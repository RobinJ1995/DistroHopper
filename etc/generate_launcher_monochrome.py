#!/usr/bin/env python3
"""Generate the monochrome (Android 13+ themed icon) layer of DistroHopper's own
launcher icon: `app/src/main/res/drawable/ic_launcher_monochrome.xml`.

The colour icon stays exactly as it is — this only adds the stencil a launcher
recolours when "Themed icons" is on. A monochrome layer is an alpha stencil:
`icons/IconRenderer.drawTinted` fills the canvas with the tint background and
draws this layer tinted with the tint foreground, so the mark has to survive
being reduced to two tones. The three signature elements all have to stay
readable:

  swirl blades   opaque        gaps between blades   transparent
  eye ovals      opaque        pupils                transparent (punched out)
  beak           opaque        halo around the face  transparent

Alpha is not limited to those two extremes, which is what lets a blade keep the
soft trailing edge it has in the colour icon. `setTint` is a SRC_IN filter, and
`VectorDrawable` rasterises the whole vector — partial alpha included — before
applying it, so for an opaque tint colour C over a rasterised pixel D:

  result.rgb = C.rgb                  the tint replaces the colour
  result.a   = C.a * D.a = D.a        per-pixel alpha passes through untouched

A pixel at alpha `a` therefore lands at lerp(tintBackground, tintForeground, a).
The corollary: only **alpha** is worth varying, never colour, because the tint
overwrites the colour regardless.

Everything below is measured off the real artwork rather than redrawn by eye —
`drawable-xxxhdpi/ic_launcher_background.png` for the swirl and
`drawable-xxxhdpi/ic_launcher_foreground.png` for the face, both 432px = 108
viewport units, so pixel/4 is a viewport unit.

Swirl, from sampling the background on rings and counting the orange runs:

  ring r      20     26     32     36     44     50
  runs        12     12     13*    13*    13*    12      (* one blade split by
  width     19.6   21.4   20.1   21.4   23.7   26.1        antialiasing noise)

12 blades on a 30 degree pitch. Tracking one blade outward in 0.5-unit radial
steps (small enough that a step can never be confused with a jump to the next
blade) its midpoint advances +53 degrees from r=18 to r=50, which fits a
*logarithmic* spiral, theta = PHASE + TWIST * ln(r / PHASE_R), at 52 degrees
per ln unit. Logarithmic matters: it front-loads the twist, so a blade turns
most of a 30 degree pitch across the visible band and reads as a swirl rather
than as spokes.

A blade is not a flat stripe. Profiling one pitch angularly (0 = white,
150 = full orange at r=36) shows a sawtooth, not a plateau:

  offset from core centre  -16   -12    -8    -4     0    +4    +7   +10
  orangeness                 8    37    82   124   141   142   141     9

so each blade has a *hard* edge on one side, roughly 14 degrees of full
strength, and then a ~12 degree ramp trailing off the other side before the
next blade begins. That asymmetry is what reads as spin; a blade with two hard
edges is a stripe, and twelve of them are circus stripes. Both the core and the
tail widen with radius, as the source's do.

Face, from connected components of the foreground (light = eye, dark = pupil,
yellow = beak):

  left eye    bbox (31.25, 35.00)-(52.50, 62.25)
  right eye   bbox (54.25, 40.00)-(76.50, 59.50)   smaller, lower, tilted
  left pupil  bbox (46.25, 45.25)-(52.75, 54.50)
  right pupil bbox (53.25, 45.50)-(59.25, 54.50)
  beak        bbox (43.25, 56.25)-(64.00, 73.00), widest at y=61, apex at y=73

How much of this layer is ever seen is worth being precise about, because it
drives FACE_SCALE. `IconRenderer.drawLayer` insets by
`AdaptiveIconDrawable.getExtraInsetFraction()` (1/4) on every side, so the 108
viewport is drawn 1.5x oversized and only viewport 18..90 — the 72dp safe
square — lands on the icon. A circular mask then keeps just r <= 36 about the
centre.

Two deliberate departures from those measurements, both forced by the stencil:

  * The face is scaled by FACE_SCALE about the icon centre. At full size it
    reaches r=24.9 of that 36, which would leave no room for a swirl at all.
  * The pupils are pulled back off the eyes' inner edges and the eyes pushed
    very slightly apart. In the colour icon the pupils sit hard against the
    edge and the eyes nearly touch, which works when the parts differ in
    colour; as flat alpha the pupils would break out of the eyes and the eyes
    would weld together. They stay well past halfway inward, so the cross-eyed
    look survives.

`--report` prints the resulting clearances (they are what keeps the mark
legible when it is drawn small), `--preview DIR` renders the tinted result the
way `IconRenderer` does. The generated XML needs nothing but the standard
library; `--preview` needs Pillow.

Run from the repository root:  python3 etc/generate_launcher_monochrome.py
"""

import math
import os
import sys

RES = "app/src/main/res"
OUT = f"{RES}/drawable/ic_launcher_monochrome.xml"

VIEWPORT = 108.0
CX = CY = 54.0

# --- swirl ---------------------------------------------------------------- #
BLADES = 12
PITCH = 360.0 / BLADES
PHASE = 6.8          # centre of a blade's full-alpha core at PHASE_R, measured //
PHASE_R = 36.0
TWIST = 52.0         # degrees of centreline rotation per ln(r), measured //
CORE = 6.0           # full-alpha half-width of a blade, degrees //
TAIL = 11.0          # soft trailing ramp beyond the core, degrees //
WIDEN = 1.35         # how much both grow between the halo and R_OUT //
BANDS = 9            # steps the tail's ramp is drawn in //
R_OUT = 78.0         # past the canvas corner (76.37), so the swirl bleeds out //
SAMPLES = 8          # polyline samples per wedge edge, uniform in ln(r) //
HALO = 2.4           # transparent gap between the face and the blades //

# --- face ----------------------------------------------------------------- #
# Ellipses are (cx, cy, rx, ry, rotation); the beak is a hand-fitted outline.  #
FACE_SCALE = 0.78
LEFT_EYE = (41.6, 48.6, 10.2, 13.4, 0.0)
RIGHT_EYE = (65.8, 49.8, 10.0, 8.9, -20.0)
LEFT_PUPIL = (46.4, 50.2, 3.0, 4.3, 0.0)
RIGHT_PUPIL = (61.1, 51.5, 2.9, 4.2, -20.0)
# Left corner, top-edge control, right corner, right control, apex, left       #
# control — a shield tapering to a point, sitting under the eyes' inner ends.  #
BEAK = {
	"left": (46.8, 63.8),
	"top": (56.4, 53.2),
	"right": (61.8, 63.2),
	"right_ctrl": (61.4, 70.2),
	"apex": (54.6, 73.6),
	"left_ctrl": (47.2, 70.0),
}


def num(v):
	"""Format a coordinate compactly (0.05 viewport units is well under a pixel)."""
	s = f"{v:.2f}".rstrip("0").rstrip(".")
	return "0" if s in ("-0", "") else s


def pt(p):
	return f"{num(p[0])},{num(p[1])}"


def scaled(p):
	"""Scale a face point about the icon centre."""
	return (CX + (p[0] - CX) * FACE_SCALE, CY + (p[1] - CY) * FACE_SCALE)


def scaled_ellipse(e):
	cx, cy, rx, ry, rot = e
	sx, sy = scaled((cx, cy))
	return (sx, sy, rx * FACE_SCALE, ry * FACE_SCALE, rot)


# --- ellipses -------------------------------------------------------------- #
KAPPA = 0.5522847498307936


def ellipse_point(e, t):
	"""Point at parameter t (radians) on a rotated ellipse."""
	cx, cy, rx, ry, rot = e
	a = math.radians(rot)
	x, y = rx * math.cos(t), ry * math.sin(t)
	return (cx + x * math.cos(a) - y * math.sin(a), cy + x * math.sin(a) + y * math.cos(a))


def ellipse_tangent(e, t):
	"""Derivative at parameter t, used to place the Bezier control points."""
	cx, cy, rx, ry, rot = e
	a = math.radians(rot)
	dx, dy = -rx * math.sin(t), ry * math.cos(t)
	return (dx * math.cos(a) - dy * math.sin(a), dx * math.sin(a) + dy * math.cos(a))


def ellipse_path(e):
	"""A rotated ellipse as four cubic Beziers, with the rotation baked into the
	control points — no <group> transform, so the drawable stays a flat list of
	paths."""
	quarter = math.pi / 2
	start = ellipse_point(e, 0.0)
	out = [f"M{pt(start)}"]

	for i in range(4):
		t0, t1 = i * quarter, (i + 1) * quarter
		p0, p1 = ellipse_point(e, t0), ellipse_point(e, t1)
		d0, d1 = ellipse_tangent(e, t0), ellipse_tangent(e, t1)
		c0 = (p0[0] + d0[0] * KAPPA, p0[1] + d0[1] * KAPPA)
		c1 = (p1[0] - d1[0] * KAPPA, p1[1] - d1[1] * KAPPA)
		out.append(f"C{pt(c0)} {pt(c1)} {pt(p1)}")

	out.append("Z")
	return "".join(out)


def ellipse_polygon(e, steps=96):
	return [ellipse_point(e, 2 * math.pi * i / steps) for i in range(steps)]


# --- beak ------------------------------------------------------------------ #
def beak_path():
	b = {k: scaled(v) for k, v in BEAK.items()}
	return (f"M{pt(b['left'])}"
		f"Q{pt(b['top'])} {pt(b['right'])}"
		f"Q{pt(b['right_ctrl'])} {pt(b['apex'])}"
		f"Q{pt(b['left_ctrl'])} {pt(b['left'])}"
		"Z")


def quad(p0, c, p1, steps):
	for i in range(1, steps + 1):
		t = i / steps
		u = 1 - t
		yield (u * u * p0[0] + 2 * u * t * c[0] + t * t * p1[0],
			u * u * p0[1] + 2 * u * t * c[1] + t * t * p1[1])


def beak_polygon(steps=28):
	b = {k: scaled(v) for k, v in BEAK.items()}
	poly = [b["left"]]
	poly += list(quad(b["left"], b["top"], b["right"], steps))
	poly += list(quad(b["right"], b["right_ctrl"], b["apex"], steps))
	poly += list(quad(b["apex"], b["left_ctrl"], b["left"], steps))
	return poly


# --- face extent and halo --------------------------------------------------- #
def face_polygons():
	"""Every face outline, as polygons in final (scaled) coordinates."""
	return {
		"left_eye": ellipse_polygon(scaled_ellipse(LEFT_EYE)),
		"right_eye": ellipse_polygon(scaled_ellipse(RIGHT_EYE)),
		"left_pupil": ellipse_polygon(scaled_ellipse(LEFT_PUPIL)),
		"right_pupil": ellipse_polygon(scaled_ellipse(RIGHT_PUPIL)),
		"beak": beak_polygon(),
	}


def face_radius():
	"""How far the face reaches from the icon centre."""
	f = face_polygons()
	return max(math.hypot(x - CX, y - CY)
		for key in ("left_eye", "right_eye", "beak") for x, y in f[key])


def halo_radius():
	"""Where the blades start. A circle rather than an outline-hugging dilation:
	the face is wider than it is tall, so a shape-following clear zone starts each
	blade at a different radius and the tips read as ragged. A round clearing
	costs a little swirl area above and below the face and looks deliberate."""
	return face_radius() + HALO


# --- swirl ----------------------------------------------------------------- #
def blade_angle(index, r):
	return PHASE + index * PITCH + TWIST * math.log(r / PHASE_R)


def blade_widening(r, r_start):
	"""Blades widen outward, as they do in the source art."""
	f = math.log(r / r_start) / math.log(R_OUT / r_start)
	return 1.0 + (WIDEN - 1.0) * f


def blade_polygon(index, lo, hi):
	"""One wedge of a blade, between two signed angular offsets from its
	centreline. `hi` is the hard edge; `lo` runs off into the soft tail."""
	r_start = halo_radius()
	radii = [math.exp(math.log(r_start) + (math.log(R_OUT) - math.log(r_start)) * i / SAMPLES)
		for i in range(SAMPLES + 1)]

	def edge(offset):
		out = []
		for r in radii:
			t = math.radians(blade_angle(index, r) + offset * blade_widening(r, r_start))
			out.append((CX + r * math.cos(t), CY + r * math.sin(t)))
		return out

	return edge(hi) + list(reversed(edge(lo)))


def tail_bands():
	"""The tail's ramp as a stack of nested wedges, widest and faintest first.

	Each wedge shares the blade's hard edge and reaches further into the tail, so
	a point deep in the tail is painted by one wedge and a point at the core by
	all of them. Returns (lo, hi, fillAlpha) per wedge, where fillAlpha is solved
	so that the *composited* result matches the intended ramp: for a strip
	covered by wedges k..BANDS, 1 - prod(1 - alpha_i) has to come out at the
	target, which gives alpha_k = 1 - (1 - T_k) / (1 - T_k+1)."""
	target = [1.0] + [1.0 - (k - 0.5) / BANDS for k in range(1, BANDS + 1)]

	out = []
	for k in range(BANDS, -1, -1):
		above = target[k + 1] if k + 1 < len(target) else 0.0
		alpha = 1.0 - (1.0 - target[k]) / (1.0 - above)
		out.append((-(CORE + TAIL * k / BANDS), CORE, alpha))

	return out


def polygon_path(poly):
	return f"M{pt(poly[0])}" + "".join(f"L{pt(p)}" for p in poly[1:]) + "Z"


# --- emit ------------------------------------------------------------------ #
def build():
	bands = []
	for lo, hi, alpha in tail_bands():
		wedges = "\n            ".join(
			polygon_path(blade_polygon(i, lo, hi)) for i in range(BLADES))
		bands.append(f"""    <path
        android:fillColor="#FFFFFF"
        android:fillAlpha="{num(alpha)}"
        android:pathData="{wedges}" />""")
	swirl = "\n\n".join(bands)

	eyes = "\n            ".join([
		ellipse_path(scaled_ellipse(LEFT_EYE)),
		ellipse_path(scaled_ellipse(RIGHT_EYE)),
		ellipse_path(scaled_ellipse(LEFT_PUPIL)),
		ellipse_path(scaled_ellipse(RIGHT_PUPIL)),
	])

	return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by etc/generate_launcher_monochrome.py — do not edit by hand. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- The swirl: {BLADES} logarithmic-spiral blades, standing in for the orange
         wedges of the colour icon's background, running from the halo around the
         face out past the canvas corner so they bleed to every edge.

         Each blade is a sawtooth, the way the source art's are: a hard edge on
         one side, a full-alpha core, then a soft ramp trailing off the other
         side. That trailing fade is what reads as spin — a blade with two hard
         edges is a stripe, not a swirl. The ramp is drawn as {BANDS} nested wedges
         sharing the hard edge, each fainter and reaching further into the tail,
         because a gradient cannot follow a spiral: linear and radial fills are
         straight, and a sweep fill is fixed in angle while the blade twists a
         full pitch on its way out. Alpha survives the SRC_IN tint untouched, so
         the ramp arrives intact. -->
{swirl}

    <!-- The eyes, with the pupils punched out as holes (evenOdd, so the hole
         subpaths need no reversed winding). -->
    <path
        android:fillColor="#FFFFFF"
        android:fillType="evenOdd"
        android:pathData="{eyes}" />

    <!-- The beak. -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="{beak_path()}" />

</vector>
"""


# --- clearance report ------------------------------------------------------ #
def poly_distance(p, poly):
	"""Distance from a point to a closed polygon's outline (negative inside)."""
	best = min(math.hypot(p[0] - q[0], p[1] - q[1]) for q in poly)
	inside = False
	n = len(poly)
	for i in range(n):
		x0, y0 = poly[i]
		x1, y1 = poly[(i + 1) % n]
		if (y0 > p[1]) != (y1 > p[1]):
			if p[0] < x0 + (p[1] - y0) / (y1 - y0) * (x1 - x0):
				inside = not inside
	return -best if inside else best


def gap(poly_a, poly_b):
	"""Clear space between two shapes that do not overlap."""
	return min(poly_distance(p, poly_b) for p in poly_a)


def rim(hole, shell):
	"""Solid margin left around a hole; negative means the hole breaks out."""
	return min(-poly_distance(p, shell) for p in hole)


def report():
	f = face_polygons()
	rmax = face_radius()
	start = halo_radius()

	lines = [
		f"face scale                    {FACE_SCALE}",
		f"face outer radius             {rmax:.2f}   (visible under a circle mask: 36)",
		f"blade start radius            {start:.2f}",
		f"swirl band                    {36 - start:.2f}  of the 36 visible radius",
		f"eye-to-eye seam               {gap(f['left_eye'], f['right_eye']):.2f}",
		f"left pupil rim                {rim(f['left_pupil'], f['left_eye']):.2f}",
		f"right pupil rim               {rim(f['right_pupil'], f['right_eye']):.2f}",
		f"pupil-to-pupil gap            {gap(f['left_pupil'], f['right_pupil']):.2f}",
		f"beak to left eye              {gap(f['beak'], f['left_eye']):.2f}",
		f"beak to right eye             {gap(f['beak'], f['right_eye']):.2f}",
	]

	bands = tail_bands()
	lines.append("band alphas (widest first)    "
		+ " ".join(f"{a:.2f}" for _, _, a in bands))
	lines.append(f"mean opacity over a pitch     {(2 * CORE + TAIL / 2) / PITCH:.0%}")

	for r in (26.0, 30.0, 36.0):
		grow = blade_widening(r, start)
		core = 2 * math.radians(CORE * grow) * r
		tail = math.radians(TAIL * grow) * r
		clear = math.radians(PITCH) * r - core - tail
		lines.append(f"at r={r:<4.0f} core {core:5.2f} wide, tail {tail:5.2f}, clear {clear:5.2f}")

	print("\n".join(lines))


# --- preview --------------------------------------------------------------- #
def tone(rgb, saturation_factor, value):
	"""IconConfig.tone(): keep the hue, scale the saturation, force the value."""
	import colorsys
	h, s, _ = colorsys.rgb_to_hsv(*[c / 255.0 for c in rgb])
	s = min(1.0, s * saturation_factor)
	return tuple(int(round(c * 255)) for c in colorsys.hsv_to_rgb(h, s, value))


def tint_pair(rgb, night):
	"""IconConfig.tintBackground / tintForeground."""
	background = tone(rgb, 0.45 if night else 0.18, 0.30 if night else 0.95)
	foreground = tone(rgb, 0.85 if night else 0.95, 0.92 if night else 0.42)
	return background, foreground


def preview(directory):
	from PIL import Image, ImageChops, ImageDraw

	bands = [(alpha, [blade_polygon(i, lo, hi) for i in range(BLADES)])
		for lo, hi, alpha in tail_bands()]
	f = face_polygons()
	face = [f["left_eye"], f["right_eye"], f["beak"]]
	holes = [f["left_pupil"], f["right_pupil"]]

	def stencil(size, ss=4):
		"""The monochrome layer alpha, mapped the way IconRenderer.drawLayer does:
		a 1/4 extra inset each side, so viewport 18..90 fills the icon. The bands
		are composited source-over in file order, exactly as the drawable's paths
		are, so the tail's stepped ramp comes out at its intended strength."""
		n = size * ss
		inset = n // 4
		scale = 1.5 * n / VIEWPORT

		def to_px(poly):
			return [(x * scale - inset, y * scale - inset) for x, y in poly]

		img = Image.new("L", (n, n), 0)
		for alpha, wedges in bands:
			layer = Image.new("L", (n, n), 0)
			ld = ImageDraw.Draw(layer)
			for poly in wedges:
				ld.polygon(to_px(poly), fill=int(round(255 * alpha)))
			# Source-over on the alpha channel is a screen blend: //
			# out = dst + src * (1 - dst). //
			img = ImageChops.screen(img, layer)

		# The face is flat opaque, and the pupils punch through it. //
		draw = ImageDraw.Draw(img)
		for poly in face:
			draw.polygon(to_px(poly), fill=255)
		for poly in holes:
			draw.polygon(to_px(poly), fill=0)

		return img.resize((size, size), Image.LANCZOS)

	def mask(size, shape, ss=4):
		n = size * ss
		img = Image.new("L", (n, n), 0)
		draw = ImageDraw.Draw(img)
		if shape == "circle":
			draw.ellipse((0, 0, n - 1, n - 1), fill=255)
		else:
			draw.rounded_rectangle((0, 0, n - 1, n - 1), radius=n * 0.24, fill=255)
		return img.resize((size, size), Image.LANCZOS)

	def tinted(size, rgb, night, shape):
		background, foreground = tint_pair(rgb, night)
		img = Image.new("RGBA", (size, size), background + (255,))
		img.paste(Image.new("RGBA", (size, size), foreground + (255,)), (0, 0), stencil(size))
		img.putalpha(mask(size, shape))
		return img

	os.makedirs(directory, exist_ok=True)

	# 1. The bare stencil, with the mask/halo guides drawn over it. //
	size = 640
	guide = Image.new("RGBA", (size, size), (255, 255, 255, 255))
	guide.paste(Image.new("RGBA", (size, size), (0, 0, 0, 255)), (0, 0), stencil(size))
	d = ImageDraw.Draw(guide)
	scale = size / 72.0  # viewport 18..90 maps onto the image //

	def ring(r, colour):
		d.ellipse(((CX - r - 18) * scale, (CY - r - 18) * scale,
			(CX + r - 18) * scale, (CY + r - 18) * scale), outline=colour, width=3)

	ring(36, (220, 40, 40, 255))
	ring(halo_radius(), (40, 120, 220, 255))
	guide.save(f"{directory}/mono_stencil.png")

	# 2. A contact sheet: the tint presets, light and dark, both mask shapes. //
	tints = [("blue", (0x42, 0x85, 0xF4)), ("orange", (0xFF, 0x70, 0x43)),
		("green", (0x34, 0xA8, 0x53)), ("purple", (0x93, 0x34, 0xE6)),
		("grey", (0x80, 0x80, 0x80))]
	cell, pad = 200, 16
	rows = [("light", False, "circle"), ("dark", True, "circle"),
		("light", False, "squircle"), ("dark", True, "squircle")]
	sheet = Image.new("RGBA", (len(tints) * (cell + pad) + pad, len(rows) * (cell + pad) + pad),
		(128, 128, 128, 255))
	for r, (_, night, shape) in enumerate(rows):
		for c, (_, rgb) in enumerate(tints):
			sheet.alpha_composite(tinted(cell, rgb, night, shape),
				(pad + c * (cell + pad), pad + r * (cell + pad)))
	sheet.save(f"{directory}/mono_tints.png")

	# 3. Small sizes, upscaled nearest-neighbour — where legibility breaks. //
	small = [48, 72, 96, 144]
	strip = Image.new("RGBA", (sum(s for s in small) * 3 + pad * (len(small) + 1), 144 * 3 + 2 * pad),
		(128, 128, 128, 255))
	x = pad
	for s in small:
		img = tinted(s, (0x42, 0x85, 0xF4), False, "circle").resize((s * 3, s * 3), Image.NEAREST)
		strip.alpha_composite(img, (x, pad))
		x += s * 3 + pad
	strip.save(f"{directory}/mono_small.png")

	# 4. The bare swirl against the real background art, framed identically, to
	#    confirm the twist runs the same way round. //
	source = Image.open(f"{RES}/drawable-xxxhdpi/ic_launcher_background.png").convert("RGBA")
	source = source.resize((size, size), Image.LANCZOS)

	full = size / VIEWPORT  # the whole 108 canvas, matching the source PNG //
	coverage = Image.new("L", (size, size), 0)
	for alpha, wedges in bands:
		layer = Image.new("L", (size, size), 0)
		ld = ImageDraw.Draw(layer)
		for poly in wedges:
			ld.polygon([(x * full, y * full) for x, y in poly], fill=int(round(255 * alpha)))
		coverage = ImageChops.screen(coverage, layer)

	bare = Image.new("RGBA", (size, size), (255, 255, 255, 255))
	bare.paste(Image.new("RGBA", (size, size), (230, 130, 60, 255)), (0, 0), coverage)

	side = Image.new("RGBA", (size * 2 + pad * 3, size + pad * 2), (128, 128, 128, 255))
	side.alpha_composite(source, (pad, pad))
	side.alpha_composite(bare, (size + pad * 2, pad))
	side.save(f"{directory}/mono_vs_source.png")

	print(f"previews written to {directory}")


def main():
	args = sys.argv[1:]

	if "--report" in args:
		report()
		return

	if "--preview" in args:
		preview(args[args.index("--preview") + 1])
		return

	os.makedirs(os.path.dirname(OUT), exist_ok=True)
	with open(OUT, "w") as handle:
		handle.write(build())
	print(f"wrote {OUT}")


if __name__ == "__main__":
	main()
