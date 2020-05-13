package com.devbuild.ddapp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.util.*
import kotlin.reflect.full.createInstance

fun yamlObjectMapper(): ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

data class CnfMain(val providers: List<CnfProvider>)
data class CnfProvider(
    val alias: String,
    val refreshRate: Int,
    val dataProvider: CnfProviderConfig,
    val imageProvider: CnfProviderConfig
)

data class CnfProviderConfig(val className: String, val config: Map<String, Any>?)

object Configuration {
    fun readDefaultConfig() =
        readConfig(Configuration.javaClass.getResourceAsStream("/default.yaml").bufferedReader().use { it.readText() })

    fun readConfig(yaml: String): CnfMain {
        val mapper = yamlObjectMapper()
        return mapper.readValue(yaml, CnfMain::class.java)
    }
}

object DataVault {
    private val imageMap = TreeMap<String, List<BufferedImage>>()

    fun init() {
        val cnf = Configuration.readDefaultConfig()
        cnf.providers.forEach {
            val dataProvider = Class.forName(it.dataProvider.className).kotlin.createInstance() as DataProvider
            val imageProvider = Class.forName(it.imageProvider.className).kotlin.createInstance() as ImageProvider

            it.dataProvider.config?.let { config -> dataProvider.config(config) }

            GlobalScope.launch {
                imageMap[it.alias] = imageProvider.provideImage(dataProvider)
                delay(it.refreshRate.toLong())
            }
        }
    }

    fun provideImages(): List<BufferedImage> = imageMap.values.flatten()
}