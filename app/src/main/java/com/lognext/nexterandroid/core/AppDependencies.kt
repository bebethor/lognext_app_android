package com.lognext.nexterandroid.core

import android.content.Context
import com.lognext.nexterandroid.core.auth.AuthRepository
import com.lognext.nexterandroid.core.auth.MsalAuthRepository
import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.OkHttpAPIClient
import com.lognext.nexterandroid.features.clock.ClockService
import com.lognext.nexterandroid.features.home.HomeService
import com.lognext.nexterandroid.features.people.PeopleService

object AppDependencies {
    private var initialized = false

    lateinit var authRepository: AuthRepository
        private set

    lateinit var apiClient: APIClient
        private set

    lateinit var homeService: HomeService
        private set

    lateinit var clockService: ClockService
        private set

    lateinit var peopleService: PeopleService
        private set

    fun initialize(context: Context) {
        if (initialized) return

        authRepository = MsalAuthRepository(context.applicationContext)
        apiClient = OkHttpAPIClient(authRepository)
        homeService = HomeService(apiClient)
        clockService = ClockService(apiClient)
        peopleService = PeopleService(apiClient)
        initialized = true
    }
}
