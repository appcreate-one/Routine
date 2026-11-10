package com.example.data

import java.util.Calendar
import kotlinx.coroutines.flow.Flow

class DashboardRepository(
    private val sharedDataDao: SharedDataDao,
    private val dailyRoutineDao: DailyRoutineDao
) {
    val allSharedData: Flow<List<SharedData>> = sharedDataDao.getAllSharedData()
    val totalSharedCount: Flow<Int> = sharedDataDao.getTotalSharedCount()
    val allRoutines: Flow<List<DailyRoutine>> = dailyRoutineDao.getAllRoutines()

    suspend fun ensureDefaultRoutinesInitialized() {
        val count = dailyRoutineDao.getRoutineCount()
        if (count < RoutineCatalog.EXACT_21_TASKS.size) {
            // Seed the 21 exact branch tasks with realistic initial progress for earlier days of the month
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, 31)
            val initialEntities = RoutineCatalog.EXACT_21_TASKS.map { template ->
                val completedDays = mutableSetOf<Int>()
                // Pre-fill prior days of current month so the 31-day grid immediately shows rich operational context
                for (day in 1 until today) {
                    if ((day + template.id) % 5 != 0) {
                        completedDays.add(day)
                    }
                }
                // Pre-check morning opening tasks for today
                if (template.id <= 5) {
                    completedDays.add(today)
                }
                DailyRoutine(
                    id = template.id,
                    taskName = template.taskName,
                    phaseTag = template.phaseTag,
                    completedDaysCsv = completedDays.sorted().joinToString(",")
                )
            }
            dailyRoutineDao.insertAllRoutines(initialEntities)
        }
    }

    suspend fun insertSharedData(
        contentType: String,
        rawContent: String,
        sourcePackage: String = "com.whatsapp",
        titleOrSubject: String = ""
    ): Long {
        val entry = SharedData(
            timestamp = System.currentTimeMillis(),
            contentType = contentType,
            rawContent = rawContent,
            sourcePackage = sourcePackage,
            titleOrSubject = titleOrSubject
        )
        return sharedDataDao.insertSharedData(entry)
    }

    suspend fun deleteSharedData(item: SharedData) {
        sharedDataDao.deleteSharedData(item)
    }

    suspend fun clearAllSharedData() {
        sharedDataDao.clearAllSharedData()
    }

    suspend fun toggleRoutineDay(taskId: Int, dayOfMonth: Int) {
        val existing = dailyRoutineDao.getRoutineById(taskId) ?: return
        val updated = existing.withDayToggled(dayOfMonth)
        dailyRoutineDao.updateRoutine(updated)
    }

    suspend fun markAllTasksForDay(dayOfMonth: Int, completed: Boolean) {
        for (template in RoutineCatalog.EXACT_21_TASKS) {
            val existing = dailyRoutineDao.getRoutineById(template.id)
                ?: DailyRoutine(
                    id = template.id,
                    taskName = template.taskName,
                    phaseTag = template.phaseTag
                )
            dailyRoutineDao.updateRoutine(existing.withDaySet(dayOfMonth, completed))
        }
    }

    suspend fun resetAllRoutines() {
        val clean = RoutineCatalog.buildInitialEntities()
        dailyRoutineDao.insertAllRoutines(clean)
    }
}
