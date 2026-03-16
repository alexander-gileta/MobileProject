import UIKit

final class NewsTableViewCell: UITableViewCell {

    static let reuseIdentifier = "NewsTableViewCell"

    private let previewImageView = UIImageView()
    private let titleLabel = UILabel()
    private let abstractLabel = UILabel()
    private let sourceLabel = UILabel()
    private var representedImageURL: URL?

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        setupLayout()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        representedImageURL = nil
        previewImageView.image = placeholderImage()
        previewImageView.contentMode = .scaleAspectFit
    }

    func configure(with article: NewsArticle) {
        titleLabel.text = article.title
        abstractLabel.text = article.abstract
        let metadataParts = [article.source, article.section, NewsDateFormatter.displayString(from: article.publishedDate)]
            .filter { $0.isEmpty == false }
        sourceLabel.text = metadataParts.joined(separator: " • ")

        previewImageView.image = placeholderImage()
        previewImageView.contentMode = .scaleAspectFit
        representedImageURL = article.imageURL

        guard let imageURL = article.imageURL else {
            return
        }

        ImageLoader.shared.loadImage(from: imageURL) { [weak self] image in
            DispatchQueue.main.async {
                guard let self, self.representedImageURL == imageURL else { return }
                if let image {
                    self.previewImageView.image = image
                    self.previewImageView.contentMode = .scaleAspectFill
                } else {
                    self.previewImageView.image = self.placeholderImage()
                    self.previewImageView.contentMode = .scaleAspectFit
                }
            }
        }
    }

    private func setupLayout() {
        previewImageView.translatesAutoresizingMaskIntoConstraints = false
        previewImageView.clipsToBounds = true
        previewImageView.layer.cornerRadius = 12
        previewImageView.backgroundColor = .secondarySystemBackground
        previewImageView.image = placeholderImage()
        previewImageView.contentMode = .scaleAspectFit

        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.font = .boldSystemFont(ofSize: 18)
        titleLabel.numberOfLines = 0

        abstractLabel.translatesAutoresizingMaskIntoConstraints = false
        abstractLabel.font = .systemFont(ofSize: 15)
        abstractLabel.textColor = .secondaryLabel
        abstractLabel.numberOfLines = 0

        sourceLabel.translatesAutoresizingMaskIntoConstraints = false
        sourceLabel.font = .systemFont(ofSize: 13, weight: .medium)
        sourceLabel.textColor = .tertiaryLabel
        sourceLabel.numberOfLines = 0

        let textStack = UIStackView(arrangedSubviews: [titleLabel, abstractLabel, sourceLabel])
        textStack.translatesAutoresizingMaskIntoConstraints = false
        textStack.axis = .vertical
        textStack.spacing = 8
        textStack.alignment = .fill

        let contentStack = UIStackView(arrangedSubviews: [previewImageView, textStack])
        contentStack.translatesAutoresizingMaskIntoConstraints = false
        contentStack.axis = .horizontal
        contentStack.spacing = 12
        contentStack.alignment = .top

        contentView.addSubview(contentStack)

        NSLayoutConstraint.activate([
            contentStack.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            contentStack.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            contentStack.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            contentStack.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -12),

            previewImageView.widthAnchor.constraint(equalToConstant: 110),
            previewImageView.heightAnchor.constraint(equalToConstant: 90)
        ])
    }

    private func placeholderImage() -> UIImage? {
        UIImage(systemName: "photo.on.rectangle.angled")
    }
}
