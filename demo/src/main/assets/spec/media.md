# 图片、视频与音乐

## 图片浏览器

标准控件布局采用用户选定的 Review Flutter 版本，迁移为 Compose：48 dp 点击区、44 dp 圆面、13 sp 粗体页码、64 dp 缩略条。支持图片列表、来源位置、分页、双击和双指缩放、缩略图跳转、保存与系统分享回调。

`GalleryItem(uri, description)`；`LucentGallery(items, initial, origin, onDismiss, onSave, onShare)`。库不自行索取存储权限或携带平台 Cookie。Demo 使用系统文件选择器保存、受限 FileProvider 分享。

长图、动画图片、快速关闭重开必须作为独立场景验证。原图/HDR/鉴权属于图片解码器和应用后端能力，不能因统一控件而删除；Demo 内置的是 SDR 自制图像，不能作为 HDR 通过证据。

## 普通视频

`LucentVideo(sources, autoplay, initialSpeed, loop, onSpeed)` 使用 Media3 ExoPlayer。普通控制结构采用 Review Compose，画质与倍速使用深色底部单选面板；进度轨道 4 dp、触摸区扩展至 48 dp。

`manualQualities` 排除空标签、未知标签、空地址，并按实际地址去重。有效手动画质超过一档才显示画质按钮；单档仅保留倍速等有效控件。

切换画质保留播放位置和播放/暂停状态。倍速为 0.5、0.75、1、1.25、1.5、2、3。缓冲有进度状态；错误可重试。后台暂停，退出释放播放器和监听，清除常亮，恢复窗口亮度及方向。

Demo 包含两档独立编码的自制 12 秒 H.264/AAC 片段。离线素材说明编码/分辨率选择可用，不代表第三方接口支持画质或所有网络格式均已验收。

## 专用模式

- Review X、RealDrama 的定制视频不被普通播放器规范覆盖。剧集模板保留选集、喜欢、下一集等独立布局与回调。
- 音乐模板应覆盖封面、迷你/展开、进度、队列、播放模式和歌词；实际媒体会话/后台服务由音乐应用接入。
- 设备远程画面、键盘预览不套用普通图片/视频的交互假设。

播放器生命周期参考：[Media3 ExoPlayer 入门](https://developer.android.com/media/media3/exoplayer/hello-world)。
