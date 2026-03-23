//
//  SceneDelegate.swift
//  GiletaProject
//
//  Created by Alexander on 30.11.2025.
//

import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?


    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = (scene as? UIWindowScene) else { return }

                window = UIWindow(windowScene: windowScene)

                let tabBar = MainTabBarController()
                window?.rootViewController = tabBar
                window?.makeKeyAndVisible()
    }


}

