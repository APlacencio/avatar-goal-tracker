package com.example.avatartracker

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.avatartracker.ui.theme.AvatarGoalTrackerTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Person
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.scale

const val MAX_REST_DAYS = 2
const val REST_DAYS_KEY = "rest_days_remaining"
// Put this at top-level, NOT inside HomeScreen or any other function
enum class BottomTab {
    HOME,
    TASKS,
    AVATAR
}


// Make sure you do NOT have any of these bad imports at the top:
// import kotlin.text.Category
// import kotlin.text.*
// If you see anything like that, DELETE it.

// ---------- TASK PERSISTENCE HELPERS ----------

private const val PREFS_NAME = "avatar_prefs"
private const val KEY_TASKS = "tasks_data"

// Encode tasks as: "title|CATEGORY|DIFFICULTY;;title2|CATEGORY|DIFFICULTY"
fun loadTasksFromPrefs(context: Context, defaultTasks: List<Task>): List<Task> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val stored = prefs.getString(KEY_TASKS, null) ?: return defaultTasks

    if (stored.isBlank()) return defaultTasks

    return stored.split(";;")
        .filter { it.isNotBlank() }
        .mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 3) return@mapNotNull null

            val title = parts[0]

            val category = try {
                Category.valueOf(parts[1])
            } catch (_: IllegalArgumentException) {
                return@mapNotNull null
            }

            val difficulty = try {
                Difficulty.valueOf(parts[2])
            } catch (_: IllegalArgumentException) {
                return@mapNotNull null
            }

            Task(
                title = title,
                category = category,
                difficulty = difficulty
            )
        }
}

fun saveTasksToPrefs(context: Context, tasks: List<Task>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val encoded = tasks.joinToString(";;") { task ->
        "${task.title}|${task.category.name}|${task.difficulty.name}"
    }
    prefs.edit()
        .putString(KEY_TASKS, encoded)
        .apply()
}

// ---------- MAIN HOME SCREEN ----------

