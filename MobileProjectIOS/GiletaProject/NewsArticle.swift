import Foundation

struct NewsResponseDTO: Decodable {
    let results: [NewsArticleDTO]
}

struct NewsArticleDTO: Decodable {
    let section: String?
    let title: String?
    let abstract: String?
    let url: String?
    let byline: String?
    let itemType: String?
    let source: String?
    let publishedDate: String?
    let multimedia: [NewsMediaDTO]?

    enum CodingKeys: String, CodingKey {
        case section
        case title
        case abstract
        case url
        case byline
        case itemType = "item_type"
        case source
        case publishedDate = "published_date"
        case multimedia
    }
}

struct NewsMediaDTO: Decodable {
    let url: String?
    let format: String?
    let height: Int?
    let width: Int?
}

struct NewsArticle: Codable, Hashable {
    let id: String
    let title: String
    let abstract: String
    let source: String
    let section: String
    let byline: String
    let publishedDate: String
    let articleURL: URL?
    let imageURL: URL?
}

extension NewsArticleDTO {
    func toDomain() -> NewsArticle {
        let titleText = normalized(title, fallback: "Без заголовка")
        let articleURL = normalizedURL(from: url)
        let imageURL = preferredImageURL()
        let identifier = articleURL?.absoluteString ?? UUID().uuidString

        return NewsArticle(
            id: identifier,
            title: titleText,
            abstract: normalized(abstract, fallback: "Описание отсутствует"),
            source: normalized(source, fallback: "The New York Times"),
            section: normalized(section, fallback: "Общее"),
            byline: normalized(byline, fallback: ""),
            publishedDate: normalized(publishedDate, fallback: ""),
            articleURL: articleURL,
            imageURL: imageURL
        )
    }

    private func preferredImageURL() -> URL? {
        let candidates = multimedia ?? []
        let preferredFormats = ["mediumThreeByTwo210", "Normal", "thumbLarge", "Standard Thumbnail"]

        for format in preferredFormats {
            if let urlString = candidates.first(where: { $0.format == format })?.url,
               let url = normalizedURL(from: urlString) {
                return url
            }
        }

        if let urlString = candidates.first(where: { $0.url?.isEmpty == false })?.url,
           let url = normalizedURL(from: urlString) {
            return url
        }

        return nil
    }

    private func normalizedURL(from value: String?) -> URL? {
        guard let value, value.isEmpty == false else {
            return nil
        }

        if value.hasPrefix("http://") {
            let secureValue = value.replacingOccurrences(of: "http://", with: "https://")
            return URL(string: secureValue)
        }

        return URL(string: value)
    }

    private func normalized(_ value: String?, fallback: String) -> String {
        guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines), trimmed.isEmpty == false else {
            return fallback
        }
        return trimmed
    }
}
