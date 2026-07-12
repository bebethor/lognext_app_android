package com.lognext.nexterandroid.features.people

import androidx.compose.ui.graphics.Color
import com.lognext.nexterandroid.ui.theme.NexterColors

data class PeopleRowData(
    val id: String,
    val personCode: String,
    val initials: String,
    val name: String,
    val role: String,
    val color: Color,
    val orgUnitName: String = "",
    val email: String = "",
    val workPhone: String = "",
    val positionTitle: String = "",
    val hireDate: String = "",
    val company: String = "",
    val managerPersonCode: String = "",
    val managerName: String = "",
    val darkText: Boolean = false,
    val isCurrentUser: Boolean = false
)

data class PeopleProject(
    val code: String,
    val name: String,
    val description: String
) {
    val id: String = code
    val isCompany: Boolean
        get() = code.isNotBlank() && code == code.uppercase() && !name.contains("proyecto", ignoreCase = true)
}

data class PeopleProjectDetail(
    val project: PeopleProject,
    val members: List<PeopleRowData>
)

data class PeopleDetailField(
    val icon: String,
    val label: String,
    val value: String
)

fun peopleInitials(name: String): String {
    return name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }
}

fun peopleColor(value: String): Color {
    val colors = listOf(
        NexterColors.Red,
        NexterColors.Blue,
        NexterColors.Violet,
        Color(0xFF3CBEA0),
        NexterColors.Aqua
    )
    val index = kotlin.math.abs(value.hashCode()) % colors.size
    return colors[index]
}
