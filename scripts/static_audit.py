#!/usr/bin/env python3
"""Fast invariants for the 2026 WorkTime rewrite."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
failures: list[str] = []


def fail(message: str) -> None:
    failures.append(message)


def read(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except Exception as exc:  # noqa: BLE001
        fail(f"Cannot read {path.relative_to(ROOT)}: {exc}")
        return ""


manifest_path = APP / "src/main/AndroidManifest.xml"
manifest_text = read(manifest_path)
try:
    manifest = ET.fromstring(manifest_text)
except Exception as exc:  # noqa: BLE001
    fail(f"AndroidManifest.xml is invalid: {exc}")
    manifest = ET.Element("manifest")

android = "{http://schemas.android.com/apk/res/android}"
application = manifest.find("application")
if application is None:
    fail("AndroidManifest.xml has no application")
elif application.attrib.get(android + "name"):
    fail("Rewrite must not boot a legacy Application container")

main_activity = next(
    (
        node
        for node in manifest.findall(".//activity")
        if node.attrib.get(android + "name") == ".MainActivity"
    ),
    None,
)
if main_activity is None:
    fail("MainActivity is missing")
elif main_activity.attrib.get(android + "screenOrientation") != "portrait":
    fail("Phone UX must be portrait as requested")

for permission in (
    "android.permission.INTERNET",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.READ_CONTACTS",
    "android.permission.RECORD_AUDIO",
    "android.permission.CAMERA",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
):
    if permission in manifest_text:
        fail(f"Unexpected permission: {permission}")

for expected in (
    'android:allowBackup="false"',
    'android:dataExtractionRules="@xml/data_extraction_rules"',
    'android:fullBackupContent="@xml/backup_rules"',
    'android:windowSoftInputMode="adjustResize"',
):
    if expected not in manifest_text:
        fail(f"Missing manifest control: {expected}")

build_text = read(APP / "build.gradle.kts")
for expected in (
    "compileSdk = 37",
    "targetSdk = 37",
    "minSdk = 26",
    'applicationIdSuffix = ".debug"',
):
    if expected not in build_text:
        fail(f"Build invariant missing: {expected}")

for forbidden in (
    "androidx.baselineprofile",
    "com.android.compose.screenshot",
    "screenshotTestImplementation",
    "datastore.preferences",
    "assembleBenchmark",
):
    if forbidden in build_text:
        fail(f"Legacy build dependency/configuration remains: {forbidden}")

settings_text = read(ROOT / "settings.gradle.kts")
if 'include(":app")' not in settings_text:
    fail("App module is not included")
for forbidden_module in (":baselineprofile", ":macrobenchmark", ":benchmark-shared"):
    if forbidden_module in settings_text:
        fail(f"Legacy module still included: {forbidden_module}")

legacy_paths = (
    APP / "src/main/java/com/worktime/app/AppContainer.kt",
    APP / "src/main/java/com/worktime/app/WorkTimeApplication.kt",
    APP / "src/main/java/com/worktime/app/data",
    APP / "src/main/java/com/worktime/app/domain",
    APP / "src/main/java/com/worktime/app/ui",
    APP / "src/test/java/com/worktime/app/data",
    APP / "src/test/java/com/worktime/app/domain",
    APP / "src/test/java/com/worktime/app/ui",
    APP / "src/androidTest/java/com/worktime/app/ui",
    APP / "src/screenshotTestDebug",
    APP / "src/release",
    APP / "schemas/com.worktime.app.data.db.WorkTimeDatabase",
    ROOT / "baselineprofile",
    ROOT / "macrobenchmark",
    ROOT / "benchmark-shared",
    ROOT / ".github/workflows/baseline-profile.yml",
    ROOT / ".github/workflows/macrobenchmark.yml",
    ROOT / "scripts/generate_baseline_profile.sh",
)
for path in legacy_paths:
    if path.exists():
        fail(f"Legacy runtime/test artifact still active: {path.relative_to(ROOT)}")

main_activity_text = read(APP / "src/main/java/com/worktime/app/MainActivity.kt")
if "ModernAppGraph.get(applicationContext)" not in main_activity_text:
    fail("MainActivity must use the process-scoped modern app graph")
if "Room.databaseBuilder" in main_activity_text:
    fail("MainActivity must not own a recreation-scoped Room database")
if "WorkTimeApplication" in main_activity_text or "com.worktime.app.ui.WorkTimeApp" in main_activity_text:
    fail("MainActivity still references legacy runtime")

modern_root = APP / "src/main/java/com/worktime/app/modern"
required_files = (
    modern_root / "ModernAppGraph.kt",
    modern_root / "model/ModernModels.kt",
    modern_root / "data/ModernDatabase.kt",
    modern_root / "data/ModernRepository.kt",
    modern_root / "backup/ModernBackupCodec.kt",
    modern_root / "ui/CalendarScreen.kt",
    modern_root / "ui/DayEditorSheet.kt",
    modern_root / "ui/Reports.kt",
    modern_root / "ui/SettingsScreen.kt",
    APP / "src/androidTest/java/com/worktime/app/modern/ModernWorkTimeSmokeTest.kt",
    APP / "schemas/com.worktime.app.modern.data.ModernDatabase/2.json",
)
for path in required_files:
    if not path.is_file():
        fail(f"Rewrite source/schema missing: {path.relative_to(ROOT)}")

app_graph = read(modern_root / "ModernAppGraph.kt")
for expected in ("context.applicationContext", '"worktime-modern.db"', "Room.databaseBuilder"):
    if expected not in app_graph:
        fail(f"ModernAppGraph invariant missing: {expected}")

for kotlin_file in modern_root.rglob("*.kt"):
    text = read(kotlin_file)
    if "fallbackToDestructiveMigration" in text:
        fail(f"Destructive Room migration fallback found: {kotlin_file.relative_to(ROOT)}")

money_roots = (
    modern_root / "model",
    modern_root / "data",
    modern_root / "backup",
)
float_pattern = re.compile(r"\b(?:Float|Double)\b|\.toFloat\(|\.toDouble\(")
for source_root in money_roots:
    for kotlin_file in source_root.rglob("*.kt"):
        if float_pattern.search(read(kotlin_file)):
            fail(f"Binary floating point in money/data layer: {kotlin_file.relative_to(ROOT)}")

backup_codec = read(modern_root / "backup/ModernBackupCodec.kt")
if "schemaVersion" not in backup_codec or "Unsupported schemaVersion" not in backup_codec:
    fail("Backup format must be versioned and reject unsupported versions")
for expected in ("MAX_BACKUP_SIZE_BYTES", "readUtf8Limited"):
    if expected not in backup_codec:
        fail(f"Backup size invariant missing: {expected}")

settings_screen = read(modern_root / "ui/SettingsScreen.kt")
if "ModernBackupCodec::readUtf8Limited" not in settings_screen:
    fail("Backup import must bound the selected input stream before JSON decode")
if ".bufferedReader()?.use { it.readText() }" in settings_screen:
    fail("Unbounded backup readText() import path returned")

repository = read(modern_root / "data/ModernRepository.kt")
if "withTransaction" not in repository or "restoreBackup" not in repository:
    fail("Backup restore must be transactional")

if failures:
    print("Static audit FAILED:")
    for item in failures:
        print(f"- {item}")
    sys.exit(1)

print("Static audit OK")
