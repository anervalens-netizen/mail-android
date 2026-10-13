# Mail builds run on Gaming in its existing WSL environment.
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"
& wsl.exe -d Ubuntu-24.04 -u runner -- env `
    JAVA_HOME=/home/runner/Mail/toolchains/jdk-21 `
    ANDROID_HOME=/opt/android-sdk `
    ANDROID_SDK_ROOT=/opt/android-sdk `
    MAIL_BUILD_WORKERS=6 `
    MAIL_BUILD_HEAP=10g `
    bash /home/runner/Mail/app/scripts/build-mail.sh @GradleArgs
exit $LASTEXITCODE
