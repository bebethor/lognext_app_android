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

    suspend fun orgTree(): OrgTreeResponse = get("/api/v1/staff/org-tree", OrgTreeResponse::class.java)

    suspend fun projects(): ProjectListResponse = get("/api/v1/staff/projects", ProjectListResponse::class.java)

    suspend fun projectMembers(projectCode: String): ProjectMembersResponse {
        return get("/api/v1/staff/projects/$projectCode/members", ProjectMembersResponse::class.java)
    }

    suspend fun searchStaff(query: String): StaffSearchResponse {
        return get("/api/v1/staff/search", StaffSearchResponse::class.java, mapOf("q" to query))
    }

    suspend fun searchStaffSmart(query: String): StaffSearchResponse {
        val candidates = searchCandidates(query)
        val mergedPeople = mutableListOf<PersonSummaryResponse>()
        val seenPersonCodes = mutableSetOf<String>()

        candidates.forEach { candidate ->
            runCatching {
                searchStaff(candidate)
            }.getOrNull()
                ?.people
                ?.filter { it.matches(query) }
                ?.forEach { person ->
                    val personCode = person.personCode.orEmpty()
                    if (personCode.isNotBlank() && seenPersonCodes.add(personCode)) {
                        mergedPeople += person
                    }
                }
        }

        return StaffSearchResponse(query = query, people = mergedPeople)
    }

    private fun searchCandidates(query: String): List<String> {
        val trimmed = query.trim()
        val normalized = trimmed
            .normalizeSearch()
            .replace("-", " ")
            .replace(".", " ")
            .replace(",", " ")
        val words = normalized.split(" ").filter { it.length >= 2 }.sortedByDescending { it.length }
        return (listOf(trimmed.uppercase(), normalized.uppercase()) + words.map { it.uppercase() })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}

data class PersonProfileResponse(
    @SerializedName("person_code") val personCode: String? = "",
    @SerializedName("full_name") val fullName: String? = "",
    @SerializedName("first_name") val firstName: String? = "",
    @SerializedName("family_name") val familyName: String? = "",
    val email: String? = "",
    @SerializedName("job_title") val jobTitle: String? = "",
    @SerializedName("org_unit_name") val orgUnitName: String? = "",
    @SerializedName("org_unit_code") val orgUnitCode: String? = "",
    @SerializedName("position_title") val positionTitle: String? = "",
    @SerializedName(
        value = "manager_name",
        alternate = [
            "reports_to_name",
            "report_to_name",
            "line_manager_name",
            "supervisor_name",
            "responsible_name",
            "approver_name"
        ]
    ) val managerName: String? = "",
    @SerializedName(
        value = "manager_person_code",
        alternate = [
            "reports_to_person_code",
            "report_to_person_code",
            "line_manager_person_code",
            "supervisor_person_code",
            "responsible_person_code",
            "approver_person_code"
        ]
    ) val managerPersonCode: String? = "",
    @SerializedName("work_phone") val workPhone: String? = "",
    @SerializedName("mobile_phone") val mobilePhone: String? = "",
    @SerializedName("hire_date") val hireDate: String? = "",
    val company: String? = "",
    val active: Boolean = true
) {
    fun toPeopleRow(isCurrentUser: Boolean = false): PeopleRowData {
        val displayName = fullName.orEmpty().ifBlank {
            listOf(firstName.orEmpty(), familyName.orEmpty()).filter { it.isNotBlank() }.joinToString(" ")
        }
        val resolvedPersonCode = personCode.orEmpty()
        val resolvedJobTitle = jobTitle.orEmpty()
        val resolvedPositionTitle = positionTitle.orEmpty()
        return PeopleRowData(
            id = resolvedPersonCode.ifBlank { displayName },
            personCode = resolvedPersonCode,
            initials = peopleInitials(displayName),
            name = displayName,
            role = resolvedJobTitle.ifBlank { resolvedPositionTitle }.ifBlank { "Sin puesto definido" },
            color = peopleColor(resolvedPersonCode.ifBlank { displayName }),
            orgUnitName = orgUnitName.orEmpty(),
            email = email.orEmpty(),
            workPhone = workPhone.orEmpty().ifBlank { mobilePhone.orEmpty() },
            positionTitle = resolvedPositionTitle,
            hireDate = formatPeopleDate(hireDate.orEmpty()),
            company = company.orEmpty(),
            managerPersonCode = managerPersonCode.orEmpty(),
            managerName = managerName.orEmpty(),
            isCurrentUser = isCurrentUser
        )
    }

    fun toPeopleRow(fallback: PeopleRowData): PeopleRowData {
        val resolved = toPeopleRow(isCurrentUser = fallback.isCurrentUser)
        return resolved.copy(
            id = resolved.id.ifBlank { fallback.id },
            personCode = resolved.personCode.ifBlank { fallback.personCode },
            initials = if (resolved.name.isBlank()) fallback.initials else resolved.initials,
            name = resolved.name.ifBlank { fallback.name },
            role = resolved.role.ifBlank { fallback.role },
            color = if (resolved.personCode.isBlank()) fallback.color else resolved.color,
            orgUnitName = resolved.orgUnitName.ifBlank { fallback.orgUnitName },
            email = resolved.email.ifBlank { fallback.email },
            workPhone = resolved.workPhone.ifBlank { fallback.workPhone },
            positionTitle = resolved.positionTitle.ifBlank { fallback.positionTitle },
            hireDate = resolved.hireDate.ifBlank { fallback.hireDate },
            company = resolved.company.ifBlank { fallback.company },
            managerPersonCode = resolved.managerPersonCode.ifBlank { fallback.managerPersonCode },
            managerName = resolved.managerName.ifBlank { fallback.managerName }
        )
    }
}

