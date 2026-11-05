# Mail 1.3.0 — search and POP3 download correction

## Root causes

- Mail 1.2 deliberately searched only the sender. Queries found in the subject,
  message body or recipients could not match. Restore broad local search while
  retaining targeted sender-only syntax via `from:`.
- The inherited Thunderbird default auto-download threshold was 128 KiB. Larger
  POP3 messages remained partial, so attachments required a second download.
  When another client deleted them from the POP3 server first, retry failed.

## New behavior

- General local search: From, To, Cc, Bcc, Subject, downloaded body content.
- Explicit `from:` search: only sender names and addresses.
- New accounts: 10 MiB auto-download threshold.
- Existing POP3 accounts at the former 128 KiB default: migrate once to 10 MiB
  and persist. Leave custom limits and non-POP3 accounts intact.
- No deletion, local-message rewrite, account reset, secret change or IMAP
  behavior change intended.
- Historic partial messages already removed from POP3 cannot be recovered by
  this change. The original RCA document remains in the separate work PST
  archive, not in the current POP3 mailbox.

## Verification (2026-11-05)

Gaming WSL Ubuntu 24.04, JDK 21, Android SDK 37; Git source `380e9c9bb8`.
The final 1.3.0 build completed successfully, job
`a2d16e31-c174-490d-92e3-b28ee91bec86`, exit 0, with:

- `:legacy:core:testDebugUnitTest` — PASS, including migration and custom-limit checks.
- `:app-common:testDebugUnitTest` — PASS.
- `:legacy:ui:legacy:detekt`, `:legacy:ui:legacy:spotlessCheck` — PASS.
- `:legacy:core:detekt`, `:legacy:core:spotlessCheck` — PASS.
- `:app-thunderbird:assembleFossRelease` — PASS with release vital lint.

Package: `eu.astancu.mail`; version `1.3.0` / versionCode `10300`.
APK SHA-256: `88ac26ca657c92d316da79c29b62538cd07f553c908203f69876327e33409167`.
Release signing certificate SHA-256:
`12b610b62daf206ecbafe4aa351c5a8763a16f386cf440927d0aecce18c9c38b`,
unchanged from 1.2.0. APK size 35,791,856 bytes.

The signed APK was copied to Gaming `C:\Dev\Mail\releases\Mail-1.3.0.apk`
and delivered by Remote Control Android file push to phone-andrei Downloads:
`content://media/external/downloads/1000089837`, transfer id
`959b48ea-c197-4d0c-9d2e-5e6fdd30a8dc`, SHA-256 confirmed.

**Not yet verified:** install/launch on the owner's OnePlus Nord 4, existing account
migration on the real profile, and an end-to-end live POP3 >128 KiB download.
No automatic installation or account modification was performed on the phone.
