package com.devbuild.ddapp

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GenericData {
    private val data = HashMap<String, Any>()

    fun <T> get(key: String): T = data[key] as T
    fun put(key: String, value: Any): Any? = data.put(key, value)
}

interface DataProvider {
    fun provideData(): GenericData
    fun config(config: Map<String, Any>) {}
}

interface DataRenderer<T> {
    fun renderData(dataProvider: DataProvider): List<T>
}

interface SingleRenderer<T> {
    fun render(): T
}

interface DataConsumer<T> {
    fun consume(data: T)
    fun init()
    fun sleep()
    fun clear()
}

abstract class Display<T>(
    val dataVault: DataVault<T>,
    val consumer: DataConsumer<T>,
    val screenSaver: SingleRenderer<T>,
    val loadingScreen: SingleRenderer<T>,
    val shutdownScreen: SingleRenderer<T>
) {
    private var job: Job? = null
    private var idleJob: Job? = null

    open fun init() {
        consumer.consume(loadingScreen.render())
        startIdle()
    }

    fun start() {
        idleJob?.let { it.cancel() }

        if (job?.isActive == true) return

        job = GlobalScope.launch {
            val images = dataVault.provideData()
            repeat(images.size) {
                consumer.consume(images[it])
                delay(5000L)
            }
            startIdle()
        }
    }

    fun startIdle() {
        if (idleJob?.isActive == true) return

        idleJob = GlobalScope.launch {
            delay(5000L)
            consumer.consume(screenSaver.render())
            delay(25000L)
        }
    }

    fun stop() {
        job?.let { it.cancel() }
        clean()
    }

    fun sleep() {
        consumer.sleep()
    }

    private fun clean() {
        consumer.clear()
    }
}
