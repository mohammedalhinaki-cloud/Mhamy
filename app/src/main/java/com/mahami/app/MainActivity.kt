package com.mahami.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import org.json.JSONArray
import org.json.JSONObject

/**
 * مهامي هو دفتر مهام بسيط: ملف Kotlin واحد، شاشة واحدة، وقائمة محفوظة محلياً.
 * لا توجد حسابات، قوائم، مواعيد، تذكيرات أو إعدادات إضافية.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = TaskStorage(applicationContext)
        setContent {
            SimpleTasksApp(
                initialTasks = storage.read(),
                onTasksChanged = storage::write,
            )
        }
    }
}

private data class SavedTask(
    val id: Long,
    val title: String,
    val isDone: Boolean = false,
)

private class TaskStorage(context: Context) {
    private val preferences = context.getSharedPreferences("simple_tasks", Context.MODE_PRIVATE)

    fun read(): List<SavedTask> = runCatching {
        val saved = JSONArray(preferences.getString(TASKS_KEY, "[]") ?: "[]")
        buildList {
            repeat(saved.length()) { index ->
                val item = saved.optJSONObject(index) ?: return@repeat
                val title = item.optString("title").trim()
                if (title.isNotEmpty()) {
                    add(
                        SavedTask(
                            id = item.optLong("id"),
                            title = title,
                            isDone = item.optBoolean("isDone"),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    fun write(tasks: List<SavedTask>) {
        val saved = JSONArray()
        tasks.forEach { task ->
            saved.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("isDone", task.isDone)
                },
            )
        }
        preferences.edit().putString(TASKS_KEY, saved.toString()).apply()
    }

    private companion object {
        const val TASKS_KEY = "tasks"
    }
}

@Composable
private fun SimpleTasksApp(
    initialTasks: List<SavedTask>,
    onTasksChanged: (List<SavedTask>) -> Unit,
) {
    var tasks by remember { mutableStateOf(initialTasks) }
    var taskTitle by rememberSaveable { mutableStateOf("") }

    fun updateTasks(updatedTasks: List<SavedTask>) {
        tasks = updatedTasks
        onTasksChanged(updatedTasks)
    }

    val addTask = {
        val title = taskTitle.trim()
        if (title.isNotEmpty()) {
            val nextId = maxOf(
                System.currentTimeMillis(),
                (tasks.maxOfOrNull { it.id } ?: 0L) + 1,
            )
            updateTasks(listOf(SavedTask(id = nextId, title = title)) + tasks)
            taskTitle = ""
        }
    }

    val activeTasks = tasks.filterNot { it.isDone }
    val completedTasks = tasks.filter { it.isDone }

    MaterialTheme(colorScheme = simpleColorScheme()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "مهامي",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "اكتب ما تريد إنجازه، ثم علّم عليه عند الانتهاء.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("مهمة جديدة") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addTask() }),
                        )
                        Button(
                            onClick = addTask,
                            enabled = taskTitle.trim().isNotEmpty(),
                        ) {
                            Text("إضافة")
                        }
                    }

                    if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
                        EmptyTasksCard()
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item(key = "active-title") {
                                SectionTitle("المهام الحالية", activeTasks.size)
                            }
                            if (activeTasks.isEmpty()) {
                                item(key = "active-empty") {
                                    Text(
                                        text = "لا توجد مهام حالية.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                }
                            } else {
                                items(activeTasks, key = { it.id }) { task ->
                                    TaskRow(
                                        task = task,
                                        onDoneChanged = { isDone ->
                                            updateTasks(tasks.map {
                                                if (it.id == task.id) it.copy(isDone = isDone) else it
                                            })
                                        },
                                        onDelete = { updateTasks(tasks.filterNot { it.id == task.id }) },
                                    )
                                }
                            }

                            if (completedTasks.isNotEmpty()) {
                                item(key = "completed-title") {
                                    SectionTitle("المكتملة", completedTasks.size)
                                }
                                items(completedTasks, key = { it.id }) { task ->
                                    TaskRow(
                                        task = task,
                                        onDoneChanged = { isDone ->
                                            updateTasks(tasks.map {
                                                if (it.id == task.id) it.copy(isDone = isDone) else it
                                            })
                                        },
                                        onDelete = { updateTasks(tasks.filterNot { it.id == task.id }) },
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "تُحفظ مهامك على هذا الجهاز فقط.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TaskRow(
    task: SavedTask,
    onDoneChanged: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onDoneChanged,
            )
            Text(
                text = task.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                fontSize = 16.sp,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                color = if (task.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            TextButton(onClick = onDelete) {
                Text("حذف")
            }
        }
    }
}

@Composable
private fun EmptyTasksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "ابدأ بأول مهمة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "اكتب المهمة في الحقل بالأعلى واضغط إضافة.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun simpleColorScheme() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    darkColorScheme(
        primary = Color(0xFF9CCBFF),
        secondary = Color(0xFFB9C9DD),
    )
} else {
    lightColorScheme(
        primary = Color(0xFF075E54),
        secondary = Color(0xFF45645F),
    )
}
