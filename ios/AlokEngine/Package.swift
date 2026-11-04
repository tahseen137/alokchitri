// swift-tools-version:6.0
// The light-meter and recommendation rules, ported from the Android `engine` module.
// Pure Swift with no UIKit or AVFoundation, so `swift test` runs on any Mac without a simulator.
// Tests use Swift Testing (not XCTest), which ships with the Command Line Tools as well as Xcode.
import PackageDescription

let package = Package(
    name: "AlokEngine",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [.library(name: "AlokEngine", targets: ["AlokEngine"])],
    targets: [
        .target(name: "AlokEngine"),
        .testTarget(name: "AlokEngineTests", dependencies: ["AlokEngine"]),
    ],
    swiftLanguageModes: [.v5]
)
