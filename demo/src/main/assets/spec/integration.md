# 接入与构建

`:lucent-ui` 提供主题、组件、背景采样及媒体UI；`:demo` 负责状态、加密存储、文件和网络操作。`review` 包保留 Review 源码组织，仅适配包名、资源和业务状态入口，避免再次造一套相似组件。

```kotlin
LucentTheme(LucentStyle()) {
    BackdropHost(overlay = { LucentHeader("设置", onBack) }) {
        SettingsContent()
    }
}
```

内容padding应包含状态栏和56dp顶栏，底部包含导航及系统安全区；设置首页没有顶栏。每个页面各自提供BackdropHost，避免采样自身或只采样部分卡片。应用字号只能由LucentTheme一次提供。
图库调用方提供源图坐标、位图和保存回调；视频调用方提供有效源列表。接入已有应用时替换DemoModel为其现有Repository，不替换账号协议、签名或备份格式。

工具链对齐 Review：JDK21、Gradle9.8.2、AGP9.4.1、Kotlin/Compose插件2.4.21、BOM2026.09.00、compileSdk37.2、target36、min26；ARM64。API最低版本从0.1.0的24调整为26，这是复用当前Review实现的兼容性边界。
保持Compose编译和运行时依赖一致。`verifyComposeAlignment` 在preBuild检查。私有签名属性通过 `LUCENT_SIGNING_PROPERTIES` 提供；仓库不存密钥或口令。正式包：`gradlew :demo:assembleRelease`，不构建另一包名/签名的测试APK。

业务接入顺序：主题与密度→页面宿主及生命周期→共享设置组件→媒体回调→账户/备份适配→真实设备验收。不要只复制颜色而漏掉交互及资源释放。
