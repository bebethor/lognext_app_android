package com.lognext.nexterandroid.core

object AppConfig {
    const val UseFakeLogin = true

    const val ClientId = "8cebd652-bfab-4e10-ae5a-c67214ac9d03"
    const val TenantId = "b3c806dc-e5d8-44d0-b9e2-aedd316928ea"
    const val Authority = "https://login.microsoftonline.com/$TenantId"
    const val BffScope = "api://$ClientId/access_as_user"
    const val BaseUrl = "https://nexter.lognext.com/"
    const val DefaultPlannerPlanId = ""
    const val RedirectUri = "msauth://com.lognext.nexterandroid/254KWMWKtDuxHRVMIEJSTVi/tmE="

    val graphScopes = listOf(
        "User.Read",
        "Sites.Read.All",
        "Files.Read.All",
        "Calendars.Read",
        "Tasks.ReadWrite"
    )

    val bffScopes = listOf(BffScope)
}
