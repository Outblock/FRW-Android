package com.flowfoundation.wallet.cache

import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.flowfoundation.wallet.utils.*
import java.io.File

class CacheManager<T>(
    private val fileName: String,
    private val type: Class<T>,
    private val isInfoCache: Boolean = true,
) {

    private val file by lazy { File(if (isInfoCache) CACHE_PATH else DATA_PATH, fileName) }

    @WorkerThread
    fun read(): T? {
        val str = file.read()
        if (str.isBlank()) {
            return null
        }

        try {
            return Gson().fromJson(str, type)
        } catch (e: Exception) {
            loge(TAG, e)
        }
        return null
    }

    fun cache(data: T) {
        ioScope { cacheSync(data) }
    }

    fun cacheSync(data: T) {
        val str = Gson().toJson(data)
        str.saveToFile(file)
    }

    fun clear() {
        ioScope { file.delete() }
    }

    fun isCacheExist(): Boolean = file.exists() && file.length() > 0

    fun modifyTime() = file.lastModified()

    fun isExpired(duration: Long): Boolean {
        return System.currentTimeMillis() - modifyTime() > duration
    }

    companion object {
        private val TAG = CacheManager::class.java.simpleName
    }
}