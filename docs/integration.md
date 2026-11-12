# 接入与扩展

工程根目录为 Compose 多模块项目。`:lucent-ui` 是组件库；`:demo` 是可运行参考与设置功能实现。无平台账号/业务协议依赖的组件优先放入库，安全存储和业务数据留在应用层。

```kotlin
dependencies { implementation(project(":lucent-ui")) }

LucentTheme(LucentStyle(font = 1f, dynamic = true)) {
    BackdropHost(overlay = { LucentHeader("设置") }) {
        LazyColumn {
            item {
                LucentSection("偏好与功能") {
                    LucentToggle("触感反馈", enabled, onEnabledChange)
                    LucentRow("WebDAV", onClick = onOpenBackup)
                }
            }
        }
    }
}
```

列表的顶部 padding 应包含系统状态栏 + 64 dp 顶栏；底部留出导航与系统安全区。不能重复提供 appFontScale。玻璃 overlay 与 content 分层，局部场景使用独立 BackdropHost。

媒体接口使用调用方提供的 URI 和描述。鉴权头、下载保存、分享、画质列表和源解析由接入方负责。没有多档有效源就不提供画质按钮。

设置页面代码作为可复制的功能参考，不承诺与其他应用备份格式兼容。生产接入时将 DemoModel 替换为已有 ViewModel/Repository；保留原账号存储、恢复事务和数据迁移规则。

## 构建

JDK 21；Android SDK 36；AGP 8.11.1；Kotlin/Compose 插件 2.2.20；Compose BOM 2026.02.00。保持编译和运行依赖一致，不能只升级编译插件后忽略运行时。

设置 `LUCENT_SIGNING_PROPERTIES` 指向本机私有 signing properties，内容包含 storeFile/storePassword/keyAlias/keyPassword。然后执行 `gradlew :demo:assembleRelease`。密钥和密码不进入仓库。APK 使用单一正式包名和 ARM64 依赖筛选。

## 授权

本工程 MIT。Review 派生代码保留 MIT 署名，AGSL 原作者许可见 `licenses/`。其他应用主要作为设计规则来源，不直接复制未明确兼容授权的业务源码。第三方库遵循其自身许可。
