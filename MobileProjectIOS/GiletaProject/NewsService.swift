import Foundation

enum NewsServiceError: LocalizedError {
    case invalidURL
    case badResponse
    case transport(Error)
    case emptyData
    case decoding(Error)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Не удалось сформировать адрес запроса."
        case .badResponse:
            return "Сервер вернул некорректный ответ."
        case let .transport(error):
            return error.localizedDescription
        case .emptyData:
            return "Сервер вернул пустой ответ."
        case let .decoding(error):
            return "Ошибка обработки данных: \(error.localizedDescription)"
        }
    }
}

protocol NewsServiceProtocol {
    func fetchLatestNews(completion: @escaping (Result<[NewsArticle], NewsServiceError>) -> Void)
}

final class NYTNewsService: NewsServiceProtocol {

    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func fetchLatestNews(completion: @escaping (Result<[NewsArticle], NewsServiceError>) -> Void) {
        guard let url = buildURL() else {
            completion(.failure(.invalidURL))
            return
        }

        let request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 30)

        session.dataTask(with: request) { data, response, error in
            if let error {
                completion(.failure(.transport(error)))
                return
            }

            guard let httpResponse = response as? HTTPURLResponse,
                  200...299 ~= httpResponse.statusCode else {
                completion(.failure(.badResponse))
                return
            }

            guard let data, data.isEmpty == false else {
                completion(.failure(.emptyData))
                return
            }

            do {
                let decoder = JSONDecoder()
                let responseDTO = try decoder.decode(NewsResponseDTO.self, from: data)
                let articles = responseDTO.results
                    .map { $0.toDomain() }
                    .filter { $0.title.isEmpty == false }
                completion(.success(articles))
            } catch {
                completion(.failure(.decoding(error)))
            }
        }.resume()
    }

    private func buildURL() -> URL? {
        var components = URLComponents()
        components.scheme = "https"
        components.host = "api.nytimes.com"
        components.path = "/svc/news/v3/content/all/all.json"
        components.queryItems = [
            URLQueryItem(name: "limit", value: "20"),
            URLQueryItem(name: "api-key", value: NewsAPIConfig.apiKey)
        ]
        return components.url
    }
}
