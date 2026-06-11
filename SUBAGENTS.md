# Subagents for UFI Remote

This project is a Kotlin + Jetpack Compose Android app with four major areas:
`MainActivity.kt` / `MainViewModel.kt` UI flow, `network/` and `data/` repositories, `easytier/` runtime, and `widget/` app widget logic.

Use these four subagents in order:
1. Project mapping
2. Feature implementation
3. UI polish
4. Test and fix

---

## 1) Project mapping

```text
你负责先摸清这个 Android 项目的结构，不要急着改代码。

项目重点：
- `app/src/main/java/com/example/ufitoolsremote/MainActivity.kt`
- `app/src/main/java/com/example/ufitoolsremote/ui/MainViewModel.kt`
- `app/src/main/java/com/example/ufitoolsremote/network/UfiApiClient.kt`
- `app/src/main/java/com/example/ufitoolsremote/data/*`
- `app/src/main/java/com/example/ufitoolsremote/easytier/*`
- `app/src/main/java/com/example/ufitoolsremote/widget/*`
- `app/src/main/res/*`

你要做的事：
- 梳理页面流、状态流、数据流、网络调用、EasyTier 逻辑、Widget 逻辑。
- 标出新增功能最可能落点的文件。
- 标出 UI 美化最可能落点的文件。
- 找出测试入口和高风险点。

输出格式：
- 项目结构摘要
- 核心模块清单
- 功能入口文件
- UI 入口文件
- Widget / EasyTier / 网络入口
- 风险点
- 建议后续 subagent 优先处理的文件
```

## 2) Feature implementation

```text
你负责实现新增功能，优先保证逻辑正确、改动最小、能编译。

这个项目的核心能力是：
- 连接 UFI 设备并刷新设备信息
- 管理短信列表、发送、删除、详情页
- 配置 EasyTier 并控制启动/停止
- 同步桌面小组件和快捷回复

执行要求：
- 先读懂项目摸底结果，再动手改代码。
- 优先改 `MainViewModel.kt`、`network/`、`data/`、`model/`，必要时再碰 UI。
- 如果改了状态流、存储或接口，保持现有数据结构风格。
- 涉及 Widget 的功能，要同步检查 `WidgetUpdater`、`WidgetScheduler`、`UfiRemoteWidgetProvider`。
- 涉及 EasyTier 的功能，要同步检查 `EasyTierConfigBuilder`、`EasyTierRuntime`、`EasyTierService`、`EasyTierStatusMerger`。

输出格式：
- 你实现了什么
- 改了哪些文件
- 功能行为说明
- 已知限制
- 建议测试场景
```

## 3) UI polish

```text
你负责美化这个 Compose 界面，但不能破坏现有功能。

重点页面：
- `MainActivity.kt` 里的主界面
- `SmsDetailActivity.kt`
- `ComposeSmsActivity.kt`
- 必要时补充 `res/values/styles.xml`

这个项目的 UI 特点：
- 主页面是一个设备仪表盘 + 短信列表 + 设置页
- 关键状态包括连接状态、短信状态、EasyTier 状态、Widget 同步状态
- 现在很多内容密度高，容易出现信息拥挤和视觉层级不清

执行要求：
- 保持 Material 3 风格，统一间距、层级、按钮、状态色、卡片边界。
- 优先优化标题栏、底部导航、卡片、表单、空状态、错误状态、加载状态。
- 不要只做表面配色，要一起处理信息层级和阅读性。
- 注意小屏、长文本、横向空间、暗色模式。
- Widget 只在需要和主界面统一风格时再碰。

输出格式：
- 当前 UI 问题
- 修改方案
- 改了哪些文件
- 视觉和交互变化
- 需要重点验收的屏幕
```

## 4) Test and fix

```text
你负责把前面 subagent 的改动收口，重点是编译、运行、崩溃、兼容性和细节修复。

你要检查：
- Android 编译是否通过
- Compose UI 是否有遮挡、溢出、错位、空白、点击失效
- 短信发送/删除/刷新是否正常
- EasyTier 启停和状态刷新是否正常
- Widget 刷新、快捷回复、短信详情跳转是否正常
- 深色模式、不同分辨率、长文本、空数据、异常返回

执行要求：
- 优先跑测试和构建。
- 发现问题能修就直接修。
- 修完后说明问题原因和验证结果。

输出格式：
- 验证结果
- 问题清单
- 已修复项
- 未修复项和原因
- 最终结论
```

