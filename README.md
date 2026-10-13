# Mail

Personal Android email client maintained by Andrei Stancu. Based on Thunderbird for Android / K-9 Mail, upstream tag `THUNDERBIRD_24_0`.

![Mail](docs/mail/logo.png)

- App name: **Mail**; Android package: `eu.astancu.mail`.
- POP3, IMAP and SMTP with password/app-password authentication.
- Local message search and multiple accounts are inherited from Thunderbird.
- Mail 1.1 adds local POP3 folders and exact-sender rules for existing and future incoming messages.
- Newly created POP3 accounts keep local messages after server deletion and default to **All messages** (no message-count limit). The setup screen lets you choose a smaller limit. Imported accounts retain their imported settings.
- FOSS release uses the existing no-funding and no-telemetry integrations.
- No Pro tier, in-app billing, donation prompts or Thundermail onboarding in the Mail FOSS release.
- Own emerald/turquoise envelope launcher, themed and notification icons.

## Install and update

Install the signed APK from this repository's Releases. Future updates must keep the application ID and signing certificate and increase versionCode. Do not uninstall before updating: uninstalling removes local app data.

Use POP3 **leave messages on server** on each receiving client if both Outlook and Mail must download the same incoming mail. Sent messages and read state are not synchronized by POP3. Configure local retention deliberately. Settings export is not a complete message backup.

## Sender search

From Mail 1.2, the message-list search matches only the sender name and email address (the From header).
A fragment such as `pop` matches every sender whose name or address contains that fragment.
Matches only in To, Cc, Bcc, subject or message text are excluded.
The current folder/account scope is retained; the existing Search everywhere action broadens the scope.

## Local POP3 rules

Open the message list menu and choose **Folders and rules** (**Foldere și reguli** in Romanian).
Select the POP3 account, enter a sender email address and a local folder name, then choose **Save and apply**.
The folder is created automatically, or an existing regular local folder with the same name is reused.
Existing Inbox messages are processed immediately; future messages are processed after each successful sync.
Stopping a rule keeps its folder and messages. Saving the same sender again updates its destination.
From Mail 1.1.1, regular local folders appear directly below Inbox in the folder list.

Rules and their folders are local to this installation; POP3 does not synchronize them to Outlook.
The rule never deletes the server copy. Complete message data is required before moving, including attachments.
Failed downloads stay in Inbox and are retried at the next sync. Rules are not included in settings exports.

See [Mail 1.1 verification](docs/mail/VERIFICATION-1.1.md).

## Authentication scope

This fork does not reuse Thunderbird's OAuth client registrations. This release is intended for the owner's POP3 account using password/app-password authentication. Google/Microsoft/other OAuth sign-in requires our own provider registrations before it can be enabled.

## Build

JDK 21 and Android SDK 37. The release signing configuration is kept outside Git and referenced by ignored `.signing/tb.release.signing.properties`.

```sh
./scripts/build-mail.sh
```

The script bounds build memory and disables minification/resource shrinking for this personal release. The APK is a signed, non-debuggable release build.

See [upstream documentation](docs/mail/UPSTREAM-README.md) and [development environment](docs/contributing/development-environment.md). Keep changes focused so upstream security fixes remain easy to merge.

## Attribution

Fork of [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android), built on K-9 Mail. This is an independent build, not an official Mozilla/Thunderbird or Microsoft product. Original LICENSE and NOTICE files are retained. Mail branding and build customization were produced with AI assistance at the owner's request.
