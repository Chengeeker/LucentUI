# 玻璃与背景采样

实现直接来自 Review：`FrostedBackdrop.kt`、`GlassNavigation.kt`、`BackdropNavigation.kt`、`NavigationGlassMaterial.kt`、`QweaNavigationView.kt` 与相关着色器。

## 采样契约

`BackdropHost.content` 包含不透明页面底色、正文、图片和全部评论；`overlay` 包含顶栏和导航。不能把采样者再录进自身来源层。每个页面有自己的宿主，局部示例也用局部宿主。背景透明会让磨砂后再次透出清晰原字，这是必须保留的历史回归项。

```kotlin
BackdropHost(overlay = { LucentHeader("阅读", onBack) }) {
    ArticleAndComments()
}
```

## 磨砂顶栏

默认20dp模糊、45%表面色，顶部95%至透明保护渐变，0.5dp底边。API31+ 使用 GraphicsLayer / BlurEffect；API26–30 使用 Review 的四分之一尺寸采样及三轮 box blur。旧系统仅在来源 revision 变化时重新模糊，非活动页面暂停采样。
磨砂顶栏独立于“液态玻璃”开关，关闭底栏液态玻璃不会取消顶栏模糊。

## 液态玻璃

API33+ 沿用 Kyant Backdrop；旧版本沿用 QWEA GLES；`NavigationGlassChoice` 提供磨砂和通透选择，含真实动画预览。图库按钮也使用 Review 的48dp阻尼和锚定拉伸。
参数及 shader 以来源文件为准。0.1.0 自行增加的0.78底层与0.86透镜不透明度下限已移除，不能再当成 Review 标准。

模拟器截图只能验证布局及当前软件渲染结果。GLES资源释放、触感强度、真实GPU折射及持续帧耗时仍需要实体设备验证。
