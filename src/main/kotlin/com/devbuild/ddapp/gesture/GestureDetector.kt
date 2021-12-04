package com.devbuild.ddapp

import com.devbuild.ddapp.eventsource.Event
import com.devbuild.ddapp.eventsource.EventListener
import com.devbuild.ddapp.eventsource.EventSource

class GestureDetector(eventSource: EventSource, val handler: (Event) -> Unit) : EventListener {
    init {
        eventSource.addEventListener(this)
    }

    override suspend fun handleEvent(e: Event) = handler.invoke(e)
}