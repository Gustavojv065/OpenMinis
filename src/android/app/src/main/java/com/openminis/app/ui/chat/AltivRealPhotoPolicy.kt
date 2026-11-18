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
- Prefer photos the customer supplied (preserving ownership and content).
  Otherwise use the bundled free Wikimedia Commons search utility in the
  currently active project directory, no API key or Termux required:
    python3 /usr/local/bin/altive-photo-search.py --query "dog grooming" --query "cat portrait" --dest public/assets/real-photos --limit 3
  Translate/adapt English search terms to the requested subject and niche
  (pet shop, cars, houses, food, barbering, etc). Use multiple SPECIFIC queries
  for distinct sections. Never apply pet photos to another business type.
- Tool must actually return DOWNLOADED and nonempty images. Read its JSON
  output; check every local file exists, has a recognizable image format and
  points to the correct subject. The project includes
  ALTIVE-PHOTO-CREDITS.json with source, author, license, and license URL.
- Use project-local downloaded assets instead of hotlinking remote sources.
  For Vite assets in public/assets/real-photos, use URLs beginning with
  /assets/real-photos/ (not /public/assets/...). Use meaningful alt text,
  CSS object-fit, stable aspect ratios and lazy loading where appropriate.
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
