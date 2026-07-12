# MyServiceCenter 项目说明

> 本文基于 2026-07-12 的代码快照整理，供后续开发对话快速定位。更新功能时，应同步修订对应章节。

## 1. 项目概览

`MyServiceCenter` 是一个单模块 Android 应用，包名为 `com.example.myservicecenter`，用于展示和编辑类似运营商服务中心的首页、个人页、通话详单、短/彩信详单及用户自定义数据。

- 版本：`1.4`（`versionCode 5`）
- SDK：`minSdk 26`、`target/compileSdk 34`
- 技术：Kotlin 为主，少量 Java 自定义控件；ViewBinding；Room；WebView；OkHttp；Glide；Material 3；BRVAH。
- 构建：AGP `8.13.2`、Kotlin `2.0.0`、JDK 17、Gradle `8.13`。
- 主入口：`HomeActivity`。

## 2. 目录与职责

| 路径 | 职责 |
| --- | --- |
| `app/src/main/java/.../ui/home/` | App 首页容器、原生首页、WebView 首页、个人页。 |
| `app/src/main/java/.../ui/main/MainActivity.kt` | 详单 WebView 页面；同步套餐、通话和短信数据。 |
| `app/src/main/java/.../ui/detail/` | 原生通话记录 RecyclerView 列表（当前不是启动主流程）。 |
| `app/src/main/java/.../ui/settings/SettingsActivity.kt` | 所有可编辑的本地展示数据与短信管理入口。 |
| `app/src/main/java/.../ui/sms/` | 短/彩信详单的新增、批量新增、编辑、搜索、删除、清空。 |
| `app/src/main/java/.../data/calllog/` | 电话助手 ContentProvider 契约及通话记录 Room 缓存。 |
| `app/src/main/java/.../data/sms/` | 短/彩信 Room 实体、DAO、数据库。 |
| `app/src/main/java/.../core/` | SharedPreferences、WebView 通用配置、在线升级、UI 辅助。 |
| `app/src/main/res/layout/` | XML 页面和条目布局；开启 ViewBinding 后生成对应 Binding。 |
| `merged_order_tabs.html` | 仓库内的详情页参考/静态文件；运行时页面实际从远端 URL 加载。 |

注意：源码的物理目录使用 `ui/settings`、`ui/sms` 等分组，但 `SettingsActivity`、`SmsDetailManageActivity`、多数 data/core 类的 Kotlin `package` 仍是根包 `com.example.myservicecenter`；移动文件或改包名时必须同时调整 Manifest 和 import。

## 3. 页面与导航

```text
HomeActivity（启动页，四个底部 Tab）
├─ Tab 0: HomeFragment（原生概览；余额卡 -> SettingsActivity）
├─ Tab 1: ModulePlaceholderFragment（占位）
├─ Tab 2: ModulePlaceholderFragment（占位）
└─ Tab 3: HomeMineFragment（个人页；服务中心项 -> MainActivity）
     └─ MainActivity（远程 merged_order_tabs.html）
         └─ 顶部更多 -> SettingsActivity
              └─ 短/彩信详单管理 -> SmsDetailManageActivity
```

- `HomeActivity` 禁用 `ViewPager2` 手势滑动，底部按钮控制页签；中间 AI 按钮当前仅 Toast。
- `HomeContentFragment` 是另一套远程 `home.html` WebView 首页实现，但当前 `HomePagerAdapter` 的 Tab 0 实际创建的是 `HomeFragment`，因此它不是默认可见路径。
- `MainActivity` 是服务中心详单入口，调用远端 `merged_order_tabs.html`，以 JavaScript bridge 与网页通信。

## 4. 数据来源与持久化

### SharedPreferences

统一由 `AppPreferences` 管理，文件名为 `service_center_prefs`。主要字段包括：手机号、姓名、星级、归属地、呼出套餐、呼入/呼出类型文案、个人页统计值、WebView 首页统计值、套餐固定费用 JSON。

`SettingsActivity` 负责读取和保存这些字段。页面应在 `onResume()` 重新读取，以反映返回设置页后的修改；现有 `HomeFragment`、`HomeMineFragment`、`CallDetailFragment` 已遵循此模式。

### Room

| 数据库文件 | 内容 | 写入/读取位置 |
| --- | --- | --- |
| `call_record_cache.db` | `CachedCallRecordEntity`，电话助手通话记录本地兜底缓存 | `MainActivity` 读取 Provider 成功后替换缓存；无权限或无数据时使用缓存。 |
| `sms_detail_records.db` | `SmsDetailRecordEntity`，可维护的短/彩信详单 | `SmsDetailManageActivity` 维护；`MainActivity` 按月份读取并注入网页。 |

### 外部 ContentProvider

通话记录来自电话助手：

- authority：`com.u2tzjtne.telephonehelper.provider`
- URI：`content://com.u2tzjtne.telephonehelper.provider/callrecords`
- 权限：`com.u2tzjtne.telephonehelper.permission.READ_CALL_RECORDS`

`MainActivity.queryCallRecords()` 按月份读取，且只保留 `isConnected = 1` 的记录；展示时间优先级为 `startTime`、`connectedTime`、`endTime`。不存在授权或 Provider 返回空时，使用 Room 缓存。

