# Brand guides

`brand-guides.json` is downloaded by the app's How-to screen (screen 8) from this repo's `main` branch:
`https://raw.githubusercontent.com/tahseen137/alokchitri/main/guides/brand-guides.json`.
Editing it here changes the guide for every user without an app update. If the download fails, the app shows
the last copy it downloaded, or else the generic steps bundled in `strings.xml`.

- Keys under `brands` are the app's `PhoneBrand` names (`XIAOMI`, `SAMSUNG`, `REALME`).
- Each step has `bn` and `en` text and an optional `screenshot` URL (shown in an 84 × 112 dp box, cropped to fill).
- Bump `version` when the format changes; the app ignores files with a version it doesn't know.
- The menu names are a first draft from the stock camera apps. Check them on real phones before launch.
