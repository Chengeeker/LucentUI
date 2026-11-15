# Gallery double-tap focus audit

The shared ReviewGallery implementation previously treated every point in the full-screen gesture layer as an image point. This made double taps in black letterbox areas zoom toward empty space. The demo does not have a separate gallery: GalleryHost presents the same shared component.

The fix computes the fitted image rectangle after the current scale, pan, and vertical drag. A tap inside that rectangle retains point-focused zoom; a tap outside it, or before an image is decoded, uses the viewport center. ContractsTest.galleryDoubleTapUsesImageBoundsAndCentersBlackBars covers letterbox center fallback, a real image point, the enlarged image bounds, and an unavailable bitmap.

The 0.2.4 build passed the demo and component modules' 25 unit tests, Release Lint, and ARM64 release build. Its APK signature, package, version, ABI, ZIP integrity, and 16 KiB alignment passed. No device or emulator run was performed for that build.