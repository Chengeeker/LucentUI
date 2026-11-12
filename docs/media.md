# 媒体浏览

## 图片

`GalleryItem(uri, description)`；`LucentGallery(items, start, sources, onDismiss, onSave)`。`sources` 是索引到 `GalleryOrigin` 的映射；调用方记录图像坐标、已解码位图及圆角，让真实源图飞入/飞回。`covered` 为true时隐藏源图像素，但不能移除其布局。
高级入口 `review.ReviewGallery` 接受 `GalleryMedia` 的尺寸、长图、动图与HDR标记。Demo使用本地生成图，Coil加载器是业务边界适配；不包含 Review 的Cookie、平台图像下载与Live照片后端。HDR开关路径保留，但没有HDR样本验收。

顶栏为返回、居中页码、下载；没有上一版自行加入的分享按钮。页码黑45%底、16dp圆角、横14/竖6内边距。缩略条64dp高，每格为容器宽16%，选中项居中白色2dp描边，未选中叠白50%；拖动缩略条切换中心页。中间三分之一区域单击显隐控件，两侧单击退出。
Demo保存使用系统文件选择器，用户取消不产生成功提示。原图缩放、长图、GIF、重试、快速进出见模拟器验收记录。

## 普通视频

`LucentVideo` 使用调用方提供的 `VideoSource(label, uri)`，播放器控件沿用 ReviewVideoChrome；真实 Media3 实例管理暂停、跳转、倍速、方向和生命周期。Demo内置720p/480p独立H.264/AAC文件。
只有至少两个标签及地址均有效且不同的画质才展示画质按钮；单源保留倍速。更换源保持位置、播放状态和倍速。支持双击播放、横拖定位、左亮度右音量、长按临时2倍、全屏。
平台URL解析、账号鉴权和下载接口由宿主提供。Review X / RealDrama 特殊播放器不作为普通播放器基准，目录中的剧集示例保持独立，不能称为两者的完整克隆。
