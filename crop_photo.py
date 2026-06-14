#!/usr/bin/env python3
"""
Crop a selfie photo to a face/shoulders square for use as the About screen
developer photo. The person's face should be in the center-left of the frame.

Usage:
    python3 crop_photo.py /path/to/photo.jpg
"""

import sys
from PIL import Image

if len(sys.argv) != 2:
    print("Usage: python3 crop_photo.py /path/to/photo.jpg")
    sys.exit(1)

source = sys.argv[1]
dest = "app/src/main/res/drawable/robinj.jpg"

img = Image.open(source)
w, h = img.size
print(f"Source image: {w}x{h}")

# Face is center-left: skip sky (top ~30%), skip arm (bottom ~28%), take left ~55%
left   = int(w * 0.00)
top    = int(h * 0.30)
right  = int(w * 0.55)
bottom = int(h * 0.72)

crop = img.crop((left, top, right, bottom))

# Square-centre the crop
cw, ch = crop.size
side = min(cw, ch)
x = (cw - side) // 2
y = (ch - side) // 2
square = crop.crop((x, y, x + side, y + side))

out = square.resize((512, 512), Image.LANCZOS)
out.save(dest, "JPEG", quality=92)
print(f"Saved cropped photo to {dest} ({out.size[0]}x{out.size[1]})")
print("If the face is off-centre, adjust the top/bottom/right percentages and re-run.")
