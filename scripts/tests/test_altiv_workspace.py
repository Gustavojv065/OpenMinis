"""Offline safety tests for ALTIV workspace operations."""
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = (Path(__file__).resolve().parents[2] /
          "src/android/app/src/main/assets/default_mount/usr/local/bin/altiv-workspace.py")
spec = importlib.util.spec_from_file_location("altiv_workspace", SCRIPT)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class WorkspaceTests(unittest.TestCase):
    def test_reject_repo_traversal(self):
        for value in ("../repo", "owner/../bad", "owner/repo/extra", "/absolute"):
            with self.assertRaises(m.WorkspaceError):
                m.workspace(value)

    def test_path_is_isolated(self):
        with tempfile.TemporaryDirectory() as d, patch.object(m, "ROOT", Path(d)):
            self.assertEqual(m.workspace("owner/repo"), Path(d) / "owner__repo")

    def test_missing_token_does_not_clone(self):
        with patch.dict(os.environ, {"ALTIV_GITHUB_MCP_TOKEN": "", "GITHUB_TOKEN": ""}):
            with self.assertRaises(m.WorkspaceError):
                m.git_auth()

    def test_reject_shell_command_replacing_source(self):
        with tempfile.TemporaryDirectory() as d:
            target = Path(d)
            (target / "index.tsx").write_text("git push origin main\\n")
            with self.assertRaises(m.WorkspaceError):
                m.verify_approved_content(target, ["index.tsx"])

    def test_reject_massive_source_deletion(self):
        import subprocess
        with tempfile.TemporaryDirectory() as d:
            target = Path(d)
            subprocess.run(["git", "init", "-q"], cwd=target, check=True)
            source = target / "index.tsx"
            source.write_text("export const example = 'hello';\\n" * 100)
            subprocess.run(["git", "add", "index.tsx"], cwd=target, check=True)
            subprocess.run(["git", "-c", "user.name=Test", "-c", "user.email=test@example.com",
                            "commit", "-qm", "initial"], cwd=target, check=True)
            source.write_text("export const example = 'short';\\n")
            with self.assertRaises(m.WorkspaceError):
                m.verify_approved_content(target, ["index.tsx"])

    def test_accept_small_safe_edit(self):
        import subprocess
        with tempfile.TemporaryDirectory() as d:
            target = Path(d)
            subprocess.run(["git", "init", "-q"], cwd=target, check=True)
            source = target / "index.tsx"
            source.write_text("export const example = 'hello';\\n" * 100)
            subprocess.run(["git", "add", "index.tsx"], cwd=target, check=True)
            subprocess.run(["git", "-c", "user.name=Test", "-c", "user.email=test@example.com",
                            "commit", "-qm", "initial"], cwd=target, check=True)
            source.write_text("export const example = 'world';\\n" * 100)
            m.verify_approved_content(target, ["index.tsx"])

    def test_diagnostic_explains_missing_credentials_without_leaking_secrets(self):
        import subprocess
        with tempfile.TemporaryDirectory() as d:
            target = Path(d)
            subprocess.run(["git", "init", "-q"], cwd=target, check=True)
            (target / "README.md").write_text("test")
            subprocess.run(["git", "add", "README.md"], cwd=target, check=True)
            subprocess.run(["git", "-c", "user.name=Test", "-c", "user.email=test@example.com",
                            "commit", "-qm", "initial"], cwd=target, check=True)
            with patch.dict(os.environ, {"ALTIV_GITHUB_MCP_TOKEN": "", "GITHUB_TOKEN": ""}):
                report = m.diagnose(target, "main")
            self.assertFalse(report["ready_for_publish"])
            self.assertEqual(report["checks"][0]["code"], "github_git_credentials")
            self.assertFalse(report["checks"][0]["ok"])
            self.assertIn("Reconectar GitHub", report["checks"][0]["suggested_fix"])

    def test_publication_plan_is_read_only_and_hashes_exact_bytes(self):
        import subprocess
        import hashlib
        with tempfile.TemporaryDirectory() as d:
            target = Path(d)
            subprocess.run(["git", "init", "-q"], cwd=target, check=True)
            source = target / "index.tsx"
            source.write_text("export const title = 'old';\\n" * 30)
            subprocess.run(["git", "add", "index.tsx"], cwd=target, check=True)
            subprocess.run(["git", "-c", "user.name=Test", "-c", "user.email=test@example.com",
                            "commit", "-qm", "initial"], cwd=target, check=True)
            source.write_text("export const title = 'new';\\n" * 30)
            before = subprocess.check_output(["git", "status", "--porcelain"], cwd=target)
            report = m.publication_plan(target, "owner/repo", "fix/accessibility", ["index.tsx"])
            after = subprocess.check_output(["git", "status", "--porcelain"], cwd=target)
            self.assertEqual(before, after)
            self.assertFalse(report["approved"])
            self.assertEqual(report["files"][0]["sha256"], hashlib.sha256(source.read_bytes()).hexdigest())
            with self.assertRaises(m.WorkspaceError):
                m.publication_plan(target, "owner/repo", "main", ["index.tsx"])
            with self.assertRaises(m.WorkspaceError):
                m.publication_plan(target, "owner/repo", "fix/accessibility", ["README.md"])

    def test_framework_detection_and_dirty_status(self):
        with tempfile.TemporaryDirectory() as d:
            target = Path(d)
            (target / ".git").mkdir()
            (target / "index.html").write_text("<h1>test</h1>")
            with patch.object(m, "run", side_effect=["feat/test", " M index.html"]):
                info = m.inspect(target)
            self.assertEqual(info["framework"], "html-static")
            self.assertTrue(info["dirty"])


if __name__ == "__main__":
    unittest.main()
