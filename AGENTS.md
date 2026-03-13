# 12368电话助手 (Telephone Helper)

## 项目概述

这是一个 Android 电话助手应用，模拟电话拨号和通话功能。应用可以模拟来电/去电界面，查询手机号码归属地，管理通话记录，并提供号码搜索功能。

应用包名：`com.u2tzjtne.telephonehelper`
版本：1.4.0 (versionCode: 40)

## 技术栈

### 编程语言
- **Kotlin** (2.0.0) - 新功能开发首选语言
- **Java** - 遗留代码和部分功能模块

### Android SDK 版本
- Compile SDK: 34
- Min SDK: 26 (Android 8.0)
- Target SDK: 29 (Android 10)
- Build Tools: 29.0.2

### 构建工具
- **Gradle** - Android Gradle Plugin 7.1.3
- 构建配置分离：根目录 `config.gradle` 集中管理版本号、签名配置等

### 核心依赖

#### 网络
- Retrofit 2.7.2 + OkHttp 3.14.7 / 4.11.0
- RxJava2 (2.2.19) + RxAndroid (2.1.1)
- Gson 2.8.6 (JSON 序列化)

#### 数据库
- Room 2.2.5 (本地 SQLite 数据库)
- Room-RxJava2 支持

#### UI 框架
- XPopup 2.10.0 (弹窗组件)
- BaseRecyclerViewAdapterHelper 3.0.4 (RecyclerView 适配器)
- AutoSize 1.2.1 (屏幕适配)
- ShapeView 9.3 (自定义形状视图)
- Material Design Components

#### 其他
- Glide 4.16.0 (图片加载)
- EventBus 3.2.0 (事件总线)
- AndPermission 2.0.3 (权限管理)
- phone-number-geo 1.0.9 (手机号归属地库)
- LogReport (崩溃日志收集)

## 项目结构

```
app/src/main/java/com/u2tzjtne/telephonehelper/
├── base/
│   └── App.java                 # Application 类，初始化崩溃日志
├── db/                          # Room 数据库层
│   ├── AppDatabase.java         # 数据库定义
│   ├── CallRecord.java          # 通话记录实体
│   ├── CallRecordDao.java       # 通话记录 DAO
│   └── PhoneLocation.java       # 号码归属地实体
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
│   │   ├── BaseActivity.java    # 基类
│   │   ├── MainActivity.java    # 主界面（拨号盘）
│   │   ├── CallActivity.java    # 通话界面 (Java 版)
│   │   ├── newCallActivity.kt   # 通话界面 (Kotlin 重构版)
│   │   ├── AddCallRecordActivity.kt  # 添加通话记录
│   │   └── HistoryActivity.java # 历史记录详情
│   ├── adapter/                 # RecyclerView 适配器
│   │   ├── CallRecordAdapter.java
│   │   └── CallHistoryAdapter.java
│   └── dialog/                  # 对话框
│       └── CopyPhoneNumberDialog.kt
└── util/                        # 工具类
    ├── AppUtils.java
    ├── ClipboardUtils.java      # 剪贴板工具
    ├── DateUtils.java           # 日期格式化
    ├── MediaPlayerHelper.java   # 音频播放管理
    ├── PhoneNumberUtils.java    # 手机号归属地查询
    ├── StatusBarUtils.java      # 状态栏处理
    └── ... (其他工具类)

app/src/main/res/
├── layout/                      # XML 布局文件
│   ├── activity_main.xml        # 主界面拨号盘
│   ├── activity_call.xml        # 通话界面
│   ├── new_call_activity.xml    # 新版通话界面
│   ├── activity_add_call_record.xml
│   ├── activity_history.xml
│   ├── item_call_record.xml     # 通话记录列表项
│   └── ...
├── raw/                         # 音频资源
│   ├── audio_call.mp3           # 拨号音
│   ├── audio_no_response.mp3    # 无人接听提示
│   ├── bohao.wav                # 按键音
│   └── calling.mp3              # 通话中提示
└── values/
    ├── colors.xml
    ├── strings.xml              # 应用名称："电话"
    └── styles.xml
```

## 主要功能模块

