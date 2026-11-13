# Lucent UI

An open design system for Android apps built with Jetpack Compose. Lucent UI pairs a public specification with reusable components and a demo app you can run.

[Read the design specification](docs/README.md) · [Download the Android demo](https://github.com/Chengeeker/LucentUI/releases/latest) · [Integrate Lucent UI](docs/integration.md)

## Explore the system

| Topic | What the specification covers |
| --- | --- |
| [Foundations](docs/foundations.md) | Color, typography, spacing, shape, and light and dark themes |
| [Components](docs/components.md) | Navigation, settings rows, dialogs, and interaction states |
| [Glass and motion](docs/glass.md) | Frosted headers, liquid glass navigation, and [transition and gesture behavior](docs/motion.md) |
| [Media](docs/media.md) | Image browsing, source-to-viewer transitions, HDR labels, and video controls |
| [Screen patterns](docs/patterns.md) | Timelines, reading, comments, and [settings and backup flows](docs/settings.md) |

## Use it

- [`lucent-ui`](lucent-ui/) contains the Compose theme, components, and media UI. Start with the [integration guide](docs/integration.md) to use them in an app.
- [`demo`](demo/) shows how the components work together in complete flows. Try navigation, media, and settings on Android 8.0 or later. The published APK targets ARM64 devices.
- [`docs`](docs/) contains the public specification, [design tokens](docs/tokens.json), and [verification guidance](docs/quality.md).

## License and contributions

Lucent UI is released under the [MIT License](LICENSE). Selected implementations draw from Review and other open-source work. See the [attributions](NOTICE.md) and [source notes](docs/scope-and-sources.md).

Feedback and contributions are welcome. For a proposed component or interaction change, include the use case and update the specification and demo alongside the implementation.
