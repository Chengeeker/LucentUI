# Lucent UI

English · [简体中文](README.zh-CN.md)

Lucent UI is an open design system for Android, built with Jetpack Compose. It combines the structure and adaptive theming of Material 3 with frosted surfaces and liquid glass, shaped into a consistent set of components, interaction patterns, and reusable UI guidance.

It includes a public specification, a Compose module, and a demo app that brings the pieces together in realistic screens and flows.

[Try the demo](https://github.com/Chengeeker/LucentUI/releases/latest) · [Read the specification](docs/README.md) · [Integration guide](docs/integration.md)

## Design foundations

Lucent UI builds on [Material Design 3](https://m3.material.io/) and [Material 3 for Compose](https://developer.android.com/develop/ui/compose/designsystems/material3): color roles, typography, shapes, components, and adaptive light and dark themes provide a familiar foundation. The system extends these foundations with its own surface treatments, layouts, motion, and interaction rules.

Glass surfaces add depth while keeping content legible. Frosted headers sample the page beneath them; liquid glass navigation adds a refractive surface and responsive movement. The implementation uses [Kyant's AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) on Android 13 and later and [QWEA's Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android) on earlier supported versions. The frosted header has its own implementation and remains available independently of the liquid glass setting. See [glass and background sampling](docs/glass.md) for implementation details and device considerations.

## What the system covers

- **Theme and foundations:** light and dark themes, color, typography, spacing, shapes, and configurable appearance. See [foundations](docs/foundations.md) and [design tokens](docs/tokens.json).
- **Components and navigation:** settings rows, cards, dialogs, navigation surfaces, and their interaction states. See [components](docs/components.md).
- **Motion and feedback:** page transitions, gestures, haptic feedback, and an optional predictive back experience. See [motion](docs/motion.md).
- **Media:** an image viewer with source-to-viewer transitions, zoom and HDR labels, plus ordinary video playback controls. See [media](docs/media.md).
- **Complete flows:** examples for timelines, reading, comments, personalization, WebDAV and backup settings. See [screen patterns](docs/patterns.md) and [settings](docs/settings.md).

The specification describes visual properties alongside states and behavior, so a component can be evaluated in the context of a working screen. The demo illustrates these patterns; application-specific data, accounts, and storage remain the responsibility of the integrating app.

## Get started

Download the [ARM64 demo APK](https://github.com/Chengeeker/LucentUI/releases/latest) for Android 8.0 or later to explore the system. To use the components in your own project, start with the [integration guide](docs/integration.md).

- [`lucent-ui`](lucent-ui/) — Compose theme, reusable components, background sampling, and media UI.
- [`demo`](demo/) — working examples and complete interface flows.
- [`docs`](docs/) — detailed specifications, tokens, source notes, and [verification guidance](docs/quality.md).

## License and contributions

Lucent UI is released under the [MIT License](LICENSE). Selected implementations are adapted from Review and other open-source work; dependencies retain their respective licenses. See [attributions](NOTICE.md), [license notices](docs/licenses.md), and [source notes](docs/scope-and-sources.md).

Feedback and contributions are welcome. For a proposed component or interaction change, include the use case and update the specification and demo alongside the implementation.
