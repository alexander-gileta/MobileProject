//
//  MainTabBarController.swift
//  GiletaProject
//
//  Created by Alexander on 30.11.2025.
//
import UIKit

class MainTabBarController: UITabBarController {

    override func viewDidLoad() {
        super.viewDidLoad()

        viewControllers = [
            createNavController(vc: UIViewController(), title: "Главная", icon: "house"),
            createNavController(vc: TasksViewController(), title: "Задачи", icon: "checklist"),
            createNavController(vc: UIViewController(), title: "Записи", icon: "note.text")
        ]
    }

    private func createNavController(vc: UIViewController, title: String, icon: String) -> UINavigationController {
        vc.title = title
        let nav = UINavigationController(rootViewController: vc)
        nav.tabBarItem.title = title
        nav.tabBarItem.image = UIImage(systemName: icon)
        return nav
    }
}
