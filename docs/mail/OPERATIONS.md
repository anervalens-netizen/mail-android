# Mail build environment

The owner's instruction is to use **Gaming** for compilations and other resource-intensive work. Do not choose Dell for these workloads. The first Mail 1.0.0 APK was built on Dell before this correction; it was not rebuilt just to change that historical fact.

## Gaming

- Windows host: Gaming.
- WSL distribution: `Ubuntu-24.04`, Linux user `runner`.
- Repository: `/home/runner/Mail/app`.
- Dedicated JDK 21: `/home/runner/Mail/toolchains/jdk-21` (the system Java 17 remains unchanged).
- Android SDK: `/opt/android-sdk`, platform/build tools 37.
- Signing material: `/home/runner/.local/share/mail-signing/`, private permissions. Ignored `.signing/tb.release.signing.properties` references the existing Mail release key.
- Build from Windows: run `scripts/build-mail-gaming.ps1`. It selects Gaming's WSL environment, six workers and a 10 GiB Gradle heap. Additional Gradle task arguments can be passed through.
- From an existing Gaming WSL shell: set `JAVA_HOME`, `ANDROID_HOME`, `MAIL_BUILD_WORKERS=6`, and `MAIL_BUILD_HEAP=10g`, then run `./scripts/build-mail.sh`.

Use the existing signing key and application ID for updates and increment versionCode. Keystores, passwords, and signing properties must never enter Git. GitHub publishing credentials are not installed by this setup; the connected GitHub API can publish source changes.

Mail 1.0.0's tested APK and checksum are retained in `/home/runner/Mail/releases/1.0.0`. See [verification](VERIFICATION.md) for the actual tests and their limits. Mail 1.1.0 was subsequently built and tested on Gaming; see [Mail 1.1 verification](VERIFICATION-1.1.md).
