package com.example.taskapp

import android.app.Application
import com.example.taskapp.di.AppContainer

class TaskApp : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
