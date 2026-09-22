# Shmesh Google Play ASO package

Prepared 21 September 2026 for `com.bihstudio.uvindex`.

Start with [COPY-REVIEW.md](COPY-REVIEW.md) to review all 15 language listings. Each locale folder contains plain UTF-8 title, short description, full description and screenshot-caption files. [listings.json](listings.json) contains the same copy in a structured format; it is not a Play Console import format.

This package is local copy prepared for publication. No Play Console changes have been made. Screenshot captions are supplied; finished screenshot images are not included. Translations have not been reviewed by native speakers.

## Positioning

Lead with local UV forecasts for everyday outdoor planning. Beach days are a use case, alongside walks and travel. The title identifies UV functionality; descriptions explain the forecast, search, reminders and widget. These are editorial keyword choices, not measured search-volume findings.

The code schedules reminders before forecast daily peaks, so the copy says peak reminders rather than threshold-based high-UV alerts. Forecast maxima are not described as safe or recommended exposure times. The copy makes no claims about tanning duration, skin-type personalization, live UV measurement, or guaranteed protection.

## Publish the copy

1. Open Shmesh in Google Play Console and open its main store listing under Store presence. Keep the existing default language unless you intend to change it.
2. Copy the matching locale's `title.txt`, `short-description.txt` and `full-description.txt` into the three fields. Add the other languages through the listing's translation controls.
3. Use Portuguese (Portugal) for `pt-PT`. The Spanish copy uses Spain wording. Separate Brazilian Portuguese and Latin American Spanish listings are not included.
4. Add localized screenshots using the plan below. Check Hebrew and Arabic captions in right-to-left layouts and keep the Shmesh brand name readable.
5. Preview each listing and save the changes for review. Publish the seven newly supported languages' listings alongside an app release that contains their language packs: Italian, Swedish, Bulgarian, Greek, Portuguese, Croatian and Turkish. The descriptions state support for 15 languages.

## Screenshot plan

Use real captures of the updated app, with its selected language matching the listing. Place each numbered caption above its corresponding screenshot. Keep app content legible; avoid decorative beach photography that hides the interface.

| Order | Caption file line | Capture |
| --- | --- | --- |
| 1 | Local UV overview | Main screen showing a location, current UV value and level |
| 2 | Hourly changes | UV chart and hourly forecast |
| 3 | Peak reminders | An actual Shmesh peak notification; remove personal notification content |
| 4 | Home-screen UV | Installed Shmesh widget on a clean launcher screen |

Show an actual available forecast rather than inventing values. The copy does not imply that the app contains a dedicated beach finder. The existing feature graphic in the parent folder has not been audited or modified by this task.

## Measure and improve

Record store visitors, acquisitions and conversion rate by language/country before applying changes. In Play Console's store listing experiments, test one change at a time, starting with the first screenshot. A useful first comparison is the current local-UV overview versus the hourly forecast as the lead image. Keep other assets unchanged during that experiment.

Wait for the experiment's result rather than selecting a winner from a few installs. Check retained-user results where available. Compare similar periods and traffic sources; seasonality and marketing changes can affect beach-related demand. No increase in downloads is guaranteed.

## Validation

Run `python docs/play-store/aso/validate.py` from the Android project root. It checks all 15 locales, file/JSON agreement, 30/80/4000-character limits, four captions per locale, and coverage of the app's language codes. It does not measure search demand or certify translation quality.

## Google references

- [Listing fields and translations](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en-GB)
- [Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842?hl=en)
- [Store listing best practices](https://support.google.com/googleplay/android-developer/answer/13393723?hl=en)
- [Preview assets](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en-GB)
- [Store listing experiments](https://support.google.com/googleplay/android-developer/answer/12053285?hl=en)
