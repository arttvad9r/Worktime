$ErrorActionPreference = "Stop"

python scripts/static_audit.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not (Test-Path ./gradlew.bat)) {
    Write-Error "Gradle Wrapper is missing."
    exit 2
}

./gradlew.bat --no-daemon `
    :app:testDebugUnitTest `
    :app:lintDebug `
    :app:lintRelease `
    :app:assembleDebug `
    :app:assembleDebugAndroidTest `
    :app:assembleRelease `
    --stacktrace
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

git diff --exit-code -- app/schemas/com.worktime.app.modern.data.ModernDatabase
exit $LASTEXITCODE
