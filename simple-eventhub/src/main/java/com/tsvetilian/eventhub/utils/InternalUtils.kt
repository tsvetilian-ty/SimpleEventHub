package com.tsvetilian.eventhub.utils

import kotlin.reflect.KClass

internal object InternalUtils {

	fun <T : Any> dynamicCast(any: Any, clazz: KClass<out T>): T? = clazz.javaObjectType.cast(any)

}