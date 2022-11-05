package com.tsvetilian.eventhub.extentions

import com.tsvetilian.eventhub.SimpleEventHub

/**
 * Allows you to emit and event by calling emit() from any file.
 */
fun Any?.emit(eventName: String, data: Any? = null) {
	SimpleEventHub.emit(eventName, data)
}