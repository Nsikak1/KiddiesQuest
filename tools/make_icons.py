#!/usr/bin/env python3
"""Draws the Kid Quest logo (a sword rising out of an open Bible) and writes every launcher icon + in-app logo.
Needs Pillow. Safe to re-run; it overwrites its own output."""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")
FONT = os.path.join(ROOT, "app/src/main/res/font/lilita_one.ttf")
W = 2048  # work canvas (drawn big, scaled down for crisp edges)

NIGHT1, NIGHT2 = (27, 20, 100), (69, 32, 156)
GOLD, GOLDLIP = (255, 201, 60), (217, 143, 0)
PINK, PINKLIP = (255, 79, 139), (185, 42, 96)
STEEL, SHADE, OUTLINE = (230, 243, 255), (185, 214, 242), (127, 166, 201)
LEATHER, CREAM, CREAMLIP = (139, 74, 43), (255, 246, 220), (226, 201, 143)

def S(v): return v * W / 1000.0
def P(pts): return [(S(x), S(y)) for x, y in pts]

def gradient(size):
    img = Image.new("RGB", (size, size)); px = img.load()
    for y in range(size):
        t = y / (size - 1)
        c = tuple(int(NIGHT1[i] + (NIGHT2[i] - NIGHT1[i]) * t) for i in range(3))
        for x in range(size): px[x, y] = c
    return img

def stars_on(img, seed=7, n=40):
    d = ImageDraw.Draw(img, "RGBA"); r = random.Random(seed); s = img.size[0]
    for _ in range(n):
        x, y, rad = r.random() * s, r.random() * s, (0.002 + r.random() * 0.006) * s
        d.ellipse((x - rad, y - rad, x + rad, y + rad), fill=(255, 255, 255, int(40 + r.random() * 90)))

def sparkle(d, cx, cy, r, color):
    k = 0.22
    pts = [(cx, cy - r), (cx + r * k, cy - r * k), (cx + r, cy), (cx + r * k, cy + r * k),
           (cx, cy + r), (cx - r * k, cy + r * k), (cx - r, cy), (cx - r * k, cy - r * k)]
    d.polygon(P(pts), fill=color)

def sword_layer():
    L = Image.new("RGBA", (W, W), (0, 0, 0, 0)); d = ImageDraw.Draw(L)
    blade = [(500, 70), (548, 190), (548, 560), (452, 560), (452, 190)]
    d.polygon(P(blade), fill=STEEL)
    d.line(P(blade + [blade[0]]), fill=OUTLINE, width=int(S(9)), joint="curve")
    d.rectangle([S(493), S(200), S(507), S(550)], fill=SHADE)
    d.rectangle([S(462), S(200), S(476), S(550)], fill=(255, 255, 255, 190))
    d.rounded_rectangle([S(320), S(568), S(680), S(630)], radius=S(31), fill=GOLDLIP)
    d.rounded_rectangle([S(320), S(558), S(680), S(620)], radius=S(31), fill=GOLD)
    d.ellipse([S(484), S(575), S(516), S(607)], fill=PINK)
    d.ellipse([S(491), S(580), S(500), S(589)], fill=(255, 255, 255, 200))
    d.rounded_rectangle([S(472), S(620), S(528), S(780)], radius=S(20), fill=LEATHER)
    for y in (655, 690, 725, 760):
        d.line([S(474), S(y), S(526), S(y)], fill=(105, 55, 30), width=int(S(7)))
    d.ellipse([S(458), S(752), S(542), S(836)], fill=GOLDLIP)
    d.ellipse([S(458), S(746), S(542), S(830)], fill=GOLD)
    return L

def book_layer():
    L = Image.new("RGBA", (W, W), (0, 0, 0, 0)); d = ImageDraw.Draw(L)
    d.polygon(P([(70, 735), (500, 780), (930, 735), (930, 935), (500, 985), (70, 935)]), fill=PINKLIP)
    d.polygon(P([(70, 720), (500, 765), (930, 720), (930, 925), (500, 975), (70, 925)]), fill=PINK)
    left = [(105, 715), (490, 755), (490, 945), (105, 905)]
    right = [(895, 715), (510, 755), (510, 945), (895, 905)]
    d.polygon(P(left), fill=CREAM); d.polygon(P(right), fill=CREAM)
    d.line(P([(105, 905), (490, 945)]), fill=CREAMLIP, width=int(S(8)))
    d.line(P([(895, 905), (510, 945)]), fill=CREAMLIP, width=int(S(8)))
    d.polygon(P([(490, 755), (510, 755), (510, 945), (490, 945)]), fill=(200, 170, 110))
    for i in range(4):
        y0 = 760 + i * 38
        d.line(P([(150, y0), (450, y0 + 300 * 0.104)]), fill=GOLD, width=int(S(9)))
        d.line(P([(550, y0 + 31), (850, y0)]), fill=GOLD, width=int(S(9)))
    return L

