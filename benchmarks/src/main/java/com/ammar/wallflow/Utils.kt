package com.ammar.wallflow

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiObject2Condition
import androidx.test.uiautomator.Until

/**
 * Waits until an object with [selector] if visible on screen and returns the object.
 * If the element is not available in [timeout], throws [AssertionError]
 */
fun UiDevice.waitAndFindObject(selector: BySelector, timeout: Long): UiObject2 {
    if (!wait(Until.hasObject(selector), timeout)) {
        throw AssertionError("Element not found on screen in ${timeout}ms (selector=$selector)")
    }

    return findObject(selector)
}

fun UiDevice.flingElementDownUp(element: UiObject2) {
    // Set some margin from the sides to prevent triggering system navigation
    element.setGestureMargin(displayWidth / 5)
    element.fling(Direction.DOWN)
    waitForIdle()
    element.fling(Direction.UP)
}

/**
 * Condition will be satisfied if given element has specified count of children
 */
fun untilHasChildren(
    childCount: Int = 1,
    op: HasChildrenOp = HasChildrenOp.AT_LEAST,
) = object : UiObject2Condition<Boolean>() {
    override fun apply(element: UiObject2) = when (op) {
        HasChildrenOp.AT_LEAST -> element.childCount >= childCount
        HasChildrenOp.EXACTLY -> element.childCount == childCount
        HasChildrenOp.AT_MOST -> element.childCount <= childCount
    }
}

fun untilHasChildrenWithRes(
    res: String,
    childCount: Int = 1,
    op: HasChildrenOp = HasChildrenOp.AT_LEAST,
) = object : UiObject2Condition<Boolean>() {
    override fun apply(element: UiObject2): Boolean {
        val filteredCount = element.children
            .filter { it.resourceName == res }
            .size
        return when (op) {
            HasChildrenOp.AT_LEAST -> filteredCount >= childCount
            HasChildrenOp.EXACTLY -> filteredCount == childCount
            HasChildrenOp.AT_MOST -> filteredCount <= childCount
        }
    }
}

enum class HasChildrenOp {
    AT_LEAST,
    EXACTLY,
    AT_MOST,
}
