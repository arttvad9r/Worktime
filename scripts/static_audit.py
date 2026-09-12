#!/usr/bin/env python3
"""Fast invariants for the 2026 Worktime rewrite."""

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
else:
    if application.attrib.get(android + "name"):
        fail("Rewrite must not boot the legacy WorkTimeApplication container")

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

if manifest.find(".//receiver[@android:name='.widget.WorkTimeWidgetProvider']", {"android": "http://schemas.android.com/apk/res/android"}) is not None:
    fail("Legacy WorkTime widget must not be registered by the rewrite")

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

main_activity_text = read(APP / "src/main/java/com/worktime/app/MainActivity.kt")
if '"worktime-modern.db"' not in main_activity_text:
    fail("Rewrite must use an isolated Room database file")
if "WorkTimeApplication" in main_activity_text or "com.worktime.app.ui.WorkTimeApp" in main_activity_text:
    fail("MainActivity still references legacy runtime")

modern_root = APP / "src/main/java/com/worktime/app/modern"
required_files = (
    modern_root / "model/ModernModels.kt",
    modern_root / "data/ModernDatabase.kt",
    modern_root / "data/ModernRepository.kt",
    modern_root / "backup/ModernBackupCodec.kt",
    modern_root / "ui/CalendarScreen.kt",
    modern_root / "ui/DayEditorSheet.kt",
    modern_root / "ui/Reports.kt",
    modern_root / "ui/SettingsScreen.kt",
)
for path in required_files:
    if not path.is_file():
        fail(f"Rewrite source missing: {path.relative_to(ROOT)}")

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

repository = read(modern_root / "data/ModernRepository.kt")
if "withTransaction" not in repository or "restoreBackup" not in repository:
    fail("Backup restore must be transactional")

if failures:
    print("Static audit FAILED:")
    for item in failures:
        print(f"- {item}")
    sys.exit(1)

print("Static audit OK")
