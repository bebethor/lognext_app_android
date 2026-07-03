package com.lognext.nexterandroid.core

import android.content.Context
import com.lognext.nexterandroid.core.auth.AuthRepository
import com.lognext.nexterandroid.core.auth.MsalAuthRepository
import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.OkHttpAPIClient

object AppDependencies {
    private var initialized = false

    lateinit var authRepository: AuthRepository
        private set

    val apiClient: APIClient by lazy { OkHttpAPIClient() }

    fun initialize(context: Context) {
        if (initialized) return

        authRepository = MsalAuthRepository(context.applicationContext)
        initialized = true
    }
}
