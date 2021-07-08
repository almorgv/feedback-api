package com.example.feedback.helpers

import com.example.feedback.models.Position
import com.example.feedback.models.Score

fun formatPosition(position: Position?): String {
    return when (position) {
        Position.TRAINEE -> "стажер"
        Position.JUNIOR -> "джуниор"
        Position.MIDDLE -> "мидл"
        Position.SENIOR -> "сеньор"
        else -> "(не указана)"
    }
}

fun formatScore(score: Score?): String {
    return when (score) {
        Score.WAY_BELOW_EXPECTATIONS -> "значительно ниже ожиданий"
        Score.BELOW_EXPECTATIONS -> "ниже ожиданий"
        Score.MEET_EXPECTATIONS -> "соответствует ожиданиям"
        Score.ABOVE_EXPECTATIONS -> "выше ожиданий"
        Score.WAY_ABOVE_EXPECTATIONS -> "значительно выше ожиданий"
        else -> "затрудняюсь дать оценку, нет данных, нерелевантно"
    }
}
