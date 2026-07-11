package com.lognext.nexterandroid.core

object AppConfig {
    const val UseFakeLogin = false

    const val ClientId = "02734a6d-c892-48c3-9388-b548ac422deb"
    const val TenantId = "b3c806dc-e5d8-44d0-b9e2-aedd316928ea"
    const val Authority = "https://login.microsoftonline.com/$TenantId"

    const val BackendApplicationIdUri = "api://8cebd652-bfab-4e10-ae5a-c67214ac9d03"
    const val BffScopeName = "access_as_user"

    const val BaseUrl = "https://nexter.lognext.com/"
    const val DefaultPlannerPlanId = ""
    const val RedirectUri = "msauth://com.lognext.nexterandroid/254KWMWKtDuxHRVMIEJSTVi%2FtmE%3D"

    val graphScopes = listOf("User.Read")

    val bffScopes = if (BackendApplicationIdUri.isBlank()) {
        emptyList()
    } else {
        listOf("$BackendApplicationIdUri/$BffScopeName")
    }
}
