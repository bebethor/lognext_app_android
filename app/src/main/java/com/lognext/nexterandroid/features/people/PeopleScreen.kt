package com.lognext.nexterandroid.features.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography

@Composable
fun PeopleScreen() {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PeopleViewModel(AppDependencies.peopleService) as T
        }
    }
    val viewModel: PeopleViewModel = viewModel(factory = factory)

    LaunchedEffect(viewModel) {
        viewModel.loadIfNeeded()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexterColors.pageBackground())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SearchCard(
            value = viewModel.searchText,
            onValueChange = viewModel::updateSearchText,
            onClear = viewModel::clearSearch
        )

        if (viewModel.isLoading) {
            LoadingPeopleState()
        } else if (viewModel.hasSearchText) {
            if (viewModel.searchResults.isEmpty()) {
                EmptyPeopleState()
            } else {
                PeopleSectionCard(
                    title = "Resultados",
                    badge = null,
                    highlighted = true,
                    people = viewModel.searchResults,
                    onPersonSelected = { viewModel.selectedPerson = it }
                )
            }
        } else {
            PeopleSectionCard(
                title = viewModel.teamSectionTitle,
                badge = "Mi equipo",
                highlighted = true,
                people = viewModel.teamPeople,
                onPersonSelected = { viewModel.selectedPerson = it }
            )

            PeopleSectionCard(
                title = "COMITÉ DE DIRECCIÓN",
                badge = null,
                highlighted = false,
                people = viewModel.leadershipPeople,
                onPersonSelected = { viewModel.selectedPerson = it }
            )

            ProjectCatalogCard(
                title = "Empresas",
                projects = viewModel.companies,
                emptyMessage = "No hay empresas disponibles."
            )
            ProjectCatalogCard(
                title = "Proyectos",
                projects = viewModel.projects,
                emptyMessage = "No hay proyectos disponibles."
            )
        }
    }

    viewModel.selectedPerson?.let { person ->
        PersonDetailDialog(
            person = person,
            manager = viewModel.managerFor(person),
            reports = viewModel.reportsFor(person),
            projects = viewModel.projectsFor(person),
            onPersonSelected = { viewModel.selectedPerson = it },
            onDismiss = { viewModel.selectedPerson = null }
        )
    }
}

