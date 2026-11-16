# LucentUI 图库原图与高清区域渲染

适用：WebAlbum 内置 LucentUI 与独立 LucentUI 源码（本地改动，尚未提交）。

## 原因

旧版图库使用 Coil 的屏幕尺寸位图作为预览，再由 Compose graphicsLayer 统一放大。原图的完整字节即使被下载，放大时也不会重新解码源像素，因此大照片会一直模糊。

## 新增能力

- 保持快速缩略图/预览图作为底层，不牺牲打开、滑动体验。
- 图库顶部新增「原图」入口；按缩放比达到 1.6 倍时，自动尝试加载原图。
- 共用组件 `GalleryHighResolutionLayer`、`GalleryHiResState`：把当前可见窗口逆映射至源图像素区域，使用 BitmapRegionDecoder 仅按需解码需要的部分，单 tile 限制约 4 MP。
- 支持常见 EXIF 旋转与镜像方向，高清 tile 与预览方向保持一致。
- 每次缩放/平移稳定后重新解码新区域；离开照片时取消过时工作。
- 默认走应用配置的 Coil 缓存；宿主可通过 `GalleryOriginalLoader` 提供有进度、认证及文件版本校验的原始文件（WebAlbum 使用专用 WebDAV 实现）。

### 宿主集成接口

```kotlin
typealias GalleryOriginalLoader =
    suspend (GalleryMedia, (loaded: Long, total: Long?) -> Unit) -> File
```

在 ReviewGallery 调用时传入 `originalLoader = ...`；必须返回完整的本地原始文件。原有调用点无需改变，默认走现有 Coil ImageLoader 的 disk cache。

## 兼容性和限制

- Android BitmapRegionDecoder 支持 JPEG、PNG、WebP、HEIF 和 AVIF，具体取决于系统实现；不支持的格式保留正常预览并提示不支持高清区域解码。
- **Ultra HDR** 增益图区域重建目前无法证明完整保留，已采取保护策略：已识别 HDR 的图片仍走原有支持增益图的预览路径，不叠加可能改变色彩的 SDR tile。原始文件可缓存，但这类照片尚不保证原像素级放大。
- 对原文件的读取及像素解码在 IO 线程进行，内存不会按全图像素数线性增加；大文件仍需要可用缓存空间。
- 测试包含源图像素几何、缩放采样、EXIF 方向/镜像、无效参数及重试；真机大图和 HDR 仍需验证。

## 原图胶囊位置与外观（2026-11-16）

- 原图入口已从左上角移动到媒体缩略图条上方、水平居中，只有单张图片时则在底部安全区域上方显示。
- 缩略图条高64dp，胶囊与条之间保留8dp间隙；没有连廊时底部边距24dp。
- 胶囊内容高度36dp，可触摸范围48dp，椭圆轮廓、文字单行省略、最大宽度258dp。
- 跟随 LucentUI 的 glass 开关：开启时沿用顶栏按钮的液态玻璃着色和光学背景；关闭时沿用顶栏按钮的40%黑色背景及0.5dp浅色描边。
- 保留 hapticClick 触觉反馈、下载进度、失败重试、原图已缓存等状态。
- WebAlbum 内置与 LucentUI 独立工程同步更新，尚未推送 GitHub。
