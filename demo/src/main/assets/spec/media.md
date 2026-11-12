# 媒体浏览

## 图片

`GalleryItem(uri, description, hdr = false)`；`LucentGallery(items, start, sources, onDismiss, onSave)`。`sources` 是索引到 `GalleryOrigin` 的映射；调用方记录图像坐标、已解码位图及圆角，让真实源图飞入/飞回。`covered` 为true时隐藏源图像素，但不能移除其布局。
高级入口 `review.ReviewGallery` 接受 `GalleryMedia` 的尺寸、长图、动图与HDR标记。Demo使用本地生成图及Android官方Ultra HDR样片，Coil加载器是业务边界适配；不包含Review的Cookie、平台图像下载与Live照片后端。静态原图通过ImageView展示Coil解码的Bitmap，保留增益图；GIF继续使用Coil动画绘制。

顶栏为返回、居中页码、下载；没有上一版自行加入的分享按钮。页码黑45%底、16dp圆角、横14/竖6内边距。缩略条64dp高，每格为容器宽16%，选中项居中白色2dp描边，未选中叠白50%；拖动缩略条切换中心页。中间三分之一区域单击显隐控件，两侧单击退出。
Demo保存使用系统文件选择器，用户取消不产生成功提示。原图缩放、长图、GIF、重试、快速进出见模拟器验收记录。

## 普通视频

`LucentVideo` 使用调用方提供的 `VideoSource(label, uri)`，播放器控件沿用 ReviewVideoChrome；真实 Media3 实例管理暂停、跳转、倍速、方向和生命周期。Demo内置720p/480p独立H.264/AAC文件。
只有至少两个标签及地址均有效且不同的画质才展示画质按钮；单源保留倍速。更换源保持位置、播放状态和倍速。支持双击播放、横拖定位、左亮度右音量、长按临时2倍、全屏。
平台URL解析、账号鉴权和下载接口由宿主提供。Review X / RealDrama 特殊播放器不作为普通播放器基准，目录中的剧集示例保持独立，不能称为两者的完整克隆。

## HDR badge (0.2.1)

The gallery uses the Review `PostContent.kt/HdrBadge` style: white regular text, 40% black background, 4dp corners, 10sp text/12sp line height with 6dp horizontal and 2dp vertical padding. The thumbnail strip uses 8sp/9sp with 3dp/1dp padding. `GalleryMedia.hdr` controls visibility; badges describe HDR images and thumbnail indices to accessibility services. This label does not assert hardware HDR playback. The same public `HdrBadge` also appears on feed and catalog images. Full still images use an ImageView without flattening the Bitmap or its gain map. HDR mode is enabled only for supported displays on API 34+, after the opening flight; leaving restores the previous window mode.


## 真实图片入口（0.2.1）

时间线第2张为“灯光 · HDR”；组件 → 图片浏览器包含7张图片：4张普通图、长图、GIF及HDR。所有图片都直接打开图库，不能将图片点击绑定到图片目录的普通页面路由。文章正文也使用同一入口。

可复用入口为 `GalleryThumbnail(media, origin, modifier, onClick)`：记录可见坐标、首帧Bitmap和圆角，飞行中只隐藏绘制，继续保留布局与点击。原始GIF首帧按最长边1024像素限制用于飞行；播放仍由Coil负责。接入示例见 `demo/.../GalleryHost.kt`、`ReviewScreens.kt`、`Patterns.kt`。GalleryHost位于整个页面栈和玻璃顶栏上方，以 `LocalGalleryOpenFrom` 接收图片列表、点击索引、来源映射，并将ReviewGallery作为同窗口覆盖层；有来源时不新建Dialog、不推入PageStack。无来源的独立调用仍可用Dialog兜底。

HDR样片来自[Android platform-samples](https://github.com/android/platform-samples/blob/main/samples/graphics/ultrahdr/src/main/assets/gainmaps/lamps.jpg)，Apache-2.0，文件未经重编码，SHA-256 `4691ba5c27cb92ce8a6b0c8d80ee83780f6cbce75a9805faf00409d54ab106e5`。Android15下以1/2/4采样解码均确认 `Bitmap.hasGainmap() == true`。依据[Android HDR显示文档](https://developer.android.com/media/grow/ultra-hdr/display)按能力切换窗口；旧设备展示SDR基础图，标签说明文件格式。截图与录屏不能证明HDR屏幕峰值亮度，实体显示仍待验收。

保存按原文件复制，包含HDR增益图，取消系统文件选择器不显示保存成功。图库关闭恢复系统栏显示及明暗图标。
