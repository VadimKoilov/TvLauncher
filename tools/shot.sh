#!/usr/bin/env bash
# Скриншот телевизора в build/screens/NAME.png.  Использование: tools/shot.sh NAME
set -euo pipefail
cd "$(dirname "$0")/.."

TV="${TV:-192.168.31.103:5555}"
NAME="${1:?usage: $0 NAME}"

mkdir -p build/screens
adb -s "$TV" exec-out screencap -p > "build/screens/$NAME.png"
echo "build/screens/$NAME.png"
