package com.example.avatartracker

enum class Category {
    FITNESS,
    LEARNING,
    CAREER,
    DISCIPLINE
}

enum class Difficulty(val xp: Int) {
    EASY(5),
    MEDIUM(10),
    HARD(20)
}

data class Task(
    val title: String,
    val category: Category,
    val difficulty: Difficulty
)