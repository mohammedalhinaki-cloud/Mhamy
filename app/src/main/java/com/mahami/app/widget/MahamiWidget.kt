package com.mahami.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mahami.app.MainActivity
import com.mahami.app.data.local.dao.TaskDao
import com.mahami.app.data.local.entity.Task
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first

/** How many task titles the widget lists before collapsing into «+N». */
private const val MAX_SHOWN_TASKS = 5

/**
 * Hilt bridge: Glance widgets are created by the system (not Hilt), so the
 * database DAO is grabbed from the app's SingletonComponent via an EntryPoint.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskDao(): TaskDao
}

/**
 * «مهامي» home-screen widget: today's date, overdue count, up to 5 of today's
 * open tasks. Tapping anywhere opens the app. Refreshes on the system's
 * 30-minute update tick and every time the app is opened (see MainActivity).
 */
class MahamiWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .taskDao()
        val today = LocalDate.now()
        val todayTasks = dao.observeTasksForDate(today).first()
        val overdueCount = dao.observeOverdue(today).first().size

        provideContent {
            GlanceTheme {
                WidgetContent(
                    context = context,
                    today = today,
                    tasks = todayTasks,
                    overdueCount = overdueCount,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    context: Context,
    today: LocalDate,
    tasks: List<Task>,
    overdueCount: Int,
) {
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .clickable(openApp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "مهامي",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = today.format(DateTimeFormatter.ofPattern("EEEE، d MMMM", Locale("ar"))),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
        if (overdueCount > 0) {
            Text(
                text = "متأخرة: $overdueCount",
                style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp),
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (tasks.isEmpty()) {
            Column(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "لا توجد مهام اليوم",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp,
                    ),
                )
            }
        } else {
            tasks.take(MAX_SHOWN_TASKS).forEach { task ->
                TaskRow(task)
            }
            val remaining = tasks.size - MAX_SHOWN_TASKS
            if (remaining > 0) {
                Text(
                    text = "+$remaining أخرى",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                    modifier = GlanceModifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Small dot marker, echoing the in-app completion circles
        // (drawable ring shapes are not available in Glance — simplest marker).
        Spacer(
            modifier = GlanceModifier
                .size(10.dp)
                .background(GlanceTheme.colors.primary)
                .cornerRadius(5.dp),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = task.title,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
        )
    }
}

/** System-side receiver that hosts [MahamiWidget]. */
class MahamiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MahamiWidget()
}
