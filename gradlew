#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION="8.11.1"
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${VERSION}-bin/raja"
HOME_DIR="$BASE/gradle-$VERSION"
if [ ! -x "$HOME_DIR/bin/gradle" ]; then
  mkdir -p "$BASE"
  curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip" -o "$BASE/gradle.zip"
  unzip -q -o "$BASE/gradle.zip" -d "$BASE"
  rm -f "$BASE/gradle.zip"
fi
exec "$HOME_DIR/bin/gradle" -p "$APP_HOME" "$@"
