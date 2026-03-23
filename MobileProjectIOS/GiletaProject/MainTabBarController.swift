import UIKit

class MainTabBarController: UITabBarController {

    override func viewDidLoad() {
        super.viewDidLoad()

        viewControllers = [
            createNavController(vc: NewsViewController(), title: "Новости", icon: "newspaper"),
            createNavController(vc: TasksViewController(), title: "Задачи", icon: "checklist"),
            createNavController(
                vc: PlaceholderViewController(
                    screenTitle: "Записи",
                    message: "Экран записей пока не реализован"
                ),
                title: "Записи",
                icon: "note.text"
            )
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
