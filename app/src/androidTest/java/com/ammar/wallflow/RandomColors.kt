package com.ammar.wallflow

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

object RandomColors {
    private fun nextColorInt(
        random: Random = Random,
    ) = (random.nextDouble() * 16777215).toInt() or (0xFF shl 24)

    fun nextColor(random: Random = Random) = Color(nextColorInt(random))
}

// internal class RandomColors {
//     private val recycle: Stack<Color> = Stack()
//     private val colors: Stack<Color> = Stack()
//
//     val color: Color
//         get() {
//             if (colors.size == 0) {
//                 while (!recycle.isEmpty()) colors.push(recycle.pop())
//                 Collections.shuffle(colors)
//             }
//             val c = colors.pop()
//             recycle.push(c)
//             return c
//         }
//
//     init {
//         recycle.addAll(categoryColorChoices)
//     }
//
//     companion object {
//         @Volatile
//         private var INSTANCE: RandomColors? = null
//
//         private fun getInstance(): RandomColors = INSTANCE ?: synchronized(this) {
//             RandomColors().also { INSTANCE = it }
//         }
//
//         fun nextColor(): Color = getInstance().color
//     }
// }
