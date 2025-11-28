package com.example.avatartracker

import android.content.Context
import android.media.SoundPool

object SoundManager {

    private var soundPool: SoundPool? = null
    private var isLoaded = false

    // Sound IDs (0 = not loaded / not used)
    private var taskCompleteId = 0
    private var taskUncompleteId = 0
    private var levelUpId = 0

    private var classWarriorId = 0
    private var classWizardId = 0
    private var classSamuraiId = 0
    private var classProfessionalId = 0

    private var restDayId = 0
    private var taskAddId = 0
    private var taskEditId = 0
    private var taskDeleteId = 0

    fun init(context: Context) {
        if (soundPool != null) return // already initialized

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .build().apply {
                setOnLoadCompleteListener { _, _, _ ->
                    isLoaded = true
                }
            }

        // ⚠️ IMPORTANT:
        // Only keep the lines for files you actually created in res/raw.
        // If Android Studio says "Unresolved reference: task_edit" etc,
        // either add that file OR delete that specific load line.

        taskCompleteId = soundPool!!.load(context, R.raw.task_complete, 1)
        taskUncompleteId = soundPool!!.load(context, R.raw.task_uncomplete, 1)
        levelUpId = soundPool!!.load(context, R.raw.level_up, 1)

        classWarriorId = soundPool!!.load(context, R.raw.class_change_warrior, 1)
        classWizardId = soundPool!!.load(context, R.raw.class_change_wizard, 1)
        classSamuraiId = soundPool!!.load(context,R.raw.class_change_samurai, 1)
        classProfessionalId = soundPool!!.load(context, R.raw.class_change_professional, 1)

        restDayId = soundPool!!.load(context, R.raw.rest_day, 1)
        taskAddId = soundPool!!.load(context, R.raw.task_add, 1)
        taskEditId = soundPool!!.load(context, R.raw.task_edit, 1)
        taskDeleteId = soundPool!!.load(context, R.raw.task_delete, 1)
    }

    private fun play(id: Int, pitch: Float = 1f) {
        val sp = soundPool ?: return
        if (!isLoaded) return
        if (id == 0) return

        sp.play(
            id,
            1f,   // left volume
            1f, // right volume
            1,   // prioirty
            0,     // loop
            pitch    // playback rate
        )
    }

    fun playTaskComplete() {
        play(taskCompleteId)
    }

    fun playTaskUncomplete() {
        play(taskUncompleteId, pitch = 0.9f)
    }

    fun playLevelUp() {
        play(levelUpId, pitch = 1.05f)
    }

    fun playClassChange(category: Category?) {
        when (category) {
            Category.FITNESS -> play(classWarriorId)
            Category.LEARNING -> play (classWizardId)
            Category.CAREER -> play(classProfessionalId)
            Category.DISCIPLINE -> play(classSamuraiId)
            null -> { /* Adventurer → no special sound for now */ }
        }
    }

    fun playRestDay() {
        play(restDayId)
    }

    fun playTaskAdd() {
        play(taskAddId)
    }

    fun playTaskEdit() {
        play(taskEditId)
    }

    fun playTaskDelete() {
        play(taskDeleteId)
    }
}