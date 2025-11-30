//
//  CreateTaskViewController.swift
//  GiletaProject
//
//  Created by Alexander on 30.11.2025.
//

import UIKit

class CreateTaskViewController: UIViewController {

    var onSave: ((Task) -> Void)?

    private let titleField: UITextField = {
        let field = UITextField()
        field.placeholder = "Название задачи"
        field.borderStyle = .roundedRect
        return field
    }()

    private let descriptionField: UITextView = {
        let tv = UITextView()
        tv.layer.borderWidth = 1
        tv.layer.cornerRadius = 8
        tv.layer.borderColor = UIColor.secondaryLabel.cgColor
        tv.textContainerInset = UIEdgeInsets(top: 8, left: 6, bottom: 8, right: 6)
        tv.font = .systemFont(ofSize: 16)
        return tv
    }()

    private var priority: Int = 1
    private var flagged: Bool = false
    private var priorityButtonsArray: [UIButton] = []
    private var flagButtonRef: UIButton!

    private let dateSwitch = UISwitch()

    private let datePicker: UIDatePicker = {
        let picker = UIDatePicker()
        picker.datePickerMode = .dateAndTime
        picker.minimumDate = Date()
        picker.isHidden = true
        return picker
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .systemBackground
        title = "Новая задача"

        navigationItem.rightBarButtonItem = UIBarButtonItem(
            title: "Сохранить",
            style: .done,
            target: self,
            action: #selector(saveTapped)
        )

        layoutUI()
        addKeyboardHideGesture()
    }

    private func layoutUI() {
        let stack = UIStackView(arrangedSubviews: [
            titleField,
            descriptionField,
            makePriorityButtons(),
            makeFlagButton(),
            makeDeadlineRow(),
            datePicker
        ])

        stack.axis = .vertical
        stack.spacing = 16

        view.addSubview(stack)

        stack.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
            descriptionField.heightAnchor.constraint(equalToConstant: 120)
        ])

        dateSwitch.addTarget(self, action: #selector(toggleDatePicker), for: .valueChanged)
    }

    private func makePriorityButtons() -> UIStackView {
        priorityButtonsArray = (1...3).map { num in
            let btn = UIButton(type: .system)
            btn.setTitle("P\(num)", for: .normal)
            btn.tag = num
            btn.layer.borderWidth = 1
            btn.layer.cornerRadius = 8
            btn.layer.borderColor = UIColor.secondaryLabel.cgColor
            btn.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
            btn.addTarget(self, action: #selector(setPriority(_:)), for: .touchUpInside)
            return btn
        }

        updatePriorityUI()

        let stack = UIStackView(arrangedSubviews: priorityButtonsArray)
        stack.axis = .horizontal
        stack.spacing = 12
        stack.distribution = .fillEqually 

        return stack
    }


    @objc private func setPriority(_ sender: UIButton) {
        priority = sender.tag
        updatePriorityUI()
    }

    private func updatePriorityUI() {
        for btn in priorityButtonsArray {
            if btn.tag == priority {
                btn.backgroundColor = .systemBlue
                btn.setTitleColor(.white, for: .normal)
            } else {
                btn.backgroundColor = .clear
                btn.setTitleColor(.systemBlue, for: .normal)
            }
        }
    }

    private func makeFlagButton() -> UIButton {
        flagButtonRef = UIButton(type: .system)
        flagButtonRef.setTitle("⚑ Флаг", for: .normal)
        flagButtonRef.addTarget(self, action: #selector(toggleFlag), for: .touchUpInside)
        updateFlagUI()
        return flagButtonRef
    }

    @objc private func toggleFlag() {
        flagged.toggle()
        updateFlagUI()
    }

    private func updateFlagUI() {
        flagButtonRef.tintColor = flagged ? .systemRed : .systemBlue
    }

    private func makeDeadlineRow() -> UIStackView {
        let label = UILabel()
        label.text = "Дедлайн"

        let row = UIStackView(arrangedSubviews: [label, dateSwitch])
        row.axis = .horizontal
        row.distribution = .equalSpacing
        return row
    }

    @objc private func toggleDatePicker() {
        UIView.animate(withDuration: 0.25) {
            self.datePicker.isHidden = !self.dateSwitch.isOn
        }
    }

    @objc private func saveTapped() {
        guard let title = titleField.text, !title.isEmpty else { return }

        let task = Task(
            title: title,
            description: descriptionField.text ?? "",
            priority: priority,
            flagged: flagged,
            deadline: dateSwitch.isOn ? datePicker.date : nil
        )

        onSave?(task)
        navigationController?.popViewController(animated: true)
    }

    private func addKeyboardHideGesture() {
        let tap = UITapGestureRecognizer(target: self, action: #selector(hideKeyboard))
        view.addGestureRecognizer(tap)
    }

    @objc private func hideKeyboard() {
        view.endEditing(true)
    }
}
