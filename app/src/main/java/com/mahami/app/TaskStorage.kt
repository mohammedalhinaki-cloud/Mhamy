package com.mahami.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A task has only the fields used by the three screens. [dueAt] is optional. */
data class Task(
    val id: Long,
    val title: String,
    val categoryId: Long? = null,
    val dueAt: Long? = null,
    val isDone: Boolean = false,
)

data class Category(
    val id: Long,
    val name: String,
    val color: Long,
)

data class MahamiData(
    val tasks: List<Task>,
    val categories: List<Category>,
)

/**
 * Small local store with no database or account. It also reads the task format
 * from the earlier simple version, so existing task titles remain available.
 */
class TaskStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): MahamiData {
        val categories = readCategories().ifEmpty { defaultCategories }
        return MahamiData(tasks = readTasks(), categories = categories)
    }

    fun write(data: MahamiData) {
        preferences.edit()
            .putString(TASKS_KEY, data.tasks.toTasksJsonArray().toString())
            .putString(CATEGORIES_KEY, data.categories.toCategoriesJsonArray().toString())
            .apply()
    }

    private fun readTasks(): List<Task> = runCatching {
        val saved = JSONArray(preferences.getString(TASKS_KEY, "[]") ?: "[]")
        buildList {
            repeat(saved.length()) { index ->
                val item = saved.optJSONObject(index) ?: return@repeat
                val title = item.optString("title").trim()
                if (title.isNotEmpty()) {
                    add(
                        Task(
                            id = item.optLong("id"),
                            title = title,
                            categoryId = item.optionalLong("categoryId"),
                            dueAt = item.optionalLong("dueAt"),
                            isDone = item.optBoolean("isDone"),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun readCategories(): List<Category> = runCatching {
        val saved = JSONArray(preferences.getString(CATEGORIES_KEY, "[]") ?: "[]")
        buildList {
            repeat(saved.length()) { index ->
                val item = saved.optJSONObject(index) ?: return@repeat
                val name = item.optString("name").trim()
                if (name.isNotEmpty()) {
                    add(
                        Category(
                            id = item.optLong("id"),
                            name = name,
                            color = item.optLong("color", DEFAULT_CATEGORY_COLORS[index % DEFAULT_CATEGORY_COLORS.size]),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun List<Task>.toTasksJsonArray() = JSONArray().also { array ->
        forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("categoryId", task.categoryId)
                    put("dueAt", task.dueAt)
                    put("isDone", task.isDone)
                },
            )
        }
    }

    private fun List<Category>.toCategoriesJsonArray() = JSONArray().also { array ->
        forEach { category ->
            array.put(
                JSONObject().apply {
                    put("id", category.id)
                    put("name", category.name)
                    put("color", category.color)
                },
            )
        }
    }

    private fun JSONObject.optionalLong(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null

    private companion object {
        const val PREFERENCES_NAME = "simple_tasks"
        const val TASKS_KEY = "tasks"
        const val CATEGORIES_KEY = "categories"
    }
}

val defaultCategories = listOf(
    Category(id = 1, name = "العمل", color = 0xFF2563EBL),
    Category(id = 2, name = "العائلة", color = 0xFF8B5CF6L),
    Category(id = 3, name = "المنزل", color = 0xFFF59E0BL),
)

val DEFAULT_CATEGORY_COLORS = listOf(
    0xFF2563EBL,
    0xFF8B5CF6L,
    0xFFF59E0BL,
    0xFF059669L,
    0xFFDB2777L,
    0xFF0891B2L,
)
