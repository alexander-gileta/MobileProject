import CryptoKit
import Foundation
import SQLite3
import UIKit

private enum ImageCachePolicy {
    static let fileLifetime: TimeInterval = 7 * 24 * 60 * 60
    static let maxDiskSizeInBytes = 100 * 1024 * 1024
}

private struct CachedImageRecord {
    let urlString: String
    let fileName: String
    let expiresAt: TimeInterval
    let lastAccessedAt: TimeInterval
    let sizeInBytes: Int
}

private final class ImageMetadataStore {

    private let databaseURL: URL
    private var database: OpaquePointer?
    private let sqliteTransient = unsafeBitCast(-1, to: sqlite3_destructor_type.self)

    init(databaseURL: URL) {
        self.databaseURL = databaseURL
        openDatabase()
        createTableIfNeeded()
    }

    deinit {
        sqlite3_close(database)
    }

    func record(for urlString: String, now: Date) -> CachedImageRecord? {
        let sql = "SELECT url, file_name, expires_at, last_accessed_at, size_bytes FROM image_cache WHERE url = ? LIMIT 1;"
        var statement: OpaquePointer?
        defer { sqlite3_finalize(statement) }

        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK else {
            return nil
        }

        sqlite3_bind_text(statement, 1, (urlString as NSString).utf8String, -1, sqliteTransient)

        guard sqlite3_step(statement) == SQLITE_ROW else {
            return nil
        }

        guard let urlCString = sqlite3_column_text(statement, 0),
              let fileNameCString = sqlite3_column_text(statement, 1) else {
            return nil
        }

        let record = CachedImageRecord(
            urlString: String(cString: urlCString),
            fileName: String(cString: fileNameCString),
            expiresAt: sqlite3_column_double(statement, 2),
            lastAccessedAt: sqlite3_column_double(statement, 3),
            sizeInBytes: Int(sqlite3_column_int64(statement, 4))
        )

        if record.expiresAt <= now.timeIntervalSince1970 {
            deleteRecord(for: urlString)
            return nil
        }

        updateLastAccessDate(for: urlString, now: now)
        return record
    }

    func upsert(urlString: String, fileName: String, expiresAt: Date, sizeInBytes: Int, now: Date) {
        let sql = """
        INSERT INTO image_cache(url, file_name, expires_at, last_accessed_at, size_bytes)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(url) DO UPDATE SET
            file_name = excluded.file_name,
            expires_at = excluded.expires_at,
            last_accessed_at = excluded.last_accessed_at,
            size_bytes = excluded.size_bytes;
        """

        var statement: OpaquePointer?
        defer { sqlite3_finalize(statement) }

        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK else {
            return
        }

        sqlite3_bind_text(statement, 1, (urlString as NSString).utf8String, -1, sqliteTransient)
        sqlite3_bind_text(statement, 2, (fileName as NSString).utf8String, -1, sqliteTransient)
        sqlite3_bind_double(statement, 3, expiresAt.timeIntervalSince1970)
        sqlite3_bind_double(statement, 4, now.timeIntervalSince1970)
        sqlite3_bind_int64(statement, 5, sqlite3_int64(sizeInBytes))
        sqlite3_step(statement)
    }

    func deleteRecord(for urlString: String) {
        let sql = "DELETE FROM image_cache WHERE url = ?;"
        var statement: OpaquePointer?
        defer { sqlite3_finalize(statement) }

        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK else {
            return
        }

        sqlite3_bind_text(statement, 1, (urlString as NSString).utf8String, -1, sqliteTransient)
        sqlite3_step(statement)
    }

    func deleteRecords(urlStrings: [String]) {
        guard urlStrings.isEmpty == false else {
            return
        }

        for urlString in urlStrings {
            deleteRecord(for: urlString)
        }
    }

