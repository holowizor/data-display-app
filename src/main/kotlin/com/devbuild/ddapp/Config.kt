package com.devbuild.ddapp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.reflect.full.createInstance

fun yamlObjectMapper(): ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

data class CnfMain(val providers: List<CnfProvider>)
data class CnfProvider(
    val alias: String,
    val refreshRate: Int,
    val dataProvider: CnfProviderConfig,
    val dataRenderer: CnfProviderConfig
)

data class CnfProviderConfig(val className: String, val config: Map<String, Any>?)

object Configuration {
    fun readDefaultConfig(configResource: String = "/default.yaml") =
        readConfig(Configuration.javaClass.getResourceAsStream(configResource).bufferedReader().use { it.readText() })

    fun readConfig(yaml: String): CnfMain {
        val mapper = yamlObjectMapper()
        return mapper.readValue(yaml, CnfMain::class.java)
    }
}

class DataVault<T> {
    private val dataMap = TreeMap<String, List<T>>()

    fun init(configResource: String = "/default.yaml") {
        val cnf = Configuration.readDefaultConfig(configResource)
        cnf.providers.forEach {
            val dataProvider = Class.forName(it.dataProvider.className).kotlin.createInstance() as DataProvider
            val dataRenderer = Class.forName(it.dataRenderer.className).kotlin.createInstance() as DataRenderer<T>

            it.dataProvider.config?.let { config -> dataProvider.config(config) }

            GlobalScope.launch {
                dataMap[it.alias] = dataRenderer.renderData(dataProvider)
                delay(it.refreshRate.toLong())
            }
        }
    }

    fun provideData(): List<T> = dataMap.values.flatten()
}