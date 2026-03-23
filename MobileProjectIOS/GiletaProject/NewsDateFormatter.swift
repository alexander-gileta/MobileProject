import Foundation

enum NewsDateFormatter {
    private static let inputFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withDashSeparatorInDate, .withColonSeparatorInTime, .withTimeZone]
        return formatter
    }()

    private static let outputFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter
    }()

    static func displayString(from rawValue: String) -> String {
        guard rawValue.isEmpty == false,
              let date = inputFormatter.date(from: rawValue) else {
            return "Дата неизвестна"
        }
        return outputFormatter.string(from: date)
    }
}
