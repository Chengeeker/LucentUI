# 组件与状态

组件库提供无业务依赖的 Compose API。状态由调用方持有，动作以回调传出。Demo → 组件与交互覆盖主要状态。

| 组件 | API | 状态与约束 |
|---|---|---|
| 分组 | LucentSection / LucentCard | 主色标题、不透明可辨识容器 |
| 设置行 | LucentRow | 普通、说明、图标、尾部值、不可用；整行可操作 |
| 开关行 | LucentToggle | 整行一个 Switch 语义目标；内置 Switch 不重复处理点击 |
| 按钮 | LucentAction | 主/次/不可用；48 dp；无普通 ripple |
| 图标按钮 | LucentIcon | 48 dp；必须提供可访问名称 |
| 图库按钮 | GalleryControl | 48 dp 目标、44 dp 渐变圆面、1 dp 白边 |
| 顶栏 | LucentHeader | 返回、标题、右侧操作；采样全页背景 |
| 导航 | LucentNavigation | 2–5 项；选中语义；点击、长按拖动、取消与回弹 |
| 结果 | LucentResult | 成功/失败明确文字；弹窗在当前窗口上层 |
| 空态 | LucentEmpty | 说明原因，必要时给恢复动作 |
| 下拉刷新 | LucentRefresh | 手动下拉显示反馈；程序化分类切换不冒充下拉手势 |
| 输入、滑块 | Material 3 + Lucent tokens | label、错误提示、提交校验；不能只提示颜色 |
| 弹窗、面板 | Material 3 + Lucent actions | 取消、确认、IME、安全区、返回键 |

## 反馈契约

1. 点击只触发一次；不可用的控件不触发回调或触感。
2. 网络和文件操作显示可见的进行中状态，屏蔽重复提交，提供取消。
3. 结果不能仅依赖可能被底部面板遮住的 Toast。后台结果回到当前页面后仍可见。
4. 服务器返回成功后才显示上传完成。取消上传不能断言远端未写入。
5. 危险操作先说明对象和后果。数据恢复先解析/校验，后一次性提交。
6. 状态必须包含 loading、content、empty、error/retry；图片与播放器还需 buffering/error/release。

## 语义和焦点

图标按钮有 contentDescription，选项有 selected 状态，开关有当前状态说明。装饰性图标不重复朗读。保留系统键盘操作和输入法行为。

[Compose 无障碍默认行为](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)是基础；自定义玻璃和手势组件仍需显式补足语义。
