package com.ammar.wallflow.data.repository

import androidx.compose.ui.graphics.Color

object RandomColors {
    fun nextColorInt() = (Math.random() * 16777215).toInt() or (0xFF shl 24)
    fun nextColor() = Color(nextColorInt())
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
