# Lucent UI

[English](README.md) · 简体中文

Lucent UI 是一套面向 Android、使用 Jetpack Compose 构建的开放设计系统。它将 Material 3 的界面结构和自适应主题与磨砂、液态玻璃效果结合，形成一套包含组件、交互模式和可复用设计规范的 UI 方案。

项目提供公开设计规范、Compose 组件模块和 Demo 应用，在完整页面及流程中展示这些设计。

[体验 Demo](https://github.com/Chengeeker/LucentUI/releases/latest) · [阅读详细设计规范](docs/README.md) · [接入指南](docs/integration.md)

## 设计基础

Lucent UI 基于 [Material Design 3](https://m3.material.io/) 和 [Compose Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3)，以颜色角色、字体、形状、组件及自适应明暗主题为基础，进一步扩展表面效果、布局、动效和交互规则。

玻璃效果在保持内容清晰的同时增加界面层次。磨砂顶栏采样下方页面内容；液态玻璃导航通过折射效果和响应式运动呈现玻璃质感。Android 13 及以上使用 [Kyant 的 AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)，较早的受支持系统使用 [QWEA 的 Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android)。磨砂顶栏采用独立实现，关闭液态玻璃后仍可使用。实现方式和设备注意事项见[玻璃与背景采样](docs/glass.md)。

## 设计系统包含什么

- **主题与基础规范：** 明暗主题、颜色、字体、间距、形状和外观设置。参见[基础规范](docs/foundations.md)和[设计变量](docs/tokens.json)。
- **组件与导航：** 设置行、卡片、弹窗、导航表面及其交互状态。参见[组件](docs/components.md)。
- **动效与反馈：** 页面切换、手势、触感反馈，以及可选的可预测性返回。参见[动效与手势](docs/motion.md)。
- **媒体浏览：** 支持源图过渡、缩放和 HDR 标签的图片浏览器，以及普通视频播放控件。参见[媒体浏览](docs/media.md)。
- **完整流程：** 动态、阅读、评论、个性化、WebDAV 和备份设置等示例。参见[页面模式](docs/patterns.md)和[设置](docs/settings.md)。

规范同时描述视觉属性、组件状态和交互行为，方便结合实际页面理解和评估组件。Demo 展示这些模式；接入应用需要自行处理业务数据、账号和存储。

## 开始使用

下载适用于 Android 8.0 及以上设备的 [ARM64 Demo 安装包](https://github.com/Chengeeker/LucentUI/releases/latest)，体验设计系统。需要在自己的项目中使用组件时，请先阅读[接入指南](docs/integration.md)。

- [`lucent-ui`](lucent-ui/)：Compose 主题、可复用组件、背景采样和媒体界面。
- [`demo`](demo/)：可运行的组件示例和完整界面流程。
- [`docs`](docs/)：中文详细规范、设计变量、来源说明和[验收指南](docs/quality.md)。

## 许可与贡献

Lucent UI 使用 [MIT 许可证](LICENSE)。部分实现改编自 Review 和其他开源项目，依赖项遵循各自的许可证。详见[署名](NOTICE.md)、[许可说明](docs/licenses.md)和[来源记录](docs/scope-and-sources.md)。

欢迎反馈和贡献。建议新增或调整组件、交互时，请说明使用场景，并同步更新规范与 Demo。
