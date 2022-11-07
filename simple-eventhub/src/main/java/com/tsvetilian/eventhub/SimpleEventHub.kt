package com.tsvetilian.eventhub

import androidx.annotation.Keep
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.tsvetilian.eventhub.core.ObserveEmit
import com.tsvetilian.eventhub.core.listeners.DisposableSubscriber
import com.tsvetilian.eventhub.core.listeners.SimpleEventHubLogger
import com.tsvetilian.eventhub.core.listeners.impl.DefaultSimpleEventHubLogger
import com.tsvetilian.eventhub.core.models.Subscriber
import com.tsvetilian.eventhub.utils.InternalUtils
import kotlinx.coroutines.*

/**
 * SimpleEventHub is a lifecycle-aware pub/sub event system for Android.
 */
@Keep
object SimpleEventHub {

	@PublishedApi
	internal val subscribers = mutableMapOf<String, MutableSet<Subscriber>>()

	@PublishedApi
	internal val eventLatestData = mutableMapOf<String, Any?>()

	private val executionJob = Job()

	@OptIn(DelicateCoroutinesApi::class)
	internal val backgroundPool: ExecutorCoroutineDispatcher =
		newFixedThreadPoolContext(nThreads = 4, "SimpleEventHub")

	@PublishedApi
	internal var logger: SimpleEventHubLogger? = DefaultSimpleEventHubLogger()

	private suspend fun emitToSubscriber(eventName: String, subscriber: Subscriber, data: Any?) {
		try {
			val transformedData = data?.let { InternalUtils.dynamicCast(data, subscriber.clazz) }

			when (subscriber.observeEmitOn) {
				ObserveEmit.ON_MAIN -> {
					withContext(Dispatchers.Main) {
						this@SimpleEventHub.logger?.log("Subscriber received event '$eventName' on main thread")
						this@SimpleEventHub.eventLatestData[eventName] = data
						subscriber.actionCallback(transformedData)
					}
				}
				ObserveEmit.ON_BACKGROUND -> {
					this@SimpleEventHub.logger?.log("Subscriber received event '$eventName' on background thread")
					this@SimpleEventHub.eventLatestData[eventName] = data
					subscriber.actionCallback(transformedData)
				}
			}

		} catch (ex: ClassCastException) {
			this.logger?.logError(
				description = "The subscriber expects ${subscriber.clazz.simpleName} data type, but the emitted data is ${data?.let { it::class.simpleName } ?: "NULL"}",
				exception = ex
			)
		} catch (ex: Throwable) {
			this.logger?.logError(description = "Unknown exception", exception = ex)
		}
	}

	/**
	 * Emits an event and let's you pass data to available subscribers.
	 *
	 * @param eventName - name of the event
	 * @param data - the data that is send to the subscribers
	 */
	fun emit(eventName: String, data: Any? = null) {
		if (eventName !in subscribers) {
			this.logger?.log("There is no available subscriber for $eventName event!")
			return
		}

		CoroutineScope(Dispatchers.IO + executionJob).launch(backgroundPool) {
			val activeSubscribers = subscribers[eventName]
			this@SimpleEventHub.logger?.log(
				description = "Schedule event '$eventName' emission with '${data?.toString() ?: "no data"}?' to ${activeSubscribers?.size} subscribers"
			)

			activeSubscribers?.let {
				for (subscriber in it) {
					emitToSubscriber(eventName = eventName, subscriber = subscriber, data = data)
				}
			}

		}
	}

	/**
	 * Subscribe for an event based on it's name.
	 * The method will return DisposableEvent that can be used for disposing of the subscriber at any time.
	 *
	 * @param eventName - name of the event that will be used to determine when to call the subscriber's receiver
	 * @param receiveCachedData - receive the cached data from the last event emission if such is available Default: false
	 * @param observeOn - the thread that will be used to receive the data Default: main thread
	 * @param receiver - callback used when a new event that matches the eventName
	 *
	 * @see ObserverEmit
	 * @return DisposableEvent
	 */
	@Suppress("UNCHECKED_CAST")
	inline fun <reified T : Any> on(
		eventName: String,
		receiveCachedData: Boolean = false,
		observeOn: ObserveEmit = ObserveEmit.ON_MAIN,
		noinline receiver: (T?) -> Unit
	): DisposableSubscriber {
		val subscriberContainer =
			Subscriber(
				eventName = eventName,
				observeEmitOn = observeOn,
				clazz = T::class,
				actionCallback = receiver as (Any?) -> Unit
			)

		if (eventName !in subscribers) {
			val initialSubsSet = mutableSetOf(subscriberContainer)
			subscribers[eventName] = initialSubsSet
		} else {
			val subscribersSet = subscribers[eventName]
			subscribersSet?.add(subscriberContainer)
		}

		if (receiveCachedData && eventName in eventLatestData) {
			subscriberContainer.actionCallback.invoke(this@SimpleEventHub.eventLatestData[eventName])
			this.logger?.log("Received latest data for '$eventName'")
		}

		return object : DisposableSubscriber {
			override fun dispose() {
				val subscribersList = subscribers[eventName]
				subscribersList?.remove(subscriberContainer)
			}

		}
	}

	/**
	 * Subscribe for an event based on event name.
	 * The subscriber will be automatically disposed when the lifecycle owner's state is Lifecycle.Event.ON_STOP
	 *
	 * @param eventName - name of the event that will be used to determine when to call the subscriber's receiver
	 * @param receiveCachedData - receive the cached data from the last event emission if such is available Default: false
	 * @param observeOn - the thread that will be used to receive the data Default: main thread
	 * @param bindToLifecycle - lifecycle owner that
	 * @param receiver - callback used when a new event that matches the eventName
	 *
	 * @see ObserverEmit
	 * @see Lifecycle.Event.ON_STOP
	 */
	@Suppress("UNCHECKED_CAST")
	inline fun <reified T : Any> on(
		eventName: String,
		receiveCachedData: Boolean = false,
		observeOn: ObserveEmit = ObserveEmit.ON_MAIN,
		bindToLifecycle: LifecycleOwner? = null,
		noinline receiver: (T?) -> Unit
	) {
		val subscriberContainer =
			Subscriber(
				eventName = eventName,
				observeEmitOn = observeOn,
				clazz = T::class,
				actionCallback = receiver as (Any?) -> Unit
			)

		bindToLifecycle?.lifecycle?.addObserver(object : LifecycleEventObserver {
			override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
				val subscribersList = subscribers[eventName]
				if (event == Lifecycle.Event.ON_STOP) {
					bindToLifecycle.lifecycle.removeObserver(this)
					subscribersList?.remove(subscriberContainer)
					this@SimpleEventHub.logger?.log("Automatically disposing listener for $eventName")
				}
			}
		})

		if (eventName !in subscribers) {
			val initialSubsSet = mutableSetOf(subscriberContainer)
			subscribers[eventName] = initialSubsSet
		} else {
			val subscribersSet = subscribers[eventName]
			subscribersSet?.add(subscriberContainer)
		}

		if (receiveCachedData && eventName in eventLatestData) {
			subscriberContainer.actionCallback.invoke(this.eventLatestData[eventName])
			this.logger?.log("Received latest data for '$eventName'")
		}

		this.logger?.log("New event subscriber for $eventName is set")
	}

	/**
	 * Cleans all data used by SimpleEventHub
	 */
	fun disposeAll() {
		subscribers.clear()
		eventLatestData.clear()
		executionJob.cancel()
	}

}