package com.lognext.nexterandroid.features.more

import com.lognext.nexterandroid.features.common.EmployeeCategory
import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class SystemsTicketFormTest {
    @Test fun consultantsOnlyHaveTheirOwnIncidentForm() {
        assertEquals(listOf(SystemsTicketForm.ConsultantIncident), SystemsTicketForm.forCategory(EmployeeCategory.Consultant))
    }
    @Test fun staffHaveIncidentsRequestsAndEquipmentWithSeparateForms() {
        assertEquals(listOf(SystemsTicketForm.StaffIncident, SystemsTicketForm.StaffRequest, SystemsTicketForm.StaffEquipment), SystemsTicketForm.forCategory(EmployeeCategory.Staff))
        assertEquals("https://forms.office.com/e/k21E14b6PP", SystemsTicketForm.StaffIncident.url)
        assertEquals("https://forms.office.com/e/RCWpNMDSSS", SystemsTicketForm.StaffRequest.url)
        assertEquals("https://forms.office.com/e/hC9g3pN3cK", SystemsTicketForm.StaffEquipment.url)
    }
    @Test fun unknownProfilesHaveNoForms() {
        assertTrue(SystemsTicketForm.forCategory(null).isEmpty())
    }
    @Test fun consultantFormKeepsItsFullIdentifierAndUsesHttps() {
        val url = URI(SystemsTicketForm.ConsultantIncident.url)
        assertEquals("https", url.scheme)
        assertEquals("forms.office.com", url.host)
        assertEquals("/Pages/ResponsePage.aspx", url.path)
        assertEquals("id=3AbIs9jl0ES54q7dMWko6nfQRcd3sH1EtThXUFtnQORUM0E5QVMwT1NYNURVMTlGUlpTSDY0RENJNiQlQCN0PWcu", url.rawQuery)
        assertEquals(4, SystemsTicketForm.entries.map { it.url }.distinct().size)
    }
}
