<!-- AGENTS.md for 12368电话助手 (Telephone Helper) -->

# 12368电话助手 (Telephone Helper)

## 项目概述

这是一个 Android 电话助手应用，模拟电话拨号和通话功能。应用可以模拟来电/去电界面，查询手机号码归属地，管理通话记录，并提供号码搜索功能。此外还支持彩铃视频播放、通话录音模拟、主题切换、自定义号码归属地、拨号音绑定等高级功能。

应用包名：`com.u2tzjtne.telephonehelper`
版本：1.6.0 (versionCode: 60)
应用名称：`电话`

## 技术栈

### 编程语言
- **Kotlin** (2.0.0) — 新功能开发首选语言
- **Java** — 遗留代码和部分功能模块

### Android SDK 版本
- Compile SDK: 34
- Min SDK: 26 (Android 8.0)
- Target SDK: 29 (Android 10)
- Build Tools: 29.0.2
- Java/Kotlin Target: 17

### 构建工具
- **Gradle** — Android Gradle Plugin 8.13.2
- Gradle 版本：8.13（通过 wrapper 管理）
- 构建配置分离：根目录 `config.gradle` 集中管理版本号、签名配置等

### 核心依赖

#### 网络
- Retrofit 2.7.2 + OkHttp 3.14.7 / 4.11.0
- RxJava2 (2.2.19) + RxAndroid (2.1.1)
- Gson 2.8.6 (JSON 序列化)

#### 数据库
- Room 2.8.4 (本地 SQLite 数据库)
- Room-RxJava2 支持 (2.2.5)
- 双数据库架构：
  - `AppDatabase` (`app.db`) — 通话记录、录音记录、自定义归属地
  - `RingVideoDatabase` (`ring_video.db`) — 彩铃视频、音乐文件、号码绑定、不播放彩铃号码、提示音号码等

#### UI 框架
- XPopup 2.10.0 (弹窗组件)
- BaseRecyclerViewAdapterHelper 3.0.4 (RecyclerView 适配器)
- AutoSize 1.2.1 (屏幕适配，设计稿基准 412dp)
- ShapeView 9.3 (自定义形状视图)
- Material Design Components 1.1.0
- UltimateBarX 0.8.1 (状态栏处理)

#### 视频播放
- GSYVideoPlayer 12.0.0 (彩铃视频播放)

#### 其他
- Glide 4.16.0 (图片加载)
- EventBus 3.2.0 (事件总线)
- XXPermissions 28.0 (权限管理)
- phone-number-geo 1.0.10-202502 (手机号归属地库)
- LogReport (崩溃日志收集)
- DeviceCompat 2.3 (设备兼容性)

#### 测试
- JUnit 4.13.2
- Mockito 4.11.0 + mockito-kotlin 4.1.0
- Robolectric 4.11.1
- AndroidX Test Core / JUnit / Espresso

## 项目结构

