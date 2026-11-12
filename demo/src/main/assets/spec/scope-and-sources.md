# 范围与来源

用户最新指令指定 Review 为单一基准。当前快照为 Review Compose 3.17.5+278，源路径以原仓库相对路径记录；源文件哈希见reference-review.json。此前12应用清单仅保留为功能覆盖来源，不再投票决定主题或布局。

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

允许差异：应用名、图标、美术及业务数据；账号后端；加密全量密码输入；额外应用能力放在目录。禁止把允许差异扩展成另做首页/更换间距/重设玻璃效果。
源码保留Review MIT署名及玻璃依赖许可。本次不修改任何原App，不创建GitHub仓库或Release。
