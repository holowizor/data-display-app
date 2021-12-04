package com.devbuild.ddapp.eventsource

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lc.kra.system.keyboard.GlobalKeyboardHook
import lc.kra.system.keyboard.event.GlobalKeyEvent
import lc.kra.system.keyboard.event.GlobalKeyListener
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class KeyboardEventSource(
    val key: Int = GlobalKeyEvent.VK_SPACE,
    val keyboardHook: GlobalKeyboardHook = GlobalKeyboardHook(true)
) :
    EventSource(), GlobalKeyListener {
    val log = LoggerFactory.getLogger("KeyboardEventSource")
    val pressedKeys = ConcurrentHashMap<Int, Long>()
    val keyCounts = ConcurrentHashMap<Int, AtomicLong>()

    init {
        keyboardHook.addKeyListener(this)
    }

    // if key is held, then multiple pressed events are sent
    override fun keyPressed(event: GlobalKeyEvent) {
        log.info("key down ${event.virtualKeyCode}")
        pressedKeys.putIfAbsent(event.virtualKeyCode, System.currentTimeMillis())
    }

    override fun keyReleased(event: GlobalKeyEvent) {
        log.info("key up ${event.virtualKeyCode}")
        val t0 = pressedKeys.remove(event.virtualKeyCode) ?: 0L
        val t1 = System.currentTimeMillis()
        if (t1 - t0 < 500) {
            if (keyCounts.putIfAbsent(event.virtualKeyCode, AtomicLong(1L)) != null) {
                keyCounts[event.virtualKeyCode]!!.addAndGet(1L);
            }
            GlobalScope.launch {
                delay(500L)
                keyCounts[event.virtualKeyCode]?.let {
                    sendGestureEvent(Event(EventType.N_CLICK, it.get()))
                }
            }
        } else {
            sendGestureEvent(
                Event(EventType.LONG_PRESS, ((t1 - t0) / 1000L))
            )
        }
    }

    private fun sendGestureEvent(e: Event) =
        GlobalScope.launch {
            notifyListeners(e)
            reset()
        }

    private fun reset() = keyCounts.clear()
}