data class PersonSummaryResponse(
    @SerializedName("person_code") val personCode: String? = "",
    @SerializedName("full_name") val fullName: String? = "",
    val email: String? = "",
    @SerializedName("job_title") val jobTitle: String? = "",
    @SerializedName("org_unit_name") val orgUnitName: String? = ""
) {
    fun toPeopleRow(isCurrentUser: Boolean = false): PeopleRowData {
        val resolvedPersonCode = personCode.orEmpty()
        val resolvedFullName = fullName.orEmpty()
        val resolvedJobTitle = jobTitle.orEmpty()
        return PeopleRowData(
            id = resolvedPersonCode.ifBlank { resolvedFullName },
            personCode = resolvedPersonCode,
            initials = peopleInitials(resolvedFullName),
            name = resolvedFullName,
            role = resolvedJobTitle.ifBlank { "Sin puesto definido" },
            color = peopleColor(resolvedPersonCode.ifBlank { resolvedFullName }),
            orgUnitName = orgUnitName.orEmpty(),
            email = email.orEmpty(),
            isCurrentUser = isCurrentUser
        )
    }

    fun matches(query: String): Boolean {
        val words = query.normalizeSearch().split(" ").filter { it.length >= 2 }
        val searchable = listOf(fullName, email, jobTitle, orgUnitName)
            .joinToString(" ") { it.orEmpty() }
            .normalizeSearch()
        return words.isNotEmpty() && words.all { searchable.contains(it) }
    }
}

data class TeamResponse(
    @SerializedName("org_unit") val orgUnit: OrgUnitResponse? = null,
    val members: List<PersonSummaryResponse> = emptyList()
)

data class OrgTreeResponse(
    @SerializedName("org_units") val orgUnits: List<OrgUnitResponse> = emptyList()
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

data class ProjectMembersResponse(
    val members: List<PersonSummaryResponse> = emptyList()
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
    @SerializedName("parent_org_unit_code") val parentOrgUnitCode: String = "",
    @SerializedName("parent_org_unit_name") val parentOrgUnitName: String = "",
    @SerializedName("manager_person_code") val managerPersonCode: String = "",
    @SerializedName("manager_name") val managerName: String = ""
)

private fun String.normalizeSearch(): String {
    val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    return normalized
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
        .trim()
}

private fun formatPeopleDate(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return ""

    val inputFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "dd/MM/yyyy"
    )
    val output = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.forLanguageTag("es-ES"))

    inputFormats.forEach { pattern ->
        runCatching {
            val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            formatter.parse(trimmed)
        }.getOrNull()?.let { return output.format(it) }
    }

    return trimmed
}
