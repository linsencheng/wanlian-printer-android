import SwiftUI

@main
struct WanLianPrintApp: App {
    @StateObject private var bluetoothManager = BluetoothManager()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(bluetoothManager)
                .environmentObject(bluetoothManager.logger)
        }
    }
}
