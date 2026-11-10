package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailyRoutine
import com.example.data.DashboardRepository
import com.example.data.RoutineCatalog
import com.example.data.SharedData
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WhatsAppStats(
    val totalShares: Int = 0,
    val textShares: Int = 0,
    val linkShares: Int = 0,
    val mediaShares: Int = 0
)

data class DashboardUiState(
    val sharedItems: List<SharedData> = emptyList(),
    val filteredSharedItems: List<SharedData> = emptyList(),
    val stats: WhatsAppStats = WhatsAppStats(),
    val routines: List<DailyRoutine> = emptyList(),
    val filteredRoutines: List<DailyRoutine> = emptyList(),
    val selectedShareFilter: String = "ALL", // "ALL", "TEXT", "LINK", "MEDIA"
    val selectedPhaseFilter: String = "All Phases",
    val routineSearchQuery: String = "",
    val selectedDayOfMonth: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, 31),
    val todayDayOfMonth: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, 31),
    val lastCapturedShareBanner: String? = null
)

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _selectedShareFilter = MutableStateFlow("ALL")
    private val _selectedPhaseFilter = MutableStateFlow("All Phases")
    private val _routineSearchQuery = MutableStateFlow("")
    private val _selectedDayOfMonth = MutableStateFlow(
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, 31)
    )
    private val _lastCapturedShareBanner = MutableStateFlow<String?>(null)

    private var sampleIndex = 0

    init {
        viewModelScope.launch {
            repository.ensureDefaultRoutinesInitialized()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allSharedData,
        repository.allRoutines,
        _selectedShareFilter,
        _selectedPhaseFilter,
        combine(_routineSearchQuery, _selectedDayOfMonth, _lastCapturedShareBanner) { q, day, banner ->
            Triple(q, day, banner)
        }
    ) { sharedList, routineList, shareFilter, phaseFilter, (searchQuery, selectedDay, banner) ->
        val stats = WhatsAppStats(
            totalShares = sharedList.size,
            textShares = sharedList.count { it.contentType == "TEXT" },
            linkShares = sharedList.count { it.contentType == "LINK" },
            mediaShares = sharedList.count {
                it.contentType in setOf("IMAGE", "VIDEO", "AUDIO", "DOCUMENT")
            }
        )

        val filteredShares = when (shareFilter) {
            "TEXT" -> sharedList.filter { it.contentType == "TEXT" }
            "LINK" -> sharedList.filter { it.contentType == "LINK" }
            "MEDIA" -> sharedList.filter {
                it.contentType in setOf("IMAGE", "VIDEO", "AUDIO", "DOCUMENT")
            }
            else -> sharedList
        }

        val effectiveRoutines = if (routineList.isEmpty()) {
            RoutineCatalog.buildInitialEntities()
        } else {
            routineList
        }

        val filteredRoutines = effectiveRoutines.filter { item ->
            val matchesPhase = phaseFilter == "All Phases" || item.phaseTag == phaseFilter
            val matchesQuery = searchQuery.isBlank() ||
                item.taskName.contains(searchQuery, ignoreCase = true) ||
                item.id.toString() == searchQuery.trim()
            matchesPhase && matchesQuery
        }

        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, 31)

        DashboardUiState(
            sharedItems = sharedList,
            filteredSharedItems = filteredShares,
            stats = stats,
            routines = effectiveRoutines,
            filteredRoutines = filteredRoutines,
            selectedShareFilter = shareFilter,
            selectedPhaseFilter = phaseFilter,
            routineSearchQuery = searchQuery,
            selectedDayOfMonth = selectedDay,
            todayDayOfMonth = today,
            lastCapturedShareBanner = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            routines = RoutineCatalog.buildInitialEntities(),
            filteredRoutines = RoutineCatalog.buildInitialEntities()
        )
    )

    /**
     * Extracts shared text, URLs, or media streams from an incoming Android Share Sheet / WhatsApp Intent.
     */
    fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val mimeType = intent.type ?: ""

        if (action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
            val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            viewModelScope.launch {
                when {
                    !sharedText.isNullOrBlank() -> {
                        val detectedType = if (containsUrl(sharedText)) "LINK" else "TEXT"
                        val contentWithAttachment = if (streamUri != null) {
                            "$sharedText\n[Media URI: $streamUri]"
                        } else {
                            sharedText
                        }
                        repository.insertSharedData(
                            contentType = detectedType,
                            rawContent = contentWithAttachment,
                            sourcePackage = "WhatsApp Share",
                            titleOrSubject = sharedSubject.ifBlank {
                                if (detectedType == "LINK") "Shared Link from WhatsApp" else "WhatsApp Text Note"
                            }
                        )
                        _lastCapturedShareBanner.value =
                            "Captured $detectedType from WhatsApp: ${sharedText.take(48)}"
                    }
                    streamUri != null -> {
                        val mediaCategory = resolveMediaCategory(mimeType)
                        repository.insertSharedData(
                            contentType = mediaCategory,
                            rawContent = streamUri.toString(),
                            sourcePackage = "WhatsApp Media",
                            titleOrSubject = sharedSubject.ifBlank { "Shared $mediaCategory ($mimeType)" }
                        )
                        _lastCapturedShareBanner.value =
                            "Captured $mediaCategory share ($mimeType)"
                    }
                }
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            val uris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (!uris.isNullOrEmpty()) {
                val mediaCategory = resolveMediaCategory(mimeType)
                viewModelScope.launch {
                    uris.forEachIndexed { idx, uri ->
                        repository.insertSharedData(
                            contentType = mediaCategory,
                            rawContent = uri.toString(),
                            sourcePackage = "WhatsApp Batch",
                            titleOrSubject = "Batch $mediaCategory #${idx + 1} ($mimeType)"
                        )
                    }
                    _lastCapturedShareBanner.value =
                        "Captured ${uris.size} shared media items from WhatsApp"
                }
            }
        }
    }

    fun setShareFilter(filter: String) {
        _selectedShareFilter.value = filter
    }

    fun setPhaseFilter(phase: String) {
        _selectedPhaseFilter.value = phase
    }

    fun setRoutineSearchQuery(query: String) {
        _routineSearchQuery.value = query
    }

    fun selectDayOfMonth(day: Int) {
        _selectedDayOfMonth.value = day.coerceIn(1, 31)
    }

    fun dismissBanner() {
        _lastCapturedShareBanner.value = null
    }

    fun toggleRoutineTaskDay(taskId: Int, dayOfMonth: Int) {
        viewModelScope.launch {
            repository.toggleRoutineDay(taskId, dayOfMonth)
        }
    }

    fun markAllTasksForDay(dayOfMonth: Int, completed: Boolean) {
        viewModelScope.launch {
            repository.markAllTasksForDay(dayOfMonth, completed)
        }
    }

    fun resetAllRoutines() {
        viewModelScope.launch {
            repository.resetAllRoutines()
        }
    }

    fun addManualOrSimulatedShare(
        contentType: String,
        rawContent: String,
        titleOrSubject: String
    ) {
        if (rawContent.isBlank()) return
        viewModelScope.launch {
            repository.insertSharedData(
                contentType = contentType,
                rawContent = rawContent.trim(),
                sourcePackage = "WhatsApp",
                titleOrSubject = titleOrSubject.ifBlank { "WhatsApp $contentType Share" }
            )
            _lastCapturedShareBanner.value = "Saved WhatsApp $contentType to Room DB"
        }
    }

    /**
     * Simulates an realistic incoming WhatsApp branch operation share for instant testing in the emulator.
     */
    fun simulateIncomingWhatsAppShare() {
        val samples = listOf(
            Triple(
                "TEXT",
                "Branch Reporting Update: 14 RO staff reported on time at 08:45 AM. Attendance marked & system opened.",
                "Morning Staff & Attendance Note"
            ),
            Triple(
                "LINK",
                "https://docs.google.com/spreadsheets/d/branch-igl-il-msme-hm-crosscheck-2hr-tracker",
                "HM Cross Check Sheet (IGL, IL & MSME)"
            ),
            Triple(
                "IMAGE",
                "content://com.whatsapp.provider.media/item/field_visit_ptp_verification_photo_042.jpg",
                "PTP Field Visit Geo-Verified Photo"
            ),
            Triple(
                "DOCUMENT",
                "content://com.whatsapp.provider.media/item/collection_deposit_slip_before_4pm.pdf",
                "Collection Deposit Bank Slip (3:40 PM)"
            ),
            Triple(
                "TEXT",
                "Disbursment Closing Alert: 9/9 scheduled disbursements completed at 2:45 PM before 3:00 PM cutoff. GRT & PD verified.",
                "Disb Closing Confirmation"
            ),
            Triple(
                "LINK",
                "https://branchops.internal/reports/ro-wise-survey-and-fresh-arrear-crosscheck",
                "RO Wise Survey & Arrear Portal Link"
            )
        )
        val (type, raw, title) = samples[sampleIndex % samples.size]
        sampleIndex++
        addManualOrSimulatedShare(type, raw, title)
    }

    fun deleteShare(item: SharedData) {
        viewModelScope.launch {
            repository.deleteSharedData(item)
        }
    }

    fun clearAllShares() {
        viewModelScope.launch {
            repository.clearAllSharedData()
        }
    }

    private fun containsUrl(text: String): Boolean {
        return Patterns.WEB_URL.matcher(text).find() ||
            text.contains("http://", ignoreCase = true) ||
            text.contains("https://", ignoreCase = true)
    }

    private fun resolveMediaCategory(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "IMAGE"
            mimeType.startsWith("video/") -> "VIDEO"
            mimeType.startsWith("audio/") -> "AUDIO"
            else -> "DOCUMENT"
        }
    }

    class Factory(private val repository: DashboardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
