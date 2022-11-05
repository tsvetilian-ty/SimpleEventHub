package com.tsvetilian.eventhub.core.models

import com.tsvetilian.eventhub.core.ObserveEmit
import kotlin.reflect.KClass

@PublishedApi
internal data class Subscriber(
	val eventName: String,
	val observeEmitOn: ObserveEmit,
	val clazz: KClass<*>,
	val actionCallback: (Any?) -> Unit
)