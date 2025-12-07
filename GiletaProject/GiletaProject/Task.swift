//
//  Task.swift
//  GiletaProject
//
//  Created by Alexander on 30.11.2025.
//
import UIKit

struct Task {
    var title: String
    var description: String?
    var priority: Int 
    var flagged: Bool
    var deadline: Date?
    var isCompleted: Bool = false
}