    func allRecordsOrderedByLastAccess() -> [CachedImageRecord] {
        let sql = "SELECT url, file_name, expires_at, last_accessed_at, size_bytes FROM image_cache ORDER BY last_accessed_at ASC;"
        var statement: OpaquePointer?
        defer { sqlite3_finalize(statement) }

        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK else {
            return []
        }

        var result: [CachedImageRecord] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            guard let urlCString = sqlite3_column_text(statement, 0),
                  let fileNameCString = sqlite3_column_text(statement, 1) else {
                continue
            }

            result.append(
                CachedImageRecord(
                    urlString: String(cString: urlCString),
                    fileName: String(cString: fileNameCString),
                    expiresAt: sqlite3_column_double(statement, 2),
                    lastAccessedAt: sqlite3_column_double(statement, 3),
                    sizeInBytes: Int(sqlite3_column_int64(statement, 4))
                )
            )
        }
        return result
    }

    private func openDatabase() {
        if sqlite3_open(databaseURL.path, &database) != SQLITE_OK {
            print("Не удалось открыть базу кэша изображений")
        }
    }

    private func createTableIfNeeded() {
        let sql = """
        CREATE TABLE IF NOT EXISTS image_cache (
            url TEXT PRIMARY KEY,
            file_name TEXT NOT NULL,
            expires_at REAL NOT NULL,
            last_accessed_at REAL NOT NULL,
            size_bytes INTEGER NOT NULL
        );
        """
        sqlite3_exec(database, sql, nil, nil, nil)
    }

    private func updateLastAccessDate(for urlString: String, now: Date) {
        let sql = "UPDATE image_cache SET last_accessed_at = ? WHERE url = ?;"
        var statement: OpaquePointer?
        defer { sqlite3_finalize(statement) }

        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK else {
            return
        }

        sqlite3_bind_double(statement, 1, now.timeIntervalSince1970)
        sqlite3_bind_text(statement, 2, (urlString as NSString).utf8String, -1, sqliteTransient)
        sqlite3_step(statement)
    }
}

private final class PersistentImageCache {

    private let fileManager: FileManager
    private let queue = DispatchQueue(label: "image.persistent.cache.queue", qos: .utility)
    private let metadataStore: ImageMetadataStore
    private let imagesDirectoryURL: URL

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager

        let baseDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first
            ?? fileManager.temporaryDirectory
        let directory = baseDirectory.appendingPathComponent("ImageCache", isDirectory: true)
        let metadataURL = directory.appendingPathComponent("metadata.sqlite", isDirectory: false)

        imagesDirectoryURL = directory