```
app/src/main/java/com/u2tzjtne/telephonehelper/
├── base/
│   └── App.java                 # Application 类，初始化崩溃日志、主题管理器
├── db/                          # Room 数据库层
│   ├── AppDatabase.java         # 主数据库（通话记录、录音、自定义归属地）
│   ├── RingVideoDatabase.java   # 彩铃数据库（彩铃视频、音乐、绑定关系）
│   ├── CallRecord.java          # 通话记录实体
│   ├── CallRecordDao.java       # 通话记录 DAO
│   ├── Recording.java           # 录音记录实体
│   ├── RecordingDao.java        # 录音 DAO
│   ├── CustomPhoneLocation.java # 自定义号码归属地实体
│   ├── RingVideo.java           # 彩铃视频实体
│   ├── RingtonePhoneBinding.java # 彩铃-号码绑定实体
│   ├── NoRingtonePhone.java     # 不显示彩铃的号码实体
│   ├── CallPromptPhone.java     # 特殊提示音号码实体
│   ├── MusicFile.java           # 音乐文件实体
│   ├── PhoneRingtoneAssignment.java # 号码拨号音绑定实体
│   └── ... (其他 DAO 和实体)
├── event/
│   └── ClipboardEvent.java      # 剪贴板事件
├── http/                        # 网络层
│   ├── HttpClient.java          # Retrofit 客户端单例
│   ├── HttpServer.java          # API 接口定义
│   ├── HttpResult.java          # 统一响应封装
│   ├── ApiException.java        # 异常处理
│   ├── bean/                    # 数据模型
│   │   ├── LoginBean.java
│   │   ├── PhoneLocalBean.java
│   │   └── PhoneLocationBean.java
│   ├── download/                # 下载相关
│   ├── handler/                 # 请求处理器
│   └── observer/                # RxJava 观察者
├── ui/                          # UI 层
│   ├── activity/                # Activity 页面
│   │   ├── BaseActivity.java    # 基类（权限请求、主题适配）
│   │   ├── MainActivity.java    # 主界面（拨号盘 + 通话记录列表）
│   │   ├── CallActivity.java    # 旧版通话界面 (Java)
│   │   ├── newCallActivity.kt   # 新版通话界面 (Kotlin，当前主力)
│   │   ├── AddCallRecordActivity.kt  # 添加通话记录
│   │   ├── HistoryActivity.java # 历史记录详情
│   │   ├── SettingsActivity.kt  # 设置界面
│   │   ├── RingVideoManageActivity.kt # 彩铃视频管理
│   │   ├── RingtoneBindingManageActivity.kt # 彩铃号码绑定管理
│   │   ├── MusicManageActivity.kt # 音乐库管理
│   │   ├── BadgeRuleManageActivity.kt # 彩铃图标规则管理
│   │   ├── BadgeRuleEditorActivity.kt # 规则编辑器
│   │   ├── CallPromptPhoneManageActivity.kt # 提示音号码管理
│   │   ├── NoRingtonePhoneManageActivity.kt # 不显示彩铃号码管理
│   │   ├── PhoneAudioBindingManageActivity.kt # 号码拨号音绑定管理
│   │   ├── CustomLocationListActivity.kt # 自定义归属地列表
│   │   ├── CustomLocationManageActivity.kt # 自定义归属地管理
│   │   └── CallRecordingDetailActivity.kt # 通话录音详情
│   ├── adapter/                 # RecyclerView 适配器
│   ├── dialog/                  # 对话框
│   │   ├── CopyPhoneNumberDialog.kt
│   │   └── ThemeSwitchDialog.kt
│   └── widget/                  # 自定义控件
│       ├── EmptyControlVideo.kt
│       ├── NoPreloadViewPager.java
│       └── SViewPager.java
└── util/                        # 工具类
    ├── AppUtils.java
    ├── ClipboardUtils.java      # 剪贴板工具
    ├── DateUtils.java           # 日期格式化
    ├── MediaPlayerHelper.java   # 音频播放管理
    ├── MusicPlaybackHelper.kt   # 音乐播放辅助
    ├── PhoneNumberUtils.java    # 手机号归属地查询
    ├── PhoneNumberFormatUtils.kt # 号码格式化
    ├── VideoPlayerHelper.kt     # 视频播放辅助
    ├── GSYVideoPlayerHelper.kt  # GSY 播放器封装
    ├── AudioRecorderHelper.kt   # 录音辅助（模拟录音计时）
    ├── ThemeManager.kt          # 主题管理器（白天/黑夜/跟随系统）
    ├── StatusBarUtils.java      # 状态栏处理
    ├── CallDialAudioSettings.kt # 拨号音设置
    ├── CallPromptSettings.kt    # 通话提示音设置
    ├── CallVibrationSettings.kt # 通话震动设置
    ├── RingtoneBadgeRenderHelper.kt # 彩铃图标角标渲染
    ├── RingtoneBadgeRuleStore.kt # 彩铃图标规则存储
    ├── PhoneDialAudioBindingHelper.kt # 拨号音绑定辅助
    └── ... (其他工具类)

app/src/main/res/
├── layout/                      # XML 布局文件
├── raw/                         # 音频/视频资源
│   ├── audio_busy.mp3           # 忙线音
│   ├── audio_call.mp3           # 拨号音
│   ├── audio_empty_number.mp3   # 空号提示音
│   ├── audio_no_response.mp3    # 无人接听提示
│   ├── audio_power_off.mp3      # 关机提示音
│   ├── audio_user_busy.m4a      # 用户正忙提示音
│   ├── bohao.wav                # 按键音
│   ├── calling.mp3              # 通话中提示
│   └── telephone_hang_up_call.mp3 # 挂断提示音
├── drawable/                    # 图片和形状资源
├── drawable-night/              # 黑夜模式覆盖资源
├── values/                      # 颜色、字符串、样式（白天模式）
├── values-night/                # 黑夜模式颜色和样式
└── xml/
    └── network_security_config.xml # 网络安全配置（允许明文 HTTP）
```

