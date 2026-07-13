package com.lognext.nexterandroid.features.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lognext.nexterandroid.core.AppConfig
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PeopleViewModel(
    private val service: PeopleService
) : ViewModel() {
    var searchText by mutableStateOf("")
        private set
    var selectedPerson by mutableStateOf<PeopleRowData?>(null)
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isLoadingDetail by mutableStateOf(false)
        private set
    var detailErrorMessage by mutableStateOf<String?>(null)
        private set
    var detailManager by mutableStateOf<PeopleRowData?>(null)
        private set
    var detailReports by mutableStateOf<List<PeopleRowData>>(emptyList())
        private set
    var detailProjects by mutableStateOf<List<PeopleProjectDetail>>(emptyList())
        private set

    var teamPeople by mutableStateOf(if (AppConfig.UseFakeLogin) sampleTeam() else emptyList())
        private set
    var leadershipPeople by mutableStateOf(leadershipTeam())
        private set

    var projectCatalog by mutableStateOf(if (AppConfig.UseFakeLogin) sampleProjectCatalog() else emptyList())
        private set

    private var apiSearchResults by mutableStateOf<List<PeopleRowData>>(emptyList())
    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private var hasLoaded = false

    private fun leadershipTeam(): List<PeopleRowData> = listOf(
        PeopleRowData(
            id = "leadership-pp",
            personCode = "PP",
            initials = "PP",
            name = "Patrick Pariente",
            role = "Director Ejecutivo (CEO)",
            color = NexterColors.Red,
            orgUnitName = "Comité de dirección",
            email = "patrick.pariente@lognext.com",
            company = "Lognext"
        ),
        PeopleRowData(
            id = "leadership-jb",
            personCode = "JB",
            initials = "JB",
            name = "Jean-Baptiste Bogliolo",
            role = "Director de Operaciones (COO)",
            color = NexterColors.Aqua,
            orgUnitName = "Comité de dirección",
            email = "jean.baptiste@lognext.com",
            company = "Lognext",
            darkText = true
        ),
        PeopleRowData(
            id = "leadership-fp",
            personCode = "FP",
            initials = "FP",
            name = "Francisco J. Pedraza",
            role = "Director Financiero (CFO)",
            color = NexterColors.Violet,
            orgUnitName = "Comité de dirección",
            email = "francisco.pedraza@lognext.com",
            company = "Lognext"
        ),
        PeopleRowData(
            id = "leadership-ms",
            personCode = "MS",
            initials = "MS",
            name = "Miguel Ángel Saiz",
            role = "Resp. Unidad de Negocio",
            color = NexterColors.Yellow,
            orgUnitName = "Comité de dirección",
            email = "miguel.saiz@lognext.com",
            company = "Lognext",
            darkText = true
        )
    )
    
    private fun sampleProjectCatalog(): List<PeopleProject> = listOf(
        PeopleProject("LOGNEXT", "Lognext", "Consultoría tecnológica y servicios de transformación digital."),
        PeopleProject("SACYR", "Sacyr", "Cliente estratégico con servicios gestionados."),
        PeopleProject("PROY-APP", "Proyecto Nexter", "Producto interno para operaciones y personas."),
        PeopleProject("PROY-DATA", "Proyecto Data Platform", "Evolución de arquitectura de datos corporativa.")
    )

    val companies: List<PeopleProject>
        get() = projectCatalog.filter { it.isCompany }

    val projects: List<PeopleProject>
        get() = projectCatalog.filter { !it.isCompany }

    val hasSearchText: Boolean
        get() = searchText.trim().isNotEmpty()

    val searchResults: List<PeopleRowData>
        get() {
            val query = searchText.trim().lowercase()
            if (query.length < 2) return emptyList()
            if (!AppConfig.UseFakeLogin) return apiSearchResults
            return allPeople().filter {
                it.name.lowercase().contains(query) ||
                    it.role.lowercase().contains(query) ||
                    it.orgUnitName.lowercase().contains(query)
            }.distinctBy { it.id }
        }

    val teamSectionTitle: String
        get() = teamPeople.firstOrNull()?.orgUnitName?.takeIf { it.isNotBlank() } ?: "Mi equipo"

    fun loadIfNeeded() {
        if (hasLoaded || AppConfig.UseFakeLogin) return
        hasLoaded = true
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            teamPeople = emptyList()
            projectCatalog = emptyList()

            val teamResult = runCatching {
                val me = service.me()
                val personCode = me.personCode.orEmpty()
                val team = service.team(personCode).members.map { member ->
                    member.toPeopleRow(isCurrentUser = member.personCode.orEmpty() == personCode)
                }
                if (team.isEmpty()) {
                    listOf(me.toPeopleRow(isCurrentUser = true))
                } else {
                    team
                }
            }

            val projectsResult = runCatching {
                service.projects().projects.map { it.toPeopleProject() }
                    .sortedBy { it.name.lowercase() }
            }

            teamResult.onSuccess { team ->
                teamPeople = team.distinctBy { it.personCode.ifBlank { it.id } }
            }.onFailure {
                errorMessage = "Inténtalo de nuevo más tarde."
            }

            projectsResult.onSuccess { projects ->
                projectCatalog = projects
            }.onFailure {
                if (errorMessage == null) {
                    errorMessage = "No se pudieron cargar los proyectos y empresas."
                }
            }

            isLoading = false
        }
    }

    fun updateSearchText(value: String) {
        searchText = value
        if (AppConfig.UseFakeLogin) return

        searchJob?.cancel()
        val query = value.trim()
        if (query.length < 2) {
            apiSearchResults = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            runCatching {
                service.searchStaffSmart(query).people.map { it.toPeopleRow() }
            }.onSuccess { people ->
                apiSearchResults = people
            }.onFailure {
                apiSearchResults = emptyList()
            }
        }
    }

    fun clearSearch() {
        updateSearchText("")
    }

    fun selectPerson(person: PeopleRowData) {
        selectedPerson = person
        detailJob?.cancel()
        resetDetail()

        if (AppConfig.UseFakeLogin) {
            detailManager = sampleManagerFor(person)
            detailReports = sampleReportsFor(person)
            detailProjects = sampleProjectsFor(person)
            return
        }

        detailJob = viewModelScope.launch {
            isLoadingDetail = true
            detailErrorMessage = null
            runCatching {
                val personCode = resolvePersonCode(person)
                val profileRequest = async { runCatching { service.profile(personCode) }.getOrNull() }
                val managerRequest = async { runCatching { service.manager(personCode) }.getOrNull() }
                val teamRequest = async { runCatching { service.team(personCode) }.getOrNull() }

                val profile = profileRequest.await()
                val manager = managerRequest.await()
                val team = teamRequest.await()

                val resolvedPerson = profile?.toPeopleRow(fallback = person) ?: person.copy(personCode = personCode)
                selectedPerson = resolvedPerson
                detailManager = manager?.manager?.toPeopleRow()
                    ?: profile?.toManagerRow(currentPersonCode = personCode)
                detailReports = team?.members
                    ?.filter { it.personCode.orEmpty() != personCode }
                    ?.map { it.toPeopleRow() }
                    .orEmpty()

                if (profile == null || manager == null || team == null) {
                    detailErrorMessage = "No se pudo cargar toda la información del perfil."
                }

                loadProjectsForPerson(personCode)
            }.onFailure {
                detailErrorMessage = "No se pudo cargar el perfil. Inténtalo de nuevo."
            }
            isLoadingDetail = false
        }
    }

    fun dismissSelectedPerson() {
        detailJob?.cancel()
        selectedPerson = null
        resetDetail()
    }

    fun managerFor(person: PeopleRowData): PeopleRowData? {
        if (!AppConfig.UseFakeLogin) return detailManager
        return sampleManagerFor(person)
    }

    fun reportsFor(person: PeopleRowData): List<PeopleRowData> {
        if (!AppConfig.UseFakeLogin) return detailReports
        return sampleReportsFor(person)
    }

    fun projectsFor(person: PeopleRowData): List<PeopleProjectDetail> {
        if (!AppConfig.UseFakeLogin) return detailProjects
        return sampleProjectsFor(person)
    }

    private suspend fun resolvePersonCode(person: PeopleRowData): String {
        person.personCode.takeIf { it.isNotBlank() }?.let { return it }
        return service.searchStaffSmart(person.name).people.firstOrNull()?.personCode.orEmpty()
            .ifBlank { throw IllegalStateException("Missing person_code") }
    }

    private fun PersonProfileResponse.toManagerRow(currentPersonCode: String): PeopleRowData? {
        val managerName = managerName.orEmpty().trim()
        val managerPersonCode = managerPersonCode.orEmpty().trim()
        if (managerName.isBlank() || managerPersonCode == currentPersonCode) return null

        return PeopleRowData(
            id = managerPersonCode.ifBlank { "manager-$currentPersonCode" },
            personCode = managerPersonCode,
            initials = peopleInitials(managerName),
            name = managerName,
            role = "Manager",
            color = peopleColor(managerPersonCode.ifBlank { managerName })
        )
    }

    private suspend fun loadProjectsForPerson(personCode: String) {
        val catalog = if (projectCatalog.isNotEmpty()) {
            projectCatalog
        } else {
            service.projects().projects.map { it.toPeopleProject() }
                .sortedBy { it.name.lowercase() }
                .also { projectCatalog = it }
        }

        detailProjects = coroutineScope {
            catalog.map { project ->
                async {
                    val members = runCatching { service.projectMembers(project.code).members }.getOrDefault(emptyList())
                    if (members.any { it.personCode.orEmpty() == personCode }) {
                        PeopleProjectDetail(project, members.map { it.toPeopleRow() })
                    } else {
                        null
                    }
                }
            }.awaitAll()
        }
            .filterNotNull()
            .sortedBy { it.project.name.lowercase() }
    }

    private fun resetDetail() {
        isLoadingDetail = false
        detailErrorMessage = null
        detailManager = null
        detailReports = emptyList()
        detailProjects = emptyList()
    }

    private fun sampleManagerFor(person: PeopleRowData): PeopleRowData? {
        if (!AppConfig.UseFakeLogin) {
            return allPeople().firstOrNull { it.personCode.isNotBlank() && it.personCode == person.managerPersonCode }
                ?: person.managerName.takeIf { it.isNotBlank() }?.let { managerName ->
                    PeopleRowData(
                        id = "manager-${person.personCode}",
                        personCode = person.managerPersonCode,
                        initials = peopleInitials(managerName),
                        name = managerName,
                        role = "Manager",
                        color = peopleColor(managerName)
                    )
                }
        }
        return when {
            person.isCurrentUser -> teamPeople.firstOrNull { it.personCode == "MS" } ?: leadershipPeople.lastOrNull()
            person.personCode == "MS" -> leadershipPeople.firstOrNull()
            person.id.startsWith("leadership-pp") -> null
            else -> teamPeople.firstOrNull { it.isCurrentUser }
        }
    }

    private fun sampleReportsFor(person: PeopleRowData): List<PeopleRowData> {
        if (!AppConfig.UseFakeLogin) {
            return allPeople().filter { it.managerPersonCode.isNotBlank() && it.managerPersonCode == person.personCode }
        }
        return when {
            person.personCode == "MS" -> teamPeople.filter { !it.isCurrentUser }
            person.isCurrentUser -> emptyList()
            person.id.startsWith("leadership-pp") -> leadershipPeople.drop(1)
            else -> emptyList()
        }
    }

    private fun sampleProjectsFor(person: PeopleRowData): List<PeopleProjectDetail> {
        val members = teamPeople + leadershipPeople.take(1)
        val assignedProjects = if (person.isCurrentUser) projects.take(2) else projects.takeLast(2)
        return assignedProjects.map { PeopleProjectDetail(it, members.take(3)) }
    }

    private fun allPeople(): List<PeopleRowData> = teamPeople + leadershipPeople

    private fun sampleTeam(): List<PeopleRowData> {
        return listOf(
            person(
                code = "JA",
                name = "Jose Alberto",
                role = "Android Developer",
                org = "Mobile & Products",
                email = "jose.alberto@lognext.com",
                phone = "+34 600 000 001",
                position = "Senior Mobile Developer",
                hireDate = "16 jun 2026",
                current = true
            ),
            person(
                code = "LC",
                name = "Laura Campos",
                role = "Product Manager",
                org = "Mobile & Products",
                email = "laura.campos@lognext.com",
                phone = "+34 600 000 002",
                position = "Product Manager",
                hireDate = "4 mar 2024"
            ),
            person(
                code = "DG",
                name = "Daniel García",
                role = "Backend Engineer",
                org = "Mobile & Products",
                email = "daniel.garcia@lognext.com",
                phone = "+34 600 000 003",
                position = "Backend Engineer",
                hireDate = "12 sep 2023"
            ),
            person(
                code = "MR",
                name = "Marta Ruiz",
                role = "UX/UI Designer",
                org = "Mobile & Products",
                email = "marta.ruiz@lognext.com",
                phone = "+34 600 000 004",
                position = "Product Designer",
                hireDate = "21 ene 2025"
            )
        )
    }

    private fun person(
        code: String,
        name: String,
        role: String,
        org: String,
        email: String,
        phone: String,
        position: String,
        hireDate: String,
        current: Boolean = false
    ): PeopleRowData {
        return PeopleRowData(
            id = code,
            personCode = code,
            initials = peopleInitials(name),
            name = name,
            role = role,
            color = peopleColor(code),
            orgUnitName = org,
            email = email,
            workPhone = phone,
            positionTitle = position,
            hireDate = hireDate,
            company = "Lognext",
            isCurrentUser = current
        )
    }
}
