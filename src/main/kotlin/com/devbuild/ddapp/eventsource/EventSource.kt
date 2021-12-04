package com.devbuild.ddapp.eventsource

import java.util.concurrent.ConcurrentLinkedQueue

enum class EventType { N_CLICK, LONG_PRESS }
data class Event(val type: EventType, val value: Long)

abstract class EventSource {
    val listeners = ConcurrentLinkedQueue<EventListener>()
    fun addEventListener(listener: EventListener) = listeners.add(listener)
    suspend fun notifyListeners(e: Event) = listeners.forEach { it.handleEvent(e) }
}

interface EventListener {
    suspend fun handleEvent(e: Event)
}
