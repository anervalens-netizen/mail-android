# Mail 1.1.0 verification

Built on Gaming, WSL Ubuntu-24.04, runner, JDK 21, Android SDK/build tools 37.
Package eu.astancu.mail, versionCode 10100, same Mail release signing key.

## Changes

- POP3 account-specific exact-sender rules and local folders, managed with a Compose screen from the message list menu.
- Rules apply to existing Inbox messages and after successful POP3 synchronization.
- Full download before moving; incomplete or failed downloads stay in Inbox for retry.
- Local MessageStore transaction preserves a source UIDL placeholder to prevent repeat downloads. No backend MOVE or DELE is submitted.
- Folder listeners refresh unread counts after moves. Stopping a rule leaves messages and folders intact.
- Rules are persisted in per-account local storage; they are not part of settings exports.
- Stale Inbox notifications for moved messages are removed; destination notifications are not synthesized.

## Automated checks

The following passed on Gaming on 2026-10-13:

- app-common:testDebugUnitTest: 27 tests, no failures (including five sender-rule tests).
- legacy:storage:testDebugUnitTest --tests '*MoveMessageOperationsTest': three SQLite tests, no failures.
- app-common:lintDebug, app-common:detekt, app-common:spotlessCheck.
- legacy:common:spotlessCheck, legacy:ui:legacy:spotlessCheck.
- app-thunderbird:assembleFossRelease, including release vital lint.

Invocation used JDK 21, six workers, 10 GiB heap, -Pci=true and -Ptb.useUploadKey=false.
A first compile found an outdated Flag import; the import was corrected. Detekt then found guard-clause count
and numeric-constant issues; these were corrected before the successful final run.

## Physical-device smoke checks

OnePlus 6T, Android 11/API 30, controlled using ADB through Server. A loopback-only synthetic POP3/SMTP
fixture used ADB reverse; no real mailbox or external delivery was involved.

- A signed upgrade from Mail 1.0.0 kept the test account and three downloaded messages.
- The new menu and Romanian rule-management screen opened successfully.
- Saving a sender rule created the folder and moved one complete matching message, leaving one unrelated
  message and one matching message with an intentionally failing attachment download in Inbox.
- After restoring download availability and adding a new matching message, synchronization automatically
  moved both the attachment message and the new message.
- A subsequent sync with the same server UIDLs issued no RETR and produced no duplicate messages.
- A 229376-byte attachment saved from the destination folder had exactly the fixture SHA-256:
  fee6bd007b78f0a1e99d91497345637d1196f100e67b1d4c08a160456ded9e2a.
- A second signed upgrade to the final APK preserved the saved rule and existing local messages.
- Stopping the rule preserved its folder/messages. A different sender rule reused the existing folder despite
  a case difference in its name and moved the remaining Inbox message.
- A stale unread-count issue observed in the first candidate was fixed by notifying controller listeners.
  The final APK showed an empty Inbox and three unread messages in the destination folder.
- Removing every message from the synthetic server, then syncing, left all four local messages intact.
- Fixture logs recorded no DELE commands.

The final APK is signed and non-debuggable. This is a focused synthetic protocol/device test, not a test
against every provider. The owner's actual mailbox rule still requires installing the update and
configuring the sender on the unlocked phone.
