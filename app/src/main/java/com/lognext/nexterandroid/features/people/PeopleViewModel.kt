package com.lognext.nexterandroid.features.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lognext.nexterandroid.core.AppConfig
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.Job
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

    var teamPeople by mutableStateOf(sampleTeam())
        private set
    var leadershipPeople by mutableStateOf(
        listOf(
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
    )
        private set

    var projectCatalog by mutableStateOf(
        listOf(
        PeopleProject("LOGNEXT", "Lognext", "Consultoría tecnológica y servicios de transformación digital."),
        PeopleProject("SACYR", "Sacyr", "Cliente estratégico con servicios gestionados."),
        PeopleProject("PROY-APP", "Proyecto Nexter", "Producto interno para operaciones y personas."),
        PeopleProject("PROY-DATA", "Proyecto Data Platform", "Evolución de arquitectura de datos corporativa.")
        )
    )
        private set

    private var apiSearchResults by mutableStateOf<List<PeopleRowData>>(emptyList())
    private var searchJob: Job? = null
    private var hasLoaded = false

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
            runCatching {
                val me = service.me()
                val meRow = me.toPeopleRow(isCurrentUser = true)
                val team = service.team(me.personCode).members.map { member ->
                    member.toPeopleRow(isCurrentUser = member.personCode == me.personCode)
                }
                val projects = service.projects().projects.map { it.toPeopleProject() }
                Triple(meRow, team, projects)
            }.onSuccess { (me, team, projects) ->
                teamPeople = (listOf(me) + team.filter { it.personCode != me.personCode }).distinctBy { it.personCode.ifBlank { it.id } }
                leadershipPeople = emptyList()
                projectCatalog = projects
            }.onFailure {
                errorMessage = "No se pudo cargar People."
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
                service.searchStaff(query).people.map { it.toPeopleRow() }
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

    fun managerFor(person: PeopleRowData): PeopleRowData? {
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

    fun reportsFor(person: PeopleRowData): List<PeopleRowData> {
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

    fun projectsFor(person: PeopleRowData): List<PeopleProjectDetail> {
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
