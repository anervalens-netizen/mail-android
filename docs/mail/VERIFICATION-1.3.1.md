# Mail 1.3.1 – FTS4 and POP3 hotfix verification

Date: 2026-11-05. Source branch: `fix/mail-search-full-fetch-20261105`.

## Changes

- Search in From, To, Cc, Bcc, subject and downloaded message content; `from:` is sender-only.
- Escape literal FTS4 queries and preserve default `simple` tokenizer boundaries for Unicode punctuation, combining marks and supplementary characters.
- Raise the previous POP3 128 KiB default to 10 MiB only once per pre-existing completed account; persist a per-account migration marker and respect any subsequent explicit 128 KiB or custom limit.
- Version `1.3.1` / versionCode `10301` (upgraded from `1.3.0` / `10300`).

## Automated verification – Gaming

- WSL Ubuntu 24.04, JDK 21, Android SDK 37; signed build via `scripts/build-mail.sh`, `-Ptb.useUploadKey=false`, `-Pci=true`.
- Job `6768038e-d183-4812-ba73-3cd648652da5`: `:app-thunderbird:assembleFossRelease`, `:legacy:core:testDebugUnitTest` and `:legacy:ui:legacy:testDebugUnitTest` all passed, exit 0.
- Prior hotfix revision `c71dc5a7c8`: 18/18 focused tests for Preferences, FTS parser and SQLite FTS4 passed; Spotless and detekt passed.
- APK SHA-256: `9fd9efe0c4f26b27a222c72855fc2fd566300b2bbe73cfbd1d9b68310c6e9949` (35,808,989 bytes).
- `apksigner verify` passed. Release certificate SHA-256 `12b610b62daf206ecbafe4aa351c5a8763a16f386cf440927d0aecce18c9c38b`, matching the existing signed Mail 1.3.0. Package `eu.astancu.mail`, version `1.3.1` / code `10301`, non-debug release.
- Signed APK stored at Gaming `C:\\Dev\\Mail\\releases\\Mail-1.3.1.apk` and Server `/home/andrei/releases/mail/Mail-1.3.1.apk`. No signing keys or credentials committed.

## Device checks

- Test OnePlus 6T: `adb install -r` succeeded and app launched into `MessageHomeActivity`. Package manager confirms version 1.3.1 and the original first-install time of 2026-10-13 12:33:07, evidence of an in-place update (not a reinstall/reset). This is a smoke check; live POP3 end-to-end scenarios were not re-run here.
- Primary OnePlus Nord 4: verified signed APK published to Downloads at `content://media/external/downloads/1000090081`, source hash confirmed by Android transfer. **Installation not verified:** secure keyguard was locked, and shell control was unavailable at the time of delivery. Do not claim primary phone updated before package-manager verification.
- Android reverse controller did not support bypassing secure keyguard; never reset local mail data or unlock the phone programmatically.

## Release and review gates

- Codex last code review at `c71dc5a7c8` reported no unresolved findings. A subsequent commit bumped only application version metadata; merge/release state should be verified independently at time of publishing.
- Check the final commit and PR status before merge. Do not claim hosted GitHub Actions checks without an actual run.
