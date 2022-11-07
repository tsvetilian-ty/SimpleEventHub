# SimpleEventHub
Lifecycle-aware pub/sub event system for Android.

![Release](https://github.com/tsvetilian-ty/SimpleEventHub/actions/workflows/release.yaml/badge.svg)
![Android Min SDK](https://img.shields.io/badge/Android%20SDK-21%2B-green)
[![GitHub license](https://img.shields.io/github/license/tsvetilian-ty/SimpleEventHub)](https://github.com/tsvetilian-ty/SimpleEventHub/blob/main/LICENSE.md)
![Maven Central](https://img.shields.io/maven-central/v/com.tsvetilian/simple-eventhub)

# Content
- [Add in your application](#using-in-your-application)
- [How to use](#how-to-use)
    - [Emit an event](#emit-an-event)
    - [Subscribers](#subscribers)
      - [Lifecycle-aware subscriber](#lifecycle-aware-subscriber)
      - [Disposable subscriber](#disposable-subscriber)

## Add in your application
If you are building with Gradle, add the following line to the `dependencies` section of your `build.gradle` file:
```groovy
implementation 'com.tsvetilian:simple-eventhub:(latest version)'
```

## How to use

### Emit an event
*__Params:__*
  - `eventName` - name of the event
  - `data` - the data that is send to the subscribers
```kotlin
SimpleEventHub.emit(eventName = "test-event", data = "message")
```

### Subscribers

### Lifecycle-aware subscriber
> The subscriber will be automatically disposed when the lifecycle owner's state is Lifecycle.Event.ON_STOP

*__Params:__*
  - `eventName` - name of the event that will be used to determine when to call the subscriber's receiver
  - `receiveCachedData` - receive the cached data from the last event emission if such is available Default: `false`
  - `observeOn` - the thread that will be used to receive the data Default: `main thread`
  - `bindToLifecycle` - lifecycle owner that the listener is bind to
  - `receiver` - callback used when a new event that matches the eventName
```kotlin
SimpleEventHub.on<String>(eventName = "test-event", bindToLifecycle = viewLifecycleOwner) {
  Log.d("LOG_FROM_RECEIVER", "$it")
}
```

### Disposable subscriber
> The method will return `DisposableSubscriber` that can be used for disposing of the subscriber at any time.
*__Params:__*
  - `eventName` - name of the event that will be used to determine when to call the subscriber's receiver
  - `receiveCachedData` - receive the cached data from the last event emission if such is available Default: `false`
  - `observeOn` - the thread that will be used to receive the data by the receiver Default: `main thread`
  - `receiver` - callback used when a new event that matches the eventName

```kotlin
val eventSubscriber = SimpleEventHub.on<String>(eventName = "test-event") {
  Log.d("LOG_FROM_RECEIVER", "$it")
}

eventSubscriber.dispose()
```
