import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[2] / "src/android/app/src/main/assets/default_mount/usr/local/bin/altiv-github-upload.py"
spec = importlib.util.spec_from_file_location("altiv_github_upload", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class GitHubBinaryUploadTest(unittest.TestCase):
    def test_binary_blob_and_remote_verification(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "image.webp"
            data = b"\\x00\\xffWEBP\\x80"
            path.write_bytes(data)
            calls = []
            def fake_api(method, url, token, payload=None, retries=3):
                calls.append((method, url, payload))
                if "/git/ref/" in url: return {"object": {"sha": "old"}}
                if "/git/commits/old" in url: return {"tree": {"sha": "oldtree"}}
                if method == "POST" and url.endswith("/git/blobs"):
                    import base64
                    self.assertEqual(base64.b64decode(payload["content"]), data)
                    return {"sha": "blobsha"}
                if method == "POST" and url.endswith("/git/trees"): return {"sha": "newtree"}
                if method == "POST" and url.endswith("/git/commits"): return {"sha": "newcommit"}
                if method == "PATCH":
                    self.assertFalse(payload["force"])
                    return {"object": {"sha": "newcommit"}}
                if "/git/commits/newcommit" in url: return {"tree": {"sha": "newtree"}}
                if "/git/trees/newtree" in url:
                    return {"tree": [{"path": "public/image.webp", "type": "blob",
                                      "sha": "blobsha", "size": len(data)}]}
                raise AssertionError((method, url))
            with patch.object(module, "api", side_effect=fake_api):
                result = module.publish("owner/repo", "fix/binary", [(path, "public/image.webp")], "secret", "upload")
            self.assertEqual(result["status"], "committed")
            self.assertEqual(result["files"]["public/image.webp"][1], len(data))

    def test_reject_traversal(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "a.png"
            path.write_bytes(b"PNG")
            with patch.object(module, "api", return_value={"object": {"sha": "old"}}):
                with self.assertRaises((module.UploadError, KeyError)):
                    module.publish("owner/repo", "main", [(path, "../bad.png")], "secret", "upload")

    def test_reject_oversized(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "large.bin"
            with path.open("wb") as out:
                out.truncate(module.MAX_BYTES + 1)
            with patch.object(module, "api", side_effect=[
                {"object": {"sha": "old"}}, {"tree": {"sha": "tree"}}
            ]):
                with self.assertRaises(module.UploadError):
                    module.publish("owner/repo", "main", [(path, "large.bin")], "secret", "upload")


if __name__ == "__main__":
    unittest.main()
