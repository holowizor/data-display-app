package com.devbuild.ddapp

import java.awt.image.BufferedImage

class GenericData {
    private val data = HashMap<String, Any>()

    fun <T> get(key: String): T = data[key] as T
    fun put(key: String, value: Any): Any? = data.put(key, value)
}

interface DataProvider {
    fun provideData(): GenericData
}

interface ImageProvider {
    fun provideImage(dataProvider: DataProvider): List<BufferedImage>
}
