package com.tsvetilian.eventhub.core.listeners

@PublishedApi
internal interface SimpleEventHubLogger {
	fun log(description: String?)
	fun logError(description: String?, exception: Throwable? = null)
}