@Composable
private fun LoadingPeopleState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = NexterColors.Red)
        Text("Cargando People…", color = NexterColors.secondaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SearchCard(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit) {
    Card(
        backgroundColor = NexterColors.cardBackground(),
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, NexterColors.border()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Buscar persona…", fontSize = NexterTypography.Body) },
                singleLine = true,
                leadingIcon = {
                    Text("⌕", color = NexterColors.tertiaryText(), fontSize = NexterTypography.Caption, fontWeight = FontWeight.Bold)
                },
                trailingIcon = {
                    if (value.isNotEmpty()) {
                        Text(
                            text = "×",
                            color = NexterColors.tertiaryText(),
                            fontSize = NexterTypography.Body,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onClear)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

@Composable
private fun PeopleSectionCard(
    title: String,
    badge: String?,
    highlighted: Boolean,
    people: List<PeopleRowData>,
    onPersonSelected: (PeopleRowData) -> Unit
) {
    Card(
        backgroundColor = NexterColors.cardBackground(),
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp,
        border = BorderStroke(
            if (highlighted) 1.5.dp else 1.dp,
            if (highlighted) NexterColors.Red.copy(alpha = 0.20f) else NexterColors.border()
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.SectionTitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                badge?.let {
                    Text(
                        text = it,
                        color = NexterColors.Red,
                        fontSize = NexterTypography.Badge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(NexterColors.Red.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, NexterColors.Red.copy(alpha = 0.25f)), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Divider(color = NexterColors.border())
            people.forEachIndexed { index, person ->
                PeopleRow(person, onClick = { onPersonSelected(person) })
                if (index != people.lastIndex) {
                    Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 66.dp))
                }
            }
        }
    }
}

@Composable
private fun PeopleRow(person: PeopleRowData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PeopleAvatar(person = person, size = 55)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = person.name,
                    color = NexterColors.primaryText(),
                    fontSize = NexterTypography.Body,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (person.isCurrentUser) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CurrentUserBadge()
                }
            }
            Text(
                text = person.role,
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Callout,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("›", color = NexterColors.tertiaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProjectCatalogCard(title: String, projects: List<PeopleProject>, emptyMessage: String) {
    Card(
        backgroundColor = NexterColors.cardBackground(),
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, NexterColors.border()),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = title.uppercase(),
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.SectionTitle,
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
            Divider(color = NexterColors.border())
            if (projects.isEmpty()) {
                Text(
                    text = emptyMessage,
                    color = NexterColors.secondaryText(),
                    fontSize = NexterTypography.Callout,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            } else {
                projects.forEachIndexed { index, project ->
                    ProjectRow(project)
                    if (index != projects.lastIndex) Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 14.dp))
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(project: PeopleProject) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = project.name,
                color = NexterColors.primaryText(),
                fontSize = NexterTypography.Body,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = project.code,
                color = NexterColors.Red,
                fontSize = NexterTypography.Badge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (project.description.isNotEmpty()) {
            Text(
                text = project.description,
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Callout
            )
        }
    }
}

@Composable
private fun EmptyPeopleState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("?", color = NexterColors.tertiaryText(), fontSize = NexterTypography.Clock, fontWeight = FontWeight.Bold)
        Text("No se encontraron personas", color = NexterColors.secondaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
        Text("Prueba con otro nombre o departamento.", color = NexterColors.tertiaryText(), fontSize = NexterTypography.Callout)
    }
}

@Composable
private fun PersonDetailDialog(
    person: PeopleRowData,
    manager: PeopleRowData?,
    reports: List<PeopleRowData>,
    projects: List<PeopleProjectDetail>,
    onPersonSelected: (PeopleRowData) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            backgroundColor = NexterColors.cardBackground(),
            shape = RoundedCornerShape(18.dp),
            elevation = 12.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 22.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹",
                        color = NexterColors.Red,
                        fontSize = NexterTypography.CardTitle,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "Perfil",
                        color = NexterColors.primaryText(),
                        fontSize = NexterTypography.SectionTitle,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Divider(color = NexterColors.border())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ProfileHero(person)
                    DetailInformationSection(
                        title = "Contacto",
                        fields = listOf(
                            PeopleDetailField("Email", person.email),
                            PeopleDetailField("Teléfono de trabajo", person.workPhone)
                        ).filter { it.value.isNotBlank() }
                    )
                    DetailInformationSection(
                        title = "Información laboral",
                        fields = listOf(
                            PeopleDetailField("Posición", person.positionTitle.ifBlank { person.role }),
                            PeopleDetailField("Empresa", person.company),
                            PeopleDetailField("Fecha de incorporación", person.hireDate)
                        ).filter { it.value.isNotBlank() }
                    )
                    ReportsToSection(manager, onPersonSelected)
                    DirectReportsSection(reports, onPersonSelected)
                    ProjectsSection(projects)
                }
            }
        }
    }
}

@Composable
private fun ProfileHero(person: PeopleRowData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NexterColors.Navy)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = (-30).dp)
                .rotate(-30f)
                .border(BorderStroke(1.5.dp, NexterColors.Red.copy(alpha = 0.20f)), RoundedCornerShape(4.dp))
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PeopleAvatar(person = person, size = 64)
            Text(person.name, color = Color.White, fontSize = NexterTypography.CardTitle, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(person.role, color = Color.White.copy(alpha = 0.55f), fontSize = NexterTypography.Callout, textAlign = TextAlign.Center)
            if (person.orgUnitName.isNotBlank()) {
                Text(
                    text = person.orgUnitName.uppercase(),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = NexterTypography.Badge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(NexterColors.Red.copy(alpha = 0.25f))
                        .border(BorderStroke(1.dp, NexterColors.Red.copy(alpha = 0.40f)), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailInformationSection(title: String, fields: List<PeopleDetailField>) {
    if (fields.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = NexterColors.tertiaryText(), fontSize = NexterTypography.SectionTitle, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(subtleBackground())
                .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(10.dp))
        ) {
            fields.forEachIndexed { index, field ->
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(field.label, color = NexterColors.tertiaryText(), fontSize = NexterTypography.Footnote, fontWeight = FontWeight.SemiBold)
                    Text(field.value, color = NexterColors.primaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
                }
                if (index != fields.lastIndex) Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
private fun ReportsToSection(manager: PeopleRowData?, onPersonSelected: (PeopleRowData) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📤 Reporta a", color = NexterColors.tertiaryText(), fontSize = NexterTypography.SectionTitle, fontWeight = FontWeight.Bold)
        if (manager == null) {
            Text("Máximo nivel — no reporta a nadie", color = NexterColors.secondaryText(), fontSize = NexterTypography.Body)
        } else {
            ProfilePersonRow(manager, onClick = { onPersonSelected(manager) })
        }
    }
}

@Composable
private fun DirectReportsSection(reports: List<PeopleRowData>, onPersonSelected: (PeopleRowData) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "📥 Le reportan (${if (reports.isEmpty()) "nadie" else reports.size.toString()})",
            color = NexterColors.tertiaryText(),
            fontSize = NexterTypography.SectionTitle,
            fontWeight = FontWeight.Bold
        )
        if (reports.isEmpty()) {
            Text("Nadie reporta directamente", color = NexterColors.secondaryText(), fontSize = NexterTypography.Body)
        } else {
            reports.forEach { report ->
                ProfilePersonRow(report, onClick = { onPersonSelected(report) })
            }
        }
    }
}

@Composable
private fun ProfilePersonRow(person: PeopleRowData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(subtleBackground())
            .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PeopleAvatar(person, size = 46)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(person.name, color = NexterColors.primaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
            Text(person.role, color = NexterColors.secondaryText(), fontSize = NexterTypography.Callout, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (person.isCurrentUser) CurrentUserBadge() else Text("›", color = NexterColors.tertiaryText(), fontSize = NexterTypography.Body)
    }
}

@Composable
private fun ProjectsSection(projects: List<PeopleProjectDetail>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Proyectos", color = NexterColors.tertiaryText(), fontSize = NexterTypography.SectionTitle, fontWeight = FontWeight.Bold)
        if (projects.isEmpty()) {
            Text(
                "No hay proyectos asignados.",
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Callout,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(subtleBackground())
                    .padding(14.dp)
            )
        } else {
            projects.forEach { project ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(subtleBackground())
                        .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(project.project.name, color = NexterColors.primaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold)
                    ProjectDetailValue("Código", project.project.code)
                    if (project.project.description.isNotEmpty()) ProjectDetailValue("Descripción", project.project.description)
                    ProjectDetailValue("Personas asignadas", project.members.joinToString(", ") { it.name })
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = NexterColors.tertiaryText(), fontSize = NexterTypography.Footnote, fontWeight = FontWeight.SemiBold)
        Text(value, color = NexterColors.primaryText(), fontSize = NexterTypography.Callout)
    }
}

@Composable
private fun PeopleAvatar(person: PeopleRowData, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(person.color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = person.initials,
            color = if (person.darkText) Color(0xFF1A3A1A) else Color.White,
            fontSize = if (size >= 55) NexterTypography.Body else NexterTypography.Callout,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CurrentUserBadge() {
    Text(
        text = "Tú",
        color = NexterColors.Red,
        fontSize = NexterTypography.Badge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(NexterColors.Red.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
private fun subtleBackground(): Color {
    return if (com.lognext.nexterandroid.ui.theme.isNexterDarkTheme()) {
        Color.White.copy(alpha = 0.06f)
    } else {
        NexterColors.Navy.copy(alpha = 0.05f)
    }
}
