#!/usr/bin/env bash
# Собирает лаунчер, ставит на телевизор, делает его главным экраном и нажимает «Домой».
# Использование: tools/deploy.sh [debug|release]   (TV=ip:port переопределяет адрес)
set -euo pipefail
cd "$(dirname "$0")/.."

TV="${TV:-192.168.31.103:5555}"
VARIANT="${1:-debug}"

case "$VARIANT" in
  debug)   TASK=":app:assembleDebug";   APK="app/build/outputs/apk/debug/app-debug.apk" ;;
  release) TASK=":app:assembleRelease"; APK="app/build/outputs/apk/release/app-release.apk" ;;
  *) echo "usage: $0 [debug|release]" >&2; exit 2 ;;
esac

adb connect "$TV" >/dev/null
./gradlew -q "$TASK"
adb -s "$TV" install -r "$APK"
adb -s "$TV" shell cmd package set-home-activity com.example.tvlauncher/.ui.HomeActivity
adb -s "$TV" shell input keyevent KEYCODE_HOME
