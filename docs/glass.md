# 玻璃与背景采样

Demo → 玻璃实验室；代码：`Backdrop.kt`、`LiquidGlass.kt`、`Navigation.kt`。

## 层级契约

`BackdropHost` 的 content 包括**实色页底、所有正文、图片、评论和回复**。overlay 是顶栏、浮动导航等采样者。采样者不得录入同一个来源层，否则产生循环采样。

```kotlin
BackdropHost(
    overlay = { LucentHeader("阅读", onBack = onBack) }
) {
    // Background is already part of the capture.
    ArticleAndComments()
}
```

局部玻璃预览需要局部 BackdropHost：把图片放进 content，玻璃控件放进 overlay。不能在全局来源层内部采样全局层。

## 磨砂

- Android 12+ 使用 GraphicsLayer + BlurEffect，默认模糊 20 dp；采样坐标由 root 坐标换算。
- 顶栏叠加 45% 表面色、顶部 95% 到透明的垂直保护渐变；底边 0.5 dp、outlineVariant 24%。
- 评论进入顶栏后与文章一起模糊。采样图层有实色底，不能让模糊结果的透明像素露出清晰原字。
- 不在静止页面启动永久 CPU 截图/模糊循环；重绘由内容变化驱动。

## 液态玻璃

Android 13+ 使用 AGSL RuntimeShader。保留 MIT 原作者说明，光学实现来源于 Review 已采用的着色器。背景采样扩大 20 dp，避免边缘折射夹取产生硬边。

| 参数 | 导航主体 | 选中透镜 | 按钮 |
|---|---:|---:|---:|
| 折射率 | 1.19 | 1.18 | 1.24 |
| 色散 | 0.008 | 0.025 | 0.025 |
| 厚度基值 | 10 | 20 | 32 |
| 光线方向 | -0.7071, -0.7071 | 相同 | 相同 |

厚度按 density/3 换算。光学、形变和触摸反馈分别控制；关闭普通水波纹不能连带删除玻璃水珠形变。

## 兼容性与降级

| 系统 | 实际路径 |
|---|---|
| API 33+ | 实时折射 + 模糊 + 弹性形变 |
| API 31–32 | 实时磨砂 + 弹性布局；无 AGSL 折射 |
| API 24–30 | 不透明表面 + 相同操作；未移植旧应用的 GLES 光学后端 |
| 用户关闭玻璃 | 语义实色卡片 |

旧系统光学等效仍是扩展边界，不能称为像素一致。若需要补齐 GLES，必须测 GPU 资源释放、横竖屏、后台恢复和真实低端机帧耗时。

官方接口参考：[Compose 绘制修饰符](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)。

导航可读性约束：通透底层 surface alpha 至少0.78，磨砂0.90；选中透镜使用 primaryContainer alpha0.86 和 onPrimaryContainer。避免透镜直接采样深色图片时，主题色文字与背景合并。黑/白两端背景在八套预置色的明暗模式下均检查正文对比度≥4.5；AGSL边缘仍保留折射与高光。
