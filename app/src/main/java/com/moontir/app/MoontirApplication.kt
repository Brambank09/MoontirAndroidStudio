package com.moontir.app

import android.app.Application
import com.moontir.app.data.local.SessionManager
import com.moontir.app.data.repository.MoontirRepository

class MoontirApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var repository: MoontirRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager(this)
        repository = MoontirRepository(sessionManager)
    }

    companion object {
        lateinit var instance: MoontirApplication
            private set
    }
}
