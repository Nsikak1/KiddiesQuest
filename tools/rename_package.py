#!/usr/bin/env python3
"""One-off: rename the template's package/app id to ng.name.nsikak.kidquest and fix every internal name."""
import os, re, shutil, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OLD_PKG, NEW_PKG = "ng.name.version5", "ng.name.nsikak.kidquest"
TEXT_EXT = {".kt", ".kts", ".xml", ".md", ".properties", ".toml", ".name", ".json", ".txt", ".pro"}
SKIP_DIRS = {".git", ".gradle", "build", ".artifacts"}
REPLACEMENTS = [  # order matters: most specific first
    (OLD_PKG, NEW_PKG),
    ("Theme.Version5", "Theme.KidQuest"),
    ("Version5Theme", "KidQuestTheme"),
    ('rootProject.name = "Version 5"', 'rootProject.name = "KidQuest"'),
]

def walk():
    for d, dirs, files in os.walk(ROOT):
        dirs[:] = [x for x in dirs if x not in SKIP_DIRS]
        for f in files:
            yield os.path.join(d, f)

# 1. move source folders  ng/name/version5 -> ng/name/nsikak/kidquest
for src_root in ("main", "test", "androidTest"):
    old = os.path.join(ROOT, "app/src", src_root, "java/ng/name/version5")
    new = os.path.join(ROOT, "app/src", src_root, "java/ng/name/nsikak/kidquest")
    if os.path.isdir(old):
        os.makedirs(os.path.dirname(new), exist_ok=True)
        shutil.move(old, new)
        print("moved", os.path.relpath(old, ROOT), "->", os.path.relpath(new, ROOT))

# 2. rewrite text files
changed = 0
for path in walk():
    if os.path.splitext(path)[1] not in TEXT_EXT and os.path.basename(path) != ".name":
        continue
    try:
        text = open(path, encoding="utf-8").read()
    except UnicodeDecodeError:
        continue
    new = text
    for a, b in REPLACEMENTS:
        new = new.replace(a, b)
    if os.path.basename(path) == ".name":
        new = "KidQuest"
    if new != text:
        open(path, "w", encoding="utf-8").write(new); changed += 1
print("rewrote", changed, "files")

# 3. remove the template's launcher icons (replaced by tools/make_icons.py output)
for rel in ("app/src/main/res/drawable-v24/ic_launcher_foreground.xml",
            "app/src/main/res/drawable/ic_launcher_background.xml"):
    p = os.path.join(ROOT, rel)
    if os.path.exists(p): os.remove(p); print("removed", rel)
for d, _, files in os.walk(os.path.join(ROOT, "app/src/main/res")):
    for f in files:
        if f.startswith("ic_launcher") and f.endswith(".webp"):
            os.remove(os.path.join(d, f))
print("old launcher icons removed")

# 4. leftovers?
left = []
for path in walk():
    try: t = open(path, encoding="utf-8").read()
    except Exception: continue
    if path.endswith("rename_package.py"): continue
    if re.search(r"version5|Version5|Version 5", t): left.append(os.path.relpath(path, ROOT))
print("leftover old names in:", left or "none")
sys.exit(1 if left else 0)
