package com.mahami.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class TaskSection {
    TASKS,
    SCHEDULED,
}

data class Task(
    val id: Long,
    val title: String,
    val categoryId: Long? = null,
    val dueAt: Long? = null,
    val isDone: Boolean = false,
    val isPinned: Boolean = false,
)

data class Category(
    val id: Long,
    val name: String,
    val color: Long,
    val section: TaskSection,
)

data class TaskDraft(
    val section: TaskSection,
    val title: String,
    val categoryId: Long? = null,
    val dueAt: Long? = null,
)

data class MahamiData(
    val tasks: List<Task>,
    val categories: List<Category>,
    val drafts: List<TaskDraft> = emptyList(),
)

/**
 * Local JSON store. The normalization step keeps all data from the earlier
 * shared-category format and splits legacy categories into the two sections.
 */
class TaskStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): MahamiData {
        val tasks = readTasks()
        val parsedCategories = readCategories()
        val drafts = readDrafts()
        val data = normalize(tasks, parsedCategories, drafts)
        if (data.requiresWrite) write(data.value)
        return data.value
    }

    fun write(data: MahamiData) {
        preferences.edit()
            .putString(TASKS_KEY, data.tasks.toTasksJsonArray().toString())
            .putString(CATEGORIES_KEY, data.categories.toCategoriesJsonArray().toString())
            .putString(DRAFTS_KEY, data.drafts.toDraftsJsonArray().toString())
            .apply()
    }

    private fun normalize(
        storedTasks: List<Task>,
        storedCategories: List<StoredCategory>,
        drafts: List<TaskDraft>,
    ): NormalizedData {
        if (storedCategories.isEmpty()) {
            val categories = if (preferences.contains(CATEGORIES_KEY)) emptyList() else defaultCategories
            return NormalizedData(
                value = MahamiData(storedTasks, categories, drafts),
                requiresWrite = !preferences.contains(CATEGORIES_KEY) && (storedTasks.isNotEmpty() || drafts.isNotEmpty()),
            )
        }

        val hasLegacyCategories = storedCategories.any { it.section == null }
        if (!hasLegacyCategories) {
            return NormalizedData(
                value = MahamiData(
                    tasks = storedTasks,
                    categories = storedCategories.map { it.toCategory() },
                    drafts = drafts,
                ),
                requiresWrite = false,
            )
        }

        var nextId = maxOf(
            System.currentTimeMillis(),
            (storedCategories.maxOfOrNull { it.id } ?: 0L) + 1L,
        )
        val taskCategories = storedCategories.map {
            Category(it.id, it.name, it.color, TaskSection.TASKS)
        }
        val scheduledIdByLegacyId = buildMap {
            storedCategories.forEach { legacy ->
                put(legacy.id, nextId)
                nextId += 1L
            }
        }
        val scheduledCategories = storedCategories.map { legacy ->
            Category(
                id = scheduledIdByLegacyId.getValue(legacy.id),
                name = legacy.name,
                color = legacy.color,
                section = TaskSection.SCHEDULED,
            )
        }
        val migratedTasks = storedTasks.map { task ->
            if (task.dueAt != null && task.categoryId != null) {
                task.copy(categoryId = scheduledIdByLegacyId[task.categoryId] ?: task.categoryId)
            } else {
                task
            }
        }
        val migratedDrafts = drafts.map { draft ->
            if (draft.section == TaskSection.SCHEDULED && draft.categoryId != null) {
                draft.copy(categoryId = scheduledIdByLegacyId[draft.categoryId] ?: draft.categoryId)
            } else {
                draft
            }
        }
        return NormalizedData(
            value = MahamiData(migratedTasks, taskCategories + scheduledCategories, migratedDrafts),
            requiresWrite = true,
        )
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
                            isPinned = item.optBoolean("isPinned"),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun readCategories(): List<StoredCategory> = runCatching {
        val saved = JSONArray(preferences.getString(CATEGORIES_KEY, "[]") ?: "[]")
        buildList {
            repeat(saved.length()) { index ->
                val item = saved.optJSONObject(index) ?: return@repeat
                val name = item.optString("name").trim()
                if (name.isNotEmpty()) {
                    add(
                        StoredCategory(
                            id = item.optLong("id"),
                            name = name,
                            color = item.optLong("color", DEFAULT_CATEGORY_COLORS[index % DEFAULT_CATEGORY_COLORS.size]),
                            section = item.optionalSection(),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun readDrafts(): List<TaskDraft> = runCatching {
        val saved = JSONArray(preferences.getString(DRAFTS_KEY, "[]") ?: "[]")
        buildList {
            repeat(saved.length()) { index ->
                val item = saved.optJSONObject(index) ?: return@repeat
                val section = item.optionalSection() ?: return@repeat
                val title = item.optString("title")
                if (title.isNotBlank()) {
                    add(
                        TaskDraft(
                            section = section,
                            title = title,
                            categoryId = item.optionalLong("categoryId"),
                            dueAt = item.optionalLong("dueAt"),
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
                    put("isPinned", task.isPinned)
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
                    put("section", category.section.name)
                },
            )
        }
    }

    private fun List<TaskDraft>.toDraftsJsonArray() = JSONArray().also { array ->
        forEach { draft ->
            array.put(
                JSONObject().apply {
                    put("section", draft.section.name)
                    put("title", draft.title)
                    put("categoryId", draft.categoryId)
                    put("dueAt", draft.dueAt)
                },
            )
        }
    }

    private fun JSONObject.optionalLong(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null

    private fun JSONObject.optionalSection(): TaskSection? =
        optString("section").takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { TaskSection.valueOf(raw) }.getOrNull()
        }

    private data class StoredCategory(
        val id: Long,
        val name: String,
        val color: Long,
        val section: TaskSection?,
    ) {
        fun toCategory() = Category(id, name, color, requireNotNull(section))
    }

    private data class NormalizedData(
        val value: MahamiData,
        val requiresWrite: Boolean,
    )

    private companion object {
        const val PREFERENCES_NAME = "simple_tasks"
        const val TASKS_KEY = "tasks"
        const val CATEGORIES_KEY = "categories"
        const val DRAFTS_KEY = "drafts"
    }
}

val defaultCategories = listOf(
    Category(id = 1, name = "العمل", color = 0xFF7C8CFFL, section = TaskSection.TASKS),
    Category(id = 2, name = "العائلة", color = 0xFFB78BFAL, section = TaskSection.TASKS),
    Category(id = 3, name = "المنزل", color = 0xFFF0B45CL, section = TaskSection.TASKS),
    Category(id = 4, name = "العمل", color = 0xFF7C8CFFL, section = TaskSection.SCHEDULED),
    Category(id = 5, name = "العائلة", color = 0xFFB78BFAL, section = TaskSection.SCHEDULED),
    Category(id = 6, name = "المنزل", color = 0xFFF0B45CL, section = TaskSection.SCHEDULED),
)

val DEFAULT_CATEGORY_COLORS = listOf(
    0xFF7C8CFFL,
    0xFFB78BFAL,
    0xFFF0B45CL,
    0xFF48C7B0L,
    0xFFFF8FA3L,
    0xFF61BFEAL,
)
