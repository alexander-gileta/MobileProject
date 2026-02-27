import UIKit

class TaskCell: UITableViewCell {

    var onComplete: (() -> Void)?

    private let titleLabel = UILabel()
    private let descriptionLabel = UILabel()
    private let priorityLabel = UILabel()
    private let flagLabel = UILabel()      // будет справа
    private let deadlineLabel = UILabel()

    private let checkButton: UIButton = {
        let btn = UIButton(type: .system)
        btn.setImage(UIImage(systemName: "circle"), for: .normal)
        return btn
    }()

    private let infoStack: UIStackView = UIStackView()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)

        checkButton.addTarget(self, action: #selector(toggleComplete), for: .touchUpInside)

        // ЛЕВЫЙ вертикальный стек с информацией о задаче
        infoStack.axis = .vertical
        infoStack.spacing = 4
        infoStack.alignment = .leading
        infoStack.addArrangedSubview(titleLabel)
        infoStack.addArrangedSubview(descriptionLabel)
        infoStack.addArrangedSubview(priorityLabel)
        // ❌ БЫЛО: infoStack.addArrangedSubview(flagLabel)
        infoStack.addArrangedSubview(deadlineLabel)

        // НАСТРОЙКА flagLabel, который будет справа
        flagLabel.font = .systemFont(ofSize: 14)
        flagLabel.textAlignment = .right
        flagLabel.setContentHuggingPriority(.required, for: .horizontal)
        flagLabel.setContentCompressionResistancePriority(.required, for: .horizontal)

        // ГОРИЗОНТАЛЬНЫЙ стек: чекбокс слева, инфо по центру, флаг справа
        let mainStack = UIStackView(arrangedSubviews: [checkButton, infoStack, flagLabel])
        mainStack.axis = .horizontal
        mainStack.spacing = 12
        mainStack.alignment = .top
        mainStack.distribution = .fill

        contentView.addSubview(mainStack)
        mainStack.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            mainStack.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            mainStack.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            mainStack.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            mainStack.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -12)
        ])

        titleLabel.font = .boldSystemFont(ofSize: 16)
        descriptionLabel.font = .systemFont(ofSize: 14)
        descriptionLabel.textColor = .secondaryLabel
        priorityLabel.font = .systemFont(ofSize: 14)
        deadlineLabel.font = .systemFont(ofSize: 14)
        deadlineLabel.textColor = .systemRed
    }

    @objc private func toggleComplete() {
        onComplete?()
    }

    func configure(with task: Task) {
        titleLabel.text = task.title
        descriptionLabel.text = task.description
        priorityLabel.text = "Приоритет: P\(task.priority)"

        // флаг справа
        if task.flagged {
            flagLabel.text = "⚑"
            flagLabel.isHidden = false
        } else {
            flagLabel.text = nil
            flagLabel.isHidden = true
        }

        if let deadline = task.deadline {
            let formatter = DateFormatter()
            formatter.dateStyle = .short
            formatter.timeStyle = .short
            deadlineLabel.text = "Дедлайн: \(formatter.string(from: deadline))"
        } else {
            deadlineLabel.text = ""
        }

        let icon = task.isCompleted ? "checkmark.circle.fill" : "circle"
        checkButton.setImage(UIImage(systemName: icon), for: .normal)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
