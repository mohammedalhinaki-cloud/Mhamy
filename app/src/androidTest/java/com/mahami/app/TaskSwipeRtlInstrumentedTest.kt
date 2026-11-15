package com.mahami.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the Arabic RTL application: a physical right swipe opens the left-side action panel. */
@RunWith(AndroidJUnit4::class)
class TaskSwipeRtlInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedTasks() {
        composeRule.activityRule.scenario.onActivity { activity ->
            TaskStorage(activity).write(
                MahamiData(
                    tasks = listOf(
                        Task(id = firstTaskId, title = "مهمة اختبار أولى", categoryId = categoryId),
                        Task(id = secondTaskId, title = "مهمة اختبار ثانية", categoryId = categoryId),
                    ),
                    categories = listOf(
                        Category(
                            id = categoryId,
                            name = "اختبار",
                            color = 0xFF7C8CFFL,
                            section = TaskSection.TASKS,
                        ),
                    ),
                ),
            )
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    fun rightSwipe_revealsThreeActions_andEachActionWorks_inArabicRtl() {
        assertEquals(
            LayoutDirection.Rtl,
            composeRule.onNodeWithTag(cardTag(firstTaskId), useUnmergedTree = true)
                .fetchSemanticsNode()
                .layoutInfo
                .layoutDirection,
        )

        // A physical right swipe opens exactly the three actions on the first task.
        swipeRight(firstTaskId)
        composeRule.onNodeWithTag(editTag(firstTaskId)).assertIsDisplayed()
        composeRule.onNodeWithTag(pinTag(firstTaskId)).assertIsDisplayed()
        composeRule.onNodeWithTag(deleteTag(firstTaskId)).assertIsDisplayed()

        // Edit opens the task editor and resets the open action row.
        composeRule.onNodeWithTag(editTag(firstTaskId)).performClick()
        composeRule.onNodeWithText("تعديل المهمة").assertIsDisplayed()
        composeRule.onNodeWithText("إغلاق").performClick()
        waitForTagToDisappear(editTag(firstTaskId))

        // Pin updates only the selected task and closes its action row.
        swipeRight(firstTaskId)
        composeRule.onNodeWithTag(pinTag(firstTaskId)).performClick()
        waitForContentDescription("مثبتة")
        composeRule.onNodeWithContentDescription("مثبتة").assertIsDisplayed()
        waitForTagToDisappear(pinTag(firstTaskId))

        // Opening another row closes the first row instead of leaving two panels active.
        swipeRight(secondTaskId)
        composeRule.onNodeWithTag(editTag(secondTaskId)).assertIsDisplayed()
        waitForTagToDisappear(editTag(firstTaskId))

        // A physical left swipe closes the active row cleanly; repeat it to catch stuck state.
        repeat(3) {
            swipeLeft(secondTaskId)
            waitForTagToDisappear(editTag(secondTaskId))
            swipeRight(secondTaskId)
            composeRule.onNodeWithTag(editTag(secondTaskId)).assertIsDisplayed()
        }

        // Delete is explicit: it opens confirmation and never removes a task from swipe distance.
        composeRule.onNodeWithTag(deleteTag(secondTaskId)).performClick()
        composeRule.onNodeWithText("حذف المهمة؟").assertIsDisplayed()
        composeRule.onNodeWithText("إلغاء").performClick()
        waitForText("مهمة اختبار ثانية")
        composeRule.onNodeWithText("مهمة اختبار ثانية").assertIsDisplayed()
        waitForTagToDisappear(deleteTag(secondTaskId))

        // The explicit confirmation performs deletion; a swipe alone never does.
        swipeRight(secondTaskId)
        composeRule.onNodeWithTag(deleteTag(secondTaskId)).performClick()
        composeRule.onNodeWithTag("delete-confirm").performClick()
        waitForTextToDisappear("مهمة اختبار ثانية")
    }

    private fun swipeRight(taskId: Long) {
        composeRule.onNodeWithTag(cardTag(taskId), useUnmergedTree = true).performTouchInput {
            val centerY = height / 2f
            down(Offset(width * 0.15f, centerY))
            moveTo(Offset(width * 0.92f, centerY), delayMillis = 360)
            up()
        }
        waitForTag(editTag(taskId))
        composeRule.waitForIdle()
    }

    private fun swipeLeft(taskId: Long) {
        // Once open, only the card's left-most local area remains visible on screen because
        // the card has shifted physically right. Begin there, then drag outside its bounds.
        composeRule.onNodeWithTag(cardTag(taskId), useUnmergedTree = true).performTouchInput {
            val centerY = height / 2f
            down(Offset(width * 0.20f, centerY))
            moveTo(Offset(-width * 0.60f, centerY), delayMillis = 300)
            up()
        }
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagToDisappear(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForContentDescription(contentDescription: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(contentDescription).fetchSemanticsNodes().isNotEmpty()
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
        const val categoryId = 41L
        const val firstTaskId = 1_001L
        const val secondTaskId = 1_002L

        fun cardTag(taskId: Long) = "task-card-$taskId"
        fun editTag(taskId: Long) = "swipe-edit-$taskId"
        fun pinTag(taskId: Long) = "swipe-pin-$taskId"
        fun deleteTag(taskId: Long) = "swipe-delete-$taskId"
    }
}
