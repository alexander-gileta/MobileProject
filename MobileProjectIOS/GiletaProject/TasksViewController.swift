import UIKit

class TasksViewController: UIViewController {

    private var tasks: [Task] = []
    private var sections: [Int] = []

    private let tableView = UITableView()
    private let emptyLabel: UILabel = {
        let label = UILabel()
        label.text = "Задач нет"
        label.textAlignment = .center
        label.font = .systemFont(ofSize: 20)
        return label
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "Задачи"

        navigationItem.rightBarButtonItem = UIBarButtonItem(
            barButtonSystemItem: .add,
            target: self,
            action: #selector(addTapped)
        )

        setupTable()
        sortTasks()
        rebuildSections()
        updateView()
    }

    private func setupTable() {
        tableView.frame = view.bounds
        tableView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(TaskCell.self, forCellReuseIdentifier: TaskCell.reuseIdentifier)
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 100
        view.addSubview(tableView)
    }

    private func updateView() {
        if tasks.isEmpty {
            tableView.isHidden = true
            emptyLabel.frame = view.bounds
            view.addSubview(emptyLabel)
        } else {
            tableView.isHidden = false
            emptyLabel.removeFromSuperview()
        }
    }

    private func sortTasks() {
        tasks.sort { $0.priority > $1.priority }
    }

    private func rebuildSections() {
        let uniquePriorities = Set(tasks.map { $0.priority })
        sections = uniquePriorities.sorted(by: >)
    }

    private func taskInfo(for indexPath: IndexPath) -> (task: Task, globalIndex: Int) {
        let priority = sections[indexPath.section]
        let filtered = tasks.enumerated().filter { $0.element.priority == priority }
        let pair = filtered[indexPath.row]
        return (pair.element, pair.offset)
    }

    @objc private func addTapped() {
        let vc = CreateTaskViewController()
        vc.onSave = { [weak self] task in
            guard let self else { return }
            self.tasks.append(task)
            self.sortTasks()
            self.rebuildSections()
            self.tableView.reloadData()
            self.updateView()
        }
        navigationController?.pushViewController(vc, animated: true)
    }
}

extension TasksViewController: UITableViewDataSource, UITableViewDelegate {
    func numberOfSections(in tableView: UITableView) -> Int {
        sections.count
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let priority = sections[section]
        return tasks.filter { $0.priority == priority }.count
    }

    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        let priority = sections[section]
        return "Приоритет P\(priority)"
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(
            withIdentifier: TaskCell.reuseIdentifier,
            for: indexPath
        ) as? TaskCell else {
            return UITableViewCell(style: .subtitle, reuseIdentifier: "FallbackTaskCell")
        }

        let info = taskInfo(for: indexPath)
        cell.configure(with: info.task)

        cell.onComplete = { [weak self, weak tableView] in
            guard let self, let tableView else { return }
            self.tasks[info.globalIndex].isCompleted.toggle()
            tableView.reloadRows(at: [indexPath], with: .automatic)
        }

        return cell
    }
}
