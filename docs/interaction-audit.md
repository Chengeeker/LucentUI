# 0.2.2 交互排查与验收

运行环境为隔离的 Android 15 模拟器。先在 0.2.1 保存示例收藏，再覆盖安装原签名 0.2.2，对照真实入口、页面内容、操作对象及返回状态。

| 范围 | 核对结果 |
| --- | --- |
| 时间线顶栏 | 标题回到列表顶部，箭头选择动态分组，搜索独立打开。图片与视频分组返回对应卡片。 |
| 写帖子 | 空正文不可发布，返回保存草稿，重启能继续编辑。发布后正文置顶并持久化，草稿清空。 |
| 动态卡片 | 正文进入原动态，评论进入同一动态评论区，评论可回复。收藏列表能打开新旧收藏；详情中的 HDR 缩略图打开第 2 张并返回原页。 |
| 组件目录 | 依次打开阅读、图片、视频、音乐、剧集、搜索、设备、插件、基础组件、玻璃、媒体设置、本地备份、网络、隐私。另验证基础组件的设置菜单、写帖和阅读入口，以及剧集选集与键盘输入。 |
| 设置与文档 | 个性化、内容样式、通知、存储、缓存、WebDAV、账号、关于，以及图标和帧率子页均核对了实际内容。浏览记录能打开原动态。规范章节两种返回均回到目录。 |

本地发帖支持文字。已发布内容与发帖草稿在本机持久化，评论与点赞用于当前进程的演示。设置和备份页面验收覆盖导航和本地状态，用户账号、服务器以及实体设备留待对应环境验证。

[发帖编辑器](evidence/interactions-0.2.2/post-editor.png) · [分组菜单](evidence/interactions-0.2.2/feed-groups.png) · [安装包验签与测试记录](verification.json)

## 0.2.5+7 时间线媒体覆盖

- 首页样例正文已去重，图片动态覆盖 1–9 张及 12 张；旧 sample-1 至 sample-8 收藏键保持原值。
- 时间线最多绘制九格，超出部分显示 +N；任一可见格打开完整相册。详情页显示全部样例图片。
- 长图、GIF、HDR 和视频时长标记纳入演示；关闭液态玻璃时返回箭头切为圆角实心样式，液态开启时保留原描边图标。
- 定向 fixture 测试：1 项通过；Release Lint：0 错误、13 警告。
- 构建使用本机 JDK 26.0.1：当前环境未安装开发文档要求的 JDK 21；Gradle 9.8.2 支持运行于 JDK 26，AGP 9.4.1 最低要求 JDK 17。
- 正式签名 ARM64 APK：6013929 字节，SHA-256 dbad1b5373dfe0b59527c065d22967cf0bbb310ca27f006da9ee120df624ecf2；APK 签名、16 KiB 对齐及 ABI 检查通过。未安装到模拟器或实体设备。


## 0.2.6+8 时间线图片与视频混排（旧候选，已由 0.2.7+9 修正为同一图库分页）

- 新增 4 张照片与 1 个视频交错的动态，视频位于中间格。照片按原相册序号打开图库，视频格显示播放图标与时长并进入播放器。
- `DemoPostsTest`：1 项通过；Release Lint：0 错误、13 警告。正式签名 APK `6013929` 字节，SHA-256 `6cbb3f251a09e1e5aa4838796073985ab6319b034a52b266fe48ce8db65d9d48`；签名、ZIP 内容完整性、16 KiB 对齐及 arm64-v8a ABI 检查通过。
- 未安装到模拟器或实体设备，混排格视觉与实际图库/播放器来回尚未验收。


## 0.2.7+9 Review 风格混合图库与 RedNote 普通态顶栏

- 将视频作为与照片并列的图库媒体项；首页卡片、详情和图库分页共用同一媒体列表。到达视频页才启动播放器，缩略条显示视频封面/播放图标/时长，图片位置仍与原媒体索引一一对应。
- 视频播放时测量播放器控制区高度并将缩略条上移，横屏时隐藏缩略条；视频页不显示图片下载按钮。
- 无玻璃时返回和下载按钮直接使用 RedNote `PlainGalleryControl` 的圆形渐变表面及无波纹点击。
- `DemoPostsTest` 1 项通过；Lint 0 错误、13 警告；APK `6013925` 字节，SHA-256 `15f821cc1bbe348d8f1e7ca5dc9d8d480eca1ddd902a0544e7d88c6f85c6ecb4`，签名/ZIP/16 KiB/arm64-v8a 检查通过。
- 未在模拟器或实体设备检查视频播放、分页手势、返回动画和按钮外观。

## 0.2.8+10 Gallery settle timing

- Review mounts the video player only after the pager is settled and positions the thumbnail strip from the measured video control height. The prior LucentUI layout keyed the strip offset to currentPage, which can change halfway through a drag.
- The strip's bottom offset and landscape visibility now use settledPage. It stays at the image position while dragging and moves above video controls after the page settles; selected-thumbnail highlighting still follows currentPage.
- The 0.2.4 double-tap image-bounds correction and its regression test are retained.
- Focused checks: DemoPostsTest 1/1 and ContractsTest 6/6 passed. Full Demo suite: 10 of 20 tests failed because Robolectric's ASM rejects Java class file major version 70 under the only installed JDK 26.0.1.
- Release Lint: 0 errors, 13 warnings. Signed ARM64 APK: 6,014,073 bytes, SHA-256 6592e0f6fed4c3b08a076154d3c02dcd973b7bcf22000a4f433c1101a33b0986. Signature matches the previous release; ZIP integrity, 16 KiB alignment and arm64-v8a-only ABI checks passed.
- No emulator or physical-device interaction check was possible; the gesture timing is confirmed by the pager-state source logic and host-side tests only.