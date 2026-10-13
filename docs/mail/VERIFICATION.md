# Mail 1.0.0 verification

Tested on 2026-10-13 with a physical OnePlus 6T running Android 11. Only synthetic messages and a localhost POP3/SMTP fixture were used; no real mailbox credentials were read.

## Build and signature

- FOSS release, package `eu.astancu.mail`, version `1.0.0`, code `10000`.
- Android minimum API 23, target API 36, compile API 37; JDK 21.
- Release APK is not debuggable. Build script uses bounded workers/memory and disables minification/resource shrinking.
- APK signature verified by Android apksigner. Signing certificate SHA-256: `12b610b62daf206ecbafe4aa351c5a8763a16f386cf440927d0aecce18c9c38b`.
- The exact distributed APK checksum is in the release asset `SHA256SUMS`.

## Automated checks

Five selected unit tests passed: two in `MailPop3DefaultsTest` and three in `SyncOptionsStateMapperKtTest`. They cover independent POP3 deletion behavior, unchanged IMAP behavior, POP3's unlimited default, preserving the setup selection, and options mapping. Relevant formatting checks and release lint passed.

## Physical-device checks

1. Signed APK installed and launched. The welcome page shows Mail and the new emerald/turquoise envelope, with accurate independent-fork attribution.
2. POP3 and SMTP authentication succeeded against the local fixture via ADB reverse ports 11110 and 11125.
3. Three synthetic incoming messages were downloaded. Searching `TestSender` returned exactly two messages. The text appeared only in the senders' display names, not their addresses, subjects, or message bodies.
4. The fixture mailbox was emptied. A manual refresh issued POP3 `STAT`, which returned zero messages; all three downloaded messages remained visible locally.
5. Installing a newly built, same-key APK with `adb install -r` preserved the test account and all three messages. This covered an update within Mail 1.0.0; it is not proof for all future database migrations.
6. Test account data was removed afterward. Final onboarding was checked separately after the navigation-only change to skip the Thundermail introduction.

## Limits and maintenance

- POP3 retains incoming mail independently. Read state, sent mail, and folders do not synchronize between Outlook and Mail. Both clients must leave server copies available long enough for the other client to download them.
- New POP3 accounts default to All messages. Selecting a smaller count changes that limit. Imported account settings are preserved, so review them after import.
- Settings export does not back up downloaded messages. No complete mailbox backup/restore feature was added in this fork.
- Google/Microsoft OAuth is not configured. Use supported password/app-password authentication; independent OAuth registrations are required before enabling OAuth.
- Real-provider connectivity, long-term battery behavior, and future upgrade migrations were not tested. Keep the application ID and signing key and increase versionCode for future releases. Do not uninstall to update.
- The upstream base is Thunderbird for Android tag `THUNDERBIRD_24_0`; keep merging upstream security fixes.
