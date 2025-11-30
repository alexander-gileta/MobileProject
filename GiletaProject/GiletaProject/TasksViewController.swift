import UIKit

class TasksViewController: UIViewController {

    private var tasks: [Task] = []

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
        updateView()
    }

    private func setupTable() {
        tableView.frame = view.bounds
        tableView.dataSource = self
        tableView.delegate = self
        tableView.register(TaskCell.self, forCellReuseIdentifier: "TaskCell")
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

    @objc private func addTapped() {
        let vc = CreateTaskViewController()
        vc.onSave = { [weak self] task in
            self?.tasks.append(task)
            self?.tableView.reloadData()
            self?.updateView()
        }
        navigationController?.pushViewController(vc, animated: true)
    }
}

extension TasksViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return tasks.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "TaskCell", for: indexPath) as! TaskCell
        let task = tasks[indexPath.row]
        cell.configure(with: task)

        cell.onComplete = { [weak self] in
            guard let self = self else { return }
            self.tasks[indexPath.row].isCompleted.toggle()
            tableView.reloadRows(at: [indexPath], with: .automatic)
        }
        return cell
    }
}