## 主要功能模块

### 1. 拨号盘 (MainActivity)
- 数字键盘输入，支持按键音
- 号码归属地实时查询（本地库 + 网络 API 兜底）
- 剪贴板号码检测（自动弹出粘贴提示）
- 通话记录列表展示，支持按号码分组
- 号码搜索功能（支持高亮显示匹配数字）
- 主题切换入口

### 2. 通话模拟 (CallActivity / newCallActivity)
- `newCallActivity` 是当前主力通话界面（Kotlin）
- `CallActivity` 为旧版 Java 实现，保留用于兼容
- 通话状态机：DIALING → RINGING → CONNECTED / BUSY / NO_ANSWER → HUNG_UP
- 通话计时、免提切换、静音、录音模拟
- 按键扩展面板（静音、录音、笔记等）
- 使用系统壁纸作为通话背景
- 彩铃视频播放（根据被叫号码查找绑定的彩铃视频）
- 音频播放：拨号音、无人接听、忙线音、挂断提示等
- 支持针对不同号码播放自定义提示音（关机、空号、用户正忙、无法接通）

### 3. 通话记录管理
- Room 数据库存储 (`AppDatabase`)
- 按号码分组展示
- 支持添加自定义通话记录（模拟历史）
- 支持删除单条或按号码删除
- ContentProvider 暴露 (`CallRecordProvider`)，支持跨应用读取
- 录音详情页展示模拟录音信息

### 4. 归属地查询
- 本地库查询 (phone-number-geo)
- 网络 API 兜底查询
- 用户自定义归属地（可覆盖本地库结果）
- 结果缓存优化（内存缓存 + 数据库存储）

### 5. 彩铃视频系统
- 彩铃视频管理（增删查）
- 彩铃与手机号码绑定（一对一/一对多）
- 播放优先级：号码绑定彩铃 > 随机彩铃 > res/raw > assets > 外部存储
- 支持不显示彩铃的号码黑名单
- 使用 GSYVideoPlayer 播放视频

### 6. 主题切换
- 三种模式：白天 / 黑夜 / 跟随系统
- 通过 `ThemeManager` 统一管理
- 切换主题后 Activity 重建
- `values-night/` 覆盖颜色资源

### 7. 设置中心
- 彩铃视频管理
- 彩铃图标规则管理（7 套预设规则）
- 音乐库管理
- 拨号音模式切换（普通拨号音 / 音乐库 / 彩铃库）
- 号码-拨号音绑定管理
- 不显示彩铃的号码管理
- 特殊提示音号码管理（关机、空号、用户正忙、无法接通）
- 通话震动时长设置

## 关键配置说明

### 根目录 `config.gradle`
集中定义了应用 ID、SDK 版本、版本号和签名配置：
- applicationId: `com.u2tzjtne.telephonehelper`
- versionCode: 60, versionName: "1.6.0"
- 签名文件：`app/keystore/U2tzJTNE.jks`

### `app/build.gradle`
- 启用 `viewBinding`
- 启用 `multiDexEnabled`
- APK 输出命名格式：`TelephoneHelper_{buildType}_{yyyy-MM-dd}_{versionName}_u{versionCode}.apk`
- Release 构建使用 `minifyEnabled false`（未启用代码混淆）

## 数据库架构

### AppDatabase (`app.db`, version 5)
| 表名 | 说明 |
|------|------|
| CallRecord | 通话记录 |
| Recording | 录音记录（支持一次通话多条录音） |
| custom_phone_location | 自定义号码归属地 |

迁移历史：
- 1→2: 初始变更
- 2→3: CallRecord 添加录音字段
- 3→4: 新增独立 Recording 表
- 4→5: 新增 custom_phone_location 表

### RingVideoDatabase (`ring_video.db`, version 5)
| 表名 | 说明 |
|------|------|
| RingVideo | 彩铃视频 |
| RingtonePhoneBinding | 彩铃-号码绑定关系 |
| NoRingtonePhone | 不显示彩铃的号码 |
| PhoneRingtoneAssignment | 号码拨号音绑定 |
| MusicFile | 音乐文件 |
| CallPromptPhone | 特殊提示音号码 |

## 网络 API

后端 API 地址：`http://114.116.40.8:18116/`