def art(glow=True, sparkles=True, tilt=16):
    """The full emblem on a transparent 1000-unit design box."""
    out = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    if glow:
        g = Image.new("RGBA", (W, W), (0, 0, 0, 0)); gd = ImageDraw.Draw(g)
        gd.ellipse([S(170), S(120), S(830), S(780)], fill=(255, 201, 60, 95))
        out = Image.alpha_composite(out, g.filter(ImageFilter.GaussianBlur(S(38))))
    sw = sword_layer().rotate(-tilt, center=(S(500), S(800)), resample=Image.BICUBIC)
    out = Image.alpha_composite(out, sw)
    out = Image.alpha_composite(out, book_layer())
    if sparkles:
        d = ImageDraw.Draw(out)
        for cx, cy, r, c in [(170, 210, 48, GOLD), (835, 300, 40, GOLD), (790, 120, 26, (255, 255, 255)),
                             (215, 430, 30, (255, 255, 255)), (880, 520, 22, GOLD)]:
            sparkle(d, S(cx), S(cy), S(r), c)
    return out

def fit(layer, scale, dy=0):
    """Scale the art about the canvas centre (the art's own centre sits near y=535 of 1000)."""
    s = int(W * scale)
    small = layer.resize((s, s), Image.LANCZOS)
    canvas = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    canvas.paste(small, ((W - s) // 2, (W - s) // 2 + int(S(dy))), small)
    return canvas

def legacy(size, shape):
    bg = gradient(W).convert("RGBA"); stars_on(bg, n=60)
    scale = 0.9 if shape == "square" else 0.8
    img = Image.alpha_composite(bg, fit(art(), scale, dy=-20 * scale))
    mask = Image.new("L", (W, W), 0); md = ImageDraw.Draw(mask)
    if shape == "square": md.rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * 0.2), fill=255)
    else: md.ellipse([0, 0, W - 1, W - 1], fill=255)
    img.putalpha(mask)
    return img.resize((size, size), Image.LANCZOS)

def foreground(size):   # adaptive icons keep important art inside the central 61%
    return fit(art(), 0.60, dy=-30 * 0.60).resize((size, size), Image.LANCZOS)

def monochrome(size):
    a = fit(art(glow=False, sparkles=False), 0.60, dy=-30 * 0.60).getchannel("A")
    img = Image.new("RGBA", (W, W), (255, 255, 255, 0)); img.putalpha(a)
    return img.resize((size, size), Image.LANCZOS)

def background(size):
    bg = gradient(W).convert("RGBA"); stars_on(bg, n=60)
    return bg.convert("RGB").resize((size, size), Image.LANCZOS)

def wordmark():
    font = ImageFont.truetype(FONT, 300)
    text = "Kid Quest"
    img = Image.new("RGBA", (1900, 520), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.text((100, 120 + 22), text, font=font, fill=(0, 0, 0, 110), stroke_width=26, stroke_fill=(0, 0, 0, 110))
    d.text((100, 120), text, font=font, fill=GOLD, stroke_width=26, stroke_fill=(60, 28, 130))
    box = img.getbbox()
    img = img.crop((box[0] - 10, box[1] - 10, box[2] + 10, box[3] + 10))
    h = int(img.size[1] * 900 / img.size[0])
    return img.resize((900, h), Image.LANCZOS)

def save(img, rel):
    path = os.path.join(RES, rel); os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)

DENS = {"mdpi": (48, 108), "hdpi": (72, 162), "xhdpi": (96, 216), "xxhdpi": (144, 324), "xxxhdpi": (192, 432)}
for name, (icon, fg) in DENS.items():
    save(legacy(icon, "square"), f"mipmap-{name}/ic_launcher.png")
    save(legacy(icon, "circle"), f"mipmap-{name}/ic_launcher_round.png")
    save(foreground(fg), f"mipmap-{name}/ic_launcher_foreground.png")
    save(monochrome(fg), f"mipmap-{name}/ic_launcher_monochrome.png")
save(background(432), "drawable-nodpi/ic_launcher_background.png")
save(fit(art(), 0.98).resize((512, 512), Image.LANCZOS), "drawable-nodpi/logo_emblem.png")
save(wordmark(), "drawable-nodpi/logo_wordmark.png")

adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
"""
for n in ("ic_launcher.xml", "ic_launcher_round.xml"):
    p = os.path.join(RES, "mipmap-anydpi-v26", n); os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w").write(adaptive)

store = os.path.join(ROOT, "store"); os.makedirs(store, exist_ok=True)
legacy(512, "square").save(os.path.join(store, "icon-512.png"))
print("icons + logos written")
