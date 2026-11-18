"""No-network regression checks for ALTIVE's licensed photo downloader."""
import runpy
from pathlib import Path
import unittest

MODULE = runpy.run_path(
    "src/android/app/src/main/assets/default_mount/usr/local/bin/altive-photo-search.py"
)

class PhotoSearchContract(unittest.TestCase):
    def test_only_licensed_commercial_reuse(self):
        for item in ("CC0 1.0", "CC BY 4.0", "CC BY-SA 4.0", "Public domain"):
            self.assertTrue(MODULE["usable_license"](item), item)
        for item in ("CC BY-NC 4.0", "CC BY-ND 4.0", "All rights reserved", ""):
            self.assertFalse(MODULE["usable_license"](item), item)

    def test_only_https_wikimedia_photos(self):
        good = "https://upload.wikimedia.org/wikipedia/commons/a/ab/animal.jpg"
        self.assertTrue(MODULE["trusted_photo_url"](good))
        for url in ("http://upload.wikimedia.org/a.jpg",
                    "https://upload.wikimedia.org.evil.test/a.jpg",
                    "https://example.com/a.jpg",
                    "https://upload.wikimedia.org/wikipedia/commons/a/icon.svg"):
            self.assertFalse(MODULE["trusted_photo_url"](url), url)

    def test_raster_photos_only_and_credits_preserved(self):
        page = {"title":"File:Dog grooming.jpg", "imageinfo":[{
            "mime":"image/jpeg", "url":"https://upload.wikimedia.org/wikipedia/commons/a/d/dog.jpg",
            "extmetadata":{"LicenseShortName":{"value":"CC BY 4.0"},
                           "Artist":{"value":"<b>Real author</b>"}}}]}
        image = MODULE["select_photo"](page)
        self.assertEqual("Real author", image["artist"])
        self.assertEqual("CC BY 4.0", image["license"])
        self.assertTrue(image["source_page"].startswith("https://commons.wikimedia.org/"))
        page["imageinfo"][0]["mime"] = "image/svg+xml"
        self.assertIsNone(MODULE["select_photo"](page))

    def test_destination_cannot_escape_customer_project(self):
        for path in (Path("../other-project"), Path("/tmp/other-project")):
            with self.assertRaises(ValueError):
                MODULE["run"](["dog"], path, 1)

if __name__ == "__main__":
    unittest.main()
