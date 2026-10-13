# Mail 1.2.0 verification

## Behavior

The message-list search now matches only From (the sender display name and email address), using
the existing CONTAINS search. To, Cc, Bcc, subject and message-content alternatives were removed.
Partial sender names and addresses are supported. Existing folder/account scope, Search everywhere
and unthreaded search results are retained. Both message-list implementations show a sender-specific
hint in English and Romanian. This is a local search of downloaded mail; POP3 has no server search.

No account, message, folder or rule storage was changed. The existing menu dispatcher was rewritten
as an equivalent when expression to satisfy the ReturnCount check exposed by this verification run.

## Build and checks

Built on Gaming, WSL Ubuntu-24.04 as runner, JDK 21, Android SDK/build tools 37, on 2026-10-13.
Package eu.astancu.mail, versionCode 10200, versionName 1.2.0, same release certificate as prior versions.

Passed:
- :legacy:ui:legacy:lintDebug
- :legacy:ui:legacy:detekt
- :legacy:ui:legacy:spotlessCheck
- :app-thunderbird:assembleFossRelease, including release vital lint

:feature:search:impl-legacy:allTests was invoked, but its JVM test compilation and execution reported
NO-SOURCE. It is not claimed as executed unit-test coverage. Behavioral verification below ran the
signed release APK against real Android UI and SQLite storage.

Final build used -Ptb.useUploadKey=false -Pci=true, six workers and a 10 GiB Gradle heap; it succeeded
in 1m 1s. An initial attempt failed the pre-existing menu ReturnCount issue; this was corrected before release.

## Physical-device verification

OnePlus 6T, Android 11, with a loopback-only synthetic POP3 fixture via ADB reverse.
Eight synthetic messages separately exercised name matches, address fragments, and false positives
from To, Cc, Bcc, subject and body. Existing test folder/rule data was retained.

Before update, Mail 1.1.1 returned seven results for pop, including To/Cc/Bcc/subject-only matches.
After a signed update to 1.2.0, the same search returned only the three sender matches:
- pop -> name-only Andrei Popescu, address popa@example.invalid, and address team.pop@other.invalid.
- POP -> the same three messages.
- andrei -> only the message from Andrei Popescu.
- popa@example.invalid -> only that sender; identical addresses in To/Cc/Bcc/subject/body were excluded.

The sender-specific search hint was observed. The Folders and rules menu still opened, and the
previously saved synthetic sender rule remained present. No real mailbox was used for these tests.

Scope of verification: ASCII case-insensitivity and substring matching were checked. No new
Unicode normalization, accent folding or search-operator syntax is introduced by this update.

APK size: 35808233 bytes.
APK SHA-256: 9bf94c4bfe5f1d2f2dabb95fa13a1f3706615625811447464f427029fc66cdcc
Certificate SHA-256: 12b610b62daf206ecbafe4aa351c5a8763a16f386cf440927d0aecce18c9c38b

AI-assisted customization at the owner's request.
