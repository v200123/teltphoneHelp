# 电话助手 - 主题切换实现指南

## 概述

本项目已实现完整的白天/黑夜主题切换功能，支持以下三种模式：
- **白天模式** - 明亮清爽的白色主题
- **黑夜模式** - 护眼的深色主题
- **跟随系统** - 自动根据系统设置切换

## 颜色搭配方案

### 白天模式配色

| 颜色用途 | 色值 | 说明 |
|---------|------|------|
| 主背景 | `#FFFFFF` | 纯白背景 |
| 卡片背景 | `#FFFFFF` | 纯白卡片 |
| 表面颜色 | `#F5F5F5` | 搜索框、输入框背景 |
| 主要文字 | `#212121` | 标题、重要内容 |
| 次要文字 | `#666666` | 描述、说明 |
| 第三级文字 | `#999999` | 时间、辅助信息 |
| 提示文字 | `#BDBDBD` | Hint 文字 |
| 分割线 | `#E0E0E0` | 细分割线 |
| 主题绿色 | `#28B17A` | 拨号按钮、选中状态 |
| 蓝色链接 | `#2196F3` | 可点击链接 |
| 红色警告 | `#F44336` | 错误、删除、挂断 |

### 黑夜模式配色

| 颜色用途 | 色值 | 说明 |
|---------|------|------|
| 主背景 | `#121212` | Material Dark 推荐背景 |
| 卡片背景 | `#1E1E1E` | 稍微亮一点的卡片背景 |
| 表面颜色 | `#2C2C2C` | 搜索框、输入框背景 |
| 主要文字 | `#FFFFFF` | 高对比度白色文字 |
| 次要文字 | `#B0B0B0` | 次级信息 |
| 第三级文字 | `#808080` | 辅助信息 |
| 提示文字 | `#666666` | Hint 文字 |
| 分割线 | `#424242` | 暗色分割线 |
| 主题绿色 | `#66BB6A` | 稍微降低饱和度的绿色 |
| 蓝色链接 | `#64B5F6` | 降低亮度的蓝色 |
| 红色警告 | `#EF5350` | 降低亮度的红色 |

## 文件结构

```
app/src/main/res/
├── values/
│   ├── colors.xml          # 白天模式颜色定义
│   ├── attrs.xml           # 主题属性定义
│   └── styles.xml          # 主题样式定义
├── values-night/
│   └── colors.xml          # 黑夜模式颜色定义
└── layout/
    ├── activity_main.xml   # 主界面（已更新主题属性）
    ├── item_call_record.xml # 通话记录项（已更新主题属性）
    ├── layout_home_dial.xml # 拨号键盘（已更新主题属性）
    ├── activity_history.xml # 历史详情（已更新主题属性）
    ├── activity_add_call_record.xml # 添加记录（已更新主题属性）
    └── dialog_theme_switch.xml # 主题切换对话框

app/src/main/java/com/u2tzjtne/telephonehelper/
├── base/
│   └── App.java            # 初始化 ThemeManager
├── ui/dialog/
│   └── ThemeSwitchDialog.kt # 主题切换对话框
└── util/
    └── ThemeManager.kt      # 主题管理工具类
```

## 使用方式

### 1. 初始化（已完成）

在 `App.java` 的 `onCreate` 中：

```java
@Override
public void onCreate() {
    super.onCreate();
    // ... 其他初始化
    
    // 初始化主题管理器
    ThemeManager.INSTANCE.init(this);
}
```

### 2. 切换主题

#### 方式一：使用对话框（推荐）

```kotlin
// 在设置按钮点击事件中
new XPopup.Builder(this)
    .asCustom(new ThemeSwitchDialog(this))
    .show()
```

#### 方式二：代码直接切换

```kotlin
// 切换到白天模式
ThemeManager.setLightMode(activity)

// 切换到黑夜模式
ThemeManager.setDarkMode(activity)

// 切换到跟随系统
ThemeManager.setFollowSystem(activity)

// 快速切换（自动取反）
ThemeManager.toggleTheme(activity)
```

### 3. 获取当前主题状态

```kotlin
// 获取当前模式
val mode = ThemeManager.getCurrentMode()
// 返回值：MODE_LIGHT(0), MODE_DARK(1), MODE_SYSTEM(2)

// 判断是否是黑夜模式
val isDark = ThemeManager.isDarkMode()

// 判断是否跟随系统
val isFollowSystem = ThemeManager.isFollowSystem()
```

## 自定义主题属性

### 在布局中使用主题属性

```xml
<!-- 背景色 -->
android:background="?attr/windowBackgroundColor"

<!-- 文字颜色 -->
android:textColor="?attr/textPrimaryColor"
android:textColor="?attr/textSecondaryColor"
android:textColor="?attr/textTertiaryColor"

<!-- 功能色 -->
android:textColor="?attr/accentGreenColor"
android:textColor="?attr/accentBlueColor"
android:textColor="?attr/accentRedColor"

<!-- 分割线 -->
android:background="?attr/dividerColor"
```

### 在代码中使用主题颜色

```kotlin
// 获取主题属性颜色
val typedValue = TypedValue()
theme.resolveAttribute(R.attr.textPrimaryColor, typedValue, true)
val color = typedValue.data

// 或者直接使用 ContextCompat
val color = ContextCompat.getColor(this, R.color.textPrimary)
```

## 添加新颜色步骤

### 步骤 1：在 `attrs.xml` 中添加属性定义

```xml
<attr name="yourNewColor" format="reference|color" />
```

### 步骤 2：在 `colors.xml` 中添加颜色值（白天模式）

```xml
<color name="yourNewColor">#FF0000</color>
```

### 步骤 3：在 `values-night/colors.xml` 中添加颜色值（黑夜模式）

```xml
<color name="yourNewColor">#AA0000</color>
```

### 步骤 4：在 `styles.xml` 的主题中绑定属性

```xml
<style name="AppTheme.Light">
    <item name="yourNewColor">@color/yourNewColor</item>
</style>

<style name="AppTheme.Dark">
    <item name="yourNewColor">@color/yourNewColor</item>
</style>
```

### 步骤 5：在布局中使用

```xml
android:background="?attr/yourNewColor"
```

## 注意事项

1. **状态栏处理**：主题切换会自动处理状态栏颜色，白天模式使用深色图标，黑夜模式使用浅色图标

2. **Activity 重建**：切换主题后会重建 Activity，确保保存好相关状态

3. **图片资源**：部分图标可能需要准备两套（白天/黑夜），使用 `drawable-night` 目录

4. **WebView**：如果使用了 WebView，需要单独处理网页的暗黑模式适配

5. **第三方库**：部分第三方库可能需要单独配置主题支持

## 扩展：自定义主题切换动画

如果需要添加切换动画，可以在 `Activity` 中添加：

```kotlin
override fun recreate() {
    // 添加过渡动画
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    super.recreate()
}
```

## 测试检查清单

- [ ] 白天模式下所有界面显示正常
- [ ] 黑夜模式下所有界面显示正常
- [ ] 文字对比度足够，易于阅读
- [ ] 状态栏图标颜色正确
- [ ] 切换主题后 Activity 状态正确恢复
- [ ] 拨号键盘颜色正确显示
- [ ] 通话界面颜色正确显示
