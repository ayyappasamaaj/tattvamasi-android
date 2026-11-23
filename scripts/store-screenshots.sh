#!/bin/bash
# Google Play screenshots, on the Chromebook's own Android (adb, as in the build notes): installs the Debug app, clears
# its data (no test leftovers), starts it on each store screen by itself (`--es shot <screen>`, AppRoot's Shot; Light
# mode) in a tall phone window at 240 dpi, and saves each window, cropped, to ~/store-screenshots/android/.
#
#   scripts/store-screenshots.sh            # WAIT=12 waits longer for the live data
#
# Needs: the Android SDK in ~/Android/Sdk, adb debugging on (adb connect 100.115.92.2:5555), FFmpeg.
set -uo pipefail
cd "$(dirname "$0")/../Tattvamasi"

ADB="$HOME/Android/Sdk/platform-tools/adb -s ${DEVICE:-100.115.92.2:5555}"
APP="com.ayyappasamaaj.tattvamasi"
WAIT="${WAIT:-8}"
OUT="$HOME/store-screenshots/android"
SHOTS=(home publications reader tanpura yatra events sabarimala)

echo "Building the Debug app…"
ANDROID_HOME="$HOME/Android/Sdk" ./gradlew -q --no-daemon assembleDebug || { echo "The build failed."; exit 1; }
$ADB install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null || { echo "Couldn't install the app."; exit 1; }
$ADB shell pm clear "$APP" >/dev/null

old_density="$($ADB shell wm density | awk '/Override/ {print $3}')"
$ADB shell wm density 240
mkdir -p "$OUT"
n=1
for shot in "${SHOTS[@]}"; do
  $ADB shell am force-stop "$APP"
  $ADB shell am start -n "$APP/.MainActivity" --es shot "$shot" >/dev/null
  sleep 2
  # A tall phone window (9:16 at 1024 px high), wherever ChromeOS put it
  task="$($ADB shell dumpsys activity activities | grep -oE "Task\{[0-9a-f]+ #[0-9]+ [^}]*A=[0-9]+:$APP" | head -1 \
    | grep -oE '#[0-9]+' | tr -d '#')"
  [ -n "$task" ] && $ADB shell cmd activity task resize "$task" 672 20 1248 1044 >/dev/null 2>&1
  wait="$WAIT"
  [ "$shot" = reader ] && wait=$((WAIT + 6))   # the PDF downloads first
  sleep "$wait"
  raw="$OUT/.raw.png"
  $ADB exec-out screencap -p > "$raw"
  # The window, without the black desktop around it: its edges found along a row and a column through the middle
  crop="$(python3 - "$raw" <<'PY'
import subprocess, sys
raw = sys.argv[1]
def line(vf):
    return subprocess.run(["ffmpeg", "-v", "error", "-i", raw, "-vf", vf + ",format=gray", "-f", "rawvideo", "-"],
                          capture_output=True).stdout
row = line("crop=iw:1:0:ih/2")
lit = [i for i, v in enumerate(row) if v > 8]
x0, x1 = lit[0], lit[-1] + 1
col = line(f"crop=1:ih:{(x0 + x1) // 2}:0")
lit = [i for i, v in enumerate(col) if v > 8]
y0, y1 = lit[0], lit[-1] + 1
print(f"crop={x1 - x0}:{y1 - y0}:{x0}:{y0}")
PY
)"
  ffmpeg -v error -y -i "$raw" -vf "${crop:-null}" -pix_fmt rgb24 "$OUT/$n-$shot.png"   # no transparency (Play)
  echo "  $n-$shot.png ($(ffprobe -v error -show_entries stream=width,height -of csv=p=0:s=x "$OUT/$n-$shot.png"))"
  n=$((n + 1))
done
rm -f "$OUT/.raw.png"
if [ -n "$old_density" ]; then $ADB shell wm density "$old_density"; else $ADB shell wm density reset; fi
$ADB shell pm clear "$APP" >/dev/null   # back to the phone's own Light / Dark choice
echo "Done: $OUT"
