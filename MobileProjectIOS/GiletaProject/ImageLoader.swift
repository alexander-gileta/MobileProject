import UIKit

final class ImageLoader {
    static let shared = ImageLoader()

    private let cache = NSCache<NSURL, UIImage>()
    private let session: URLSession

    private init(session: URLSession = .shared) {
        self.session = session
    }

    func loadImage(from url: URL, completion: @escaping (UIImage?) -> Void) {
        let key = url as NSURL

        if let cached = cache.object(forKey: key) {
            completion(cached)
            return
        }

        session.dataTask(with: url) { [weak self] data, response, _ in
            guard let self,
                  let httpResponse = response as? HTTPURLResponse,
                  200...299 ~= httpResponse.statusCode,
                  let data,
                  let image = UIImage(data: data) else {
                completion(nil)
                return
            }

            self.cache.setObject(image, forKey: key)
            completion(image)
        }.resume()
    }
}
