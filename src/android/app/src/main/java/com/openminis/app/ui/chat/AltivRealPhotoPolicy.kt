package com.openminis.app.ui.chat

/**
 * Default licensed-photo retrieval workflow for new/updated commercial sites.
 * This is an instruction to use real shell/browser tools, not permission to
 * touch unrelated projects, bypass image licenses or fabricate search results.
 */
internal object AltivRealPhotoPolicy {
    val instructions: String = """
[ALTIVE REAL PHOTO ASSETS — required for image-bearing web projects]
- Customer asks for real photos, a gallery, product/category imagery or a
  professional website where a photo is expected: SEARCH THE PUBLIC WEB for
  relevant actual photographs. Do not assume lack of assets means lack of
  internet. Generic SVG icons/emoji/gradient illustrations are NOT photos and
  must NOT silently replace requested photography.
- Prefer photos supplied and authorized by the customer. Otherwise use
  the bundled ALTIVE MULTI-SOURCE SEARCH (from the current project directory):
    python3 /usr/local/bin/altive-media-search.py --source auto --query "dog grooming bath" --niche pet-shop --role hero --match-any dog --match-any grooming --dest public/assets/real-photos --limit 3
  SOURCES: Wikimedia Commons and Openverse work with no API key.
  Pexels, Pixabay and Unsplash are OPTIONAL only when their OWN API keys
  are securely available in environment variables ALTIVE_PEXELS_API_KEY,
  ALTIVE_PIXABAY_API_KEY and ALTIVE_UNSPLASH_ACCESS_KEY. Do NOT use the
  OpenAI/Gemini/ChatGPT credential as a photo provider key. Do not solicit
  customer keys in chat, save them in the project or print them in logs.
  The customer can add the OPTIONAL key through ALTIVE Settings >
  Environment Variables (encrypted at rest and enabled for the sandbox);
  no Termux, shell instructions or key pasted into conversation are needed.
  Enabled environment keys are available to sandbox subprocesses: use only
  provider-specific credentials, and warn the customer to avoid untrusted
  project scripts while they are enabled.
  No key? Continue with Wikimedia. Do not pretend Pexels or Unsplash ran.
- Wikimedia, Pexels and Pixabay allow locally saved licensed images under
  their applicable terms; verify actual source, creator, license and any
  additional restrictions. Unsplash API is DIFFERENT: MUST hotlink the exact
  image URL from photo.urls, show photographer + Unsplash attribution, and
  trigger download_location when selecting an image for the site. Do not
  download Unsplash API images into local assets. Openverse results from
  untrusted hosts are DISCOVERY_ONLY pending review, never auto-downloaded.
- ALTIVE-PHOTO-CREDITS.json contains origin and visual_verdict=NOT_VERIFIED.
  It is not an automatic semantic QA pass. For EACH selected image call
  read_image with the expected subject AND business action using actual
  vision (native model or configured vision group). Reject mismatches and
  re-search with bounded tries; if vision unavailable report NOT_VERIFIED.
- Confirm that the final website visibly credits all sources that require
  attribution, and that its code loads local URLs or Unsplash hotlinked URLs
  as required. Never fake completed visual inspection.
- The legacy Wikimedia-only helper remains available for existing workflows:
    python3 /usr/local/bin/altive-photo-search.py --query "dog grooming" --query "cat portrait" --dest public/assets/real-photos --limit 3
  Translate/adapt English search terms to the requested subject and niche
  (pet shop, cars, houses, food, barbering, etc). Use multiple SPECIFIC queries
  for distinct sections. Never apply pet photos to another business type.
- FIRST determine business niche and each photo slot's meaning. For recognized
  niches pass --niche (e.g. --niche automotive-detailing) and use repeatable
  --exclude-any forbidden scene filters; the helper rejects off-topic metadata
  BEFORE downloading. Never search just "car" when the job is car detailing.
  Example:
    python3 /usr/local/bin/altive-photo-search.py --niche automotive-detailing --role hero --query "car detailing polish" --match-any polishing --match-any detailing --exclude-any "gas station" --limit 3
  For auto-detailing, fuel pumps and gas station architecture are WRONG even
  when a vehicle is visible. Photos must show relevant cleaning, polishing,
  coating or interior detailing. Metadata filtering is preliminary; call
  read_image where actual vision is available, not just HTTP 200.
- Tool must actually return DOWNLOADED and nonempty images. Read its JSON
  output; check every local file exists, has a recognizable image format and
  points to the correct subject. The project includes
  ALTIVE-PHOTO-CREDITS.json with source, author, license, and license URL.
- For downloaded media use project-local files (Vite URLs beginning
  /assets/real-photos/, not /public/assets/...). Exception: Unsplash
  API images MUST use direct Unsplash hotlinked URLs and explicit attribution.
  Use meaningful alt text, object-fit, stable aspect ratio, lazy loading
  and appropriately sized image variants for mobile performance.
- Wikimedia Commons only allows specific CC0/public-domain, CC BY and CC BY-SA
  sources through the helper. Retain the correct attribution in a visible
  credits page/footer or accompanying attribution file when license requires it.
  Respect share-alike and other license conditions; do not scrape Google
  Images or treat every internet image as free for commercial use.
- Photos from generic stock/media repositories must be labeled illustrative:
  NEVER claim a generic pet/house/vehicle/product represents the customer's
  own animal, listed property, exact stock item or location.
- If access is blocked or no compatible real images are found, try new
  relevant queries and available browser/network tools, then report the ACTUAL
  failure, images missing and source limitations. Do not claim imagery
  "unavailable" without attempting searches. Do not fabricate download logs.
- After insertion run build and inspect actual browser images at mobile/desktop
  with decode status and screenshot-based SUBJECT comparison. A successful HTTP
  response, SVG icon or unrelated photo is NOT image validation.
- Preserve existing customer files, saved tokens and licenses. Do not use
  paid providers, purchase assets, bypass authentication, change host network
  security, or publish commercial assets without rights.
[END ALTIVE REAL PHOTO ASSETS]
    """.trimIndent()
}
