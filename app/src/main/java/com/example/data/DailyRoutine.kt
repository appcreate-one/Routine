package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity to track completion status of each daily routine task across the 31 days of the month.
 *
 * [completedDaysCsv] stores a comma-separated list of completed day numbers (1..31), e.g. "1,2,5,10".
 */
@Entity(tableName = "daily_routine")
data class DailyRoutine(
    @PrimaryKey
    val id: Int, // 1 to 21 corresponding to the exact 21 routine categories
    val taskName: String,
    val phaseTag: String,
    val completedDaysCsv: String = ""
) {
    fun completedDaysSet(): Set<Int> {
        if (completedDaysCsv.isBlank()) return emptySet()
        return completedDaysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..31 }
            .toSet()
    }

    fun isDayCompleted(day: Int): Boolean {
        return day in completedDaysSet()
    }

    fun withDayToggled(day: Int): DailyRoutine {
        val current = completedDaysSet().toMutableSet()
        if (day in current) {
            current.remove(day)
        } else if (day in 1..31) {
            current.add(day)
        }
        val updatedCsv = current.sorted().joinToString(",")
        return copy(completedDaysCsv = updatedCsv)
    }

    fun withDaySet(day: Int, completed: Boolean): DailyRoutine {
        val current = completedDaysSet().toMutableSet()
        if (completed && day in 1..31) {
            current.add(day)
        } else {
            current.remove(day)
        }
        val updatedCsv = current.sorted().joinToString(",")
        return copy(completedDaysCsv = updatedCsv)
    }

    fun completedCount(): Int = completedDaysSet().size
}

object RoutineCatalog {
    data class RoutineTemplate(
        val id: Int,
        val taskName: String,
        val phaseTag: String
    )

    /**
     * Exact 21 daily routine categories specified for Branch Operations tracking.
     */
    val EXACT_21_TASKS: List<RoutineTemplate> = listOf(
        RoutineTemplate(1, "Staff Reporting cross check", "Morning Opening"),
        RoutineTemplate(2, "Field Visits (If Need)", "Morning Opening"),
        RoutineTemplate(3, "Branch Reporting", "Morning Opening"),
        RoutineTemplate(4, "Attendance Marking", "Morning Opening"),
        RoutineTemplate(5, "System Opening", "Morning Opening"),
        RoutineTemplate(6, "Today Business Planing", "Morning Opening"),
        RoutineTemplate(7, "Disbursment", "Core Ops & HM"),
        RoutineTemplate(8, "GRT & PD", "Core Ops & HM"),
        RoutineTemplate(9, "Fresh Arrear Cross check", "Core Ops & HM"),
        RoutineTemplate(10, "All Pending mails clearnes", "Core Ops & HM"),
        RoutineTemplate(11, "All Other registers and files cross check", "Core Ops & HM"),
        RoutineTemplate(12, "HM Cross Check - IGL, IL & MSME (Every 2 hr. gap)", "Core Ops & HM"),
        RoutineTemplate(13, "Disb Closing (Try to complete the Disb before 3.00PM)", "Afternoon Closing"),
        RoutineTemplate(14, "Collection Deposit (Before 4.00PM)", "Afternoon Closing"),
        RoutineTemplate(15, "Demand pending Follow up 3.00PM Onwards", "Afternoon Closing"),
        RoutineTemplate(16, "PTP Visit Cross Check", "Afternoon Closing"),
        RoutineTemplate(17, "RO wise Survey Cross check", "Afternoon Closing"),
        RoutineTemplate(18, "Cash Book Updation", "EOD Verification"),
        RoutineTemplate(19, "EOD", "EOD Verification"),
        RoutineTemplate(20, "System Closing before 6.00PM", "EOD Verification"),
        RoutineTemplate(21, "Cross check all above mentioned points once again", "EOD Verification")
    )

    fun buildInitialEntities(prepopulateSampleDays: Set<Int> = emptySet()): List<DailyRoutine> {
        val csv = prepopulateSampleDays.sorted().joinToString(",")
        return EXACT_21_TASKS.map { template ->
            DailyRoutine(
                id = template.id,
                taskName = template.taskName,
                phaseTag = template.phaseTag,
                completedDaysCsv = csv
            )
        }
    }
}
