import SafariServices
import UIKit

final class NewsViewController: UIViewController {

    private let newsService: NewsServiceProtocol
    private let additionalRequestLogger = AdditionalRequestLogger()

    private var articles: [NewsArticle] = []
    private var refreshTimer: Timer?
    private var isRequestInFlight = false

    private let tableView = UITableView(frame: .zero, style: .plain)
    private let loadingIndicator: UIActivityIndicatorView = {
        let indicator = UIActivityIndicatorView(style: .large)
        indicator.hidesWhenStopped = true
        return indicator
    }()
    private let stateLabel: UILabel = {
        let label = UILabel()
        label.textAlignment = .center
        label.numberOfLines = 0
        label.textColor = .secondaryLabel
        label.font = .systemFont(ofSize: 17)
        return label
    }()
    private lazy var retryButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Повторить", for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 17, weight: .semibold)
        button.addTarget(self, action: #selector(retryTapped), for: .touchUpInside)
        return button
    }()
    private lazy var refreshBarButtonItem: UIBarButtonItem = {
        let indicator = UIActivityIndicatorView(style: .medium)
        indicator.startAnimating()
        return UIBarButtonItem(customView: indicator)
    }()

    init(newsService: NewsServiceProtocol = NYTNewsService()) {
        self.newsService = newsService
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "Новости"

        setupTableView()
        setupStateViews()
        fetchNews(showBlockingLoader: true)
        additionalRequestLogger.sendDebugRequest()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        startAutoRefresh()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        stopAutoRefresh()
    }

    deinit {
        stopAutoRefresh()
    }

    private func setupTableView() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(NewsTableViewCell.self, forCellReuseIdentifier: NewsTableViewCell.reuseIdentifier)
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 160
        tableView.separatorInset = UIEdgeInsets(top: 0, left: 16, bottom: 0, right: 16)
        tableView.isHidden = true

        let refreshControl = UIRefreshControl()
        refreshControl.addTarget(self, action: #selector(handlePullToRefresh), for: .valueChanged)
        tableView.refreshControl = refreshControl

        view.addSubview(tableView)

        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    private func setupStateViews() {
        loadingIndicator.translatesAutoresizingMaskIntoConstraints = false
        stateLabel.translatesAutoresizingMaskIntoConstraints = false
        retryButton.translatesAutoresizingMaskIntoConstraints = false
        retryButton.isHidden = true

        view.addSubview(loadingIndicator)
        view.addSubview(stateLabel)
        view.addSubview(retryButton)

        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            loadingIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -28),

            stateLabel.topAnchor.constraint(equalTo: loadingIndicator.bottomAnchor, constant: 16),
            stateLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            stateLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),

            retryButton.topAnchor.constraint(equalTo: stateLabel.bottomAnchor, constant: 16),
            retryButton.centerXAnchor.constraint(equalTo: view.centerXAnchor)
        ])
    }

    private func fetchNews(showBlockingLoader: Bool) {
        guard isRequestInFlight == false else { return }
        isRequestInFlight = true

        if showBlockingLoader, articles.isEmpty {
            stateLabel.text = "Загрузка новостей..."
            retryButton.isHidden = true
            loadingIndicator.startAnimating()
            tableView.isHidden = true
        } else if articles.isEmpty == false {
            navigationItem.rightBarButtonItem = refreshBarButtonItem
        }

        newsService.fetchLatestNews { [weak self] result in
            DispatchQueue.main.async {
                guard let self else { return }
                self.isRequestInFlight = false
                self.tableView.refreshControl?.endRefreshing()
                self.navigationItem.rightBarButtonItem = nil
                self.loadingIndicator.stopAnimating()

                switch result {
                case let .success(articles):
                    self.articles = articles
                    self.stateLabel.text = articles.isEmpty ? "Новостей пока нет" : nil
                    self.retryButton.isHidden = true
                    self.tableView.isHidden = articles.isEmpty
                    self.tableView.reloadData()
                case let .failure(error):
                    if self.articles.isEmpty {
                        self.tableView.isHidden = true
                        self.stateLabel.text = "Не удалось загрузить новости.\n\(error.localizedDescription)"
                        self.retryButton.isHidden = false
                    } else {
                        print("Ошибка обновления новостей: \(error.localizedDescription)")
                    }
                }
            }
        }
    }

    private func startAutoRefresh() {
        stopAutoRefresh()
        refreshTimer = Timer.scheduledTimer(withTimeInterval: 120, repeats: true) { [weak self] _ in
            self?.fetchNews(showBlockingLoader: false)
        }
    }

    private func stopAutoRefresh() {
        refreshTimer?.invalidate()
        refreshTimer = nil
    }

    @objc private func retryTapped() {
        fetchNews(showBlockingLoader: true)
    }

    @objc private func handlePullToRefresh() {
        fetchNews(showBlockingLoader: false)
    }
}

extension NewsViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        articles.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(
            withIdentifier: NewsTableViewCell.reuseIdentifier,
            for: indexPath
        ) as? NewsTableViewCell else {
            return UITableViewCell(style: .subtitle, reuseIdentifier: "FallbackNewsCell")
        }

        cell.configure(with: articles[indexPath.row])
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)

        guard let url = articles[indexPath.row].articleURL else { return }
        let safariViewController = SFSafariViewController(url: url)
        present(safariViewController, animated: true)
    }
}
