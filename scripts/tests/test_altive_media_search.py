"""Offline contract tests for ALTIVE's multi-source licensed media search."""
import os
from pathlib import Path
import runpy
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

PATH = ("src/android/app/src/main/assets/default_mount/usr/local/bin/"
        "altive-media-search.py")
M = runpy.run_path(PATH)


class MultiSourceMediaContract(unittest.TestCase):
    def test_auto_sources_do_not_require_expensive_providers(self):
        select = M["configured_providers"]
        self.assertEqual(["wikimedia", "openverse"], select("auto", {}))
        self.assertEqual(["pexels", "pixabay", "wikimedia", "unsplash",
                          "openverse"], select("auto", {
                              "ALTIVE_PEXELS_API_KEY": "test",
                              "ALTIVE_PIXABAY_API_KEY": "test",
                              "ALTIVE_UNSPLASH_ACCESS_KEY": "test",
                          }))

    def test_exact_media_hosts_and_https_only(self):
        safe = M["safe_https"]
        allowed = M["TRUSTED_DOWNLOAD_HOSTS"]["pexels"]
        self.assertTrue(safe("https://images.pexels.com/photos/a.jpg", allowed, True))
        for url in ("http://images.pexels.com/a.jpg",
                    "https://images.pexels.com.evil.test/a.jpg",
                    "https://evil.test/a.jpg",
                    "https://user:pw@images.pexels.com/a.jpg",
                    "https://images.pexels.com/a.svg",
                    "https://images.pexels.com:8080/a.jpg"):
            self.assertFalse(safe(url, allowed, True), url)

    def test_provider_metadata_and_commercial_license(self):
        normalize = M["normalize"]
        p = normalize("pexels", {"alt": "Barber cutting hair",
            "src": {"large": "https://images.pexels.com/photos/10.jpg"},
            "url": "https://www.pexels.com/photo/10", "photographer": "Test"})
        self.assertEqual("download", p["mode"])
        self.assertEqual("Pexels License", p["license"])
        self.assertTrue(M["quality_filter"](p, ["barber"], []))
        px = normalize("pixabay", {"tags": "barber, haircut",
            "webformatURL": "https://cdn.pixabay.com/photo/10.jpg",
            "pageURL": "https://pixabay.com/photos/10", "user": "Artist"})
        self.assertEqual("Pixabay Content License", px["license"])
        self.assertTrue(M["quality_filter"](px, ["barber"], []))
        self.assertFalse(M["quality_filter"](px, ["barber"], ["haircut"]))

    def test_unsplash_requires_hotlink_and_track(self):
        p = M["normalize"]("unsplash", {
            "alt_description": "Barber trimming beard",
            "urls": {"regular": "https://images.unsplash.com/photo-1?fit=crop"},
            "links": {"html": "https://unsplash.com/photos/a",
                      "download_location": "https://api.unsplash.com/photos/a/download"},
            "user": {"name": "Photographer", "links": {
                "html": "https://unsplash.com/@photographer"}},
        })
        self.assertEqual("hotlink", p["mode"])
        self.assertTrue(M["quality_filter"](p, ["barber"], []))
        self.assertIn("download_location", p)
        self.assertNotIn("download", M["TRUSTED_DOWNLOAD_HOSTS"].get("unsplash", {}))
        calls = []
        with patch.dict(M["register_unsplash_download"].__globals__,
                        {"api_json": lambda source, url, key: calls.append(
                            (source, url, key))}):
            M["register_unsplash_download"](p, {
                "ALTIVE_UNSPLASH_ACCESS_KEY": "private-test-key"})
        self.assertEqual(1, len(calls))
        self.assertTrue(calls[0][1].endswith("/download"))

    def test_openverse_is_discovery_only_and_rejects_nc(self):
        normalize = M["normalize"]
        image = {"title": "Haircut", "license": "by",
                 "license_version": "4.0", "creator": "Someone",
                 "foreign_landing_url": "https://www.flickr.com/photos/a/123",
                 "url": "https://untrusted.test/photo.jpg"}
        p = normalize("openverse", image)
        self.assertEqual("review", p["mode"])
        self.assertTrue(M["quality_filter"](p, ["haircut"], []))
        image["license"] = "by-nc"
        self.assertIsNone(normalize("openverse", image))

    def test_image_magic_bytes(self):
        check = M["image_format"]
        self.assertEqual(".jpg", check(b"\xff\xd8\xffok"))
        self.assertEqual(".png", check(b"\x89PNG\r\n\x1a\nok"))
        self.assertEqual(".webp", check(b"RIFFxxxxWEBPok"))
        for data in (b"<svg>not an image</svg>", b"GIF89a", b"", b"<!DOCTYPE html>"):
            with self.assertRaises(ValueError):
                check(data)

    def test_auto_creates_credits_but_no_api_secrets(self):
        photo = {"provider": "pexels", "title": "Barber cutting hair",
                 "description": "Barber cutting hair",
                 "url": "https://images.pexels.com/photos/a.jpg",
                 "source_page": "https://www.pexels.com/photo/a",
                 "artist": "Artist", "license": "Pexels License",
                 "license_url": "https://www.pexels.com/license/",
                 "mode": "download"}
        fn = M["run"]
        with TemporaryDirectory() as tmp:
            original = Path.cwd()
            try:
                os.chdir(tmp)
                with patch.dict(fn.__globals__, {
                    "fetch_candidates": lambda provider, query, env: [photo]
                        if provider == "pexels" else [],
                    "download_candidate": lambda candidate, folder, destination:
                        "public/assets/real-photos/test.jpg",
                }):
                    result = fn(["barber hair"], Path("public/assets/real-photos"),
                        limit=1, source="auto", role="hero", match_any=("barber",),
                        environ={"ALTIVE_PEXELS_API_KEY": "NEVER-STORE-THIS-KEY"})
                self.assertEqual("FOUND_NOT_VISUALLY_VERIFIED", result["status"])
                self.assertEqual("NOT_VERIFIED",
                                 result["images"][0]["visual_verdict"])
                self.assertEqual("/assets/real-photos/test.jpg",
                                 result["images"][0]["src"])
                contents = Path("ALTIVE-PHOTO-CREDITS.json").read_text()
                self.assertNotIn("NEVER-STORE-THIS-KEY", contents)
                self.assertIn("Pexels License", contents)
            finally:
                os.chdir(original)

    def test_unsplash_tracking_failure_never_marks_selected(self):
        photo = M["normalize"]("unsplash", {
            "alt_description": "Barber cutting hair",
            "urls": {"regular": "https://images.unsplash.com/photo-a"},
            "links": {"html": "https://unsplash.com/photos/a",
                      "download_location": "https://api.unsplash.com/photos/a/download"},
            "user": {"name": "Photographer"}})
        fn = M["run"]
        with TemporaryDirectory() as tmp:
            original = Path.cwd()
            try:
                os.chdir(tmp)
                with patch.dict(fn.__globals__, {
                    "fetch_candidates": lambda *_: [photo],
                    "register_unsplash_download": lambda *_:
                        (_ for _ in ()).throw(RuntimeError("tracking failed")),
                }):
                    result = fn(["barber"], Path("public/assets/real-photos"),
                        limit=1, source="unsplash", match_any=("barber",),
                        environ={"ALTIVE_UNSPLASH_ACCESS_KEY": "test"})
                self.assertEqual([], result["images"])
                self.assertFalse(Path("ALTIVE-PHOTO-CREDITS.json").exists())
            finally:
                os.chdir(original)

    def test_escape_from_project_is_rejected(self):
        run = M["run"]
        for p in (Path("../client-b"), Path("/tmp/other-client")):
            with self.assertRaises(ValueError):
                run(["barber"], p, environ={})


if __name__ == "__main__":
    unittest.main()
