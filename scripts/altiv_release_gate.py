#!/usr/bin/env python3
"""ALTIVE pre-release gate. Candidate builds != customer-approved releases.

Run from repository root:
  python3 scripts/altiv_release_gate.py --mode candidate
  python3 scripts/altiv_release_gate.py --mode release --evidence-file /secure/qa.json --apk /path/app.apk
Never commit private device screenshots, tokens or QA evidence to a public repo.
"""
import argparse
import hashlib
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "src/android/app/src"
WORKFLOW = ".github/workflows/altiv-android-arm64.yml"

# Check actual code + regression tests in THIS checkout, not just PR descriptions.
SOURCE_CHECKS = {
    "Root quality memory, cross-niche": (
        f"{ANDROID}/main/java/com/openminis/app/ui/chat/AltivRootQualityMemory.kt",
        "GLOBAL LESSONS — ANY CUSTOMER NICHE"),
    "Web task routing invokes global lessons": (
        f"{ANDROID}/main/java/com/openminis/app/ui/chat/AltivVisualBuilder.kt",
        "AltivRootQualityMemory.instructions"),
    "Image correctness is NOT inferred from DOM": (
        f"{ANDROID}/main/java/com/openminis/app/browser/AltivVisualAudit.kt",
        'semanticImageApproval:"NOT_VERIFIED"'),
    "Missing-image placeholders flagged": (
        f"{ANDROID}/main/java/com/openminis/app/browser/AltivVisualAudit.kt",
        "assetPlaceholders"),
    "No license bypass at entry": (
        f"{ANDROID}/main/java/com/openminis/app/ui/commercial/AltivCommercialEntry.kt",
        "AltivSubscriptionPolicy.allowsAppAccess"),
    "Subscription permission enforced": (
        f"{ANDROID}/main/java/com/openminis/app/commercial/AltivSubscription.kt",
        "allowsAppAccess"),
    "Project memory retrieval": (
        f"{ANDROID}/main/java/com/openminis/app/data/repository/MemoryRepository.kt",
        "getProjectMemory"),
    "Project memory save": (
        f"{ANDROID}/main/java/com/openminis/app/data/repository/MemoryRepository.kt",
        "writeProjectMemory"),
    "Memory tool supports project key": (
        f"{ANDROID}/main/java/com/openminis/app/tools/AgentTools.kt",
        "project_key"),
    "Branded tool activity": (
        f"{ANDROID}/main/java/com/openminis/app/ui/chat/ChatToolFormatting.kt",
        "toolAgentName"),
    "Branded computer sheet": (
        f"{ANDROID}/main/java/com/openminis/app/ui/chat/ChatToolDetailUI.kt",
        "LocalContext.current.getString(R.string.app_name)"),
    "GitHub MCP connection catalog": (
        f"{ANDROID}/main/java/com/openminis/app/mcp/MCPQuickConnectCatalog.kt", None),
    "Supabase project scoping": (
        f"{ANDROID}/main/java/com/openminis/app/mcp/SupabaseMcpProjectScope.kt", None),
    "Supabase account scoping": (
        f"{ANDROID}/main/java/com/openminis/app/mcp/SupabaseMcpAccountScope.kt", None),
    "Skills routing": (
        f"{ANDROID}/main/java/com/openminis/app/data/repository/AltivProfessionalSkillRouting.kt", None),
    "Localhost address verification": (
        f"{ANDROID}/main/java/com/openminis/app/ui/terminal/LocalPreviewAddress.kt", None),
    "Backup export": (
        f"{ANDROID}/main/java/com/openminis/app/backup/BackupExporter.kt", None),
    "Backup import": (
        f"{ANDROID}/main/java/com/openminis/app/backup/BackupImporter.kt", None),
}
TEST_NAMES = (
    "AltivToolBrandingTest", "AltivVisualBuilderTest", "AltivVisualAuditTest",
    "AltivProjectMemoryTest", "AltivMemoryToolContractTest",
    "AltivSubscriptionPolicyTest", "AltivLicenseActivationContractTest",
    "MCPQuickConnectCatalogTest", "SupabaseMcpProjectScopeTest",
    "SupabaseMcpAccountScopeTest", "AltivGitHubPatConfigTest",
    "AltivProfessionalSkillRoutingTest", "LocalPreviewAddressTest",
    "AltivOnlineCostPolicyTest", "AltivCustomerFacingBrandTest",
    "AltivCustomerSettingsPolicyTest", "AltivSkillBundleArchiveTest",
)
WORKFLOW_MARKERS = (
    ":app:testAltivDebugUnitTest", ":app:assembleAltivDebug",
    "apksigner", "aapt", "sha256sum",
    "ALTIV_SIGNING_KEYSTORE_B64", "if-no-files-found: error",
)
MANUAL_ACCEPTANCE = (
    "license_denied_access", "license_valid_access", "project_memory_isolation",
    "nvidia_and_openrouter_keys", "github_mcp_authorized",
    "supabase_scoped_access", "skills_import_and_execution",
    "localhost_live_preview", "mobile_visual_qa_390",
    "desktop_visual_qa_1440", "image_subjects_checked",
    "backup_restore_tested", "persistent_signing_verified",
    "merged_pr_inventory_reviewed",
)

