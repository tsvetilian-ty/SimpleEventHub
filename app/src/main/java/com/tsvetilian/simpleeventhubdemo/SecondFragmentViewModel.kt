package com.tsvetilian.simpleeventhubdemo

import androidx.lifecycle.ViewModel
import com.tsvetilian.eventhub.SimpleEventHub

class SecondFragmentViewModel : ViewModel() {

	fun updateEvent(event: String) = SimpleEventHub.emit(
		eventName = "update-both-fragments",
		data = DataWrapper(
			justTheData = "Emitted value: $event"
		)
	)

}