@Composable
fun HomeScreen() {

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Default static tasks (used only if nothing is saved yet)
    val defaultTasks = remember {
        listOf(
            Task("Morning workout", Category.FITNESS, Difficulty.MEDIUM),
            Task("Read 20 pages", Category.LEARNING, Difficulty.EASY),
            Task("Study for cert", Category.CAREER, Difficulty.HARD),
            Task("No snooze button", Category.DISCIPLINE, Difficulty.MEDIUM)
        )
    }

    // Editable, persistent task list
    val tasks: SnapshotStateList<Task> = remember {
        mutableStateListOf<Task>().apply {
            addAll(loadTasksFromPrefs(context, defaultTasks))
        }
    }

    var completedTasks by remember {
        mutableStateOf(
            prefs.getStringSet("completed_tasks", emptySet()) ?: emptySet()
        )
    }

    var xp by remember {
        mutableStateOf(
            prefs.getInt("xp", 0)
        )
    }

    var restDaysRemaining by remember {
        mutableStateOf(
            prefs.getInt(REST_DAYS_KEY, MAX_REST_DAYS)
        )
    }

    var categoryXp by remember {
        mutableStateOf(
            mutableMapOf(
                Category.FITNESS to prefs.getInt("xp_fitness", 0),
                Category.LEARNING to prefs.getInt("xp_learning", 0),
                Category.CAREER to prefs.getInt("xp_career", 0),
                Category.DISCIPLINE to prefs.getInt("xp_discipline", 0)
            )
        )
    }

    var levelUpTrigger by remember { mutableStateOf(0) }

    val level = xp / 100 + 1
    val xpInLevel = xp % 100

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskBeingEditedIndex by remember { mutableStateOf<Int?>(null) }

    val dominantCategory = categoryXp.maxByOrNull { it.value }?.key

    val avatarClassLabel = when (dominantCategory) {
        Category.FITNESS -> "Warrior"
        Category.LEARNING -> "Wizard"
        Category.CAREER -> "Professional"
        Category.DISCIPLINE -> "Samurai"
        else -> "Adventurer"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Avatar section
        HeroHeader(
            avatarClassLabel = avatarClassLabel,
            level = level,
            xpInLevel = xpInLevel,
            levelUpTrigger = levelUpTrigger
        )

        Divider(
            modifier = Modifier
                .padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section header
        Text(
            text = "Today's Tasks",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Category XP:",
            style = MaterialTheme.typography.bodySmall
        )

        Text("Fitness: ${categoryXp[Category.FITNESS] ?: 0}")
        Text("Learning: ${categoryXp[Category.LEARNING] ?: 0}")
        Text("Career: ${categoryXp[Category.CAREER] ?: 0}")
        Text("Discipline: ${categoryXp[Category.DISCIPLINE] ?: 0}")

        Spacer(modifier = Modifier.height(8.dp))

        // Use internal tasks list
        tasks.forEach { task ->

            val isCompleted = task.title in completedTasks
            val checkScale by animateFloatAsState(
                targetValue = if (isCompleted) 1f else 0f,
                label = "checkScale"
            )

            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable {
                        val previousLevel = xp / 100 + 1
                        val previousDominantCategory = categoryXp.maxByOrNull { it.value }?.key

                        val wasCompleted = task.title in completedTasks

                        if (wasCompleted) {
                            // un-completing
                            completedTasks = completedTasks - task.title
                            xp = (xp - task.difficulty.xp).coerceAtLeast(0)

                            val oldValue = categoryXp[task.category] ?: 0
                            categoryXp[task.category] = (oldValue - task.difficulty.xp).coerceAtLeast(0)

                            SoundManager.playTaskUncomplete()
                        } else {
                            // completing
                            completedTasks = completedTasks + task.title
                            xp += task.difficulty.xp

                            val oldValue = categoryXp[task.category] ?: 0
                            categoryXp[task.category] = oldValue + task.difficulty.xp

                            SoundManager.playTaskComplete()
                        }

                        val newLevel = xp / 100 + 1
                        val newDominantCategory = categoryXp.maxByOrNull { it.value }?.key

                        if (newLevel > previousLevel) {
                            SoundManager.playLevelUp()
                            levelUpTrigger++ // tell UI to animate
                        }

                        if (newDominantCategory != previousDominantCategory) {
                            SoundManager.playClassChange(newDominantCategory)
                        }

                        // Save to Shared Preferences
                        with(prefs.edit()) {
                            putInt("xp", xp)
                            putStringSet("completed_tasks", completedTasks.toSet())
                            putInt("xp_fitness", categoryXp[Category.FITNESS] ?: 0)
                            putInt("xp_learning", categoryXp[Category.LEARNING] ?: 0)
                            putInt("xp_career", categoryXp[Category.CAREER] ?: 0)
                            putInt("xp_discipline", categoryXp[Category.DISCIPLINE] ?: 0)
                            apply()
                        }

                        // Also save tasks layout (for future edits)
                        saveTasksToPrefs(context, tasks)
                    }

            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .then(Modifier),
                        tint = if (isCompleted) {
                            Color.Green
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Column {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCompleted) {
                                Color.Green
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }

                Text(
                    text = "${task.category} • ${task.difficulty} • ${task.difficulty.xp} XP",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // TODO: hook up real rest-day logic later
                SoundManager.playRestDay()
            }
        ) {
            Text("Use Rest Day ($restDaysRemaining left)")
        }
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            existingTask = taskBeingEditedIndex?.let { tasks[it] },
            onDismiss = { showTaskDialog = false },
            onSave = { newTask ->
                if (taskBeingEditedIndex == null) {
                    // Add
                    tasks.add(newTask)
                } else {
                    // Edit
                    tasks[taskBeingEditedIndex!!] = newTask
                }
                saveTasksToPrefs(context, tasks)
                showTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskEditorDialog(
    existingTask: Task?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(existingTask?.category ?: Category.FITNESS) }
    var selectedDifficulty by remember { mutableStateOf(existingTask?.difficulty ?: Difficulty.EASY)}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (existingTask == null) "Add Task" else "Edit Task")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Category")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Category.values().forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    category.name.lowercase()
                                        .replaceFirstChar { it.uppercase() }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Difficulty")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Difficulty.values().forEach { difficulty ->
                        FilterChip(
                            selected = selectedDifficulty == difficulty,
                            onClick = { selectedDifficulty = difficulty },
                            label = {
                                difficulty.name.lowercase()
                                    .replaceFirstChar { it.uppercase() }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            Task(
                                title = title.trim(),
                                category = selectedCategory,
                                difficulty = selectedDifficulty
                            )
                        )
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MainNavScreen() {
    var selectedTab by remember {
        mutableStateOf<BottomTab>(BottomTab.HOME)
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                BottomTab.HOME -> HomeScreen()
                BottomTab.TASKS -> TaskManagementScreen()
                BottomTab.AVATAR -> AvatarScreen()
            }
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    Surface(
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Home",
                selected = selectedTab == BottomTab.HOME,
                onClick = { onTabSelected(BottomTab.HOME) },
                icon = Icons.Filled.Home
            )

            BottomNavItem(
                label = "Tasks",
                selected = selectedTab == BottomTab.TASKS,
                onClick = { onTabSelected(BottomTab.TASKS) },
                icon = Icons.Filled.List
            )

            BottomNavItem(
                label = "Avatar",
                selected = selectedTab == BottomTab.AVATAR,
                onClick = { onTabSelected(BottomTab.AVATAR) },
                icon = Icons.Filled.Person
            )
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector
) {
    val contentColor =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}


@Composable
fun TaskManagementScreen() {
    val context = LocalContext.current

    // Use same default tasks as HomeScreen
    val defaultTasks = remember {
        listOf(
            Task("Morning workout", Category.FITNESS, Difficulty.MEDIUM),
            Task("Read 20 pages", Category.LEARNING, Difficulty.EASY),
            Task("Study for cert", Category.CAREER, Difficulty.MEDIUM)
        )
    }

    val tasks = remember {
        mutableStateListOf<Task>().apply {
            addAll(loadTasksFromPrefs(context, defaultTasks))
        }
    }

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskBeingEditedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            text = "Task Management",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                taskBeingEditedIndex = null
                showTaskDialog = true
            }
        ) {
            Text("Add Task")
        }

        Spacer(modifier = Modifier.height(16.dp))

        tasks.forEachIndexed { index, task ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        // No XP changes here, this screen is purely for managing tasks.
                    }
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${task.category} • ${task.difficulty} • ${task.difficulty.xp} XP",
                    style = MaterialTheme.typography.bodySmall
                )

                Row {
                    TextButton(
                        onClick = {
                            taskBeingEditedIndex = index
                            showTaskDialog = true
                        }
                    ) {
                        Text("Edit")
                    }
                    TextButton(
                        onClick = {
                            tasks.removeAt(index)
                            saveTasksToPrefs(context, tasks)
                            SoundManager.playTaskDelete()
                        }
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            existingTask = taskBeingEditedIndex?.let { tasks[it] },
            onDismiss = { showTaskDialog = false },
            onSave = { newTask ->
                if (taskBeingEditedIndex == null) {
                    // Add
                    tasks.add(newTask)
                    SoundManager.playTaskAdd()
                } else {
                    // Edit
                    tasks[taskBeingEditedIndex!!] = newTask
                    SoundManager.playTaskEdit()
                }
                saveTasksToPrefs(context, tasks)
                showTaskDialog = false
            }
        )
    }
}


@Composable
fun HeroHeader(
    avatarClassLabel: String,
    level: Int,
    xpInLevel: Int,
    levelUpTrigger: Int
) {
    val avatarEmoji = when (avatarClassLabel) {
        "Warrior" -> "🗡️"
        "Wizard" -> "🧙‍♂️"
        "Professional" -> "💼"
        "Samurai" -> "🗡️🇯🇵"
        else -> "🧭"
    }

    // Scale animation for level-up pulse
    val scale = remember { Animatable(1f) }

    LaunchedEffect(levelUpTrigger) {
        // Only animate if we've actually leveled at least once
        if (levelUpTrigger > 0) {
            // Quick pulse: 1.0 → 1.2 → 1.0
            scale.snapTo(1f)
            scale.animateTo(
                1.2f,
                animationSpec = tween(durationMillis = 160)
            )
            scale.animateTo(
                1f,
                animationSpec = tween(durationMillis = 120)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar emoji big and proud, with scale applied
        Text(
            text = avatarEmoji,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.scale(scale.value)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$avatarClassLabel (Level $level)",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "XP: $xpInLevel / 100",
            style = MaterialTheme.typography.bodyMedium
        )

        LinearProgressIndicator(
            progress = xpInLevel / 100f,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        )
    }
}





@Composable
fun AvatarScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Read XP + category XP from the same prefs HomeScreen uses
    val xp = prefs.getInt("xp", 0)

    val fitnessXp = prefs.getInt("xp_fitness", 0)
    val learningXp = prefs.getInt("xp_learning", 0)
    val careerXp = prefs.getInt("xp_career", 0)
    val disciplineXp = prefs.getInt("xp_discipline", 0)

    val categoryXpMap = mapOf(
        Category.FITNESS to fitnessXp,
        Category.LEARNING to learningXp,
        Category.CAREER to careerXp,
        Category.DISCIPLINE to disciplineXp
    )

    val level = xp / 100 + 1
    val xpInLevel = xp % 100

    val dominantCategory = categoryXpMap.maxByOrNull { it.value }?.key

    val avatarClassLabel = when (dominantCategory) {
        Category.FITNESS -> "Warrior"
        Category.LEARNING -> "Wizard"
        Category.CAREER -> "Professional"
        Category.DISCIPLINE -> "Samurai"
        else -> "Adventurer"
    }

    val avatarEmoji = when (avatarClassLabel) {
        "Warrior" -> "🗡️"
        "Wizard" -> "🧙‍♂️"
        "Professional" -> "💼"
        "Samurai" -> "🗡️🇯🇵"
        else -> "🧭"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Avatar",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = avatarEmoji,
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$avatarClassLabel (Level $level)",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "XP: $xpInLevel / 100",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Category XP",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Fitness: $fitnessXp")
        Text("Learning: $learningXp")
        Text("Career: $careerXp")
        Text("Discipline: $disciplineXp")
    }
}


// ---------- PREVIEW ----------

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AvatarGoalTrackerTheme {
        MainNavScreen()
    }
}
