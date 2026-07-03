package com.lognext.nexterandroid.core

import com.lognext.nexterandroid.core.auth.AuthRepository
import com.lognext.nexterandroid.core.auth.PlaceholderAuthRepository
import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.OkHttpAPIClient

object AppDependencies {
    val apiClient: APIClient by lazy { OkHttpAPIClient() }
    val authRepository: AuthRepository by lazy { PlaceholderAuthRepository() }
}
