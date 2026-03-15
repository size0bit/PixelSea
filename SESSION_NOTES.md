# Session Notes

## Current Status

The project is past backlog/setup work. Gallery and Viewer are functional on device, build tooling has been upgraded, and the Gallery -> Viewer open flow has been migrated onto Compose shared-transition APIs. The current transition behavior is usable again, but still considered "good enough for now" rather than final-polish quality.

Completed areas:

- GitHub backlog metadata and issues were initialized for `size0bit/PixelSea`.
- Gradle wrapper files and wrapper JAR were added.
- Repository text encoding was normalized and `.editorconfig` was added.
- `.gradle-user-home/` was added to `.gitignore` so local Gradle cache can be kept without polluting git.
- MediaStore timestamp handling was fixed and then strengthened with filename-based time fallback.
- Gallery and Viewer were changed to share one full ordered photo collection instead of diverging data sources.
- Viewer return-to-gallery positioning was fixed.
- Permission states were modeled explicitly.
- Gallery and Viewer empty/error states were added.
- Gallery refresh on foreground resume was added so deleted or newly added media is reflected after returning to the app.
- MediaStore querying and `Photo` mapping were centralized.
- Image validation rules were consolidated.
- Unit tests were added for timestamp normalization, image validation, and date grouping.
- `ktlint`-based formatting and static checks were added.
- `README.md` was added with project/module overview and common commands.
- A right-side quick-scroll thumb was added to the Gallery and iteratively refined on device.
- Compose/Kotlin/KSP toolchain versions were upgraded so shared-transition APIs compile in all app/feature modules.
- Gallery -> Viewer navigation was reworked from a page-replacement / hand-rolled overlay approach to a Compose shared-transition based approach.
- Viewer now receives the initially clicked photo directly so cold-start open has a real first frame before the repository-backed photo list is ready.
- Gallery no longer keeps the old "temporarily hide the clicked grid cell" behavior that could leave blank cells after repeated open/close cycles.

## Current Product Behavior

Verified working behavior:

- App launches successfully from IDEA on test device.
- Gallery grid preview works.
- Viewer opens the correct image and can swipe through adjacent photos.
- Time grouping now reflects actual photo/screenshot time much more reliably.
- Returning from Viewer to Gallery restores location correctly in the tested cases.
- Deleting media from the system gallery and returning to the app refreshes the Gallery.
- Gallery quick-scroll thumb is now draggable and no longer flies to the top after dragging to the bottom and then dragging upward again.
- Quick-scroll date bubble is visible and currently renders normally.
- Gallery -> Viewer open/close behavior is basically usable again after the shared-transition migration.
- Repeated open/close no longer leaves the old obvious "blank grid cell until another click" bug from the temporary hidden-thumbnail logic.

Current quick-scroll behavior:

- No visible track; only a pill thumb is shown.
- Thumb appears while scrolling/dragging and auto-hides later.
- Dragging the thumb shows a date bubble.
- The thumb includes subtle up/down direction marks.
- Quick-scroll mapping now uses date-header anchors rather than every grid item.
- Target scroll updates were reduced to improve stability and perceived follow-through.

## Important Implementation Notes

Key technical decisions already in the codebase:

- Time ordering uses normalized `timestampMillis`.
- Timestamp resolution order is:
  1. `DATE_TAKEN`
  2. parsed timestamp from filename
  3. `DATE_ADDED`
  4. final fallback
- Gallery and Viewer both read from the same full ordered collection to avoid mismatched item ordering and wrong-image issues.
- Gallery grouping logic was moved out of the UI layer into reusable data-layer utilities with unit tests.
- Current Gallery -> Viewer structure:
  - `Gallery` is mounted all the time in `MainActivity`
  - `Viewer` is shown as an overlay when active
  - shared-element visibility is caller-managed from `MainActivity`
  - `Viewer` gets both `initialPhotoId` and `initialPhotoUri`
  - `Viewer` can render the clicked photo immediately before the full photo list finishes loading
- Image loading / transition stability details:
  - Gallery thumbnails use stable Coil memory cache keys: `thumb-{photoId}`
  - Viewer requests use `placeholderMemoryCacheKey("thumb-{photoId}")`
  - Clicking a thumbnail also pre-enqueues the full image request through Coil
- Quick scroll currently lives in:
  - [feature/gallery/src/main/kotlin/com/pixelsea/feature/gallery/ui/GalleryScreen.kt](./feature/gallery/src/main/kotlin/com/pixelsea/feature/gallery/ui/GalleryScreen.kt)
