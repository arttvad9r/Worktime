#!/usr/bin/env python3
"""Fast repository invariants that do not require Android SDK/Gradle."""

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


def parse_xml(path: Path) -> ET.Element:
    try:
        return ET.parse(path).getroot()
    except Exception as exc:  # noqa: BLE001
        fail(f"XML parse failed: {path.relative_to(ROOT)}: {exc}")
        return ET.Element("invalid")


manifest_path = APP / "src/main/AndroidManifest.xml"
manifest_root = parse_xml(manifest_path)
manifest_text = manifest_path.read_text(encoding="utf-8")
android_name = "{http://schemas.android.com/apk/res/android}name"
android_screen_orientation = "{http://schemas.android.com/apk/res/android}screenOrientation"
main_activity = next(
    (
        node
        for node in manifest_root.findall(".//activity")
        if node.attrib.get(android_name) == ".MainActivity"
    ),
    None,
)
if main_activity is not None and android_screen_orientation in main_activity.attrib:
    fail("MainActivity must not lock screen orientation; adaptive layout requires a resizable window")

widget_receiver = next(
    (
        node
        for node in manifest_root.findall(".//receiver")
        if node.attrib.get(android_name) == ".widget.WorkTimeWidgetProvider"
    ),
    None,
)
widget_actions = {
    action.attrib.get("{http://schemas.android.com/apk/res/android}name")
    for action in (widget_receiver.findall("./intent-filter/action") if widget_receiver is not None else [])
}
for required_action in (
    "android.appwidget.action.APPWIDGET_UPDATE",
    "android.intent.action.DATE_CHANGED",
    "android.intent.action.TIME_SET",
    "android.intent.action.TIMEZONE_CHANGED",
):
    if required_action not in widget_actions:
        fail(f"Widget receiver is missing manifest action: {required_action}")
if "android.intent.action.TIME_CHANGED" in widget_actions:
    fail("Widget uses invalid TIME_CHANGED manifest action; Android ACTION_TIME_CHANGED is TIME_SET")

for permission in (
    "android.permission.INTERNET",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.READ_CONTACTS",
    "android.permission.RECORD_AUDIO",
    "android.permission.CAMERA",
):
    if permission in manifest_text:
        fail(f"Unexpected runtime/privacy-sensitive permission: {permission}")

for expected in (
    'android:allowBackup="false"',
    'android:dataExtractionRules="@xml/data_extraction_rules"',
    'android:fullBackupContent="@xml/backup_rules"',
    'android:windowSoftInputMode="adjustResize"',
):
    if expected not in manifest_text:
        fail(f"Missing required manifest control: {expected}")

base_strings = APP / "src/main/res/values/strings.xml"
ru_strings = APP / "src/main/res/values-ru/strings.xml"
base_widget_strings = APP / "src/main/res/values/widget_strings.xml"
ru_widget_strings = APP / "src/main/res/values-ru/widget_strings.xml"

xml_paths = (
    base_strings,
    ru_strings,
    base_widget_strings,
    ru_widget_strings,
    APP / "src/main/res/values/themes.xml",
    APP / "src/main/res/values-night/themes.xml",
    APP / "src/main/res/mipmap-anydpi/ic_launcher.xml",
    APP / "src/main/res/mipmap-anydpi/ic_launcher_round.xml",
    APP / "src/main/res/mipmap-anydpi-v33/ic_launcher.xml",
    APP / "src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml",
    APP / "src/main/res/drawable/ic_launcher_foreground.xml",
    APP / "src/main/res/drawable/ic_launcher_monochrome.xml",
    APP / "src/main/res/xml/data_extraction_rules.xml",
    APP / "src/main/res/xml/backup_rules.xml",
)
for xml_path in xml_paths:
    parse_xml(xml_path)


def resource_keys(path: Path, tag: str, *, translatable_only: bool = False) -> set[str]:
    root = parse_xml(path)
    return {
        node.attrib["name"]
        for node in root.findall(tag)
        if "name" in node.attrib
        and (not translatable_only or node.attrib.get("translatable", "true") != "false")
    }


