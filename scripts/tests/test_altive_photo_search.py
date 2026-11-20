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

    def test_semantic_topic_prefilter_rejects_off_topic_photos(self):
        match = MODULE["matches_subject_metadata"]
        expected = ["espresso", "cafe", "coffee", "barista", "latte"]
        self.assertFalse(match("File:New York City skyline.jpg",
            "Aerial view of high rise buildings", expected))
        self.assertFalse(match("File:Cruise ship.jpg",
            "Ocean cruise vessel at sea", expected))
        self.assertFalse(match("File:Bench in park.jpg",
            "Empty park bench", expected))
        self.assertTrue(match("File:Espresso cup.jpg",
            "Coffee prepared by a barista", expected))
        self.assertTrue(match("File:Coffee roasting.jpg",
            "Specialty beans", expected))
        self.assertTrue(match("File:Café interior.jpg", "", expected))
        # No keywords is valid for generic requests and remains unscreened.
        self.assertTrue(match("File:Random.jpg", "", []))

    def test_slot_requires_specific_subject_not_just_business_keyword(self):
        match = MODULE["matches_subject_metadata"]
        self.assertFalse(match("File:Coffee shop interior.jpg",
            "Coffee tables and chairs", ["espresso"]))
        self.assertTrue(match("File:Espresso in a cup.jpg",
            "Drink", ["espresso"]))
        self.assertFalse(match("File:Boat.jpg", "Lake", ["cat", "kitten", "feline"]))
        self.assertTrue(match("File:Cat portrait.jpg", "Domestic cat", ["cat", "kitten"]))

    def test_automotive_detailer_rejects_gas_station_metadata(self):
        forbidden = MODULE["NICHE_EXCLUSIONS"]["automotive-detailing"]
        reject = MODULE["matches_excluded_metadata"]
        self.assertTrue(reject("File:Gas station.jpg",
            "Cars at a fuel pump", forbidden))
        self.assertTrue(reject("File:Petrol-station parking.jpg",
            "Car parked at petrol station", forbidden))
        self.assertTrue(reject("File:Car polishing at gas station.jpg",
            "Detailing beside gasoline station", forbidden))
        self.assertFalse(reject("File:Car polishing.jpg",
            "A worker uses a polisher on black paint", forbidden))
        self.assertFalse(reject("File:Auto detailing.jpg",
            "Ceramic coating application", forbidden))

    def test_negative_filter_uses_whole_words_not_substrings(self):
        reject = MODULE["matches_excluded_metadata"]
        self.assertFalse(reject("File:Fuel.jpg", "Gastronomy",
            ["gas"]))
        self.assertTrue(reject("File:Gas station.jpg", "",
            ["gas station"]))
        self.assertTrue(reject("File:Fuel pump.jpg", "",
            ["fuel pump"]))
        self.assertFalse(reject("File:Coffee cup.jpg", "",
            ["gas station"]))

    def test_other_niche_filters_are_available(self):
        niches = MODULE["NICHE_EXCLUSIONS"]
        for item in ("barbershop", "pet-shop", "coffee-shop", "fashion-store"):
            self.assertIn(item, niches)
            self.assertTrue(niches[item])
        self.assertFalse(MODULE["matches_excluded_metadata"](
            "File:Car ceramic coating.jpg", "",
            niches["automotive-detailing"]))

    def test_destination_cannot_escape_customer_project(self):
        for path in (Path("../other-project"), Path("/tmp/other-project")):
            with self.assertRaises(ValueError):
                MODULE["run"](["dog"], path, 1)

if __name__ == "__main__":
    unittest.main()
