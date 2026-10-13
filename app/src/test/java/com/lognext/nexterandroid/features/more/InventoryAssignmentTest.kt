package com.lognext.nexterandroid.features.more

import org.junit.Assert.*
import org.junit.Test

class InventoryAssignmentTest {
    private val today = inventoryDate("2026-10-13")!!
    private fun item(from: String? = null, to: String? = null) = InventoryAssignment("test", "Laptop", assignedFrom = from, assignedTo = to)

    @Test fun boundaryDaysAreInclusive() {
        assertEquals(InventoryPeriod.CURRENT, item("2026-10-13", "2026-10-13").period(today))
    }
    @Test fun separatesUpcomingAndHistoricalAssignments() {
        assertEquals(InventoryPeriod.UPCOMING, item("2026-10-14").period(today))
        assertEquals(InventoryPeriod.HISTORY, item("2024-01-01", "2026-10-12").period(today))
    }
    @Test fun missingBoundsAndFutureSentinelRemainCurrent() {
        assertEquals(InventoryPeriod.CURRENT, item().period(today))
        assertEquals(InventoryPeriod.CURRENT, item("2024-01-01", "2999-12-31").period(today))
    }
    @Test fun parsesIsoAndOdataAndRejectsInvalidDates() {
        assertEquals(today, inventoryDate("2026-10-13T00:00:00"))
        assertEquals(0L, inventoryDate("/Date(0+0000)/")!!.time)
        assertNull(inventoryDate("2026-02-30"))
        assertNull(inventoryDate("invalid"))
        assertNull(inventoryDate(null))
    }
}