def string_keys(path: Path, *, translatable_only: bool = False) -> set[str]:
    return resource_keys(path, "string", translatable_only=translatable_only)


def plural_keys(path: Path, *, translatable_only: bool = False) -> set[str]:
    return resource_keys(path, "plurals", translatable_only=translatable_only)


def check_russian_resource_parity(
    base_path: Path,
    ru_path: Path,
    tag: str,
    label: str,
) -> set[str]:
    base_keys = resource_keys(base_path, tag, translatable_only=True)
    ru_keys = resource_keys(ru_path, tag, translatable_only=True)
    if base_keys != ru_keys:
        missing_ru = sorted(base_keys - ru_keys)
        extra_ru = sorted(ru_keys - base_keys)
        if missing_ru:
            fail(f"Russian {label} resources missing keys: {', '.join(missing_ru)}")
        if extra_ru:
            fail(f"Russian {label} resources have extra keys: {', '.join(extra_ru)}")
    return base_keys


base_keys = check_russian_resource_parity(base_strings, ru_strings, "string", "app string")
widget_base_keys = check_russian_resource_parity(
    base_widget_strings,
    ru_widget_strings,
    "string",
    "widget string",
)
base_plural_keys = check_russian_resource_parity(base_strings, ru_strings, "plurals", "app plural")
widget_plural_keys = check_russian_resource_parity(
    base_widget_strings,
    ru_widget_strings,
    "plurals",
    "widget plural",
)
defined_string_keys = string_keys(base_strings) | string_keys(base_widget_strings)
defined_plural_keys = plural_keys(base_strings) | plural_keys(base_widget_strings)

string_ref_pattern = re.compile(r"\bR\.string\.([A-Za-z0-9_]+)")
plural_ref_pattern = re.compile(r"\bR\.plurals\.([A-Za-z0-9_]+)")
for kotlin_file in (APP / "src/main/java").rglob("*.kt"):
    text = kotlin_file.read_text(encoding="utf-8")
    referenced_string_keys = set(string_ref_pattern.findall(text))
    missing_string_keys = sorted(referenced_string_keys - defined_string_keys)
    if missing_string_keys:
        fail(
            f"Missing string resource(s) referenced by {kotlin_file.relative_to(ROOT)}: "
            + ", ".join(missing_string_keys)
        )
    referenced_plural_keys = set(plural_ref_pattern.findall(text))
    missing_plural_keys = sorted(referenced_plural_keys - defined_plural_keys)
    if missing_plural_keys:
        fail(
            f"Missing plural resource(s) referenced by {kotlin_file.relative_to(ROOT)}: "
            + ", ".join(missing_plural_keys)
        )

layer_import_guards = {
    "ui": ("com.worktime.app.data",),
    "domain": ("com.worktime.app.data", "com.worktime.app.ui"),
    "data": ("com.worktime.app.ui",),
}
import_pattern = re.compile(r"^\s*import\s+([A-Za-z0-9_.]+)", re.MULTILINE)
for layer, forbidden_prefixes in layer_import_guards.items():
    layer_root = APP / f"src/main/java/com/worktime/app/{layer}"
    for kotlin_file in layer_root.rglob("*.kt"):
        imports = import_pattern.findall(kotlin_file.read_text(encoding="utf-8"))
        for imported in imports:
            if any(imported == prefix or imported.startswith(prefix + ".") for prefix in forbidden_prefixes):
                fail(
                    f"Forbidden production layer dependency in {kotlin_file.relative_to(ROOT)}: "
                    f"{layer} imports {imported}"
                )

expected_domains = {
    "root",
    "file",
    "database",
    "sharedpref",
    "external",
    "device_root",
    "device_file",
    "device_database",
    "device_sharedpref",
}
data_rules = parse_xml(APP / "src/main/res/xml/data_extraction_rules.xml")
for section_name in ("cloud-backup", "device-transfer"):
    section = data_rules.find(section_name)
    if section is None:
        fail(f"data extraction rules missing {section_name}")
        continue
    excluded = {node.attrib.get("domain") for node in section.findall("exclude")}
    if excluded != expected_domains:
        fail(f"{section_name} does not exclude every app-data domain")

