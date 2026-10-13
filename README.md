# Mail

Personal Android email client maintained by Andrei Stancu. Based on Thunderbird for Android / K-9 Mail, upstream tag `THUNDERBIRD_24_0`.

![Mail](docs/mail/logo.png)

- App name: **Mail**; Android package: `eu.astancu.mail`.
- POP3, IMAP and SMTP with password/app-password authentication.
- Local message search and multiple accounts are inherited from Thunderbird.
- Newly created POP3 accounts keep local messages after server deletion and have no message-count limit. Imported accounts retain their imported settings.
- FOSS release uses the existing no-funding and no-telemetry integrations.
- No Pro tier, in-app billing, donation prompts or Thundermail onboarding in the Mail FOSS release.
- Own emerald/turquoise envelope launcher, themed and notification icons.

## Install and update

Install the signed APK from this repository's Releases. Future updates must keep the application ID and signing certificate and increase versionCode. Do not uninstall before updating: uninstalling removes local app data.

Use POP3 **leave messages on server** on each receiving client if both Outlook and Mail must download the same incoming mail. Sent messages and read state are not synchronized by POP3. Configure local retention deliberately. Settings export is not a complete message backup.

## Authentication scope

This fork does not reuse Thunderbird's OAuth client registrations. This release is intended for the owner's POP3 account using password/app-password authentication. Google/Microsoft/other OAuth sign-in requires our own provider registrations before it can be enabled.

## Build

JDK 21 and Android SDK 37. The release signing configuration is kept outside Git and referenced by ignored `.signing/tb.release.signing.properties`.

```sh
./gradlew :app-thunderbird:assembleFossRelease -Ptb.useUploadKey=false
```

See [upstream documentation](docs/mail/UPSTREAM-README.md) and [development environment](docs/contributing/development-environment.md). Keep changes focused so upstream security fixes remain easy to merge.

## Attribution

Fork of [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android), built on K-9 Mail. This is an independent build, not an official Mozilla/Thunderbird or Microsoft product. Original LICENSE and NOTICE files are retained. Mail branding and build customization were produced with AI assistance at the owner's request.
