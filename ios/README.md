# 挽联打印 iOS P0

独立原生 iOS 工程，使用 Swift、SwiftUI 和 CoreBluetooth。第一阶段只验证：

`iPhone → BLE → XP-TT426B → TSPL → 小尺寸位图打印`

## 工程

- Xcode 工程：`ios/WanLianPrint/WanLianPrint.xcodeproj`
- Scheme：`WanLianPrint`
- Deployment Target：iOS 16.0
- 页面：BLE 设备、BLE Diagnostics、测试打印

## BLE 策略

- 扫描所有附近 BLE Peripheral，不按名称过滤。
- 连接后调用 `discoverServices(nil)`，随后为每个 Service 调用 `discoverCharacteristics(nil, for:)`。
- 优先选择 `WRITE WITHOUT RESPONSE`，没有时回退到 `WRITE`。
- 每次发送通过 `maximumWriteValueLength(for:)` 动态计算 chunk。
- Without Response 使用 `canSendWriteWithoutResponse` 和 `peripheralIsReady` 做流控。
- With Response 等待 `didWriteValueFor` 后才发送下一块。

## 测试打印

- TSPL 基础测试
- 小矩形
- 英文 `TEST`
- 中文 `测试`（CoreGraphics/UIFont → PrintMask → 1-bit TSPL BITMAP）

PrintMask 语义保持为：背景 `NOT_PRINT`、文字/图形 `PRINT`。发送字节与 Android 已实机验证的 XP-TT426B 极性一致。

## GitHub Actions

`.github/workflows/build-ios.yml` 在 macOS runner 上执行真实 `xcodebuild`，关闭代码签名并上传：

- `WanLianPrint-unsigned.ipa`
- `WanLianPrint.app`
- Xcode/Swift 版本
- 完整 xcodebuild 日志

unsigned IPA 不能直接替代 Apple 开发者签名安装包；它主要用于确认工程可编译并保留 CI 构建产物。iPhone 实机安装仍需要 Apple 开发者签名或合规的侧载签名流程。
