package com.lognext.nexterandroid.features.more

import com.lognext.nexterandroid.features.common.EmployeeCategory

enum class SystemsTicketForm(val url: String) {
    StaffIncident("https://forms.office.com/e/k21E14b6PP"),
    StaffRequest("https://forms.office.com/e/RCWpNMDSSS"),
    StaffEquipment("https://forms.office.com/e/hC9g3pN3cK"),
    ConsultantIncident("https://forms.office.com/Pages/ResponsePage.aspx?id=3AbIs9jl0ES54q7dMWko6nfQRcd3sH1EtThXUFtnQORUM0E5QVMwT1NYNURVMTlGUlpTSDY0RENJNiQlQCN0PWcu");

    companion object {
        fun forCategory(category: EmployeeCategory?): List<SystemsTicketForm> = when (category) {
            EmployeeCategory.Staff -> listOf(StaffIncident, StaffRequest, StaffEquipment)
            EmployeeCategory.Consultant -> listOf(ConsultantIncident)
            null -> emptyList()
        }
    }
}
