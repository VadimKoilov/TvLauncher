#!/usr/bin/env bash
# Собирает лаунчер, ставит на телевизор, делает его главным экраном и нажимает «Домой».
# Использование: tools/deploy.sh [debug|release]   (TV=ip:port переопределяет адрес)
set -euo pipefail
cd "$(dirname "$0")/.."

TV="${TV:-192.168.1.238:5555}"
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

# Служба кнопки ⚙ на пульте. Другие включённые службы спец. возможностей не трогаем.
SERVICE="com.example.tvlauncher/com.example.tvlauncher.settings.SettingsKeyService"
ENABLED="$(adb -s "$TV" shell settings get secure enabled_accessibility_services | tr -d '\r')"
case ":$ENABLED:" in
  *":$SERVICE:"*) ;;
  ":null:"|"::") adb -s "$TV" shell settings put secure enabled_accessibility_services "$SERVICE" ;;
  *) adb -s "$TV" shell settings put secure enabled_accessibility_services "$ENABLED:$SERVICE" ;;
esac
adb -s "$TV" shell settings put secure accessibility_enabled 1

adb -s "$TV" shell input keyevent KEYCODE_HOME
