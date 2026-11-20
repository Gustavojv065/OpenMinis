#!/usr/bin/env python3
"""ALTIVE Media Search: licensed multi-source photos for new web projects.

No API payments, account changes or unverified visual claims. In automatic
mode Wikimedia and Openverse work without keys; Pexels/Pixabay/Unsplash are
OPTIONAL and enabled only through environment credentials already configured.

Example (from the project workspace):
  python3 /usr/local/bin/altive-media-search.py --niche barbershop \
    --role hero --query "barber cutting hair" --match-any barber --limit 2

Source rules:
  Wikimedia: local approved CC/PD files, project credits.
  Pexels/Pixabay: local illustrative files, source credit, API key required.
  Unsplash: hotlink ONLY using returned photo.urls and download tracking.
  Openverse: metadata-only discovery unless a trusted licensed download path
             is explicitly implemented; never fetch arbitrary hosts.
The script cannot independently SEE photos: every result is NOT_VERIFIED until
a real vision-capable tool has inspected the downloaded file or screenshot.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import runpy
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request

commons = runpy.run_path(str(Path(__file__).with_name("altive-photo-search.py")))
USER_AGENT = "ALTIVEDevAI/2.0 (https://github.com/Gustavojv065/OpenMinis)"
MAX_IMAGE_BYTES = 8 * 1024 * 1024
MAX_JSON_BYTES = 2_000_000
PROVIDERS = ("pexels", "pixabay", "wikimedia", "unsplash", "openverse")
API_KEYS = {
    "pexels": "ALTIVE_PEXELS_API_KEY",
    "pixabay": "ALTIVE_PIXABAY_API_KEY",
    "unsplash": "ALTIVE_UNSPLASH_ACCESS_KEY",
}
TRUSTED_DOWNLOAD_HOSTS = {
    "pexels": {"images.pexels.com"},
    "pixabay": {"cdn.pixabay.com"},
    "wikimedia": {"upload.wikimedia.org"},
}
TRUSTED_API_HOSTS = {
    "pexels": "api.pexels.com",
    "pixabay": "pixabay.com",
    "unsplash": "api.unsplash.com",
    "openverse": "api.openverse.org",
}
LICENSE_URLS = {
    "pexels": "https://www.pexels.com/license/",
    "pixabay": "https://pixabay.com/service/license-summary/",
    "unsplash": "https://unsplash.com/license",
}


def safe_https(url, allowed_hosts, require_raster=False):
    try:
        parsed = urlparse(url)
        if (parsed.scheme != "https" or parsed.hostname not in allowed_hosts
                or parsed.username or parsed.password
                or parsed.port not in (None, 443)):
            return False
        if require_raster and not parsed.path.lower().endswith(
                (".jpg", ".jpeg", ".png", ".webp")):
            return False
        return True
    except (TypeError, ValueError):
        return False


def api_json(provider, url, key=""):
    """Read only documented provider endpoints, bounded response, no secrets in errors."""
    allowed = {TRUSTED_API_HOSTS[provider]}
    if not safe_https(url, allowed):
        raise ValueError("Invalid official provider API URL")
    headers = {"User-Agent": USER_AGENT, "Accept": "application/json"}
    if provider == "pexels":
        headers["Authorization"] = key
    elif provider == "unsplash":
        headers["Authorization"] = "Client-ID " + key
    try:
        with commons["opener"].open(Request(url, headers=headers), timeout=18) as response:
            if response.status != 200:
                raise RuntimeError("Provider response was not HTTP 200")
            raw = response.read(MAX_JSON_BYTES + 1)
            if len(raw) > MAX_JSON_BYTES:
                raise ValueError("Oversized provider response")
            return json.loads(raw)
    except HTTPError as exc:
        # Pixabay's query URL contains the secret: NEVER include raw URL/exception.
        raise RuntimeError("HTTP " + str(exc.code)) from None
    except (OSError, URLError, TimeoutError):
        raise RuntimeError("Network or provider unavailable") from None


def configured_providers(choice, environ):
    if choice != "auto":
        return [choice]
    return [p for p in PROVIDERS
            if p not in API_KEYS or environ.get(API_KEYS[p], "").strip()]


def normalize(provider, raw):
    """Normalize remote JSON into a reviewable image candidate."""
    if provider == "pexels":
        alt = str(raw.get("alt") or "")
        src = raw.get("src") or {}
        return dict(provider=provider, title=alt, description=alt,
            url=src.get("large") or src.get("large2x") or "",
            source_page=raw.get("url") or "", artist=raw.get("photographer") or "",
            license="Pexels License", license_url=LICENSE_URLS[provider],
            mode="download")
    if provider == "pixabay":
        tags = str(raw.get("tags") or "")
        return dict(provider=provider, title=tags, description=tags,
            url=raw.get("webformatURL") or "", source_page=raw.get("pageURL") or "",
            artist=raw.get("user") or "", license="Pixabay Content License",
            license_url=LICENSE_URLS[provider], mode="download")
    if provider == "unsplash":
        user = raw.get("user") or {}
        links = raw.get("links") or {}
        src = raw.get("urls") or {}
        title = str(raw.get("alt_description") or raw.get("description") or "")
        return dict(provider=provider, title=title, description=title,
            url=src.get("regular") or src.get("small") or "",
            source_page=links.get("html") or "",
            artist=user.get("name") or "",
            artist_url=(user.get("links") or {}).get("html") or "",
            license="Unsplash License", license_url=LICENSE_URLS[provider],
            mode="hotlink", download_location=links.get("download_location") or "")
    if provider == "openverse":
        license_code = str(raw.get("license") or "").lower()
        if license_code not in ("cc0", "pdm", "by", "by-sa"):
            return None
        return dict(provider=provider, title=str(raw.get("title") or ""),
            description=str(raw.get("title") or ""),
            url=raw.get("url") or "", source_page=raw.get("foreign_landing_url") or "",
            artist=raw.get("creator") or "",
            license=license_code + " " + str(raw.get("license_version") or ""),
            license_url=raw.get("license_url") or "",
            mode="review")
    if provider == "wikimedia":
        photo = commons["select_photo"](raw)
        if photo is None:
            return None
        return dict(provider=provider, title=photo["title"],
            description=photo["description"], url=photo["url"],
            source_page=photo["source_page"], artist=photo["artist"],
            license=photo["license"], license_url=photo["license_url"],
            mode="download")
    return None


def fetch_candidates(provider, query, environ):
    key = environ.get(API_KEYS.get(provider, ""), "").strip()
    if provider in API_KEYS and not key:
        raise ValueError(provider + " requires its own API key (not an OpenAI key)")
    if provider == "wikimedia":
        return [normalize(provider, p) for p in commons["candidates"](query)]
    if provider == "pexels":
        url = "https://api.pexels.com/v1/search?" + urlencode(
            {"query": query, "per_page": 25})
        items = api_json(provider, url, key).get("photos", [])
    elif provider == "pixabay":
        url = "https://pixabay.com/api/?" + urlencode({
            "key": key, "q": query, "image_type": "photo",
            "safesearch": "true", "per_page": 25,
        })
        items = api_json(provider, url, key).get("hits", [])
    elif provider == "unsplash":
        url = "https://api.unsplash.com/search/photos?" + urlencode({
            "query": query, "per_page": 25, "content_filter": "high",
        })
        items = api_json(provider, url, key).get("results", [])
    elif provider == "openverse":
        url = "https://api.openverse.org/v1/images/?" + urlencode({
            "q": query, "page_size": 25, "license": "cc0,pdm,by,by-sa",
        })
        items = api_json(provider, url).get("results", [])
    else:
        raise ValueError("Unknown provider")
    if not isinstance(items, list):
        raise ValueError("Invalid provider response")
    return [normalize(provider, item) for item in items[:25]
            if isinstance(item, dict)]


def image_format(data):
    if data.startswith(b"\xff\xd8\xff"):
        return ".jpg"
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return ".png"
    if len(data) > 12 and data.startswith(b"RIFF") and data[8:12] == b"WEBP":
        return ".webp"
    raise ValueError("Downloaded media is not a valid JPG/PNG/WebP")


def download_candidate(candidate, folder, destination):
    provider = candidate["provider"]
    url = candidate["url"]
    if not safe_https(url, TRUSTED_DOWNLOAD_HOSTS.get(provider, set()),
                      require_raster=True):
        raise ValueError("Untrusted image host or type")
    try:
        req = Request(url, headers={"User-Agent": USER_AGENT})
        with commons["opener"].open(req, timeout=25) as response:
            if not safe_https(response.geturl(),
                              TRUSTED_DOWNLOAD_HOSTS[provider], True):
                raise ValueError("Redirected outside trusted image host")
            if response.status != 200:
                raise ValueError("Image HTTP status not 200")
            if response.headers.get_content_type() not in (
                "image/jpeg", "image/png", "image/webp"):
                raise ValueError("Unexpected media MIME type")
            length = response.headers.get("Content-Length")
            if length and int(length) > MAX_IMAGE_BYTES:
                raise ValueError("Image too large")
            data = response.read(MAX_IMAGE_BYTES + 1)
    except HTTPError as exc:
        raise RuntimeError("Image HTTP " + str(exc.code)) from None
    except (OSError, URLError, TimeoutError):
        raise RuntimeError("Image download unavailable") from None
    if not data or len(data) > MAX_IMAGE_BYTES:
        raise ValueError("Image empty or too large")
    ext = image_format(data)
    filename = provider + "-" + hashlib.sha256(
        (url + candidate["title"]).encode()).hexdigest()[:16] + ext
    output = folder / filename
    if output.is_symlink():
        raise ValueError("Existing image path must not be a symlink")
    if output.exists():
        # A reused asset must still have valid content, not just a file name.
        if image_format(output.read_bytes()[:16]) != ext:
            raise ValueError("Existing image content does not match expected format")
    else:
        with output.open("xb") as handle:
            handle.write(data)
    return str(destination / filename).replace("\\", "/")


def register_unsplash_download(candidate, environ):
    url = candidate.get("download_location") or ""
    if not safe_https(url, {"api.unsplash.com"}):
        raise ValueError("Unsplash download tracking endpoint unavailable")
    api_json("unsplash", url, environ[API_KEYS["unsplash"]])


def quality_filter(candidate, match_any, excluded):
    if not candidate or not candidate.get("title"):
        return False
    provider = candidate["provider"]
    url = candidate.get("url") or ""
    if provider in TRUSTED_DOWNLOAD_HOSTS:
        if not safe_https(url, TRUSTED_DOWNLOAD_HOSTS[provider], True):
            return False
    elif provider == "unsplash":
        if not safe_https(url, {"images.unsplash.com"}):
            return False
    elif provider == "openverse":
        # Discovery only; arbitrary Openverse hosts must never be downloaded.
        if not safe_https(candidate.get("source_page") or "", set(
                ["commons.wikimedia.org", "www.flickr.com", "flickr.com",
                 "www.pexels.com", "pixabay.com", "unsplash.com"])):
            return False
    trusted_pages = {
        "pexels": {"www.pexels.com", "pexels.com"},
        "pixabay": {"pixabay.com", "www.pixabay.com"},
        "wikimedia": {"commons.wikimedia.org"},
        "unsplash": {"unsplash.com", "www.unsplash.com"},
    }
    if provider in trusted_pages and not safe_https(
            candidate.get("source_page") or "", trusted_pages[provider]):
        return False
    if not commons["matches_subject_metadata"](
            candidate["title"], candidate.get("description", ""), match_any):
        return False
    if commons["matches_excluded_metadata"](
            candidate["title"], candidate.get("description", ""), excluded):
        return False
    return True


def run(queries, destination, limit=3, source="auto", role="", niche="",
        match_any=(), exclude_any=(), environ=None):
    environ = environ if environ is not None else os.environ
    if source != "auto" and source not in PROVIDERS:
        raise ValueError("Invalid source")
    if not 1 <= len(queries) <= 8 or not 1 <= limit <= 8:
        raise ValueError("Query and per-query limits must be 1..8")
    if destination.is_absolute() or not destination.parts or ".." in destination.parts:
        raise ValueError("Destination must stay in project")
    cwd = Path.cwd().resolve()
    folder = (cwd / destination).resolve()
    if not folder.is_relative_to(cwd):
        raise ValueError("Destination escapes customer project")
    manifest = cwd / "ALTIVE-PHOTO-CREDITS.json"
    previous = []
    if manifest.exists():
        previous = json.loads(manifest.read_text(encoding="utf-8"))
        if not isinstance(previous, list) or any(
                not isinstance(x, dict) or "path" not in x for x in previous):
            raise ValueError("Invalid credits manifest: refusing overwrite")
    selected, review_only, errors, seen = [], [], [], set()
    forbidden = tuple(exclude_any) + commons["NICHE_EXCLUSIONS"].get(niche, ())
    providers = configured_providers(source, environ)
    for query in queries:
        count = 0
        for provider in providers:
            if count >= limit:
                break
            try:
                candidates = fetch_candidates(provider, query, environ)
            except (RuntimeError, ValueError):
                errors.append({"source": provider, "query": query,
                               "error": "No access, quota, or provider unavailable"})
                continue
            for c in candidates:
                if not quality_filter(c, match_any, forbidden):
                    continue
                if c["source_page"] in seen:
                    continue
                seen.add(c["source_page"])
                if c["mode"] == "review":
                    review_only.append({k: c.get(k) for k in (
                        "provider", "title", "source_page", "artist",
                        "license", "license_url")})
                    continue
                try:
                    if c["mode"] == "download":
                        folder.mkdir(parents=True, exist_ok=True)
                        path = download_candidate(c, folder, destination)
                        src = "/" + path.removeprefix("public/").lstrip("/")
                    else:
                        # Unsplash API requires a hotlinked URL and download event.
                        register_unsplash_download(c, environ)
                        path = "remote:" + c["url"]
                        src = c["url"]
                    selected.append({
                        "path": path, "src": src, "source": provider,
                        "title": c["title"], "role": role, "query": query,
                        "artist": c["artist"], "artist_url": c.get("artist_url", ""),
                        "license": c["license"], "license_url": c["license_url"],
                        "source_page": c["source_page"], "visual_verdict": "NOT_VERIFIED",
                        "requires_attribution": True,  # Always show source credits; required for Unsplash.
                        "delivery": c["mode"],
                    })
                    count += 1
                except (ValueError, RuntimeError, OSError):
                    errors.append({"source": provider, "query": query,
                                   "error": "Media rejected, tracking failed or download unavailable"})
                if count >= limit:
                    break
    if selected:
        merged = {str(item["path"]): item for item in previous}
        merged.update({x["path"]: x for x in selected})
        manifest.write_text(json.dumps(list(merged.values()), ensure_ascii=False,
            indent=2) + "\n", encoding="utf-8")
    return {
        "status": "FOUND_NOT_VISUALLY_VERIFIED" if selected else "REVIEW_ONLY"
                  if review_only else "NO_USABLE_MEDIA",
        "images": selected, "discovery_only": review_only[:12],
        "errors": errors, "sources_tried": providers,
        "credits": manifest.name if selected else None,
        "note": "Always run real read_image / screenshot semantic review before publishing. "
                "Unsplash images must remain hotlinked and show author/Unsplash attribution.",
    }


def main():
    parser = argparse.ArgumentParser(
        description="Search licensed multi-source photos. Never bypass provider quotas.")
    parser.add_argument("--query", action="append", required=True)
    parser.add_argument("--source", choices=("auto",) + PROVIDERS, default="auto")
    parser.add_argument("--dest", default="public/assets/real-photos", type=Path)
    parser.add_argument("--limit", type=int, default=3)
    parser.add_argument("--niche", choices=("",) + tuple(commons["NICHE_EXCLUSIONS"]),
                        default="")
    parser.add_argument("--role", default="")
    parser.add_argument("--match-any", action="append", default=[])
    parser.add_argument("--exclude-any", action="append", default=[])
    args = parser.parse_args()
    try:
        result = run(args.query, args.dest, args.limit, args.source, args.role,
            args.niche, args.match_any, args.exclude_any)
        print(json.dumps(result, indent=2, ensure_ascii=False))
        return 0 if result["images"] else 2
    except (ValueError, OSError, RuntimeError, json.JSONDecodeError):
        print(json.dumps({"status": "ERROR",
            "message": "Invalid project path or search configuration; no secrets disclosed."}))
        return 2


if __name__ == "__main__":
    sys.exit(main())