## 5. WebView 集成

公共配置位于 `CommonWebViewSupport`：启用 JavaScript、DOM Storage、混合内容、文件访问，并将生命周期、控制台、资源、HTTP/SSL 错误写入 Logcat。调试包的 `MyServiceCenterApplication` 会开启 WebView 远程调试，但当前 Manifest 未声明该 Application，故该开关目前不会生效。

| 页面 | 远端地址 | Bridge | 注入内容 |
| --- | --- | --- | --- |
| WebView 首页（非默认入口） | `https://www.lastcoffee.top:8200/home.html` | `HomeBridge.openSettings()` | 流量、余额、通话分钟、积分、待领取权益；网页卡片点击打开设置。 |
| 详单页 | `https://www.lastcoffee.top:8200/merged_order_tabs.html` | `PackageDetailBridge` | 基础资料、套餐固定费、按月通话详单、按月短信详单；支持网页触发 Android 编辑弹窗。 |

详单页 URL 会附加 `phoneNumber`、`name`、`badgeLevel` 参数。Android 侧调用的网页函数包括 `renderFixedFeeList`、基础资料同步函数，以及通话/短信列表渲染函数；修改网页契约时必须同时检查 `MainActivity.EditorBridge` 和 JS 注入脚本。

## 6. 在线升级

`AppUpdateManager` 使用 OkHttp 请求 `http://www.lastcoffee.top:8082/api/check`，参数为 `packageName` 与 `versionCode`。服务端返回是否更新、强制更新、版本、下载 URL、文件大小与 MD5。

下载后保存至 `externalFilesDir(DIRECTORY_DOWNLOADS)`，校验大小/MD5，并通过 `FileProvider` 安装。升级检查由 `HomeActivity` 冷启动时发起，网络请求及下载进度会输出到 `AppUpdateManager` 日志。

## 7. 关键 UI 约束与既有实现

- `activity_main.xml` 采用独立的 `topBarContainer` 覆盖在 AppBar 内容之上；`MainActivity` 会 `bringToFront()`。调整滚动/沉浸式布局时不能把整块 AppBar 一并固定。
- 原生通话列表的搜索/筛选 `layout_call_detail_filters.xml` 作为 `CallRecordAdapter` 的 header 添加，不应再放在 `SwipeRefreshLayout` 外部固定显示。
- 原生通话记录短于 60 秒时显示“X秒”；呼入/呼出类型和归属地/套餐展示可被设置页覆盖，空值需回退默认文案。
- TabLayout 需要在控件本身设置白色背景；相关资源是 `bg_tab_selected.xml`、`bg_tab_right_fade.xml`、`bg_tab_bottom_divider.xml`，主题在 `values/themes.xml`。
- 项目有大量 `values-sw*dp/dimens.xml`，改动通用尺寸时先确认是否被更具体的 smallest-width 资源覆盖。

## 8. 构建、调试与检查

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

常用 Logcat 标签：`HomeActivity`、`AppUpdateManager`、`HomeWebView`、`PackageDetailWebView`。

本地已经存在 `app/release/app-release.apk`；不要将构建产物或 IDE 目录的变更混入功能提交。当前工作区已有用户的 `.idea` 改动，处理业务需求时应保留并忽略它们。

## 9. 已知风险与后续修改入口

1. 远端 Web 页面与升级服务均依赖 `lastcoffee.top`，服务不可用时核心展示或升级会降级/失败；布局调整前应确认网页函数契约。
2. Manifest 设置了 `usesCleartextTraffic="true"`，且 WebView 允许混合内容和文件 URL 的通用访问。若面向正式安全发布，应收紧这些策略并审查网页来源。
3. `MyServiceCenterApplication` 没有在 Manifest 的 `<application android:name="...">` 中注册；如需要 WebView 调试初始化或未来 Application 初始化，需补充 `android:name=".MyServiceCenterApplication"` 并回归测试。
4. `HomeContentFragment` 与 `MainActivity` 的远端 WebView 均使用 JS bridge。新增 bridge 方法只能暴露必要接口，并需要兼顾网页加载完成前后重复注入的幂等性。
5. 电话助手 Provider 不是系统标准通话记录 Provider；测试机必须安装/授权对应电话助手，否则通话详单仅显示历史缓存。

## 10. 后续对话定位建议

- 改首页、底部 Tab、升级弹窗：从 `HomeActivity.kt` 与 `ui/home/` 开始。
- 改详单网页、网页与原生数据同步、通话 Provider：从 `ui/main/MainActivity.kt` 开始。
- 改通话记录列表的原生筛选/条目文案：从 `CallDetailFragment.kt` 和 `CallRecordAdapter.kt` 开始。
- 改用户可维护字段：先检查 `AppPreferences.kt`、`SettingsActivity.kt`，再在消费页面的 `onResume()` 刷新。
- 改短/彩信数据：`SmsDetailManageActivity.kt` + `data/sms/`；详单网页展示还要检查 `MainActivity` 的 `requestSmsDetailListForMonth()`。
