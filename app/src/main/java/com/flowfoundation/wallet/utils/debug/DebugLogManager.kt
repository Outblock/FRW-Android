package com.flowfoundation.wallet.utils.debug


object DebugLogManager {
    private const val MAX_TWEAKS_SIZE = 50

    val tweaks: MutableList<DebugTweak<Any>> = object : ArrayList<DebugTweak<Any>>() {
        override fun add(element: DebugTweak<Any>): Boolean {
            if (size >= MAX_TWEAKS_SIZE) {
                removeAt(0)
            }
            return super.add(element)
        }
    }

    fun addDebugTweak(tag: String?, msg: Any?) {
        val debugTweak = DebugTweak(
            category = "Log",
            name = tag ?: "",
            defaultValue = msg
        )
        tweaks.add(debugTweak as DebugTweak<Any>)
    }
}