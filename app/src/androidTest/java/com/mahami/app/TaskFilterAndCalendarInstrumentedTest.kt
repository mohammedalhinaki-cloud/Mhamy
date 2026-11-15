package com.mahami.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskFilterAndCalendarInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedData() {
        composeRule.activityRule.scenario.onActivity { activity ->
            TaskStorage(activity).write(
                MahamiData(
                    tasks = listOf(
                        Task(id = selectedTaskId, title = "مهمة التصنيف المختار", categoryId = selectedCategoryId),
                        Task(id = otherTaskId, title = "مهمة تصنيف آخر", categoryId = otherCategoryId),
                    ),
                    categories = listOf(
                        Category(selectedCategoryId, "الأولوية", 0xFF7C8CFFL, TaskSection.TASKS),
                        Category(otherCategoryId, "لاحقاً", 0xFF48C7B0L, TaskSection.TASKS),
                        Category(scheduledCategoryId, "مواعيد", 0xFFF0B45CL, TaskSection.SCHEDULED),
                    ),
                ),
            )
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    fun selectedTaskFilter_isStoredAndRestoredAfterReopen() {
        composeRule.onNodeWithTag("category-filter-$selectedCategoryId").performClick()
        waitForText("مهمة التصنيف المختار")
        waitForTextToDisappear("مهمة تصنيف آخر")

        composeRule.activityRule.scenario.onActivity { activity ->
            assertEquals(selectedCategoryId, TaskStorage(activity).read().selectedTasksCategoryId)
        }

        // Recreation represents opening the application again; the data-backed filter must
        // still hide tasks from all other categories rather than reverting to every task.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("مهمة التصنيف المختار").assertIsDisplayed()
        waitForTextToDisappear("مهمة تصنيف آخر")
    }

    @Test
    fun tappingCalendarDate_opensScheduledEditorWithThatDateAlreadySelected() {
        val selectedDate = LocalDate.now()
        composeRule.onNodeWithTag("tab-CALENDAR").performClick()
        composeRule.onNodeWithTag("calendar-day-$selectedDate").performClick()

        waitForText("مهمة جديدة")
        // No manual date picker is necessary: the selected day already fills the required due date.
        waitForTextToDisappear("اختيار تاريخ ووقت")
        composeRule.onNodeWithTag("task-editor-title").performTextInput("موعد من التقويم")
        composeRule.onNodeWithText("إضافة").performClick()
        waitForText("موعد من التقويم")

        composeRule.activityRule.scenario.onActivity { activity ->
            val saved = TaskStorage(activity).read().tasks.single { it.title == "موعد من التقويم" }
            val savedDate = Instant.ofEpochMilli(requireNotNull(saved.dueAt))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            assertEquals(selectedDate, savedDate)
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTextToDisappear(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }

    private companion object {
        const val selectedTaskId = 2_001L
        const val otherTaskId = 2_002L
        const val selectedCategoryId = 51L
        const val otherCategoryId = 52L
        const val scheduledCategoryId = 53L
    }
}
