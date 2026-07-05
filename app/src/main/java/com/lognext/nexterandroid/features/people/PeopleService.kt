package com.lognext.nexterandroid.features.people

import com.google.gson.annotations.SerializedName
import com.lognext.nexterandroid.core.network.APIClient
import com.lognext.nexterandroid.core.network.RestService

class PeopleService(
    apiClient: APIClient
) : RestService(apiClient) {
    suspend fun me(): PersonProfileResponse = get("/api/v1/staff/me", PersonProfileResponse::class.java)

    suspend fun profile(personCode: String): PersonProfileResponse {
        return get("/api/v1/staff/profile/$personCode", PersonProfileResponse::class.java)
    }

    suspend fun team(personCode: String): TeamResponse = get("/api/v1/staff/team/$personCode", TeamResponse::class.java)

    suspend fun manager(personCode: String): ManagerResponse = get("/api/v1/staff/manager/$personCode", ManagerResponse::class.java)

    suspend fun projects(): ProjectListResponse = get("/api/v1/staff/projects", ProjectListResponse::class.java)

    suspend fun searchStaff(query: String): StaffSearchResponse {
        return get("/api/v1/staff/search", StaffSearchResponse::class.java, mapOf("q" to query))
    }
}

data class PersonProfileResponse(
    @SerializedName("person_code") val personCode: String = "",
    @SerializedName("full_name") val fullName: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("family_name") val familyName: String = "",
    val email: String = "",
    @SerializedName("job_title") val jobTitle: String = "",
    @SerializedName("org_unit_name") val orgUnitName: String = "",
    @SerializedName("org_unit_code") val orgUnitCode: String = "",
    @SerializedName("position_title") val positionTitle: String = "",
    @SerializedName("manager_name") val managerName: String = "",
    @SerializedName("manager_person_code") val managerPersonCode: String = "",
    @SerializedName("work_phone") val workPhone: String = "",
    @SerializedName("mobile_phone") val mobilePhone: String = "",
    @SerializedName("hire_date") val hireDate: String = "",
    val company: String = "",
    val active: Boolean = true
) {
    fun toPeopleRow(isCurrentUser: Boolean = false): PeopleRowData {
        val displayName = fullName.ifBlank { listOf(firstName, familyName).filter { it.isNotBlank() }.joinToString(" ") }
        return PeopleRowData(
            id = personCode.ifBlank { displayName },
            personCode = personCode,
            initials = peopleInitials(displayName),
            name = displayName,
            role = positionTitle.ifBlank { jobTitle },
            color = peopleColor(personCode.ifBlank { displayName }),
            orgUnitName = orgUnitName,
            email = email,
            workPhone = workPhone.ifBlank { mobilePhone },
            positionTitle = positionTitle,
            hireDate = hireDate,
            company = company,
            managerPersonCode = managerPersonCode,
            managerName = managerName,
            isCurrentUser = isCurrentUser
        )
    }
}

data class PersonSummaryResponse(
    @SerializedName("person_code") val personCode: String = "",
    @SerializedName("full_name") val fullName: String = "",
    val email: String = "",
    @SerializedName("job_title") val jobTitle: String = "",
    @SerializedName("org_unit_name") val orgUnitName: String = ""
) {
    fun toPeopleRow(isCurrentUser: Boolean = false): PeopleRowData {
        return PeopleRowData(
            id = personCode.ifBlank { fullName },
            personCode = personCode,
            initials = peopleInitials(fullName),
            name = fullName,
            role = jobTitle,
            color = peopleColor(personCode.ifBlank { fullName }),
            orgUnitName = orgUnitName,
            email = email,
            isCurrentUser = isCurrentUser
        )
    }
}

data class TeamResponse(
    @SerializedName("org_unit") val orgUnit: OrgUnitResponse? = null,
    val members: List<PersonSummaryResponse> = emptyList()
)

data class ManagerResponse(
    val manager: PersonSummaryResponse? = null
)

data class StaffSearchResponse(
    val query: String = "",
    val people: List<PersonSummaryResponse> = emptyList()
)

data class ProjectListResponse(
    val projects: List<ProjectResponse> = emptyList()
)

data class ProjectResponse(
    @SerializedName("project_code") val projectCode: String = "",
    @SerializedName("project_name") val projectName: String = "",
    val description: String = ""
) {
    fun toPeopleProject(): PeopleProject = PeopleProject(projectCode, projectName, description)
}

data class OrgUnitResponse(
    @SerializedName("org_unit_code") val orgUnitCode: String = "",
    @SerializedName("org_unit_name") val orgUnitName: String = "",
    @SerializedName("manager_person_code") val managerPersonCode: String = "",
    @SerializedName("manager_name") val managerName: String = ""
)
