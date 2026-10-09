import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            DeviceScreen()
                .tabItem { Label("设备", systemImage: "antenna.radiowaves.left.and.right") }

            BLEDiagnosticsScreen()
                .tabItem { Label("诊断", systemImage: "waveform.path.ecg") }

            TestPrintScreen()
                .tabItem { Label("测试打印", systemImage: "printer") }
        }
    }
}
