package com.tsvetilian.eventhub.core.listeners.impl

import android.util.Log
import com.tsvetilian.eventhub.core.listeners.SimpleEventHubLogger

@PublishedApi
internal class DefaultSimpleEventHubLogger : SimpleEventHubLogger {

	private val defaultLogTag = "SimpleEventHub"

	override fun log(description: String?) {
		Log.d(defaultLogTag, "$description")
	}

	override fun logError(description: String?, exception: Throwable?) {
		Log.d(
			defaultLogTag,
			"$description. Exception: ${exception?.message ?: "no exception thrown"}")
	}
}