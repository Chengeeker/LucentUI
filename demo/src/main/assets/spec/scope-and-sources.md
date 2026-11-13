# 范围与来源

Lucent UI 的主题、组件和交互实现选取了 Review Compose 3.17.5+278 作为设计来源。下表记录各部分的来源和适配方式，供复用、维护和许可追溯。源文件以原仓库相对路径记录，哈希见 [来源清单](reference-review.json)。

| 区域 | 来源 | 适配 |
|---|---|---|
| 主题及排版 | Components/Palettes/FontWeight | 参数替代业务Model |
| 设置首页/个性化 | SettingsScreens/ThemeScreen/AppearanceTemplate | 路由、名称与Demo状态 |
| 顶栏/底栏/页面 | Shell/PageStack/FrostedBackdrop | 三个Demo目的地、独立页面宿主 |
| 液态玻璃 | GlassNavigation/BackdropNavigation/QweaNavigationView | 原依赖、shader和手势 |
| 图库 | GalleryScreen/GalleryFlight/GalleryGlassButton | 本地图像加载及SAF保存，无平台媒体协议 |
| 视频 | VideoChrome | 媒体源与播放状态适配 |
| 动态/评论 | PostCard/CommentRow | 本地fixture与交互，无远端认证/富文本 |
| WebDAV | BackupScreen | 表单结构对齐，保留Demo加密备份协议 |

组件允许接入方提供品牌、媒体、业务数据、账号后端和备份策略。复用时请同时对照相应章节中的间距、状态、触感与手势规则。
源码保留 Review MIT 署名及玻璃依赖许可，详见 [开源许可](licenses.md)。
