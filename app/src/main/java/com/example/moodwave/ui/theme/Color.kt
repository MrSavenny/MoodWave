package com.example.moodwave.ui.theme

import androidx.compose.ui.graphics.Color

val PastelLavender = Color(0xFFE8E3F1)
val PastelPeach = Color(0xFFF7E3D6)
val PastelMint = Color(0xFFDDECE0)
val PastelSky = Color(0xFFE0F2FE)
val PastelPink = Color(0xFFFCE7F3)

val Mood1 = Color(0xFFC5CAE9) // 😭 Muy Triste
val Mood2 = Color(0xFFB3E5FC) // 😢 Triste
val Mood3 = Color(0xFFE0F2FE) // 🌧️ Melancólico
val Mood4 = Color(0xFFFECDD3) // 😣 Estresado
val Mood5 = Color(0xFFFCA5A5) // 😡 Enojado
val Mood6 = Color(0xFFFFCCBC) // 😤 Frustrado
val Mood7 = Color(0xFFFED7AA) // 😰 Ansioso
val Mood8 = Color(0xFFE2E8F0) // 🥱 Cansado
val Mood9 = Color(0xFFFEE2E2) // ⚡ Agitado
val Mood10 = Color(0xFFB2DFDB) // 💭 Reflexivo
val Mood11 = Color(0xFFD1FAE5) // 😐 Neutral
val Mood12 = Color(0xFFDCEDC8) // ☕ Calmado
val Mood13 = Color(0xFFA7F3D0) // 🧘 En Paz
val Mood14 = Color(0xFFE0F2FE) // 🌱 Renovado
val Mood15 = Color(0xFFFFF9C4) // 🙂 Satisfecho
val Mood16 = Color(0xFFFFE0B2) // 😄 Feliz
val Mood17 = Color(0xFFF3E8FF) // 🥳 Emocionado
val Mood18 = Color(0xFFE0E7FF) // ✨ Inspirado
val Mood19 = Color(0xFFFEF08A) // 🤩 Radiante
val Mood20 = Color(0xFFF472B6) // 💖 Agradecido

fun getMoodColor(level: Int): Color {
    return when (level) {
        1 -> Mood1
        2 -> Mood2
        3 -> Mood3
        4 -> Mood4
        5 -> Mood5
        6 -> Mood6
        7 -> Mood7
        8 -> Mood8
        9 -> Mood9
        10 -> Mood10
        11 -> Mood11
        12 -> Mood12
        13 -> Mood13
        14 -> Mood14
        15 -> Mood15
        16 -> Mood16
        17 -> Mood17
        18 -> Mood18
        19 -> Mood19
        20 -> Mood20
        else -> Mood11
    }
}

fun getMoodEmoji(level: Int): String {
    return when (level) {
        1 -> "😭"
        2 -> "😢"
        3 -> "🌧️"
        4 -> "😣"
        5 -> "😡"
        6 -> "😤"
        7 -> "😰"
        8 -> "🥱"
        9 -> "⚡"
        10 -> "💭"
        11 -> "😐"
        12 -> "☕"
        13 -> "🧘"
        14 -> "🌱"
        15 -> "🙂"
        16 -> "😄"
        17 -> "🥳"
        18 -> "✨"
        19 -> "🤩"
        20 -> "💖"
        else -> "😐"
    }
}

fun getMoodWord(level: Int): String {
    return when (level) {
        1 -> "Muy Triste"
        2 -> "Triste"
        3 -> "Melancólico"
        4 -> "Estresado"
        5 -> "Enojado"
        6 -> "Frustrado"
        7 -> "Ansioso"
        8 -> "Cansado"
        9 -> "Agitado"
        10 -> "Reflexivo"
        11 -> "Neutral"
        12 -> "Calmado"
        13 -> "En Paz"
        14 -> "Renovado"
        15 -> "Satisfecho"
        16 -> "Feliz"
        17 -> "Emocionado"
        18 -> "Inspirado"
        19 -> "Radiante"
        20 -> "Agradecido"
        else -> "Neutral"
    }
}
