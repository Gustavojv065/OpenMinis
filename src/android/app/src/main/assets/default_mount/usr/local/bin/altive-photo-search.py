#!/usr/bin/env python3
"""ALTIVE DEV AI: source real, commercially reusable photographs automatically.

Runs inside Alpine ARM64, no paid keys or Termux. Example:
  python3 /usr/local/bin/altive-photo-search.py --query "dog grooming" \
    --query "cat portrait" --dest public/assets/real-photos --limit 3

Only downloads JPG, PNG and WebP from Wikimedia Commons with machine-readable
CC0, public domain, CC BY or CC BY-SA licenses. Saves credits with the project.
Never assumes a photo is semantically correct without a visual review.
"""
import argparse
import hashlib
from html import unescape
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import sys
import unicodedata
from urllib.parse import quote, urlencode, urlparse
from urllib.request import Request, build_opener, HTTPRedirectHandler

API = "https://commons.wikimedia.org/w/api.php"
USER_AGENT = "ALTIVEDevAI/1.0 (https://github.com/Gustavojv065/OpenMinis; licensed asset search)"
MAX_BYTES = 8 * 1024 * 1024

class PlainText(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.fragments = []
    def handle_data(self, data):
        self.fragments.append(data)

def clean(value, max_len=260):
    parser = PlainText()
    parser.feed(unescape(str(value or "")))
    return re.sub(r"\s+", " ", "".join(parser.fragments)).strip()[:max_len]

def field(metadata, key):
    return clean(metadata.get(key, {}).get("value", ""))

def usable_license(name):
    value = name.casefold().strip()
    return (value in ("cc0", "cc0 1.0", "public domain", "public domain mark 1.0")
        or re.fullmatch(r"cc by(?:-sa)?\s+(?:[1-4](?:\.\d)?)", value) is not None)

def matches_subject_metadata(title, description, required_keywords):
    """Conservative topic pre-filter, never a substitute for image vision.

    If a specific subject is required, don't pick the first unrelated file
    merely because Wikimedia's search API returned it. Use Unicode word
    boundaries and normalised accent marks to match user-specified synonyms.
    """
    if not required_keywords:
        return True
    text = unicodedata.normalize("NFKD", f"{title} {description}").casefold()
    text = "".join(ch for ch in text if not unicodedata.combining(ch))
    words = set(re.findall(r"[a-z0-9]+", text))
    for phrase in required_keywords:
        term = unicodedata.normalize("NFKD", phrase).casefold()
        term = "".join(ch for ch in term if not unicodedata.combining(ch))
        required = re.findall(r"[a-z0-9]+", term)
        if required and all(word in words for word in required):
            return True
    return False


def trusted_photo_url(url):
    try:
        u = urlparse(url)
        return (u.scheme == "https" and u.hostname == "upload.wikimedia.org"
            and u.username is None and u.password is None and u.port in (None, 443)
            and u.path.lower().endswith((".jpg", ".jpeg", ".png", ".webp")))
    except (ValueError, TypeError):
        return False

class WikimediaRedirectGuard(HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        host = urlparse(newurl).hostname
        if host != urlparse(request.full_url).hostname or urlparse(newurl).scheme != "https":
            raise ValueError("Cross-site media redirect blocked")
        return super().redirect_request(request, fp, code, msg, headers, newurl)

opener = build_opener(WikimediaRedirectGuard())

def get_json(url):
    request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
    with opener.open(request, timeout=20) as response:
        if response.status != 200:
            raise RuntimeError(f"Commons HTTP {response.status}")
        payload = response.read(2_000_001)
    if len(payload) > 2_000_000:
        raise RuntimeError("Oversized Commons response")
    return json.loads(payload)

def candidates(query):
    params = {
        "action": "query", "generator": "search", "gsrsearch": query,
        "gsrnamespace": 6, "gsrlimit": 25, "prop": "imageinfo",
        "iiprop": "url|mime|extmetadata|size", "iiurlwidth": 1200,
        "format": "json", "formatversion": 2,
    }
    body = get_json(API + "?" + urlencode(params))
    if body.get("error"):
        raise RuntimeError("Commons API error: " + clean(body["error"].get("info", "")))
    return body.get("query", {}).get("pages", [])

def select_photo(page):
    info = (page.get("imageinfo") or [None])[0]
    if not info or info.get("mime") not in ("image/jpeg", "image/png", "image/webp"):
        return None
    meta = info.get("extmetadata", {})
    license_name = field(meta, "LicenseShortName")
    if not usable_license(license_name):
        return None
    url = info.get("thumburl") or info.get("url") or ""
    if not trusted_photo_url(url):
        return None
    title = page.get("title", "")
    if not title.startswith("File:"):
        return None
    return {
        "title": title, "description": field(meta, "ImageDescription"),
        "url": url, "artist": field(meta, "Artist") or
        field(meta, "Credit") or "Autor não identificado",
        "license": license_name, "license_url": field(meta, "LicenseUrl"),
        "source_page": "https://commons.wikimedia.org/wiki/" +
            quote(title.replace(" ", "_"), safe=":()/"),
        "ext": (".jpg" if info["mime"] == "image/jpeg" else
            ".png" if info["mime"] == "image/png" else ".webp"),
    }

def download_photo(photo, dest, relative):
    filename = "commons-" + hashlib.sha256(photo["title"].encode()).hexdigest()[:16] + photo["ext"]
    output = dest / filename
    if not output.exists():
        request = Request(photo["url"], headers={"User-Agent": USER_AGENT})
        with opener.open(request, timeout=35) as response:
            if not trusted_photo_url(response.geturl()):
                raise ValueError("Untrusted image URL")
            if not response.headers.get_content_type().startswith("image/"):
                raise ValueError("Media response was not an image")
            length = response.headers.get("Content-Length")
            if length and int(length) > MAX_BYTES:
                raise ValueError("Image size exceeds 8 MB")
            data = response.read(MAX_BYTES + 1)
        if not data or len(data) > MAX_BYTES:
            raise ValueError("Image empty or exceeds 8 MB")
        # Never overwrite an existing client asset.
        with output.open("xb") as handle:
            handle.write(data)
    return str(relative / filename).replace("\\", "/")

def run(queries, destination, per_query, match_any=(), role=""):
    if destination.is_absolute() or ".." in destination.parts or not destination.parts:
        raise ValueError("Destination must remain inside the current project")
    cwd = Path.cwd().resolve()
    dest = (cwd / destination).resolve()
    if not dest.is_relative_to(cwd):
        raise ValueError("Destination escapes project workspace")
    dest.mkdir(parents=True, exist_ok=True)
    assets, errors, seen = [], [], set()
    rejected_off_topic = 0
    for query in queries:
        found = 0
        try:
            pages = candidates(query)
        except Exception as exc:
            errors.append({"query": query, "error": str(exc)[:180]})
            continue
        for page in pages:
            photo = select_photo(page)
            if not photo or photo["title"] in seen:
                continue
            if not matches_subject_metadata(
                photo["title"], photo.get("description", ""), match_any
            ):
                rejected_off_topic += 1
                continue
            try:
                path = download_photo(photo, dest, destination)
                assets.append({"query": query, "path": path,
                    "visual_verdict": "NOT_VERIFIED", "role": role, **{
                    key: photo[key] for key in
                    ("title", "artist", "license", "license_url", "source_page")}})
                seen.add(photo["title"])
                found += 1
            except Exception as exc:
                errors.append({"query": query, "error": f"Skipped photo: {str(exc)[:130]}"})
            if found >= per_query:
                break
    manifest = cwd / "ALTIVE-PHOTO-CREDITS.json"
    previous = []
    if manifest.exists():
        try:
            previous = json.loads(manifest.read_text(encoding="utf-8"))
            if not isinstance(previous, list):
                raise ValueError("Manifest is not a list")
        except (ValueError, OSError):
            errors.append({"error": "Existing credits manifest invalid: preserved, not overwritten."})
            return {"status": "ERROR", "images": assets, "errors": errors}
    merged = {item["path"]: item for item in previous
        if isinstance(item, dict) and isinstance(item.get("path"), str)}
    merged.update({item["path"]: item for item in assets})
    manifest.write_text(json.dumps(list(merged.values()), indent=2,
        ensure_ascii=False) + "\n", encoding="utf-8")
    return {"status": "DOWNLOADED" if assets else "NO_LICENSED_PHOTOS",
        "images": assets, "errors": errors, "credits": manifest.name,
        "rejected_off_topic": rejected_off_topic,
        "note": "Subject relevance and actual browser rendering still require verification."}

def main():
    parser = argparse.ArgumentParser(description="Find genuine license-checked web photos")
    parser.add_argument("--query", action="append", required=True,
        help="Subject in English; repeat for different topics")
    parser.add_argument("--dest", default="public/assets/real-photos", type=Path)
    parser.add_argument("--limit", type=int, default=3, help="Per-query images (1..8)")
    parser.add_argument("--match-any", action="append", default=[],
        help="A required visible subject keyword or synonym from metadata; repeat")
    parser.add_argument("--role", default="",
        help="Intended image slot: hero, espresso card, coffee gallery, etc.")
    args = parser.parse_args()
    if not 1 <= args.limit <= 8 or not 1 <= len(args.query) <= 8:
        parser.error("Use 1..8 queries and 1..8 photos per query")
    try:
        result = run(args.query, args.dest, args.limit,
            match_any=args.match_any, role=args.role)
    except Exception as exc:
        result = {"status": "ERROR", "images": [], "errors": [str(exc)]}
    print(json.dumps(result, indent=2, ensure_ascii=False))
    return 0 if result["status"] == "DOWNLOADED" else 2

if __name__ == "__main__":
    sys.exit(main())
