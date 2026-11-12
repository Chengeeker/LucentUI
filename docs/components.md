# 组件契约

## 设置

`LucentCard/Section/Row/Toggle` 委托 Review 的 `SettingsCard/SettingsSection/SettingsTile`。设置行左右16dp、文本上下8dp、图标与文字间16dp。普通入口显示灰色线性图标和右箭头；有自定义 trailing 时不叠加箭头。
开关整行可操作并暴露 Switch 语义，子 Switch 不重复触发。单选块使用 RadioButton 语义，右侧圆点对整段标题及说明垂直居中。
首页按四组排列：偏好与功能、存储与备份、账号设置、关于与支持。首页没有额外大标题或固定顶栏；顶安全区下20dp，组间32dp。

## 顶栏与底栏

`LucentHeader` 使用56dp TopAppBar、18sp粗体标题、线性返回箭头；实色页面内容和评论共同进入顶栏磨砂来源。
`LucentNavigation` 是三个目的地的 Review 悬浮底栏入口。普通模式4dp内边距、22dp图标、11.5sp标签；选中面跟随240ms位移动画。非悬浮模式保留 Review 标准 NavigationBar。三个目的地的名称允许业务替换，尺寸不能按名称重新发明。

## 反馈

确认、失败和完成采用 `ReviewAlertDialog` 动画。忙碌期间禁用重复提交；取消不能宣称远端一定回滚。普通点击无水波纹，触感只发一次。真正独立窗口中的控件仍提供触感，不依赖 Activity 顶层触摸监听。

空态、加载、失败、重试、禁用、选中均应可见且可操作。Demo 的基础组件目录保留这些状态供复用验证。
