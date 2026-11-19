# Kid Quest

Application id: `ng.name.nsikak.kidquest`  (minSdk 21)

## Build
1. Open this folder in Android Studio and let Gradle sync, or use a terminal here.
2. Debug build:    `./gradlew --no-daemon assembleDebug`
3. Release build:  `./gradlew --no-daemon assembleRelease`  (signed with the debug certificate for now)
4. Unit tests:     `./gradlew --no-daemon test`

## Checking the project without Android Studio
`python3 tools/verify.py`  runs 8 groups of checks (names, resources, manifest, imports, icons/assets,
call signatures, missing imports, and - if `kotlinc` is installed - the Kotlin parse and all unit tests).
Add `--quick` to skip the Kotlin steps. Set `KOTLINC=/path/to/kotlinc` if it is not on your PATH.

## Regenerating art and sound (needs Pillow / numpy)
- `python3 tools/make_icons.py`   launcher icons, logo emblem, "Kid Quest" wordmark, store/icon-512.png
- `python3 tools/make_sounds.py`  every sound effect
