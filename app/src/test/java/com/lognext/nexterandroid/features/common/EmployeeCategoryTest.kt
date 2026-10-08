package com.lognext.nexterandroid.features.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmployeeCategoryTest {
    @Test fun recognizesApiJobCategoriesRegardlessOfCaseAndSpaces() {
        assertEquals(EmployeeCategory.Staff, EmployeeCategory.fromJobTitle(" staff "))
        assertEquals(EmployeeCategory.Consultant, EmployeeCategory.fromJobTitle("CONSULTOR"))
        assertEquals("STF", EmployeeCategory.Staff.badge)
        assertEquals("CON", EmployeeCategory.Consultant.badge)
    }

    @Test fun doesNotGuessCategoriesForMissingValuesOrOrganizationalUnits() {
        listOf(null, "", "BECARIOS", "Consultoría", "SECURITAS DIRECT").forEach {
            assertNull(EmployeeCategory.fromJobTitle(it))
        }
    }
}