def collect_static_errors():
    errors = []
    for label, (rel, needle) in SOURCE_CHECKS.items():
        p = ROOT / rel
        if not p.is_file():
            errors.append(f"{label}: missing {rel}")
        elif needle and needle not in p.read_text(encoding="utf-8"):
            errors.append(f"{label}: missing expected contract in {rel}")
    workflow_path = ROOT / WORKFLOW
    if not workflow_path.exists():
        errors.append("Android CI workflow missing")
    else:
        workflow = workflow_path.read_text(encoding="utf-8")
        for name in TEST_NAMES:
            if f".{name}'" not in workflow:
                errors.append(f"Android CI does not run {name}")
        for marker in WORKFLOW_MARKERS:
            if marker not in workflow:
                errors.append(f"Android CI missing {marker}")
    return errors

def release_errors(args):
    errors = []
    if os.environ.get("GITHUB_REF") != "refs/heads/main":
        errors.append("Final release must be reviewed from the main branch")
    if not args.evidence_file or not args.apk:
        return errors + ["Final release requires --evidence-file and --apk"]
    evidence_path, apk = Path(args.evidence_file), Path(args.apk)
    if not evidence_path.is_file() or not apk.is_file():
        return errors + ["QA evidence file and compiled APK must exist"]
    try:
        e = json.loads(evidence_path.read_text(encoding="utf-8"))
        expected_sha = os.environ.get("GITHUB_SHA", "")
        if not expected_sha or e.get("commit_sha") != expected_sha:
            errors.append("Evidence commit SHA does not match this main build")
        checksum = hashlib.sha256(apk.read_bytes()).hexdigest()
        if e.get("apk_sha256") != checksum:
            errors.append("Evidence APK SHA-256 does not match actual APK")
        if not all(e.get(key) is True for key in MANUAL_ACCEPTANCE):
            missing = [key for key in MANUAL_ACCEPTANCE if e.get(key) is not True]
            errors.append("Physical/manual validation incomplete: " + ", ".join(missing))
    except (OSError, ValueError) as exc:
        errors.append(f"Unreadable release evidence: {exc}")
    return errors

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--mode", choices=("candidate", "release"), required=True)
    p.add_argument("--evidence-file", default=None)
    p.add_argument("--apk", default=None)
    args = p.parse_args()
    errors = collect_static_errors()
    if args.mode == "release":
        errors.extend(release_errors(args))
    for err in errors:
        print(f"FAIL: {err}", file=sys.stderr)
    if errors:
        print(f"ALTIVE {args.mode}: BLOCKED ({len(errors)} missing checks)", file=sys.stderr)
        return 1
    if args.mode == "candidate":
        print("ALTIVE review candidate: contracts present; Android tests, APK packaging "
              "and physical QA are STILL required. NOT a customer release.")
    else:
        print("ALTIVE release evidence matched this main commit and APK. "
              "Confirm distribution authorization before publishing.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
