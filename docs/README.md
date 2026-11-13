# Lucent UI 设计规范

Lucent UI 是面向 Android 和 Jetpack Compose 的开放设计系统。这份规范记录界面参数、组件状态和交互规则。可以结合 [Demo](https://github.com/Chengeeker/LucentUI/releases/latest) 体验完整流程，并在自己的应用中复用 [Compose 组件](../lucent-ui/)。

| 章节 | 内容 |
| --- | --- |
| [基础与设计变量](foundations.md) | 主题、调色板、字号、间距和形状 |
| [组件](components.md) | 设置行、卡片、导航、弹窗与反馈状态 |
| [玻璃效果](glass.md) | 背景采样、磨砂顶栏与液态玻璃导航 |
| [动效与手势](motion.md) | 页面切换、返回、触感与图片过渡 |
| [媒体浏览](media.md) | 图片浏览、HDR 标签、缩放和普通视频播放器 |
| [设置与备份](settings.md) | 个性化、WebDAV、缓存和本地迁移 |
| [页面与流程](patterns.md) | 时间线、阅读、评论与设置入口 |
| [接入与构建](integration.md) | Compose 模块、页面宿主和业务接入 |
| [验收](quality.md) | 交互、兼容性和设备验证 |

机器可读参数见 [设计变量](tokens.json)。复用组件时，可对照 [Demo 源码](../demo/) 和 [接入指南](integration.md) 检查状态、手势与反馈。

实现出处见 [来源与适配](scope-and-sources.md)，许可见 [开源署名](../NOTICE.md)。Review 的 [界面对照记录](visual-review.md) 可用于追溯组件来源和历史验收。
