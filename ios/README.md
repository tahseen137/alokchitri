# Alokchitri for iPhone

Native SwiftUI app, built in Xcode on a Mac. This folder grows one feature per pull request.

| Folder | What it is |
| --- | --- |
| `AlokEngine/` | Swift package: the light meter and recommendation rules, a line-for-line port of the Android `engine` module. No UIKit, so its tests run with `swift test`. |

## Keeping iPhone and Android in step

The Kotlin engine is the source of truth. `engine/golden/recommendations.txt` holds its answer for 774 inputs
(3 cameras × 6 goals × EV −3 to 17, plus colour-temperature and RAW-off cases).

- `./gradlew :engine:test` (repo root) fails if the Kotlin engine's output changes.
- `swift test` (in `ios/AlokEngine`) fails if the Swift port disagrees with that file, and prints the first differing line.

After an intended rule change: edit `RecommendationEngine.kt`, run `UPDATE_GOLDEN=1 ./gradlew :engine:test`,
make the same change in `RecommendationEngine.swift`, run `swift test`, and commit all three files together.

## Running the engine tests

```sh
cd ios/AlokEngine
swift test
```

Needs Swift 6 and Xcode 16 or newer. The tests use Swift Testing, not XCTest.

With only the Command Line Tools (no Xcode), point SwiftPM at their Testing framework:

```sh
CLT=/Library/Developer/CommandLineTools/Library/Developer
swift test -Xswiftc -F -Xswiftc $CLT/Frameworks \
  -Xlinker -rpath -Xlinker $CLT/Frameworks -Xlinker -rpath -Xlinker $CLT/usr/lib
```
