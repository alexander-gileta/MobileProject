import Foundation

final class AdditionalRequestLogger {

    struct DebugPayload: Encodable {
        let source: String
        let timestamp: String
        let event: String
    }

    func sendDebugRequest() {
        guard let url = URL(string: "https://jsonplaceholder.typicode.com/posts") else {
            print("Дополнительный POST-запрос не выполнен: некорректный URL")
            return
        }

        let payload = DebugPayload(
            source: "GiletaProject",
            timestamp: ISO8601DateFormatter().string(from: Date()),
            event: "news_screen_opened"
        )

        guard let body = try? JSONEncoder().encode(payload) else {
            print("Дополнительный POST-запрос не выполнен: не удалось закодировать body")
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json; charset=UTF-8", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("GiletaProject-iOS", forHTTPHeaderField: "X-App-Client")
        request.httpBody = body

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error {
                print("Дополнительный POST-запрос завершился ошибкой: \(error.localizedDescription)")
                return
            }

            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
            let bodyText: String
            if let data, let string = String(data: data, encoding: .utf8) {
                bodyText = string
            } else {
                bodyText = "<empty body>"
            }

            print("Дополнительный POST-запрос выполнен. Status: \(statusCode). Body: \(bodyText)")
        }.resume()
    }
}