        if fileManager.fileExists(atPath: directory.path) == false {
            try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true, attributes: nil)
        }

        metadataStore = ImageMetadataStore(databaseURL: metadataURL)
        scheduleCleanup()
    }

    func loadImage(for url: URL, completion: @escaping (UIImage?) -> Void) {
        queue.async { [weak self] in
            guard let self else {
                completion(nil)
                return
            }

            let urlString = url.absoluteString
            let now = Date()

            guard let record = self.metadataStore.record(for: urlString, now: now) else {
                completion(nil)
                return
            }

            let fileURL = self.imagesDirectoryURL.appendingPathComponent(record.fileName, isDirectory: false)

            guard self.fileManager.fileExists(atPath: fileURL.path) else {
                self.metadataStore.deleteRecord(for: urlString)
                completion(nil)
                return
            }

            guard let data = try? Data(contentsOf: fileURL, options: .mappedIfSafe),
                  let image = UIImage(data: data) else {
                try? self.fileManager.removeItem(at: fileURL)
                self.metadataStore.deleteRecord(for: urlString)
                completion(nil)
                return
            }

            completion(image)
        }
    }

    func store(imageData: Data, for url: URL) {
        queue.async { [weak self] in
            guard let self else { return }

            let fileName = self.fileName(for: url)
            let fileURL = self.imagesDirectoryURL.appendingPathComponent(fileName, isDirectory: false)

            do {
                try imageData.write(to: fileURL, options: .atomic)
                self.metadataStore.upsert(
                    urlString: url.absoluteString,
                    fileName: fileName,
                    expiresAt: Date().addingTimeInterval(ImageCachePolicy.fileLifetime),
                    sizeInBytes: imageData.count,
                    now: Date()
                )
                self.cleanupIfNeeded()
            } catch {
                print("Не удалось сохранить изображение в кэш: \(error.localizedDescription)")
            }
        }
    }

    func scheduleCleanup() {
        queue.async { [weak self] in
            self?.cleanupIfNeeded()
        }
    }

    private func cleanupIfNeeded() {
        let records = metadataStore.allRecordsOrderedByLastAccess()
        guard records.isEmpty == false else {
            return
        }

        let now = Date().timeIntervalSince1970
        var totalSize = 0
        var urlsToDelete: [String] = []
        var fileURLsToDelete: [URL] = []
        var validRecords: [CachedImageRecord] = []

        for record in records {
            let fileURL = imagesDirectoryURL.appendingPathComponent(record.fileName, isDirectory: false)
            let isExpired = record.expiresAt <= now
            let fileExists = fileManager.fileExists(atPath: fileURL.path)

            if isExpired || fileExists == false {
                urlsToDelete.append(record.urlString)
                fileURLsToDelete.append(fileURL)
                continue
            }

            totalSize += record.sizeInBytes
            validRecords.append(record)
        }

        if totalSize > ImageCachePolicy.maxDiskSizeInBytes {
            for record in validRecords {
                guard totalSize > ImageCachePolicy.maxDiskSizeInBytes else {
                    break
                }
                totalSize -= record.sizeInBytes
                urlsToDelete.append(record.urlString)
                fileURLsToDelete.append(imagesDirectoryURL.appendingPathComponent(record.fileName, isDirectory: false))
            }
        }

        for fileURL in fileURLsToDelete {
            try? fileManager.removeItem(at: fileURL)
        }
        metadataStore.deleteRecords(urlStrings: urlsToDelete)
    }

    private func fileName(for url: URL) -> String {
        let digest = SHA256.hash(data: Data(url.absoluteString.utf8))
        let hash = digest.map { String(format: "%02x", $0) }.joined()
        let fileExtension = url.pathExtension.isEmpty ? "img" : url.pathExtension
        return "\(hash).\(fileExtension)"
    }
}

final class ImageLoader {
    static let shared = ImageLoader()

    private let memoryCache = NSCache<NSURL, UIImage>()
    private let session: URLSession
    private let persistentCache: PersistentImageCache

    private init(
        session: URLSession = .shared,
        persistentCache: PersistentImageCache = PersistentImageCache()
    ) {
        self.session = session
        self.persistentCache = persistentCache
        memoryCache.countLimit = 150
    }

    func loadImage(from url: URL, completion: @escaping (UIImage?) -> Void) {
        let cacheKey = url as NSURL

        if let image = memoryCache.object(forKey: cacheKey) {
            completion(image)
            return
        }

        persistentCache.loadImage(for: url) { [weak self] diskImage in
            if let diskImage {
                self?.memoryCache.setObject(diskImage, forKey: cacheKey)
                completion(diskImage)
                return
            }

            self?.downloadImage(from: url, cacheKey: cacheKey, completion: completion)
        }
    }

    private func downloadImage(from url: URL, cacheKey: NSURL, completion: @escaping (UIImage?) -> Void) {
        session.dataTask(with: url) { [weak self] data, response, _ in
            guard let self,
                  let httpResponse = response as? HTTPURLResponse,
                  200...299 ~= httpResponse.statusCode,
                  let data,
                  let image = UIImage(data: data) else {
                completion(nil)
                return
            }

            self.memoryCache.setObject(image, forKey: cacheKey)
            self.persistentCache.store(imageData: data, for: url)
            completion(image)
        }.resume()
    }
}