接口列表：
- `POST /get_code` — 获取验证码
- `POST /login` — 登录
- `POST /phone_location` — 查询手机号归属地

> ⚠️ 应用使用明文 HTTP 传输，且 `network_security_config.xml` 已配置允许明文流量。

## 构建命令

```bash
# Windows
.\gradlew.bat build
.\gradlew.bat installDebug
.\gradlew.bat assembleRelease
.\gradlew.bat clean

# 运行测试
.\gradlew.bat test
```

## 代码规范

### 语言规范
- 代码注释使用中文
- 类名使用大驼峰命名法 (PascalCase)
- 方法名、变量名使用小驼峰命名法 (camelCase)
- 常量使用全大写下划线分隔 (SCREAMING_SNAKE_CASE)

### 架构模式
- Activity 负责 View 逻辑
- HTTP 层使用单例模式封装 (`HttpClient`)
- 数据库访问使用 Room + RxJava2
- 工具类以 `Helper`、`Utils`、`Manager` 命名

### 线程规范
- 网络请求和数据库操作必须在 IO 线程
- UI 更新必须在主线程
- 使用 RxJava 的 `subscribeOn(Schedulers.io())` 和 `observeOn(AndroidSchedulers.mainThread())`
- Kotlin 新代码部分使用 `lifecycleScope` 协程

## 权限说明

应用需要以下权限：
- `READ_CLIPBOARD` — 读取剪贴板号码
- `INTERNET` — 网络请求
- `READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE` — 读取系统壁纸、存储权限
- `WRITE_EXTERNAL_STORAGE` — 存储写入
- `MODIFY_AUDIO_SETTINGS` — 修改音频输出（免提切换）
- `RECORD_AUDIO` — 录音权限
- `VIBRATE` — 震动

自定义权限：
- `com.u2tzjtne.telephonehelper.permission.READ_CALL_RECORDS`
- `com.u2tzjtne.telephonehelper.permission.WRITE_CALL_RECORDS`

## 签名配置

签名配置位于 `config.gradle`：
```gradle
storeFile: file("app/keystore/U2tzJTNE.jks")
storePassword: "U2tzJTNE"
keyAlias: "U2tzJTNE"
keyPassword: "U2tzJTNE"
```

**注意**：生产环境应使用更安全的密钥管理方案。

## 测试说明

### 现有测试
- `app/src/test/java/.../CallStateTest.kt` — 对 `newCallActivity.CallState` 枚举的单元测试

### 测试依赖
- JUnit 4 + Mockito + mockito-kotlin
- Robolectric（Android 单元测试）
- AndroidX Test + Espresso（UI 测试框架已引入，暂无测试用例）

### 手动测试要点
1. **拨号功能**：验证数字键盘输入、号码格式化显示
2. **归属地查询**：验证本地库、网络查询、自定义归属地
3. **通话模拟**：验证各种状态切换（拨号中、振铃、接通、忙音、无人接听、挂断）
4. **通话记录**：验证增删改查操作
5. **剪贴板检测**：复制手机号后进入应用应自动提示
6. **彩铃视频**：验证视频播放、号码绑定、黑名单
7. **主题切换**：验证白天/黑夜/跟随系统模式
8. **特殊提示音**：验证关机、空号、用户正忙等提示音命中逻辑

## 安全注意事项

1. **HTTP 明文传输**：应用使用 HTTP (`http://114.116.40.8:18116/`) 而非 HTTPS
2. **硬编码密钥**：签名密钥信息存储在 `config.gradle` 中
3. **剪贴板读取**：应用会监听剪贴板内容以检测电话号码
4. **存储权限**：应用需要外部存储权限以读取系统壁纸和彩铃视频
5. ** exported Provider**：`CallRecordProvider` 设置了 `exported="true"`

## 迁移说明

项目正在进行 Java 到 Kotlin 的迁移：
- 新功能优先使用 Kotlin 开发
- `CallActivity` 已重构为 `newCallActivity.kt`（当前主力）
- `AddCallRecordActivity`、`SettingsActivity` 及所有管理类 Activity 均已使用 Kotlin 实现
- 保留 Java 版本的 `CallActivity` 以确保兼容性

## 开发环境要求

- Android Studio Arctic Fox 或更高版本
- JDK 17 (Kotlin JVM Target)
- Android SDK 34
- Gradle 8.0+
