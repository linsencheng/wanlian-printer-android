import Combine
import Foundation

struct AppLogEntry: Identifiable {
    let id = UUID()
    let timestamp: Date
    let message: String
}

final class AppLogger: ObservableObject {
    @Published private(set) var entries: [AppLogEntry] = []

    private let maximumEntries = 1_000
    private let formatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "HH:mm:ss.SSS"
        return formatter
    }()

    func log(_ message: String) {
        let entry = AppLogEntry(timestamp: Date(), message: message)
        entries.append(entry)
        if entries.count > maximumEntries {
            entries.removeFirst(entries.count - maximumEntries)
        }
        #if DEBUG
        print(formatted(entry))
        #endif
    }

    func clear() {
        entries.removeAll(keepingCapacity: true)
    }

    var exportedText: String {
        entries.map(formatted).joined(separator: "\n")
    }

    func formatted(_ entry: AppLogEntry) -> String {
        "[\(formatter.string(from: entry.timestamp))] \(entry.message)"
    }
}
