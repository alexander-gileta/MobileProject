import Foundation

enum NewsContentSource {
    case cache(savedAt: Date, isFresh: Bool)
    case network(receivedAt: Date)
}

struct NewsPayload {
    let articles: [NewsArticle]
    let source: NewsContentSource
}

protocol NewsRepositoryProtocol {
    func loadCachedNews(completion: @escaping (NewsPayload?) -> Void)
    func refreshLatestNews(completion: @escaping (Result<NewsPayload, NewsServiceError>) -> Void)
    func purgeExpiredCache(completion: (() -> Void)?)
}

final class CachedNewsRepository: NewsRepositoryProtocol {

    private let remoteService: NewsServiceProtocol
    private let cacheStorage: NewsCacheStorageProtocol

    init(
        remoteService: NewsServiceProtocol = NYTNewsService(),
        cacheStorage: NewsCacheStorageProtocol = FileNewsCacheStorage()
    ) {
        self.remoteService = remoteService
        self.cacheStorage = cacheStorage
    }

    func loadCachedNews(completion: @escaping (NewsPayload?) -> Void) {
        cacheStorage.load { snapshot in
            guard let snapshot else {
                completion(nil)
                return
            }

            let payload = NewsPayload(
                articles: snapshot.articles,
                source: .cache(savedAt: snapshot.savedAt, isFresh: snapshot.isFresh)
            )
            completion(payload)
        }
    }

    func refreshLatestNews(completion: @escaping (Result<NewsPayload, NewsServiceError>) -> Void) {
        remoteService.fetchLatestNews { [weak self] result in
            switch result {
            case let .success(articles):
                self?.cacheStorage.save(articles, completion: nil)
                completion(.success(NewsPayload(articles: articles, source: .network(receivedAt: Date()))))
            case let .failure(error):
                completion(.failure(error))
            }
        }
    }

    func purgeExpiredCache(completion: (() -> Void)? = nil) {
        cacheStorage.purgeExpiredCache(completion: completion)
    }
}
