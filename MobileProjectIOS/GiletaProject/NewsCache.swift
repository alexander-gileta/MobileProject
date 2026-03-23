import Foundation

enum NewsCachePolicy {
    static let freshnessInterval: TimeInterval = 15 * 60
    static let maxRetentionInterval: TimeInterval = 24 * 60 * 60
}

struct CachedNewsSnapshot {
    let articles: [NewsArticle]
    let savedAt: Date

    var isFresh: Bool {
        Date().timeIntervalSince(savedAt) <= NewsCachePolicy.freshnessInterval
    }
}

protocol NewsCacheStorageProtocol {
    func load(completion: @escaping (CachedNewsSnapshot?) -> Void)
    func save(_ articles: [NewsArticle], completion: (() -> Void)?)
    func purgeExpiredCache(completion: (() -> Void)?)
}

final class FileNewsCacheStorage: NewsCacheStorageProtocol {

    private struct CachePayload: Codable {
        let savedAt: Date
        let articles: [NewsArticle]
    }

    private let fileManager: FileManager
    private let queue = DispatchQueue(label: "news.cache.storage.queue", qos: .utility)
    private let cacheFileURL: URL

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager

        let baseDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first
            ?? fileManager.temporaryDirectory
        let directory = baseDirectory.appendingPathComponent("NewsCache", isDirectory: true)
        let fileURL = directory.appendingPathComponent("latest_news.json", isDirectory: false)

        cacheFileURL = fileURL
        createDirectoryIfNeeded(at: directory)
    }

    func load(completion: @escaping (CachedNewsSnapshot?) -> Void) {
        queue.async { [weak self] in
            guard let self else {
                completion(nil)
                return
            }

            let snapshot = self.loadSnapshotIfAvailable()
            completion(snapshot)
        }
    }

    func save(_ articles: [NewsArticle], completion: (() -> Void)? = nil) {
        queue.async { [weak self] in
            guard let self else {
                completion?()
                return
            }

            let payload = CachePayload(savedAt: Date(), articles: articles)
            do {
                let encoder = JSONEncoder()
                encoder.dateEncodingStrategy = .iso8601
                let data = try encoder.encode(payload)
                try data.write(to: self.cacheFileURL, options: .atomic)
            } catch {
                print("Не удалось сохранить кэш новостей: \(error.localizedDescription)")
            }

            completion?()
        }
    }

    func purgeExpiredCache(completion: (() -> Void)? = nil) {
        queue.async { [weak self] in
            guard let self else {
                completion?()
                return
            }

            self.removeCacheIfExpired()
            completion?()
        }
    }

    private func loadSnapshotIfAvailable() -> CachedNewsSnapshot? {
        removeCacheIfExpired()

        guard fileManager.fileExists(atPath: cacheFileURL.path) else {
            return nil
        }

        do {
            let data = try Data(contentsOf: cacheFileURL)
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            let payload = try decoder.decode(CachePayload.self, from: data)
            return CachedNewsSnapshot(articles: payload.articles, savedAt: payload.savedAt)
        } catch {
            print("Не удалось прочитать кэш новостей: \(error.localizedDescription)")
            try? fileManager.removeItem(at: cacheFileURL)
            return nil
        }
    }

    private func removeCacheIfExpired() {
        guard fileManager.fileExists(atPath: cacheFileURL.path) else {
            return
        }

        do {
            let data = try Data(contentsOf: cacheFileURL)
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            let payload = try decoder.decode(CachePayload.self, from: data)
            let age = Date().timeIntervalSince(payload.savedAt)
            if age > NewsCachePolicy.maxRetentionInterval {
                try? fileManager.removeItem(at: cacheFileURL)
            }
        } catch {
            try? fileManager.removeItem(at: cacheFileURL)
        }
    }

    private func createDirectoryIfNeeded(at url: URL) {
        guard fileManager.fileExists(atPath: url.path) == false else {
            return
        }

        do {
            try fileManager.createDirectory(at: url, withIntermediateDirectories: true, attributes: nil)
        } catch {
            print("Не удалось создать папку для кэша новостей: \(error.localizedDescription)")
        }
    }
}
