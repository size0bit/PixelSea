# PixelSea

PixelSea 是一个 Android 多模块相册原型项目，当前聚焦在本地 `MediaStore` 图片浏览。应用首页展示手机拍摄的照片和系统截图，支持按日期分组浏览、进入大图查看、左右滑动切换图片，以及从大图返回时回到对应时间轴位置。

项目目前已经补齐了基础工程能力：

- 可通过 Gradle Wrapper 构建
- 仓库文本编码已统一
- Gallery 和 Viewer 共用同一份有序照片集合
- `MediaStore` 时间语义、筛选规则和 `Photo` 映射已收拢
- 已接入 `ktlint`，支持统一格式化和静态风格检查

## 当前能力

- 首页只显示两类图片：
  - 相机拍摄的照片
  - 系统截图
- 首页按日期分组显示图片
- 点击缩略图进入 Viewer
- Viewer 支持左右滑动切换图片
- 返回 Gallery 时，尽量回到刚刚查看的图片附近
- 从系统相册删除或新增图片后，App 回到前台会自动刷新
- 权限状态已显式建模：
  - 首次请求
  - 已授权
  - 已拒绝
  - 永久拒绝并跳设置页

## 当前边界

- 不是完整相册替代品，不展示设备里的所有图片
- 目前没有视频支持
- 目前没有收藏、编辑、分享、删除、搜索、相册分类等功能
- `feature:settings`、`core:network`、`core:sync`、`core:utils` 仍是占位模块
- Viewer 的交互和主题仍偏原型状态，后续还可以继续优化

## 环境要求

- Android Studio / IntelliJ IDEA with Android support
- JDK 17
- Android SDK 36
- 最低运行版本：Android 10 (`minSdk = 29`)

如果在命令行执行 Gradle，建议显式指定 JDK 17：

```powershell
$env:JAVA_HOME="C:\Users\EVOLUTION\.jdks\dragonwell-17.0.18"
.\gradlew.bat --version
```

## 运行项目

1. 用 Android Studio 或 IntelliJ IDEA 打开仓库根目录。
2. 等待 Gradle Sync 完成。
3. 连接测试机或启动模拟器。
4. 运行 `app` 模块。
5. 首次进入时授予图片读取权限。

命令行构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

## 常用命令

编译主工程：

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

运行 `core:data` 单测：

```powershell
.\gradlew.bat :core:data:testDebugUnitTest
```

运行 Kotlin 风格检查：

```powershell
.\gradlew.bat lintKotlin
```

自动格式化 Kotlin 代码：

```powershell
.\gradlew.bat formatKotlin
```

## 模块结构

### App

- `:app`
  - 单 Activity 入口
  - Compose 导航
  - 应用主题和启动壳

### Feature

- `:feature:gallery`
  - 首页网格
  - 权限流
  - 日期分组展示
  - 返回定位逻辑
- `:feature:viewer`
  - 大图查看
  - 左右滑动切换
  - 缩放/拖拽
  - 错误态和空态
- `:feature:settings`
  - 当前为占位模块

### Core

- `:core:data`
  - `MediaStore` 查询
  - `Photo` 模型和仓库
  - 时间归一化
  - 图片筛选与映射
  - 数据层单测
- `:core:ui`
  - 通用 UI 抽象
- `:core:network`
  - 当前为占位模块
- `:core:sync`
  - 当前为占位模块
- `:core:utils`
  - 当前为占位模块

### Build Logic

- `build-logic`
  - 自定义 Gradle convention plugins
  - Compose / Hilt / Android library 配置收口

## 近期做过的关键修复

- 修复 `MediaStore` 时间戳秒/毫秒混用导致的时间轴错误
- 修复旧分页实现导致的时间轴重复和 Viewer 定位错误
- 改为 Gallery / Viewer 共享同一份全量有序照片集合
- 修复从 Viewer 返回 Gallery 时的滚动定位
- 增加删除图片后回前台自动刷新
- 增加 Gallery / Viewer 的空态和错误态
- 收拢 `MediaStore` 查询、图片筛选和 `Photo` 映射逻辑

## 代码质量

仓库当前提供以下基础质量门禁：

- `ktlint` 风格检查
- `ktlint` 自动格式化
- `core:data` 单元测试

推荐在提交前至少执行：

```powershell
.\gradlew.bat formatKotlin
.\gradlew.bat lintKotlin
.\gradlew.bat :core:data:testDebugUnitTest
```