legacy_rules = parse_xml(APP / "src/main/res/xml/backup_rules.xml")
legacy_excluded = {node.attrib.get("domain") for node in legacy_rules.findall("exclude")}
if legacy_excluded != expected_domains:
    fail("legacy backup rules do not exclude every app-data domain")

framework_money_patterns = re.compile(r"\b(?:Float|Double)\b|\.toFloat\(|\.toDouble\(")
for source_root in (
    APP / "src/main/java/com/worktime/app/domain",
    APP / "src/main/java/com/worktime/app/data",
):
    for kotlin_file in source_root.rglob("*.kt"):
        text = kotlin_file.read_text(encoding="utf-8")
        if framework_money_patterns.search(text):
            fail(f"Binary floating-point token in domain/data: {kotlin_file.relative_to(ROOT)}")

for kotlin_file in (APP / "src/main/java").rglob("*.kt"):
    text = kotlin_file.read_text(encoding="utf-8")
    if "fallbackToDestructiveMigration" in text:
        fail(f"Destructive Room fallback found: {kotlin_file.relative_to(ROOT)}")

build_file = (APP / "build.gradle.kts").read_text(encoding="utf-8")
baseline_profile_build_file = (ROOT / "baselineprofile/build.gradle.kts").read_text(encoding="utf-8")
catalog_file = (ROOT / "gradle/libs.versions.toml").read_text(encoding="utf-8")
wrapper_file = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8")
workflows_dir = ROOT / ".github/workflows"
workflow_file = (workflows_dir / "android.yml").read_text(encoding="utf-8")
baseline_profile_workflow_file = (workflows_dir / "baseline-profile.yml").read_text(encoding="utf-8")
verify_script_file = (ROOT / "scripts/verify.sh").read_text(encoding="utf-8")
baseline_profile_helper_file = (ROOT / "scripts/generate_baseline_profile.sh").read_text(encoding="utf-8")
release_builder_file = (ROOT / "scripts/build_release_candidate.sh").read_text(encoding="utf-8")
release_script_file = (ROOT / "scripts/create_github_release.sh").read_text(encoding="utf-8")
checked_in_baseline_path = APP / "src/release/generated/baselineProfiles/baseline-prof.txt"
checked_in_startup_path = APP / "src/release/generated/baselineProfiles/startup-prof.txt"
checked_in_profile_text: dict[str, str] = {}
for label, path in (
    ("Baseline Profile", checked_in_baseline_path),
    ("Startup Profile", checked_in_startup_path),
):
    if not path.is_file():
        fail(f"Checked-in {label} is missing: {path.relative_to(ROOT)}")
    elif path.stat().st_size == 0:
        fail(f"Checked-in {label} is empty: {path.relative_to(ROOT)}")
    else:
        checked_in_profile_text[label] = path.read_text(encoding="utf-8")
signer_fingerprint_path = ROOT / "release/production-signing-cert-sha256.txt"
if not signer_fingerprint_path.is_file():
    fail("Pinned production signing fingerprint file is missing")
else:
    signer_fingerprint = signer_fingerprint_path.read_text(encoding="utf-8").strip()
    if re.fullmatch(r"(?:[0-9A-Fa-f]{2}:){31}[0-9A-Fa-f]{2}", signer_fingerprint) is None:
        fail("Pinned production signing fingerprint must be a colon-separated SHA-256 digest")
if "distributionSha256Sum=" not in wrapper_file:
    fail("Gradle wrapper distributionSha256Sum is missing")
if re.search(r"release\s*\{[^}]*signingConfig\s*=\s*signingConfigs\.getByName\(\"debug\"\)", build_file, re.DOTALL):
    fail("Release build must not use debug signing")
