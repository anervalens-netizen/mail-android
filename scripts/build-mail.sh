#!/usr/bin/env bash
# Mail fork: reproducible personal FOSS APK; signing materials stay outside Git.
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
test -d "$ANDROID_HOME/platforms"
test -f .signing/tb.release.signing.properties
./gradlew :app-thunderbird:assembleFossRelease \
  -Ptb.useUploadKey=false -Pci=true --no-daemon \
  --max-workers="${MAIL_BUILD_WORKERS:-2}" \
  "-Dorg.gradle.jvmargs=-Xmx${MAIL_BUILD_HEAP:-5g} -Dfile.encoding=UTF-8 -XX:+UseG1GC" \
  --console=plain "$@"
