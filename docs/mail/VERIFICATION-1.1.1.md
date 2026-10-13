# Mail 1.1.1 verification

This update places regular local folders immediately after Inbox, before Outbox and other special folders.
It changes display order only; folder IDs, messages and sender rules are untouched.
Existing ordering within the local-folder group is preserved. Server-backed regular folders keep their prior priority.

Built on Gaming on 2026-10-13, using JDK 21 and Android SDK 37, six workers and a 10 GiB Gradle heap.
Package eu.astancu.mail, versionCode 10101, versionName 1.1.1, same Mail release certificate.

Checks passed:
- :legacy:ui:folder:lintDebug
- :legacy:ui:folder:detekt
- :legacy:ui:folder:spotlessCheck
- :app-thunderbird:assembleFossRelease, including vital release lint

:legacy:ui:folder:testDebugUnitTest was invoked and reported NO-SOURCE; this module has no unit tests.
No new automated tests were added for this display-order customization.

On the OnePlus 6T (Android 11), a signed update from 1.1.0 to 1.1.1 retained the synthetic POP3 account,
the existing local ReportsTest folder, and all four messages with the same three unread flags.
The drawer order was observed as Inbox, ReportsTest, Outbox, Drafts, Sent, Trash.
The folder opened successfully and showed all four existing messages.
No real mailbox was accessed for these checks.

APK SHA-256: 8b18eb763df20b98803d51401a1dd5af5fa7229d9a5605b1295fbd34a785d380
APK size: 35791491 bytes.

AI-assisted customization at the owner's request.