if re.search(
    r"release\s*\{.*?optimization\s*\{\s*enable\s*=\s*true",
    build_file,
    re.DOTALL,
) is None:
    fail("Release build must enable AGP optimization (R8 + resource shrinking)")
for required_task in (
    ":app:lintRelease",
    ":app:assembleRelease",
    ":app:assembleBenchmark",
    ":macrobenchmark:assembleBenchmark",
    ":baselineprofile:assemble",
):
    if required_task not in workflow_file:
        fail(f"CI release gate is missing task: {required_task}")
    if required_task not in verify_script_file:
        fail(f"Local verification gate is missing task: {required_task}")
for required_token in (
    'create("pixel6Api37")',
    "apiLevel = 37",
    'systemImageSource = "google"',
    'testedAbi = "x86_64"',
):
    if required_token not in build_file:
        fail(f"API 37 managed-device configuration is missing: {required_token}")
for required_token in (
    'create("pixel6Api34")',
    "apiLevel = 34",
    'systemImageSource = "aosp"',
    'testedAbi = "x86_64"',
):
    if required_token not in baseline_profile_build_file:
        fail(f"API 34 Baseline Profile device configuration is missing: {required_token}")
for required_token in (
    ":app:pixel6Api37DebugAndroidTest",
    "android.testInstrumentationRunnerArguments.class=com.worktime.app.ui.WorkTimeSmokeTest",
    "target-platform-smoke:",
):
    if required_token not in workflow_file:
        fail(f"API 37 target-platform smoke gate is missing: {required_token}")
if "./scripts/generate_baseline_profile.sh" not in baseline_profile_workflow_file:
    fail("Baseline Profile workflow must delegate generation to scripts/generate_baseline_profile.sh")
if ":app:generateBaselineProfile" in baseline_profile_workflow_file:
    fail("Baseline Profile workflow must not duplicate the Gradle generation command inline")
for required_token in (
    ":app:generateBaselineProfile",
    "androidx.benchmark.enabledRules=BaselineProfile",
    "baseline-prof.txt",
):
    if required_token not in baseline_profile_helper_file:
        fail(f"Baseline Profile helper is missing required generation control: {required_token}")
for required_builder_token in (
    "production-signing-cert-sha256.txt",
    "WORKTIME_SIGNING_SMOKE",
    "Release APK is signed by the wrong certificate",
):
    if required_builder_token not in release_builder_file:
        fail(f"Release candidate builder is missing signer control: {required_builder_token}")
for required_release_token in (
    "gh release create",
    "--verify-tag",
    "--draft",
    "SHA256SUMS.txt",
    "git ls-remote",
    "production-signing-cert-sha256.txt",
    "signerSha256=",
):
    if required_release_token not in release_script_file:
        fail(f"GitHub release helper is missing: {required_release_token}")
if 'WORKTIME_SIGNING_SMOKE: "1"' not in workflow_file:
    fail("CI disposable signing job must explicitly opt into signing-smoke mode")
action_ref_pattern = re.compile(r"^\s*(?:-\s*)?uses:\s*([^@\s]+)@([^\s#]+)", re.MULTILINE)
for workflow_path in sorted(workflows_dir.glob("*.yml")):
    workflow_text = workflow_path.read_text(encoding="utf-8")
    for action, ref in action_ref_pattern.findall(workflow_text):
        if action.startswith("./"):
            continue
        if re.fullmatch(r"[0-9a-fA-F]{40}", ref) is None:
            fail(
                "GitHub Action is not pinned to a full commit SHA in "
                f"{workflow_path.relative_to(ROOT)}: {action}@{ref}"
            )
if 'testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"' not in build_file:
    fail("AndroidJUnitRunner is not configured")
if 'androidx.test:runner' not in build_file and 'androidx.test:runner' not in catalog_file:
    fail("Explicit androidx.test:runner dependency is missing")
if failures:
    print("static-audit: FAILED", file=sys.stderr)
    for item in failures:
        print(f" - {item}", file=sys.stderr)
    raise SystemExit(1)

print("static-audit: OK")
