# 挽联打印 Android

一款完全离线的 Android 挽联排版与蓝牙打印应用，面向 Xprinter XP-TT426B 及兼容 TSPL/TSPL2 的 203 DPI 打印机。

应用不依赖打印机中文字库：中文正文和连续装饰边框由 Android Canvas 渲染为黑白打印 Mask，再通过 Bluetooth Classic SPP 或 BLE 发送 TSPL `BITMAP` 数据。

## 功能

- Kotlin + Jetpack Compose + Material 3
- Android 8.0+，支持 Android 12+ 蓝牙权限
- Bluetooth Classic SPP 与 BLE 扫描、连接、断开和自动重连
- 中文竖排、自动字号、字距、边距和纸张尺寸设置
- 8 种 Canvas/Path 连续边框，可打印 1 米以上长幅
- 黑布白字实时预览，与实际 PrintMask 完全分离
- 双指缩放、拖动、Fit、100% 和全屏预览
- 文本、边框、排版、更多四类紧凑编辑面板
- 通过系统文件选择器导入 TTF/OTF 字体，预览与打印共用同一字体
- 1～6 人落款编辑，支持姓名竖列连续或同顶线并排布局
- 本地模板保存、撤销和重做
- TSPL BITMAP 二值化、8 bit 对齐、分块发送和白字极性适配
- 无账号、广告、云服务或联网权限

## 构建

环境要求：Android Studio、JDK 17+、Android SDK 35。

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

生成的 Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接的 Android 设备：

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## 使用

1. 在“打印设备”页面授权蓝牙并连接 XP-TT426B。
2. 在“设置 → 打印机测试”先执行白字极性测试。
3. 返回编辑器输入挽联，设置边框和纸张参数。
4. 检查黑底白字预览，点击“打印”并确认参数。

当前极性语义：`PRINT` 像素使打印头工作并转印白色色带，`NOT_PRINT` 像素保留原始黑色介质。

## 主要结构

```text
bluetooth/   Classic SPP 与 BLE
printing/    Bitmap、边框和 TSPL
storage/     本地模板与设备偏好
ui/editor/   挽联编辑器与全屏预览
ui/device/   蓝牙设备管理
ui/settings/ 打印和高级设置
```

> 项目处于 MVP 阶段，打印前请先使用小尺寸测试图确认耗材方向、浓度和极性。
