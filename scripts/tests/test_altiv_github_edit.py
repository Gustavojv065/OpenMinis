"""Offline contract tests for ALTIV safe GitHub text edits."""
import base64
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

SCRIPT = (Path(__file__).resolve().parents[2] /
          "src/android/app/src/main/assets/default_mount/usr/local/bin/altiv-github-edit.py")
spec = importlib.util.spec_from_file_location("altiv_github_edit", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class SafeTextEditTests(unittest.TestCase):
    def setUp(self):
        self.original = "color: green; background: black;"
        self.calls = []
        self.sha = "old-blob-sha"

    def fake_api(self, method, url, token, payload=None):
        self.calls.append((method, url, payload))
        if method == "GET":
            body = self.original if not any(x[0] == "PUT" for x in self.calls) else self.updated
            return {"type": "file", "size": len(body), "sha": self.sha if body == self.original else "new-sha",
                    "content": base64.b64encode(body.encode()).decode()}
        if method == "PUT":
            self.assertEqual(payload["sha"], self.sha)
            self.updated = base64.b64decode(payload["content"]).decode()
            return {"content": {"sha": "new-sha"}, "commit": {"sha": "commit-123"}}
        raise AssertionError(method)

    def run_edit(self, old="color: green", new="color: red", count=1, apply=False):
        with patch.object(module, "api", side_effect=self.fake_api):
            return module.edit("owner/repo", "feat/test", "src/styles.css",
                               old, new, count, "fake-token", "test", apply)

    def test_preview_does_not_write(self):
        self.assertEqual(self.run_edit()["status"], "preview")
        self.assertFalse(any(x[0] == "PUT" for x in self.calls))

    def test_exact_replacement_preserves_other_content(self):
        result = self.run_edit(apply=True)
        self.assertEqual(result["commit"], "commit-123")
        self.assertEqual(self.updated, "color: red; background: black;")

    def test_missing_or_ambiguous_match_refuses_write(self):
        for old in ("missing", "color"):
            self.calls.clear()
            with self.assertRaises(module.EditError):
                self.run_edit(old=old, count=2)
            self.assertFalse(any(x[0] == "PUT" for x in self.calls))

    def test_path_traversal_refused(self):
        with self.assertRaises(module.EditError):
            module.edit("owner/repo", "main", "../.env", "a", "b", 1, "token", "test")
        self.assertEqual(self.calls, [])


if __name__ == "__main__":
    unittest.main()
