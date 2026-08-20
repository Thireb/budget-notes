package com.budgetnotes.app

import android.app.Application

class BudgetNotesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Databases stay closed until PIN/biometric unlock.
        container = AppContainer(this)
    }
}
