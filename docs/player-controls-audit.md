# 0.2.3 播放器与返回验收

在 Android 15 模拟器上从 0.2.2 覆盖安装原签名 ARM64 包。播放器验证以实际控件位置、系统旋转帧和返回手势为准。

| 检查 | 结果 |
| --- | --- |
| 图库 | 主图拖动中缩略图位置稳定，停稳后对齐选中页。缩略条可拖动选图，底边高度一致。 |
| 进度条 | 竖屏与横屏触摸区均为16dp，播放行与进度条布局符合规范。 |
| 全屏 | 旋转采样中横屏标题直接位于顶部。按顶栏返回和系统返回均退出全屏，保留当前画质及倍速。 |
| 触感 | Compose 测试核对播放、倍速、画质、全屏和进度操作各发出一次 View 触感请求。开关关闭后请求被抑制。 |
| 返回设置 | 旧版升级默认关闭，打开后重启保留，开关两种状态均实测系统边缘返回。 |

[竖屏](evidence/player-0.2.3/portrait.png) · [横屏](evidence/player-0.2.3/landscape.png) · [旋转中间帧](evidence/player-0.2.3/enter-01.png) · [主图拖动时的缩略图条](evidence/player-0.2.3/gallery-swipe-1.png) · [返回开关](evidence/player-0.2.3/predictive-setting.png)

实体震动强度和厂商系统旋转表现仍需在实际手机上验收。验包摘要见 [verification.json](verification.json)。