- Transition-related files now primarily live in:
  - [app/src/main/java/com/example/pixelsea/MainActivity.kt](./app/src/main/java/com/example/pixelsea/MainActivity.kt)
  - [feature/gallery/src/main/kotlin/com/pixelsea/feature/gallery/ui/GalleryScreen.kt](./feature/gallery/src/main/kotlin/com/pixelsea/feature/gallery/ui/GalleryScreen.kt)
  - [feature/viewer/src/main/kotlin/com/pixelsea/feature/viewer/ui/ViewerScreen.kt](./feature/viewer/src/main/kotlin/com/pixelsea/feature/viewer/ui/ViewerScreen.kt)

## Remaining Rough Edges

Known areas that may still need refinement:

- Quick-scroll hand feel is much better than before, but could still be tuned further if needed.
- Some files in the repo may still contain old mojibake/encoding residue outside the paths already cleaned.
- Terminal-side Gradle verification in this environment can be unreliable because of local sandbox/JDK/Android toolchain path issues, even when IDEA/device runs are fine.
- The current Gallery -> Viewer transition is serviceable but not fully polished:
  - cold-start first-open behavior improved, but transition feel can still be refined later
  - first-open smoothness and overall animation polish were intentionally deprioritized for now
- Shared-transition API usage is somewhat version-sensitive. If builds fail in `feature:gallery` or `feature:viewer`, check Compose animation API signatures before changing app logic.

## Environment Notes

Local toolchain paths verified in this workspace:

- JDK:
  - `C:\Users\EVOLUTION\.jdks\dragonwell-17.0.18`
  - Java binary: `C:\Users\EVOLUTION\.jdks\dragonwell-17.0.18\bin\java.exe`
- Local Gradle distribution already present:
  - `C:\Users\EVOLUTION\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat`
- Proxy expected for networked Gradle work:
  - local proxy endpoint: `127.0.0.1:7897`
  - set both HTTP and HTTPS proxy to that endpoint before running dependency resolution or wrapper downloads

Version state currently in the repo:

- AGP: `8.9.1`
- Kotlin: `1.9.25`
- KSP: `1.9.25-1.0.20`
- Compose BOM: `2024.09.00`
- Compose compiler extension: `1.5.15`
- Coil Compose: `2.5.0`

Compose convention plugin details:

- `build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt`
  - sets `kotlinCompilerExtensionVersion = "1.5.15"`
  - injects the Compose BOM into modules
  - injects `androidx.compose.animation:animation`
  - injects `material3`, `ui-tooling-preview`, and debug `ui-tooling`

Recommended terminal environment before Gradle commands:

```powershell
$env:JAVA_HOME='C:\Users\EVOLUTION\.jdks\dragonwell-17.0.18'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:HTTP_PROXY='http://127.0.0.1:7897'
$env:HTTPS_PROXY='http://127.0.0.1:7897'
$env:GRADLE_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'
```

If `gradlew.bat` tries to download the wrapper and fails, prefer the already unpacked local Gradle binary above.

Known command pattern that worked in this session:

```powershell
$env:JAVA_HOME='C:\Users\EVOLUTION\.jdks\dragonwell-17.0.18'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:HTTP_PROXY='http://127.0.0.1:7897'
$env:HTTPS_PROXY='http://127.0.0.1:7897'
$env:GRADLE_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897'
& 'C:\Users\EVOLUTION\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat' :app:compileDebugKotlin
```

## Recommended Next Actions

Most sensible next steps, in priority order:

1. Leave Gallery -> Viewer transition logic alone unless a concrete regression appears; it is currently functional enough.
2. If transition polish is revisited later, focus on first-open smoothness and animation feel, not basic correctness.
3. Return to higher-value Viewer polish:
   - double-tap zoom
   - pinch zoom
   - pan
   - tap to show/hide UI
4. Continue data-layer hardening:
   - add more filename timestamp parsing tests
   - add more grouping/sorting edge-case tests

## Useful Commands

Quality and tests:

```powershell
.\gradlew.bat formatKotlin lintKotlin
.\gradlew.bat :core:data:testDebugUnitTest
```

Common compile checks:

```powershell
.\gradlew.bat :feature:gallery:compileDebugKotlin
.\gradlew.bat :feature:viewer:compileDebugKotlin
.\gradlew.bat :app:compileDebugKotlin
```

## Suggested Resume Prompt

Use a prompt like this next time:

```text
Continue PixelSea from SESSION_NOTES.md. The Gallery/Viewer core flow and current shared-transition implementation are already compiling and basically usable. Do not restart the transition rewrite unless there is a concrete regression. Prefer moving to the next product task, such as Viewer gestures or Gallery filters.
```
