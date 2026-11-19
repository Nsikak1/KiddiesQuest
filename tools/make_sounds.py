#!/usr/bin/env python3
"""Synthesises every sound effect (no copyrighted audio). Needs numpy."""
import os, wave
import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/res/raw"); os.makedirs(OUT, exist_ok=True)
SR = 22050
rng = np.random.default_rng(3)

def save(name, x):
    d = (np.clip(x, -1, 1) * 32767 * 0.8).astype("<i2")
    with wave.open(f"{OUT}/{name}.wav", "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(d.tobytes())

def T(d): return np.arange(int(SR * d)) / SR
def fade(x, a=0.01, r=0.05):
    y = x.copy(); na, nr = int(SR * a), int(SR * r)
    y[:na] *= np.linspace(0, 1, na); y[-nr:] *= np.linspace(1, 0, nr); return y
def sine(f, d, decay=0.0): t = T(d); return np.sin(2 * np.pi * f * t) * np.exp(-decay * t)
def ring(t, start, partials, decay=6): return sum(np.sin(2 * np.pi * fr * t) * np.exp(-decay * np.clip(t - start, 0, None)) * (t >= start) * a for fr, a in partials)

# sword drawn (logo + start of round)
t = T(0.9); f = 600 + 2200 * np.clip(t / 0.35, 0, 1)
chirp = np.sin(2 * np.pi * np.cumsum(f) / SR) * (t < 0.35) * 0.5
noise = rng.normal(0, 1, len(t)) * 0.12 * (t < 0.35) * np.linspace(0.3, 1, len(t))
save("sfx_draw", fade(chirp + noise + ring(t, 0.33, [(2400, .3), (3600, .2), (5100, .12)]), 0.02, 0.2))
save("sfx_tick", fade(sine(880, 0.07), 0.003, 0.03))
save("sfx_go", fade(np.concatenate([sine(660, 0.12), sine(990, 0.22, 3)]), 0.005, 0.06))
arp = np.concatenate([sine(f, 0.14, 2) for f in (523.25, 659.25, 783.99)] + [sine(1046.5, 0.45, 2.5)])
save("sfx_win", fade(arp, 0.005, 0.1))
t = T(0.35); save("sfx_wrong", fade(np.sign(np.sin(2 * np.pi * (190 - 60 * t / 0.35) * t)) * 0.35, 0.005, 0.1))
save("sfx_star", fade(np.concatenate([sine(1320, 0.08, 5), sine(1760, 0.2, 6)]), 0.003, 0.08))

# NEW: correct answer = quick metallic sword slash + bright ring
t = T(0.55)
burst = rng.normal(0, 1, len(t)) * np.exp(-30 * t) * 0.5
sweep = np.sin(2 * np.pi * np.cumsum(1500 + 3500 * np.clip(t / 0.08, 0, 1)) / SR) * (t < 0.08) * 0.4
save("sfx_slash", fade(burst + sweep + ring(t, 0.07, [(2600, .35), (3900, .22), (5600, .12)], decay=7), 0.002, 0.15))

# NEW: wrong answer = dull wooden clunk (low thump + a little noise)
t = T(0.3)
thump = np.sin(2 * np.pi * (140 - 70 * t / 0.3) * t) * np.exp(-14 * t) * 0.9
knock = rng.normal(0, 1, len(t)) * np.exp(-60 * t) * 0.25
save("sfx_clunk", fade(thump + knock, 0.002, 0.08))

# NEW: new record fanfare
notes = [(523.25, .12), (659.25, .12), (783.99, .12), (1046.5, .12), (783.99, .1), (1046.5, .1), (1318.5, .7)]
save("sfx_record", fade(np.concatenate([sine(f, d, 1.2) + 0.4 * sine(f * 2, d, 2.5) for f, d in notes]), 0.005, 0.2))
print("sounds:", sorted(os.listdir(OUT)))