### 1. 拨号盘 (MainActivity)
- 数字键盘输入
- 号码归属地实时查询
- 剪贴板号码检测（自动弹出粘贴提示）
- 通话记录列表展示
- 号码搜索功能（支持高亮显示匹配数字）

### 2. 通话模拟 (CallActivity / newCallActivity)
- 模拟来电/去电界面
- 通话计时
- 免提切换
- 按键扩展面板（静音、录音等）
- 使用系统壁纸作为通话背景
- 音频播放：拨号音、无人接听、挂断提示等

### 3. 通话记录管理
- Room 数据库存储
- 按号码分组展示
- 支持添加自定义通话记录（模拟历史）
- 支持删除单条或按号码删除

### 4. 归属地查询
- 本地库查询 (phone-number-geo)
- 网络 API 兜底查询
- 结果缓存优化

## 构建命令

### 编译项目
```bash
# Windows
.\gradlew.bat build

# Linux/Mac
./gradlew build
```

### 安装 Debug 版本
```bash
.\gradlew.bat installDebug
```

### 打包 Release
```bash
.\gradlew.bat assembleRelease
```

输出 APK 命名格式：`TelephoneHelper_{buildType}_{yyyy-MM-dd}_{versionName}_u{versionCode}.apk`

### 清理构建
```bash
.\gradlew.bat clean
```

## 代码规范

### 语言规范
- 代码注释使用中文
- 类名使用大驼峰命名法 (PascalCase)
- 方法名、变量名使用小驼峰命名法 (camelCase)
- 常量使用全大写下划线分隔 (SCREAMING_SNAKE_CASE)

### 架构模式
- 采用 MVP/MVVM 混合模式
- Activity 负责 View 逻辑
- HTTP 层使用单例模式封装
- 数据库访问使用 Room + RxJava2

### 线程规范
- 网络请求和数据库操作必须在 IO 线程
- UI 更新必须在主线程
- 使用 RxJava 的 `subscribeOn(Schedulers.io())` 和 `observeOn(AndroidSchedulers.mainThread())`

## 权限说明

应用需要以下权限：
- `READ_CLIPBOARD` - 读取剪贴板号码
- `INTERNET` - 网络请求
- `READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE` - 读取系统壁纸、存储权限
- `MODIFY_AUDIO_SETTINGS` - 修改音频输出（免提切换）

## 签名配置

签名配置位于 `config.gradle`：
```gradle
storeFile: file("app/keystore/U2tzJTNENew.jks")
storePassword: "123456"
keyAlias: "key0"
keyPassword: "123456"
```

**注意**：生产环境应使用更安全的密钥管理方案。

## 测试说明

本项目暂无自动化测试套件。手动测试要点：

1. **拨号功能**：验证数字键盘输入、号码格式化显示
2. **归属地查询**：验证本地库和网络查询
3. **通话模拟**：验证各种状态切换（拨号中、接通、挂断）
4. **通话记录**：验证增删改查操作
5. **剪贴板检测**：复制手机号后进入应用应自动提示

## 安全注意事项

1. **HTTP 明文传输**：应用使用 HTTP (`http://114.116.40.8:18116/`) 而非 HTTPS
2. **硬编码密钥**：签名密钥信息存储在 config.gradle 中
3. **剪贴板读取**：应用会监听剪贴板内容以检测电话号码
4. **存储权限**：应用需要外部存储权限以读取系统壁纸

## 网络 API

后端 API 地址：`http://114.116.40.8:18116/`

接口列表：
- `POST /get_code` - 获取验证码
- `POST /login` - 登录
- `POST /phone_location` - 查询手机号归属地

## 迁移说明

项目正在进行 Java 到 Kotlin 的迁移：
- 新功能优先使用 Kotlin 开发
- `CallActivity` 已重构为 `newCallActivity.kt`
- `AddCallRecordActivity` 已使用 Kotlin 实现
- 保留 Java 版本以确保兼容性

## 开发环境要求

- Android Studio Arctic Fox 或更高版本
- JDK 17 (Kotlin JVM Target)
- Android SDK 34
- Gradle 7.0+
