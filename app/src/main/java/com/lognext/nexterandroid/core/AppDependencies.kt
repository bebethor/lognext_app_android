package com.lognext.nexterandroid.core

import android.content.Context
import com.lognext.nexterandroid.core.auth.AuthRepository
import com.lognext.nexterandroid.core.auth.MsalAuthRepository
import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.OkHttpAPIClient
import com.lognext.nexterandroid.features.clock.ClockService
import com.lognext.nexterandroid.features.home.HomeService
import com.lognext.nexterandroid.features.more.VacationService
import com.lognext.nexterandroid.features.people.PeopleService

object AppDependencies {
    private var initialized = false

    lateinit var authRepository: AuthRepository
        private set

    lateinit var responseCache: com.lognext.nexterandroid.core.network.ResponseCache
        private set

    fun clearResponseCache(delete: Boolean = false) {
        val account = (authRepository.authState.value as? com.lognext.nexterandroid.core.auth.AuthState.Authenticated)?.user?.username
        if (account != null) { if (delete) responseCache.clear(account) else responseCache.invalidate(account) }
    }

    lateinit var apiClient: APIClient
        private set

    lateinit var homeService: HomeService
        private set

    lateinit var clockService: ClockService
        private set

    lateinit var peopleService: PeopleService
        private set

    lateinit var vacationService: VacationService
        private set

    fun initialize(context: Context) {
        if (initialized) return

        authRepository = MsalAuthRepository(context.applicationContext)
        responseCache = com.lognext.nexterandroid.core.network.ResponseCache(context.applicationContext)
        apiClient = OkHttpAPIClient(authRepository, responseCache)
        homeService = HomeService(apiClient)
        clockService = ClockService(apiClient)
        peopleService = PeopleService(apiClient)
        vacationService = VacationService(apiClient)
        initialized = true
    }
}
